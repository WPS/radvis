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

package de.wps.radvis.backend.organisation.schnittstelle.repositoryImpl;

import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.organisation.domain.FalscherGeometrieTypException;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerwaltungseinheitBkgFormatImportRepositoryImpl extends AbstractVerwaltungseinheitImportRepository
	implements VerwaltungseinheitImportRepository {

	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final String GF_PROPERTY = "GF";
	private static final String NAME_PROPERTY = "GEN";
	private static final String AGS_PROPERTY = "AGS";
	private static final List<String> BUNDESLAENDER_MIT_REGIERUNGSBEZIRKEN_AGS = List.of(
		// Nur die Bundesländer, die bis heute Regierungsbezirke benutzen; siehe hierzu auch
		// https://de.wikipedia.org/wiki/Regierungsbezirk
		// https://de.wikipedia.org/wiki/Amtlicher_Gemeindeschl%C3%BCssel
		"05", // Nordrhein-Westfalen
		"06", // Hessen
		"08", // Baden-Württemberg
		"09"  // Bayern
	);
	private static final String WURZEL_KEY = "wurzel";
	private final OrganisationsArt obersteGebietskoerperschaftArt;
	private final String obersteGebietskoerperschaftName;
	private final File verwaltungsgrenzenVerzeichnis;

	public VerwaltungseinheitBkgFormatImportRepositoryImpl(CoordinateReferenceSystemConverter coordinateConverter,
		@NonNull OrganisationsArt obersteGebietskoerperschaftArt, @NonNull String obersteGebietskoerperschaftName,
		File verwaltungsgrenzenVerzeichnis) {
		super(coordinateConverter);
		this.verwaltungsgrenzenVerzeichnis = verwaltungsgrenzenVerzeichnis;
		this.obersteGebietskoerperschaftArt = obersteGebietskoerperschaftArt;
		this.obersteGebietskoerperschaftName = obersteGebietskoerperschaftName;

		require(verwaltungsgrenzenVerzeichnis.exists(),
			"Verwaltungsgrenzenverzeichnis existiert nicht: " + verwaltungsgrenzenVerzeichnis.getAbsolutePath());
		require(verwaltungsgrenzenVerzeichnis.isDirectory(),
			"Verwaltungsgrenzenverzeichnis ist kein Verzeichnis: " + verwaltungsgrenzenVerzeichnis.getAbsolutePath());
	}

	protected GebietskoerperschaftsFeatures getFeaturesFromFiles() throws IOException {
		File staatFile = new File(verwaltungsgrenzenVerzeichnis, "VG250_STA.shp");
		File bundeslandFile = new File(verwaltungsgrenzenVerzeichnis, "VG250_LAN.shp");
		File regierungsbezirkFile = new File(verwaltungsgrenzenVerzeichnis, "VG250_RBZ.shp");
		File landkreisFile = new File(verwaltungsgrenzenVerzeichnis, "VG250_KRS.shp");
		File gemeindeFile = new File(verwaltungsgrenzenVerzeichnis, "VG250_GEM.shp");

		require(staatFile.exists(), "Staat File existiert nicht: " + staatFile.getAbsolutePath());
		require(bundeslandFile.exists(), "Bundesland File existiert nicht: " + bundeslandFile.getAbsolutePath());
		require(regierungsbezirkFile.exists(),
			"Regierungsbezirk File existiert nicht: " + regierungsbezirkFile.getAbsolutePath());
		require(landkreisFile.exists(), "Landkreis File existiert nicht: " + landkreisFile.getAbsolutePath());
		require(gemeindeFile.exists(), "Gemeinde File existiert nicht: " + gemeindeFile.getAbsolutePath());

		SimpleFeatureCollection staatFeatures = readShapeFile(staatFile, CHARSET);
		SimpleFeatureCollection bundeslandFeature = readShapeFile(bundeslandFile, CHARSET);
		SimpleFeatureCollection regierungsbezirkFeatures = readShapeFile(regierungsbezirkFile, CHARSET);
		SimpleFeatureCollection landkreisFeatures = readShapeFile(landkreisFile, CHARSET);
		SimpleFeatureCollection gemeindeFeatures = readShapeFile(gemeindeFile, CHARSET);

		return new GebietskoerperschaftsFeatures(Optional.of(staatFeatures),
			Optional.of(bundeslandFeature), Optional.of(regierungsbezirkFeatures), Optional.of(landkreisFeatures),
			gemeindeFeatures);
	}

	@Override
	protected List<Gebietskoerperschaft> getGebietskoerperschaftenFromFeatures(GebietskoerperschaftsFeatures features) {
		if (!obersteGebietskoerperschaftArt.equals(OrganisationsArt.STAAT)) {
			throw new UnsupportedOperationException(
				"Dieser Import ist nur für STAAT als oberste Gebietskörperschafts-Art implementiert.");
		}

		SimpleFeatureCollection staatFeatures = filtereNachLandflaechen(features.staat().get());
		SimpleFeatureCollection bundeslandFeatures = filtereNachLandflaechen(features.bundesland().get());
		SimpleFeatureCollection regierungsbezirkFeatures = filtereNachLandflaechen(features.regierungsbezirk().get());
		SimpleFeatureCollection kreisFeatures = filtereNachLandflaechen(features.kreis().get());
		SimpleFeatureCollection gemeindeFeatures = filtereNachLandflaechen(features.gemeinde());

		// Staat
		if (staatFeatures.size() != 1) {
			throw new RuntimeException(
				"Staat ShapeFile muss genau ein Feature enthalten, wenn oberste Gebietskörperschafts-Art STAAT ist.");
		}

		Map<String, Gebietskoerperschaft> fachIdToOrganisation = new HashMap<>();
		KoordinatenReferenzSystem sourceReferenzSystem;

		try {
			sourceReferenzSystem = getKoordinatenReferenzSystem(staatFeatures);
		} catch (FactoryException e) {
			throw new RuntimeException("Ermittlung der SRID für Staat Shapefile fehlgeschlagen.");
		}

		try (SimpleFeatureIterator iterator = staatFeatures.features()) {
			SimpleFeature feature = iterator.next();

			MultiPolygon geometry = getPolygon(sourceReferenzSystem, feature);

			Gebietskoerperschaft org = new Gebietskoerperschaft(0, obersteGebietskoerperschaftName, null,
				OrganisationsArt.STAAT, geometry, true);

			fachIdToOrganisation.put(WURZEL_KEY, org);
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Staat Shapefile enthält falschen GeometrieTyp.", e);
		}

		// Bundesländer
		try {
			fachIdToOrganisation.putAll(
				createGebietskoerperschaften(bundeslandFeatures, Collections.unmodifiableMap(fachIdToOrganisation),
					sourceReferenzSystem, OrganisationsArt.BUNDESLAND));
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Bundesland Shapefile enthält falschen GeometrieTyp.", e);
		}

		// Regierungsbezirke
		try {
			fachIdToOrganisation.putAll(createGebietskoerperschaften(regierungsbezirkFeatures,
				Collections.unmodifiableMap(fachIdToOrganisation), sourceReferenzSystem,
				OrganisationsArt.REGIERUNGSBEZIRK));
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Regierungsbezirk Shapefile enthält falschen GeometrieTyp.", e);
		}

		// Kreise
		try {
			fachIdToOrganisation
				.putAll(createGebietskoerperschaften(kreisFeatures, Collections.unmodifiableMap(fachIdToOrganisation),
					sourceReferenzSystem, OrganisationsArt.KREIS));
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Kreis Shapefile enthält falschen GeometrieTyp.", e);
		}

		// Gemeinde
		try {
			fachIdToOrganisation
				.putAll(
					createGebietskoerperschaften(gemeindeFeatures, Collections.unmodifiableMap(fachIdToOrganisation),
						sourceReferenzSystem, OrganisationsArt.GEMEINDE));
		} catch (FalscherGeometrieTypException e) {
			throw new RuntimeException("Gemeinde Shapefile enthält falschen GeometrieTyp.", e);
		}

		List<Gebietskoerperschaft> result = new ArrayList<>(fachIdToOrganisation.values());
		result.sort(Comparator.comparing(Gebietskoerperschaft::getFachId));

		return result;
	}

	public SimpleFeatureCollection filtereNachLandflaechen(SimpleFeatureCollection ungefilterteFeatures) {
		// Der VG250-Datensatz hält jeweils genau einen Eintrag mit Geofaktor (GF) = 4 pro Verwaltungseinheit.
		// Das sind genau die Einträge für Land und ohne Gewässer (Nord- und Ostsee + Bodensee), die wir hier brauchen.
		if (ungefilterteFeatures.isEmpty()) {
			return ungefilterteFeatures;
		}
		SimpleFeatureType featureType = ungefilterteFeatures.getSchema();

		List<SimpleFeature> gefilterteFeatures = new ArrayList<>();

		try (SimpleFeatureIterator iterator = ungefilterteFeatures.features()) {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();

				if (feature.getProperty(GF_PROPERTY).getValue().toString().equals("4")) {
					gefilterteFeatures.add(feature);
				}
			}
		}

		return new ListFeatureCollection(featureType, gefilterteFeatures);
	}

	private Map<String, Gebietskoerperschaft> createGebietskoerperschaften(SimpleFeatureCollection featureCollection,
		Map<String, Gebietskoerperschaft> uebergeordneteOrganisationen, KoordinatenReferenzSystem sourceReferenzSystem,
		OrganisationsArt importArt) throws FalscherGeometrieTypException {
		Map<String, Gebietskoerperschaft> result = new HashMap<>();
		try (SimpleFeatureIterator iterator = featureCollection.features()) {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();

				MultiPolygon geometry = getPolygon(sourceReferenzSystem, feature);
				String ags = feature.getProperty(AGS_PROPERTY).getValue().toString();
				String uebergeordneteId = parseUebergeordneteAgs(ags, importArt);
				Gebietskoerperschaft uebergeordneteOrganisation = uebergeordneteOrganisationen.get(uebergeordneteId);
				if (uebergeordneteOrganisation == null) {
					throw new RuntimeException(
						"Für Organisation " + ags + " konnte die übergeordnete Organisation mit AGS " + uebergeordneteId
							+ " nicht gefunden werden.");
				}
				Gebietskoerperschaft org = new Gebietskoerperschaft(
					parseFachId(ags, importArt),
					feature.getProperty(NAME_PROPERTY).getValue().toString(),
					uebergeordneteOrganisation,
					importArt,
					geometry,
					true);

				result.put(ags, org);
			}
		}

		return result;
	}

	private String parseUebergeordneteAgs(String ags, OrganisationsArt art) {
		switch (art) {
		case REGIERUNGSBEZIRK:
			return ags.substring(0, 2);
		case KREIS:
			if (BUNDESLAENDER_MIT_REGIERUNGSBEZIRKEN_AGS.contains(ags.substring(0, 2))) {
				return ags.substring(0, 3);
			} else {
				return ags.substring(0, 2);
			}
		case GEMEINDE:
			return ags.substring(0, 5);
		default:
			return WURZEL_KEY;
		}
	}

	private Integer parseFachId(String ags, OrganisationsArt art) {
		return switch (art) {
		case BUNDESLAND -> Integer.parseInt(ags.substring(0, 2));
		case REGIERUNGSBEZIRK -> Integer.parseInt(ags.substring(2, 3));
		case KREIS -> Integer.parseInt(ags.substring(3, 5));
		case GEMEINDE -> Integer.parseInt(ags.substring(5));
		default -> 0;
		};
	}

	@Override
	protected void checkRequiredProperties(GebietskoerperschaftsFeatures features) {
		if (features.bundesland().isEmpty() || features.regierungsbezirk().isEmpty() || features.kreis().isEmpty()
			|| features.staat().isEmpty()) {
			throw new RuntimeException(
				"Nur für Staat mit Bundesländern und Regierungsbezirken und Kreisen implementiert");
		}

		SimpleFeatureCollection bundeslandFeatures = features.bundesland().get();
		SimpleFeatureCollection regierungsbezirkFeatures = features.regierungsbezirk().get();
		SimpleFeatureCollection kreisFeatures = features.kreis().get();
		SimpleFeatureCollection gemeindeFeatures = features.gemeinde();

		try (SimpleFeatureIterator iterator = bundeslandFeatures.features()) {
			if (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				if (!hasProperties(feature, AGS_PROPERTY, NAME_PROPERTY)) {
					throw new RuntimeException(
						"Feature für Bundesland muss folgendes Properties enthalten: " + AGS_PROPERTY + ", "
							+ NAME_PROPERTY);
				}
			}
		}

		try (SimpleFeatureIterator iterator = regierungsbezirkFeatures.features()) {
			if (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				if (!hasProperties(feature, AGS_PROPERTY, NAME_PROPERTY)) {
					throw new RuntimeException(
						"Feature für Regierungsbezirk muss folgendes Properties enthalten: " + AGS_PROPERTY + ", "
							+ NAME_PROPERTY);
				}
			}
		}

		try (SimpleFeatureIterator iterator = kreisFeatures.features()) {
			if (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				if (!hasProperties(feature, AGS_PROPERTY, NAME_PROPERTY)) {
					throw new RuntimeException(
						"Feature für Kreis muss folgendes Properties enthalten: " + AGS_PROPERTY + ", "
							+ NAME_PROPERTY);
				}
			}
		}

		try (SimpleFeatureIterator iterator = gemeindeFeatures.features()) {
			if (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				if (!hasProperties(feature, AGS_PROPERTY, NAME_PROPERTY)) {
					throw new RuntimeException(
						"Feature für Gemeinden muss folgendes Properties enthalten: " + AGS_PROPERTY + ", "
							+ NAME_PROPERTY);
				}
			}
		}
	}

	public static boolean checkShapeFiles(File gebietskoerperschaftShpVerzeichnis) {
		File gemeindeFile = new File(gebietskoerperschaftShpVerzeichnis, "VG250_GEM.shp");
		SimpleFeatureCollection featureCollection;

		try {
			if (!gemeindeFile.exists() || Files.size(gemeindeFile.toPath()) == 0) {
				return false;
			}

			featureCollection = readShapeFile(gemeindeFile, CHARSET);
		} catch (IOException e) {
			log.info("Die Datei {} konnte nicht eingelesen werden.", gemeindeFile.getAbsolutePath());
			return false;
		}

		try (SimpleFeatureIterator iterator = featureCollection.features()) {
			if (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				return hasProperties(feature, AGS_PROPERTY, NAME_PROPERTY);
			}
		}

		return false;
	}

	@Override
	public List<Organisation> getCustomAdditionalOrganisationen() {
		return List.of(
			new Organisation("Dritter / Sonstiger", null, OrganisationsArt.SONSTIGES, Set.of(), true),
			new Organisation("Unbekannt", null, OrganisationsArt.SONSTIGES, Set.of(), true)
		);
	}
}
