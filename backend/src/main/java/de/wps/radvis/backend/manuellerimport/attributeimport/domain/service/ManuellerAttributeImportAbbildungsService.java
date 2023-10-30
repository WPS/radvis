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

import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;

import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.SimpleFeatureTypeFactory;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMapping;
import de.wps.radvis.backend.manuellerimport.common.FortschrittLogger;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.exception.GeometryTypeMismatchException;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryFactory;
import de.wps.radvis.backend.manuellerimport.common.domain.service.AbstractManuellerImportAbbildungsService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import de.wps.radvis.backend.matching.domain.service.MatchingKorrekturService;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
public class ManuellerAttributeImportAbbildungsService extends AbstractManuellerImportAbbildungsService {

	private final InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory;

	private final SimpleMatchingService simpleMatchingService;

	public ManuellerAttributeImportAbbildungsService(
		InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory,
		SimpleMatchingService simpleMatchingService) {
		this.inMemoryKantenRepositoryFactory = inMemoryKantenRepositoryFactory;
		this.simpleMatchingService = simpleMatchingService;
	}

	public List<FeatureMapping> bildeFeaturesAb(List<SimpleFeature> featuresInBereich,
		AbstractImportSession importSession) {
		MatchingStatistik matchingStatistik = new MatchingStatistik();
		InMemoryKantenRepository inMemoryKantenRepository = inMemoryKantenRepositoryFactory
			.create(importSession.getOrganisation());

		AtomicInteger fortschritt = new AtomicInteger(0);
		AtomicLong index = new AtomicLong(0);
		log.info("Bilde erstellte features auf Kanten ab...");
		List<FeatureMapping> abgebildeteFeatures = new ArrayList<>();

		featuresInBereich.forEach(feature -> {
			LineString linestring = null;
			try {
				linestring = extractLinestring(feature);
			} catch (GeometryTypeMismatchException e) {
				importSession.addLogEintrag(
					ImportLogEintrag
						.ofWarnung("Feature " + feature.getID() + " wird nicht importiert: " + e.getMessage()));
			}

			if (linestring != null) {
				Map<String, Object> attribute = extractAttribute(feature);
				FeatureMapping featureMapping = new FeatureMapping(index.getAndIncrement(), attribute, linestring);

				featureMapping = matcheFeatureMapping(featureMapping, matchingStatistik, inMemoryKantenRepository);

				abgebildeteFeatures.add(featureMapping);
			}

			FortschrittLogger.logProgressInPercent(featuresInBereich.size(), fortschritt, 4);
		});

		log.info("Matchingstatistik:");
		log.info(matchingStatistik.toString());
		log.info("Attributabbildung fertig");

		return abgebildeteFeatures;
	}

	private Map<String, Object> extractAttribute(SimpleFeature feature) {
		Map<String, Object> result = new HashMap<>();
		feature.getProperties().forEach(property -> {
			if (!property.getName().toString().equals(SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_THE_GEOM)) {
				result.put(property.getName().toString(), property.getValue());
			} else {
				result.put(property.getName().toString(), property.getValue().toString());
			}
		});

		return result;
	}

	public FeatureMapping rematchFeaturemapping(FeatureMapping featureMapping, Verwaltungseinheit organisation) {
		require(featureMapping.getKantenAufDieGemappedWurde().isEmpty());

		Envelope boundingBoxFuerKanten = featureMapping.getImportedLineString().getEnvelopeInternal();
		boundingBoxFuerKanten.expandBy(MatchingKorrekturService.MAX_DISTANCE_TO_MATCHED_GEOMETRY);

		InMemoryKantenRepository inMemoryKantenRepository = inMemoryKantenRepositoryFactory.create(
			boundingBoxFuerKanten, organisation);

		return matcheFeatureMapping(featureMapping, new MatchingStatistik(), inMemoryKantenRepository);
	}

	private FeatureMapping matcheFeatureMapping(FeatureMapping featureMapping,
		MatchingStatistik matchingStatistik,
		InMemoryKantenRepository inMemoryMatchingKantenRepository) {
		Optional<OsmMatchResult> osmMatchResult = simpleMatchingService.matche(
			(LineString) CoordinateReferenceSystemConverterUtility.transformGeometry(
				featureMapping.getImportedLineString(),
				KoordinatenReferenzSystem.ETRS89_UTM32_N), matchingStatistik);

		if (osmMatchResult.isPresent()) {
			List<Kante> matchingKanten = inMemoryMatchingKantenRepository.findKantenById(
				osmMatchResult.get().getOsmWayIds().stream().map(OsmWayId::getValue).collect(Collectors.toSet()));
			matchingKanten.stream()
				// Matches für die keine Kanten innerhalb der Organisation liegen rausfiltern
				.filter(Objects::nonNull)
				.filter(kante -> LineStrings.calculateUeberschneidungslinestring(kante.getGeometry(),
					osmMatchResult.get().getGeometrie()).isPresent())
				// sicherstellen, dass nur auf DLM oder RadVIS gemapped wird
				.filter(kante -> kante.getQuelle().equals(QuellSystem.DLM)
					|| kante.getQuelle().equals(QuellSystem.RadVis))
				.forEach(kante -> featureMapping.add(new MappedGrundnetzkante(kante.getGeometry(), kante.getId(),
					osmMatchResult.get().getGeometrie())));
		}

		return featureMapping;
	}
}
