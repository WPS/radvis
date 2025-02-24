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

package de.wps.radvis.backend.manuellerimport.common.domain.repository;

import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class InMemoryKantenRepositoryFactory {
	private final KantenRepository kantenRepository;

	public InMemoryKantenRepository create(MultiPolygon bereich) {
		log.info("Hole Kanten des Bereichs aus der DB");
		Set<Kante> kanten = kantenRepository.getKantenInBereich(bereich);
		log.info("... {} DLM-Kanten aus der DB geholt", kanten.size());
		return new InMemoryKantenRepository(kanten);
	}

	public InMemoryKantenRepository create(Envelope envelope, MultiPolygon bereich) {
		Set<Kante> kanten = kantenRepository.getKantenInBereich(envelope);
		return new InMemoryKantenRepository(kanten.stream()
			.filter(kante -> bereich.intersects(kante.getGeometry()))
			.collect(Collectors.toSet()));
	}
}
