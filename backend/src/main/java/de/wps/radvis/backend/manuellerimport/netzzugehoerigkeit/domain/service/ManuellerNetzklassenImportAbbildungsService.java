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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.manuellerimport.common.FortschrittLogger;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryFactory;
import de.wps.radvis.backend.manuellerimport.common.domain.service.AbstractManuellerImportAbbildungsService;
import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
public class ManuellerNetzklassenImportAbbildungsService extends AbstractManuellerImportAbbildungsService {

	public static final double MIN_OVERLAP_FRACTION = 0.1;

	private final SimpleMatchingService simpleMatchingService;

	private final InMemoryKantenRepositoryFactory ueberschneidungsRepositoryFactory;

	public ManuellerNetzklassenImportAbbildungsService(
		SimpleMatchingService simpleMatchingService,
		InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory) {
		this.simpleMatchingService = simpleMatchingService;
		this.ueberschneidungsRepositoryFactory = inMemoryKantenRepositoryFactory;
	}

	public MatchingErgebnis findKantenFromLineStrings(Set<LineString> importedLineStrings,
		Verwaltungseinheit organisation) {
		MatchingStatistik statistik = new MatchingStatistik();
		InMemoryKantenRepository ueberschneidungsRepository = ueberschneidungsRepositoryFactory.create(
			organisation.getBereich()
				.orElse(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createMultiPolygon()));
		log.info("Führe Matching auf DLM für {} LineStrings aus...", importedLineStrings.size());
		AtomicInteger count = new AtomicInteger(0);

		Set<LineString> nichtImportierteLineStrings = new HashSet<>();
		Set<Long> matches = new HashSet<>();
		importedLineStrings.forEach(lineString -> {
			FortschrittLogger.logProgressInPercent(importedLineStrings.size(), count, 4);

			Optional<OsmMatchResult> result = simpleMatchingService.matche(lineString, statistik);

			if (result.isEmpty()) {
				nichtImportierteLineStrings.add(lineString);
				return;
			}

			List<Long> matchedKantenIDs = ueberschneidungsRepository.findKantenById(
				result.get().getOsmWayIds().stream().map(
					OsmWayId::getValue).collect(Collectors.toSet()))
				.stream()
				// Matches für die keine Kanten innerhalb der Organisation liegen rausfiltern
				.filter(Objects::nonNull)
				.filter(kante -> kante
					.getUeberschneidunsanteilWith(result.get().getGeometrie()) > MIN_OVERLAP_FRACTION)
				.map(Kante::getId)
				.collect(Collectors.toList());

			if (matchedKantenIDs.isEmpty()) {
				nichtImportierteLineStrings.add(lineString);
				return;
			}

			matches.addAll(matchedKantenIDs);
		});

		log.info("Matching mit folgendem Ergebnis beendet:");
		log.info(statistik.toString());

		log.info("Ermittle Kanten für {} UeberschneidungsLineStrings:", matches.size());
		return new MatchingErgebnis(matches, nichtImportierteLineStrings);
	}

	@AllArgsConstructor
	public static class MatchingErgebnis {
		public Set<Long> matchedKanten;
		public Set<LineString> nichtGematchteLineStrings;
	}
}
