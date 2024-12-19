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

package de.wps.radvis.backend.matching.domain.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import de.wps.radvis.backend.matching.domain.repository.GrundnetzKantenResolver;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;

public class GrundnetzMappingService {
	private final SimpleMatchingService simpleMatchingService;

	public GrundnetzMappingService(SimpleMatchingService simpleMatchingService) {
		this.simpleMatchingService = simpleMatchingService;
	}

	public List<MappedGrundnetzkante> mappeAufGrundnetz(LineString lineString, MatchingStatistik matchingStatistik,
		GrundnetzKantenResolver grundnetzKantenRepository) {
		Optional<OsmMatchResult> osmMatchResult = simpleMatchingService.matche(lineString, matchingStatistik);

		if (osmMatchResult.isPresent()) {
			List<Kante> matchingKanten = grundnetzKantenRepository.getKanten(
				osmMatchResult.get().getOsmWayIds().stream().map(OsmWayId::getValue).collect(Collectors.toSet()));
			return matchingKanten.stream()
				.filter(kante -> LineStrings.calculateUeberschneidungslinestring(kante.getGeometry(),
					osmMatchResult.get().getGeometrie()).isPresent())
				.map(kante -> new MappedGrundnetzkante(kante.getGeometry(), kante.getId(),
					osmMatchResult.get().getGeometrie()))
				.toList();
		}

		return Collections.emptyList();
	}
}
