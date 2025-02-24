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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.KantenMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.Haendigkeit;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.KantenKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedFeature;
import de.wps.radvis.backend.manuellerimport.common.FortschrittLogger;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryFactory;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManuellerAttributeImportUebernahmeService {

	private final InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory;
	private final MappingService mappingService;
	private final EntityManager entityManager;
	private Laenge minimaleSegmentLaenge;

	public ManuellerAttributeImportUebernahmeService(
		@NonNull InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory,
		MappingService mappingService,
		EntityManager entityManager, Laenge minimaleSegmentLaenge) {
		this.minimaleSegmentLaenge = minimaleSegmentLaenge;
		this.inMemoryKantenRepositoryFactory = inMemoryKantenRepositoryFactory;
		this.mappingService = mappingService;
		this.entityManager = entityManager;

	}

	@Transactional
	public void attributeUebernehmen(List<String> attribute, Verwaltungseinheit organisation,
		List<FeatureMapping> featureMappings, AttributeMapper attributeMapper,
		AttributeImportKonfliktProtokoll kantenKonfliktProtokolle) {

		log.info("FeatureMappings werden zu KantenMappings invertiert");

		InMemoryKantenRepository inMemoryKantenRepository = inMemoryKantenRepositoryFactory.create(
			organisation.getBereich().orElse(
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createMultiPolygon()));

		List<KantenMapping> kantenMappings = invertMappingAndCreateMappedFeatures(featureMappings,
			inMemoryKantenRepository, attributeMapper);

		log.info("Es wurden {} KantenMappings erstellt", kantenMappings.size());

		log.info("Es werden folgende Attribute importiert: {}", attribute.toString());

		AtomicInteger fortschritt = new AtomicInteger(0);
		for (KantenMapping kantenMapping : kantenMappings) {
			KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(
				kantenMapping.getKante().getId(),
				kantenMapping.getKante().getGeometry());
			attribute.stream().sorted(attributeMapper::sortAttribute).forEach(
				attribut -> mappingService.map(attributeMapper, attribut, kantenMapping, kantenKonfliktProtokoll));

			if (!kantenKonfliktProtokoll.getKonflikte().isEmpty()) {
				kantenKonfliktProtokolle.addKonflikt(kantenKonfliktProtokoll);
			}
			kantenMapping.getKante().defragmentiereLinearReferenzierteAttribute(minimaleSegmentLaenge);

			FortschrittLogger.logProgressInPercent(kantenMappings.size(), fortschritt, 5);
		}

		log.info("Kanten mit Konflikten: {}", kantenKonfliktProtokolle.getKantenKonfliktProtokolle().size());

		log.info("... gruppiert nach Anzahl der Konflikte: \n {}",
			kantenKonfliktProtokolle.getKantenKonfliktProtokolle().stream()
				.collect(Collectors.groupingBy(kkp -> kkp.getKonflikte().size(), Collectors.counting())));

		log.info("Konflikte insgesamt: {}",
			kantenKonfliktProtokolle.getKantenKonfliktProtokolle().stream().mapToLong(kkp -> kkp.getKonflikte().size())
				.sum());

		log.info("... davon mit linRef(0,1): {}",
			kantenKonfliktProtokolle.getKantenKonfliktProtokolle().stream().flatMap(kkp -> kkp.getKonflikte().stream())
				.filter(k -> k.getLinearReferenzierterAbschnitt().equals(LinearReferenzierterAbschnitt.of(0, 1)))
				.count());
		log.info("Schreibe in DB...");
		entityManager.flush();
		entityManager.clear();
	}

	List<KantenMapping> invertMappingAndCreateMappedFeatures(List<FeatureMapping> featureMappings,
		InMemoryKantenRepository inMemoryKantenRepository, AttributeMapper attributeMapper) {
		Map<Kante, KantenMapping> result = new HashMap<>();
		featureMappings.forEach(featureMapping -> {
			featureMapping.getKantenAufDieGemappedWurde().forEach(mappedGrundnetzkante -> {
				Optional<Kante> kante = inMemoryKantenRepository.findKanteById(mappedGrundnetzkante.getKanteId());
				if (kante.isPresent()) {
					KantenMapping kantenMapping = result.compute(kante.get(),
						(k, kM) -> kM == null ? new KantenMapping(k) : kM);
					Haendigkeit geometrischeHaendigkeit = Haendigkeit.of(featureMapping.getImportedLineString(),
						kante.get().getGeometry());

					Optional<Seitenbezug> attributierterSeitenbezug = hatAttributiertenSeitenbezug(
						featureMapping.getProperties(), attributeMapper);

					kantenMapping.add(MappedFeature.of(
						featureMapping.getImportedLineString(),
						featureMapping.getProperties(),
						mappedGrundnetzkante.getLinearReferenzierterAbschnitt(),
						geometrischeHaendigkeit,
						attributierterSeitenbezug.orElse(null)));
				} else {
					throw new RuntimeException("Gemappte GrundnetzKante mit id " + mappedGrundnetzkante.getKanteId()
						+ " nicht im ImMemoryKantenRepo vorhanden!");
				}
			});
		});
		return new ArrayList<>(result.values());
	}

	private Optional<Seitenbezug> hatAttributiertenSeitenbezug(Map<String, Object> featureProperties,
		AttributeMapper attributeMapper) {
		if (attributeMapper instanceof AttributivSeitenbezogenerMapper attributivSeitenbezogenerMapper) {
			if (featureProperties.containsKey(attributivSeitenbezogenerMapper.getSeiteAttributName())) {
				return attributivSeitenbezogenerMapper.mapSeiteIfPresentAndValid(
					(String) featureProperties.get(attributivSeitenbezogenerMapper.getSeiteAttributName()));
			}
		}
		return Optional.empty();
	}
}
