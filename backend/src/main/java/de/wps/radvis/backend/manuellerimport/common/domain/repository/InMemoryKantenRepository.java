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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Configurable;

import de.wps.radvis.backend.netz.domain.entity.Kante;

@Configurable
public class InMemoryKantenRepository {

	private final Map<Long, Kante> idToKante;

	InMemoryKantenRepository(Set<Kante> kanten) {
		idToKante = new HashMap<>();
		kanten.forEach(kante -> idToKante.put(kante.getId(), kante));
	}

	public List<Kante> findKantenById(Set<Long> osmWayIds) {
		return osmWayIds.stream().map(idToKante::get).collect(Collectors.toList());
	}

	public Optional<Kante> findKanteById(Long kanteId) {
		return Optional.ofNullable(idToKante.get(kanteId));
	}
}
