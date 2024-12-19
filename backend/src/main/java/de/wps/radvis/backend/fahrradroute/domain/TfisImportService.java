/*
 * Copyright (c) 2023 WPS - Workplace Solutions GmbH
 *
 * Licensed under the EUPL, Version 1.2 or as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.wps.radvis.backend.fahrradroute.domain;

import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.springframework.data.util.Lazy;

import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.VarianteKategorie;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TfisImportService {

	public static final List<String> RELEVANTE_ATTRIBUTE = List.of("objid", "inf", "nam", "wgb", "weg_shp_id", "olg",
		"kat", "bez", "art");

	public static final List<String> RELEVANTE_VARIANTE_ATTRIBUTE = List.of("objid", "weg_shp_id", "art");

	private PreparedGeometry bereichBW;

	private final VerwaltungseinheitService verwaltungseinheitService;
	private final ShapeFileRepository shapeFileRepository;
	private final KantenRepository kantenRepository;
	private final Lazy<DlmMatchingRepository> dlmMatchingRepositorySupplier;

	public TfisImportService(VerwaltungseinheitService verwaltungseinheitService,
		ShapeFileRepository shapeFileRepository, KantenRepository kantenRepository,
		Lazy<DlmMatchingRepository> dlmMatchingRepositorySupplier) {
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.shapeFileRepository = shapeFileRepository;
		this.kantenRepository = kantenRepository;
		this.dlmMatchingRepositorySupplier = dlmMatchingRepositorySupplier;
	}

	public static boolean geometrienZuStarkAbweichend(LineString kantenGeometry, LineString simpleFeatureGeometry) {
		double distanceBetweenStartKnoten = kantenGeometry.getStartPoint()
			.distance(simpleFeatureGeometry.getStartPoint());
		double distanceBetweenEndKnoten = kantenGeometry.getEndPoint()
			.distance(simpleFeatureGeometry.getEndPoint());
		double lengthDiff = Math.abs(kantenGeometry.getLength() - simpleFeatureGeometry.getLength());
		return lengthDiff > KnotenIndex.SNAPPING_DISTANCE ||
			Math.max(distanceBetweenStartKnoten, distanceBetweenEndKnoten) > KnotenIndex.SNAPPING_DISTANCE;
	}

	public ProfilMatchResult routeProfil(LineString netzbezugLineString) throws KeinMatchGefundenException {
		return dlmMatchingRepositorySupplier.get().matchGeometryUndDetails(netzbezugLineString, "foot");
	}

	public List<Kante> findMatchingKanten(List<SimpleFeature> mussGematchedWerden,
		AbbildungAufKantenStatistik importStatistik) {
		Stream<Kante> gematcheKanten = mussGematchedWerden.stream()
			.map(simpleFeature -> {
				LineString lineString = extractLineString(simpleFeature);
				try {
					OsmMatchResult result = dlmMatchingRepositorySupplier.get().matchGeometry(lineString, "bike");
					List<Kante> matches = result.getOsmWayIds().stream().map(OsmWayId::getValue)
						.map(kantenRepository::findById)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.filter(kante -> LineStrings.calculateUeberschneidungslinestring(
							kante.getGeometry(), result.getGeometrie()).isPresent())
						.collect(Collectors.toList());
					if (matches.isEmpty()) {
						importStatistik.anzahlFeaturesOhneMatch++;
					}
					return matches;
				} catch (KeinMatchGefundenException e) {
					importStatistik.anzahlFeaturesOhneMatch++;
					return new ArrayList<Kante>();
				}
			})
			.flatMap(List::stream);
		return gematcheKanten.collect(Collectors.toList());
	}

	public void validate(Path tfisRadwegePath, List<String> requiredAttribute) {
		File tfisRadwegeShapeFile = tfisRadwegePath.toFile();
		if (!tfisRadwegeShapeFile.exists()) {
			throw new RuntimeException(
				"TFIS-Radwege Datei existiert nicht in: " + tfisRadwegeShapeFile.getAbsolutePath());
		}

		try (Stream<SimpleFeature> simpleFeatureStream = shapeFileRepository.readShape(tfisRadwegeShapeFile)) {

			log.info("Validiere SHP...");
			Optional<SimpleFeature> first = simpleFeatureStream.findFirst();

			if (first.isEmpty()) {
				throw new RuntimeException("No Features present!");
			}

			Geometry geometry = (Geometry) first.get().getDefaultGeometry();

			if (geometry.getSRID() != KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()) {
				throw new RuntimeException(
					"Wrong SRID. Expected: '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
						+ "' Actual: '" + geometry.getSRID() + "'");
			}

			if (!geometry.getGeometryType().equals(Geometry.TYPENAME_MULTILINESTRING)) {
				throw new RuntimeException(
					"Wrong Geometry Type. Expected: '" + Geometry.TYPENAME_MULTILINESTRING
						+ "' Actual: '" + geometry.getGeometryType() + "'");
			}

			requiredAttribute.forEach(requiredAttribut -> {
				boolean isAttributPresent = first.get().getProperties().stream().anyMatch(
					property -> property.getName().toString().equals(requiredAttribut));
				if (!isAttributPresent) {
					log.warn("Attribut '" + requiredAttribut + "' nicht vorhanden!");
					throw new RuntimeException(
						"Nicht alle erforderlichen Attribute sind gesetzt!");
				}
			});
		} catch (IOException | ShapeProjectionException e1) {
			throw new RuntimeException(e1);
		}
	}

	public MultiLineString konstruiereOriginalGeometrie(List<SimpleFeature> simpleFeatures) {
		LineString[] lineStrings = simpleFeatures.stream()
			.map(this::extractLineString)
			.toArray(LineString[]::new);
		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createMultiLineString(lineStrings);
	}

	public double anteilInBW(MultiLineString originalGeometrie) {
		Geometry streckenInBW = originalGeometrie.intersection(getBereichBW().getGeometry());
		return streckenInBW.getLength() / originalGeometrie.getLength();
	}

	public boolean sindAttributeProGruppeEindeutig(Map<String, List<SimpleFeature>> groupedByObjid) {
		boolean nameUndStartpunktProGruppeEindeutig = sindNameUndStartpunktEindeutig(groupedByObjid);

		boolean beschreibungProGroupEindeutig = isAttributProObjidEindeutig(groupedByObjid, this::extractBeschreibung);
		if (!beschreibungProGroupEindeutig) {
			log.info("Beschreibung der Gruppe nicht eindeutig!");
		}
		boolean kurzbeschreibungProGroupEindeutig = isAttributProObjidEindeutig(groupedByObjid,
			this::extractKurzbeschreibung);
		if (!kurzbeschreibungProGroupEindeutig) {
			log.info("Kurzbeschreibung der Gruppe nicht eindeutig!");
		}
		boolean infoProGroupEindeutig = isAttributProObjidEindeutig(groupedByObjid, this::extractInfo);
		if (!infoProGroupEindeutig) {
			log.info("Info der Gruppe nicht eindeutig!");
		}
		boolean offizielleLaengeProGroupEindeutig = isAttributProObjidEindeutig(groupedByObjid,
			this::extractOffizielleLaenge);
		if (!offizielleLaengeProGroupEindeutig) {
			log.info(
				"OffizielleLaenge der Gruppe nicht eindeutig!");
		}
		boolean kategorieProGroupEindeutig = isAttributProObjidEindeutig(groupedByObjid, this::extractKategorie);
		if (!kategorieProGroupEindeutig) {
			log.info("Kategorie der Gruppe nicht eindeutig!");
		}
		return nameUndStartpunktProGruppeEindeutig && beschreibungProGroupEindeutig && kurzbeschreibungProGroupEindeutig
			&& infoProGroupEindeutig && offizielleLaengeProGroupEindeutig && kategorieProGroupEindeutig;
	}

	public boolean sindNameUndStartpunktEindeutig(Map<String, List<SimpleFeature>> groupedByObjid) {
		boolean nameProGroupEindeutig = isAttributProObjidEindeutig(groupedByObjid, this::extractName);
		if (!nameProGroupEindeutig) {
			log.info("Namen der Gruppe nicht eindeutig!");
		}
		boolean startpunktProObjidEindeutig = isAttributProObjidEindeutig(groupedByObjid, this::extractStartpunkt);
		if (!startpunktProObjidEindeutig) {
			log.info("Startpunkt der Gruppe nicht eindeutig!");
		}
		return nameProGroupEindeutig && startpunktProObjidEindeutig;
	}

	public boolean isGeometryInBW(SimpleFeature simpleFeature) {
		return getBereichBW().intersects(this.extractLineString(simpleFeature));
	}

	public boolean isNotStichwegOrAlternativStrecke(SimpleFeature simpleFeature) {
		String art = simpleFeature.getAttribute("art").toString();
		return art.equals("1000");
	}

	public boolean isStichwegOrAlternativStrecke(SimpleFeature simpleFeature) {
		String art = simpleFeature.getAttribute("art").toString();
		return art.equals("1002") || art.equals("1001");
	}

	public boolean isLandesradfernweg(SimpleFeature simpleFeature) {
		String inf = simpleFeature.getAttribute("inf").toString();
		return inf.toLowerCase().endsWith("landesradfernweg");
	}

	public LineString extractLineString(SimpleFeature simpleFeature) {
		return (LineString) ((MultiLineString) simpleFeature.getDefaultGeometry()).getGeometryN(0);
	}

	public String extractName(SimpleFeature simpleFeature) {
		return simpleFeature.getAttribute("nam").toString();
	}

	public VarianteKategorie extractVarianteKategorie(SimpleFeature simpleFeature) {
		String art = simpleFeature.getAttribute("art").toString();
		require(art.equals("1001") || art.equals("1002"));
		if (art.equals("1001")) {
			return VarianteKategorie.ALTERNATIVSTRECKE;
		} else {
			return VarianteKategorie.ZUBRINGERSTRECKE;
		}
	}

	public Optional<Point> extractStartpunkt(SimpleFeature simpleFeature) {
		String inf = simpleFeature.getAttribute("inf").toString();

		String[] parts = inf.split(";");

		if (parts.length == 0 || parts[0].isEmpty()) {
			log.trace("Couldn't parse Startpunkt from '" + inf + "'");
			return Optional.empty();
		}
		String[] ordinates = parts[0].split(" ");

		if (ordinates.length != 2) {
			log.trace("Couldn't parse Startpunkt from '" + inf + "'");
			return Optional.empty();
		}

		double x;
		double y;

		try {
			x = Double.parseDouble(ordinates[0]);
			y = Double.parseDouble(ordinates[1]);
		} catch (NumberFormatException e) {
			log.trace("Couldn't parse Startpunkt from '" + inf + "'");
			return Optional.empty();
		}

		return Optional.of(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(x, y)));
	}

	public String extractDlmId(SimpleFeature simpleFeature) {
		String weg_shp_id = simpleFeature.getAttribute("weg_shp_id").toString();

		return weg_shp_id.split("#")[0];
	}

	public Set<String> extractDlmIds(List<SimpleFeature> simpleFeatures) {
		return simpleFeatures.stream().map(this::extractDlmId).collect(Collectors.toSet());
	}

	private boolean isAttributProObjidEindeutig(Map<String, List<SimpleFeature>> groupedByObjid,
		Function<SimpleFeature, Object> attributeExtractor) {
		return groupedByObjid.values().stream().allMatch(list -> this.isAttributeEindeutig(list, attributeExtractor));
	}

	private boolean isAttributeEindeutig(List<SimpleFeature> list, Function<SimpleFeature, Object> attributeExtractor) {
		if (list.isEmpty()) {
			return true;
		}

		Object firstValue = attributeExtractor.apply(list.get(0));

		Predicate<Object> compareToFirstValue = firstValue == null ? Objects::isNull : firstValue::equals;
		return list.stream().map(attributeExtractor).allMatch(compareToFirstValue);
	}

	public static Collector<SimpleFeature, ?, Map<String, List<SimpleFeature>>> groupingByObjid() {
		return Collectors.groupingBy(TfisImportService::extractObjid);
	}

	private PreparedGeometry getBereichBW() {
		if (this.bereichBW == null) {
			this.bereichBW = verwaltungseinheitService.getBundeslandBereichPrepared();
		}
		return this.bereichBW;
	}

	public static String extractObjid(SimpleFeature simpleFeature) {
		return simpleFeature.getAttribute("objid").toString();
	}

	public static String extractVariantenId(SimpleFeature simpleFeature) {
		// Das Attribut weg_shp_id setzt sich aus "? # ? # variantenId # objId" zusammen, z.B.:
		// weg_shp_id=DEBWB0010000vh4O#DEBWB00100015IMK#DEBWB001000191IF##DEBWB00100010VxQ##
		return simpleFeature.getAttribute("weg_shp_id").toString()
			.split("#")[2];
	}

	public Laenge extractOffizielleLaenge(SimpleFeature feature) {
		String raw = feature.getAttribute("olg").toString();

		try {
			return Laenge.of(Long.parseLong(raw));
		} catch (NumberFormatException e) {
			log.trace("Kann offizielleLaenge nicht parsen: '" + raw + "'");
			return null;
		}
	}

	public Kategorie extractKategorie(SimpleFeature feature) {
		String raw = feature.getAttribute("kat").toString();

		switch (raw) {
		case "1210":
			// Achtung: 1210 steht für Radfernweg, NICHT für Landesradfernweg
			return Kategorie.RADFERNWEG;
		case "1220":
			return Kategorie.UEBERREGIONALER_RADWANDERWEG;
		case "1230":
			return Kategorie.REGIONALER_RADWANDERWEG;
		case "1240":
			return Kategorie.VERBINDUNGSRADWANDERWEG;
		case "1250":
			return Kategorie.RADVERKEHRSNETZ;
		case "1280":
			return Kategorie.UNMARKIERTER_RADWANDERVORSCHLAG;
		case "1299":
			return Kategorie.SONSTIGER_RADWANDERWEG;
		default:
			throw new RuntimeException("Unbekannte Kategorie: '" + raw + "'");
		}
	}

	public String extractInfo(SimpleFeature feature) {
		return feature.getAttribute("inf").toString();
	}

	public String extractBeschreibung(SimpleFeature feature) {
		return feature.getAttribute("wgb").toString();
	}

	public String extractKurzbeschreibung(SimpleFeature feature) {
		return feature.getAttribute("bez").toString();
	}
}
