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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteVariante;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradroutenVariantenTfisImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractFahrradroutenVariantenTfisImportJob extends AbstractJob {

	protected final TfisImportService tfisImportService;
	protected final KantenRepository kantenRepository;
	protected final ShapeFileRepository shapeFileRepository;
	protected final Path tfisRadwegePath;

	public AbstractFahrradroutenVariantenTfisImportJob(
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

	protected Set<TfisId> importFromTfis(FahrradroutenVariantenTfisImportStatistik importStatistik) {
		log.info(this.getClass().getSimpleName() + " gestartet");
		tfisImportService.validate(tfisRadwegePath, TfisImportService.RELEVANTE_VARIANTE_ATTRIBUTE);

		try (Stream<SimpleFeature> simpleFeatureStream = this.shapeFileRepository.readShape(tfisRadwegePath.toFile())) {
			log.info("Extrahiere TFIS-Routen aus {}", tfisRadwegePath.getFileName());

			Set<TfisId> zuBeruecksichtigendeFahrradrouten = getZuBeruecksichtigendeFahrradroutenTfisIds();

			Map<String, List<SimpleFeature>> groupedByTfisId = simpleFeatureStream
				.peek(simpleFeature -> importStatistik.filterFeaturesStatistik.anzahlFeaturesGesamt++)
				.filter(tfisImportService::isStichwegOrAlternativStrecke)
				.peek(simpleFeature -> importStatistik
					.varianteFound(TfisImportService.extractVariantenId(simpleFeature)))
				.filter(simpleFeature -> zuBeruecksichtigendeFahrradrouten
					.contains(TfisId.of(TfisImportService.extractObjid(simpleFeature))))
				.peek(
					simpleFeature -> {
						importStatistik.filterFeaturesStatistik.anzahlFeaturesIsVarianteMitFahrradrouteInRadvis++;
						importStatistik.varianteHasFahrradroute(TfisImportService.extractVariantenId(simpleFeature));
					})
				.map(shapeFileRepository::transformGeometryToUTM32)
				.collect(TfisImportService.groupingByObjid());

			importStatistik.anzahlVariantenIgnoriert += importStatistik.anzahlVariantenMitFahrradrouteNichtBetrachtet;
			log.info("Varianten ohne Fahrradroute in RadVIS gefunden: "
				+ String.join(", ", importStatistik.varianteIdsOhneFahrradroute));
			importStatistik.anzahlFahrradroutenMitVarianten = groupedByTfisId.size();

			Set<TfisId> tfisroutenWithVarianten = new HashSet<>();

			groupedByTfisId.forEach((tfisIdString, fahrradrouteFeatures) -> {
				Map<String, List<SimpleFeature>> groupedByVariantenId = fahrradrouteFeatures.stream()
					.collect(Collectors.groupingBy(TfisImportService::extractVariantenId));

				importStatistik.anzahlVariantenBetrachtet += groupedByVariantenId.size();

				List<FahrradrouteVariante> fahrradrouteVarianten = new ArrayList<>();
				groupedByVariantenId.forEach((varianteId, varianteFeatures) -> createFahrradrouteVariante(tfisIdString,
					varianteId, varianteFeatures, importStatistik)
						.ifPresent(fahrradrouteVarianten::add)
				);
				TfisId tfisId = TfisId.of(tfisIdString);
				saveFahrradroutenVarianten(tfisId, fahrradrouteVarianten, importStatistik);
				tfisroutenWithVarianten.add(tfisId);
			});
			return tfisroutenWithVarianten;
		} catch (ShapeProjectionException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Optional<FahrradrouteVariante> createFahrradrouteVariante(String tfisId, String varianteId,
		List<SimpleFeature> varianteFeatures,
		FahrradroutenVariantenTfisImportStatistik importStatistik) {
		log.info(
			"Die Variante " + varianteId + " (Fahrradroute " + tfisId
				+ ") wird versucht zu importieren");

		// Importierbare Variantenfeatures zusammensuchen
		List<SimpleFeature> varianteFeatuesInBW = getImportierbareVariantenFeatures(varianteFeatures,
			importStatistik
		);

		if (varianteFeatuesInBW.isEmpty()) {
			importStatistik.anzahlVariantenIgnoriert++;
			importStatistik.anzahlVariantenAusserhalbBW++;
			return Optional.empty();
		}

		MultiLineString originalGeometrie = tfisImportService.konstruiereOriginalGeometrie(
			varianteFeatures);

		if (tfisImportService.anteilInBW(originalGeometrie) < 0.25) {
			log.info("Die Variante liegt größtenteils (> 75%) nicht in BW");
			importStatistik.anzahlVariantenIgnoriert++;
			importStatistik.anzahlVariantenAusserhalbBW++;
			return Optional.empty();
		}

		// Abbilden auf unser Netz
		List<Kante> zugeordneteKanten = getZugeordneteKanten(varianteFeatuesInBW, importStatistik);
		List<AbschnittsweiserKantenBezug> netzBezug = zugeordneteKanten.stream()
			.map(
				kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());

		// Aus den abgebildeten Kanten einen zusammenhaengenden Netzbezug erstellen
		Optional<LineString> linestringFromNetzbezug = AbschnittsweiserKantenBezug
			.erstelleNetzbezugLineString(netzBezug)
			.map(StreckeVonKanten::getStrecke);

		ProfilMatchResult matchResult = new ProfilMatchResult(null, new ArrayList<>(),
			new ArrayList<>());
		if (linestringFromNetzbezug.isEmpty()) {
			importStatistik.erstellungZusammenhaengenderLinestringStatistik.anzahlVariantenNichtZusammengesetzterLinestring++;
			log.warn("Es wurde kein zusammenhängender Linestring für die Variante gefunden.");
		} else {
			try {
				matchResult = tfisImportService.routeProfil(linestringFromNetzbezug.get());
			} catch (KeinMatchGefundenException e) {
				log.warn("Es konnte nicht über den Netzbezuglinestring der Variante geroutet werden.");
				importStatistik.erstellungZusammenhaengenderLinestringStatistik.anzahlVariantenNichtRoutebarerLineString++;
			}

			if (!matchResult.getOsmWayIdsAsOrderedList().isEmpty()) {
				netzBezug = extractNetzbezug(matchResult.getOsmWayIdsAsOrderedList());
			}
		}

		// Speichern der Variante
		return Optional.of(FahrradrouteVariante.tfisVarianteBuilder()
			.tfisId(TfisId.of(varianteId))
			.kategorie(tfisImportService.extractVarianteKategorie(varianteFeatures.get(0)))
			.abschnittsweiserKantenBezug(netzBezug)
			.linearReferenzierteProfilEigenschaften(matchResult.getProfilEigenschaften())
			.geometrie(matchResult.getGeometrie())
			.build());
	}

	private List<Kante> getZugeordneteKanten(List<SimpleFeature> varianteFeatuesInBW,
		FahrradroutenVariantenTfisImportStatistik importStatistik) {
		Set<String> dlmIdsInBW = tfisImportService.extractDlmIds(varianteFeatuesInBW);

		List<Kante> kanteInMemRepository = kantenRepository.findAllByDlmIdIn(
			dlmIdsInBW.stream().map(DlmId::of).collect(Collectors.toList()));

		List<SimpleFeature> mussGematchedWerden = new ArrayList<>();
		List<Kante> zugeordneteKanten = new ArrayList<>();

		varianteFeatuesInBW.forEach(simpleFeature -> {
			Optional<Kante> kanteMitGleicherDlmId = kanteInMemRepository.stream().filter(
				kante -> kante.getDlmId().getValue().equals(tfisImportService.extractDlmId(simpleFeature)))
				.findFirst();
			if (kanteMitGleicherDlmId.isEmpty()) {
				mussGematchedWerden.add(simpleFeature);
				importStatistik.abbildungAufKantenStatistik.anzahlFeaturesOhneDlmIdInRadvis++;
			} else if (TfisImportService.geometrienZuStarkAbweichend(
				kanteMitGleicherDlmId.get().getGeometry(),
				tfisImportService.extractLineString(simpleFeature))) {
				mussGematchedWerden.add(simpleFeature);
				importStatistik.abbildungAufKantenStatistik.anzahlFeaturesMitZuStarkAbweichenderGeometrie++;
			} else {
				// In diesem Fall hat die Kante eine aehnliche Geometrie und eine passende dlmId
				zugeordneteKanten.add(kanteMitGleicherDlmId.get());
				importStatistik.abbildungAufKantenStatistik.anzahlFeaturesMitDlmIdInRadvis++;
			}
		});

		zugeordneteKanten
			.addAll(tfisImportService.findMatchingKanten(mussGematchedWerden,
				importStatistik.abbildungAufKantenStatistik));
		return zugeordneteKanten;
	}

	private List<SimpleFeature> getImportierbareVariantenFeatures(List<SimpleFeature> varianteFeatures,
		FahrradroutenVariantenTfisImportStatistik importStatistik) {
		List<SimpleFeature> varianteFeatuesInBW = varianteFeatures.stream()
			.filter(tfisImportService::isGeometryInBW)
			.collect(Collectors.toList());

		if (varianteFeatuesInBW.size() != varianteFeatures.size()) {
			log.info("Die Variante hat " + varianteFeatuesInBW.size() + " von "
				+ varianteFeatures.size() + " Features in BW.");
			importStatistik.filterFeaturesStatistik.anzahlFeaturesAusserhalbBW += (varianteFeatures.size()
				- varianteFeatuesInBW.size());
		}
		return varianteFeatuesInBW;
	}

	private List<AbschnittsweiserKantenBezug> extractNetzbezug(List<OsmWayId> dlmIds) {
		List<Long> netzbezugKanteIdsInOrder = dlmIds.stream().map(OsmWayId::getValue).collect(Collectors.toList());

		// Die Kanten muessen hier nochmal neu aus der DB geholt werden, da es sein kann, dass durch das Routen ueber
		// den NetzbezugLineString KantenIds hinzugekommen sind, die vorher noch nicht im Netzbezug waren.
		Map<Long, Kante> allById = new HashMap<>();
		kantenRepository.findAllById(netzbezugKanteIdsInOrder).forEach(kante -> allById.put(kante.getId(), kante));

		return netzbezugKanteIdsInOrder.stream()
			.map(allById::get)
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());
	}

	protected abstract void saveFahrradroutenVarianten(TfisId tfisId, List<FahrradrouteVariante> fahrradrouteVarianten,
		FahrradroutenVariantenTfisImportStatistik importStatistik);

	protected abstract Set<TfisId> getZuBeruecksichtigendeFahrradroutenTfisIds();
}
