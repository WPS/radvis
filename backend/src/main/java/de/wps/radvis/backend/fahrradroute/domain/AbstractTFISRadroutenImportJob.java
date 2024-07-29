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

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.fahrradroute.domain.entity.AbstractTfisImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute.FahrradrouteBuilder;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractTFISRadroutenImportJob extends AbstractJob {

	protected final TfisImportService tfisImportService;
	protected final KantenRepository kantenRepository;
	protected final ShapeFileRepository shapeFileRepository;
	protected final Path tfisRadwegePath;

	public AbstractTFISRadroutenImportJob(
		JobExecutionDescriptionRepository repository,
		TfisImportService tfisImportService,
		KantenRepository kantenRepository,
		ShapeFileRepository shapeFileRepository,
		Path tfisRadwegePath) {
		super(repository);
		this.tfisImportService = tfisImportService;
		this.kantenRepository = kantenRepository;
		this.shapeFileRepository = shapeFileRepository;
		this.tfisRadwegePath = tfisRadwegePath;
	}

	protected void importFromTfis(AbstractTfisImportStatistik importStatistik) {
		LocalDateTime importDate = LocalDateTime.now();
		log.info(this.getClass().getSimpleName() + " gestartet");
		tfisImportService.validate(tfisRadwegePath, TfisImportService.RELEVANTE_ATTRIBUTE);

		try (Stream<SimpleFeature> simpleFeatureStream = this.shapeFileRepository.readShape(tfisRadwegePath.toFile())) {
			log.info("Extrahiere TFIS-Routen aus {}", tfisRadwegePath.getFileName());

			Map<String, List<SimpleFeature>> groupedByObjid = filterFeaturesToImport(simpleFeatureStream
				.peek(simpleFeature -> importStatistik.anzahlFeaturesInShapefile++), importStatistik)
					.peek(simpleFeature -> importStatistik.anzahlDavonZuImportierendeFeatures++)
					.filter(tfisImportService::isNotStichwegOrAlternativStrecke)
					.peek(simpleFeature -> importStatistik.anzahlDavonHauptstreckenFeaturesInShapefile++)
					.map(shapeFileRepository::transformGeometryToUTM32)
					.collect(TfisImportService.groupingByObjid());
			importStatistik.anzahlDavonZuImportierendeFeatures = groupedByObjid.size();

			log.info("Started With: {}", importStatistik.toString());

			AtomicInteger count = new AtomicInteger();

			groupedByObjid.forEach((objid, simpleFeatures) -> {
				List<SimpleFeature> simpleFeaturesInBW = getSimpleFeaturesInBW(objid, simpleFeatures, count,
					groupedByObjid.size());

				if (simpleFeaturesInBW.isEmpty()) {
					return;
				}

				Optional<MultiLineString> originalgeometrie = getOriginalgeometrie(simpleFeatures);

				if (originalgeometrie.isEmpty()) {
					return;
				}

				importStatistik.anzahlRoutenInBw++;

				Set<String> dlmIdsInBW = tfisImportService.extractDlmIds(simpleFeaturesInBW);

				List<Kante> kanten = kantenRepository.findAllByDlmIdIn(
					dlmIdsInBW.stream().map(DlmId::of).collect(Collectors.toList()));

				List<SimpleFeature> nichtGemappteFeatures = simpleFeaturesInBW.stream()
					.filter(simpleFeature -> kanten.stream().map(Kante::getDlmId).map(DlmId::getValue)
						.noneMatch(dlmId -> dlmId.equals(tfisImportService.extractDlmId(simpleFeature))))
					.collect(Collectors.toList());

				if (nichtGemappteFeatures.size() > 0) {
					log.warn("Anzahl nicht gemappter Features: " + nichtGemappteFeatures.size());
				}
				importStatistik.abbildungAufKantenStatistik.anzahlFeaturesOhneDlmIdInRadvis += nichtGemappteFeatures
					.size();

				// Kantengeometrien der DlmIds aus der TFIS-Datei mit der Geometrie in RadVis vergleichen
				// wenn die Laenge zu sehr abweicht, dann nicht direkt über die dlmId die RadVIS Kante nehmen, sondern
				// erstmal die TFIS-Geometrie auf RadVIS matchen und das Ergebnis davon nehmen
				simpleFeaturesInBW.stream()
					.filter(simpleFeature -> !nichtGemappteFeatures.contains(simpleFeature))
					.forEach(simpleFeature -> {
						Optional<Kante> kanteZuSimpleFeature = kanten.stream()
							.filter(kante -> kante.getDlmId().getValue()
								.equals(tfisImportService.extractDlmId(simpleFeature)))
							.findFirst();
						if (kanteZuSimpleFeature.isEmpty()) {
							return;
						}
						LineString kantenGeometry = kanteZuSimpleFeature.get().getGeometry();
						LineString simpleFeatureGeometry = tfisImportService.extractLineString(simpleFeature);
						if (TfisImportService.geometrienZuStarkAbweichend(kantenGeometry, simpleFeatureGeometry)) {
							log.info(
								"Die TFIS-Geometrie und die RadVIS-Geometrie mit der selben DLM-Id haben eine zu unterschiedliche Geometrie. Es wird versucht diese Geomtrie zu matchen.");
							importStatistik.abbildungAufKantenStatistik.anzahlFeaturesMitZuStarkAbweichenderGeometrie++;
							kanten.remove(kanteZuSimpleFeature.get());
							nichtGemappteFeatures.add(simpleFeature);
						}
					});

				Set<SimpleFeature> nichtGemappteFeaturesOhneMatch = new HashSet<>();

				importStatistik.abbildungAufKantenStatistik.anzahlFeaturesMitDlmIdInRadvis = kanten.size();
				List<AbschnittsweiserKantenBezug> netzBezug = getAbschnittsweiserKantenBezuege(
					kanten, nichtGemappteFeatures, importStatistik.abbildungAufKantenStatistik);

				SimpleFeature first = simpleFeatures.get(0);
				String name = tfisImportService.extractName(first);
				log.info("Name: " + name);

				TfisId tfisId = TfisId.of(objid);
				Optional<Point> startpunkt = tfisImportService.extractStartpunkt(first);
				ProfilMatchResult matchResult = getProfilMatchResultOrDefaultValues(netzBezug, startpunkt);
				if (matchResult.getProfilEigenschaften().size() == 0) {
					importStatistik.anzahlProfilinformationenNichtErmittelbar++;
				}

				// Aus den neuen KantenIds, die in Reihenfolge sind,
				// einen neuen Netzbezug (List<AbschnittsweiserKantenBezug>) zusammensetzten
				if (!matchResult.getOsmWayIdsAsOrderedList().isEmpty()) {
					List<Long> netzbezugKanteIdsInOrder = matchResult.getOsmWayIdsAsOrderedList().stream()
						.map(OsmWayId::getValue).collect(Collectors.toList());

					List<Kante> allById = StreamSupport.stream(kantenRepository.findAllById(netzbezugKanteIdsInOrder)
						.spliterator(), false).collect(Collectors.toList());

					netzBezug = netzbezugKanteIdsInOrder.stream()
						.map(kanteId -> allById.stream()
							.filter(kante -> kante.getId().equals(kanteId)).findFirst().orElseThrow())
						.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
						.collect(Collectors.toList());

				}

				Fahrradroute.FahrradrouteBuilder builder = initFahrradrouteBuilder(tfisId, first, importStatistik);
				builder
					.tfisId(tfisId)
					.name(FahrradrouteName.of(name))
					.originalGeometrie(originalgeometrie.get())
					.iconLocation(startpunkt.orElse(null))
					.abschnittsweiserKantenBezug(netzBezug)
					.netzbezugLineString(matchResult.getGeometrie())
					.linearReferenzierteProfilEigenschaften(matchResult.getProfilEigenschaften())
					.zuletztBearbeitet(importDate);

				Fahrradroute fahrradroute = saveFahrradroute(builder, importStatistik);

				log.info("Startpunkt: " + startpunkt.orElse(null));

				if (fahrradroute.getNetzbezugLineString().isPresent()) {
					importStatistik.anzahlNetzbezugLineStringErfolgreich++;
				} else {
					log.warn("Fahrradroute ohne NetzbezugLineString wurde erstellt");
				}

				logLinksFuerNichtGemappteFeatureOhneMatch(nichtGemappteFeaturesOhneMatch, fahrradroute.getId(),
					FahrradrouteTyp.RADVIS_ROUTE);
			});

		} catch (ShapeProjectionException | IOException e) {
			throw new RuntimeException(e);
		}

		log.info("JobStatistik: " + importStatistik);
	}

	protected abstract Fahrradroute saveFahrradroute(FahrradrouteBuilder builder,
		AbstractTfisImportStatistik statistik);

	protected abstract FahrradrouteBuilder initFahrradrouteBuilder(TfisId forTfisId, SimpleFeature feature,
		AbstractTfisImportStatistik statistik);

	protected abstract Stream<SimpleFeature> filterFeaturesToImport(Stream<SimpleFeature> peek,
		AbstractTfisImportStatistik statistik);

	private List<AbschnittsweiserKantenBezug> getAbschnittsweiserKantenBezuege(List<Kante> kanten,
		List<SimpleFeature> nichtGemappteFeatures, AbbildungAufKantenStatistik abbildungAufKantenStatistik) {
		List<Kante> gematchteKanten = tfisImportService.findMatchingKanten(nichtGemappteFeatures,
			abbildungAufKantenStatistik);

		return Stream.concat(kanten.stream(), gematchteKanten.stream())
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());
	}

	private void logLinksFuerNichtGemappteFeatureOhneMatch(Set<SimpleFeature> nichtGemappteFeaturesOhneMatch,
		Long fahrradrouteId, FahrradrouteTyp fahrradrouteTyp) {

		String fahrradrouteTypFilterValue;

		if (fahrradrouteTyp.equals(FahrradrouteTyp.TFIS_ROUTE)) {
			fahrradrouteTypFilterValue = "TFIS-Route";
		} else {
			fahrradrouteTypFilterValue = "RadVIS-Route";
		}

		nichtGemappteFeaturesOhneMatch.forEach(simpleFeature -> {
			String deepLinkTemplate = "http://localhost:4200/viewer/fahrradrouten/%s?layers=&view=%s&netzklassen=&hintergrund=&signatur=&mitVerlauf=false&infrastrukturen=fahrradrouten&tabellenVisible=true&filter_fahrradrouten=fahrradrouteTyp:%s";

			Envelope env = tfisImportService.extractLineString(simpleFeature)
				.getEnvelopeInternal();
			log.info(String.format(deepLinkTemplate,
				fahrradrouteId,
				Stream.of(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY())
					.map(Object::toString)
					.collect(Collectors.joining(";")),
				fahrradrouteTypFilterValue));
		});
	}

	private List<SimpleFeature> getSimpleFeaturesInBW(String objid, List<SimpleFeature> simpleFeatures,
		AtomicInteger count, int groupedByObjidSize) {
		log.info("----------(" + count.incrementAndGet() + "/" + groupedByObjidSize + ")------------");
		log.info("Route mit objid " + objid + " hat " + simpleFeatures.size() + " Features");
		if (simpleFeatures.isEmpty()) {
			return List.of();
		}

		int anzahlDlmIdsGesamt = tfisImportService.extractDlmIds(simpleFeatures).size();
		if (anzahlDlmIdsGesamt != simpleFeatures.size()) {
			log.warn("Anzahl verschiedener DlmIds (" + anzahlDlmIdsGesamt
				+ ") entspricht nicht der Anzahl an Features (" + simpleFeatures.size() + ")");
		}

		List<SimpleFeature> simpleFeaturesInBW = simpleFeatures.stream()
			.filter(tfisImportService::isGeometryInBW)
			.collect(Collectors.toList());

		log.info("Features in BW: " + simpleFeaturesInBW.size());

		if (simpleFeaturesInBW.isEmpty()) {
			log.warn("Route liegt nicht in BW");
		}

		return simpleFeaturesInBW;
	}

	private Optional<MultiLineString> getOriginalgeometrie(List<SimpleFeature> simpleFeatures) {
		MultiLineString originalGeometrie = tfisImportService.konstruiereOriginalGeometrie(simpleFeatures);

		double anteilInBW = tfisImportService.anteilInBW(originalGeometrie);
		log.info("Anteil der Route in BW: " + anteilInBW);

		if (anteilInBW < 0.25) {
			log.warn("Route liegt größtenteils (> 75%) nicht in BW");
			return Optional.empty();
		}
		return Optional.of(originalGeometrie);
	}

	private ProfilMatchResult getProfilMatchResultOrDefaultValues(List<AbschnittsweiserKantenBezug> netzBezug,
		Optional<Point> originalStartpunkt) {
		Optional<LineString> netzbezugLineString = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			netzBezug).map(StreckeVonKanten::getStrecke);

		if (netzbezugLineString.isPresent() && originalStartpunkt.isPresent()) {
			double abstandVonStart = netzbezugLineString.get().getStartPoint().distance(originalStartpunkt.get());
			double abstandVonEnde = netzbezugLineString.get().getEndPoint().distance(originalStartpunkt.get());
			if (abstandVonEnde < abstandVonStart) {
				netzbezugLineString = Optional.of(netzbezugLineString.get().reverse());
			}
		}

		if (netzbezugLineString.isPresent()) {
			try {
				ProfilMatchResult profilMatchResult = tfisImportService.routeProfil(netzbezugLineString.get());

				boolean profilRoutenResultGleich = netzbezugLineString.get().buffer(KnotenIndex.SNAPPING_DISTANCE)
					.contains(profilMatchResult.getGeometrie());
				if (!profilRoutenResultGleich) {
					log.warn(
						"Durch das Erstellen der Profilinformationen wurde die Geometrie des Netzbezuges verändert!");
				} else {
					log.info("Netzbezug Geometrie ist durch das Erstellen der Profilinformationen gleich geblieben.");
				}

				List<Long> alterNetzbezugKantenIds = netzBezug.stream().map(akb -> akb.getKante().getId())
					.collect(Collectors.toList());
				List<Long> neuerNetzbezugKantenIds = profilMatchResult.getOsmWayIdsAsOrderedList().stream()
					.map(OsmWayId::getValue).collect(Collectors.toList());
				if (alterNetzbezugKantenIds.size() != neuerNetzbezugKantenIds.size()
					|| !alterNetzbezugKantenIds.containsAll(neuerNetzbezugKantenIds)) {
					log.warn(
						"Durch das Erstellen der Profilinformationen wurde die Liste an KantenIds des Netzbezuges verändert!");
				} else {
					log.info("Netzbezug KantenIds sind durch das Erstellen der Profilinformationen gleich geblieben.");
				}

				return profilMatchResult;
			} catch (KeinMatchGefundenException e) {
				log.error("Konnte zusammengesetzten LineString nicht matchen für Route");
			}
		}
		return new ProfilMatchResult(null, new ArrayList<>(), new ArrayList<>());
	}
}
