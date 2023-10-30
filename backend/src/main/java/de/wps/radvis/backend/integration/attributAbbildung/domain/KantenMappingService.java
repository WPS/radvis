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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenMapping;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.MappedKante;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;

public class KantenMappingService {

	private final KantenMappingRepository kantenMappingRepository;
	private final KantenRepository kantenRepository;

	public KantenMappingService(KantenMappingRepository kantenMappingRepository, KantenRepository kantenRepository) {
		require(kantenMappingRepository, notNullValue());
		require(kantenRepository, notNullValue());
		this.kantenMappingRepository = kantenMappingRepository;
		this.kantenRepository = kantenRepository;
	}

	public KantenMapping getOrCreate(Long grundnetzKantenId, QuellSystem quellSystem) {
		Optional<KantenMapping> result = kantenMappingRepository
			.findByGrundnetzKantenIdAndQuellsystem(grundnetzKantenId, quellSystem);
		return result.orElse(new KantenMapping(grundnetzKantenId, quellSystem, new ArrayList<>()));
	}

	public KantenMapping save(KantenMapping kantenMapping) {
		return kantenMappingRepository.save(kantenMapping);
	}

	public List<Long> getDlmKanteIds(Long radNETZKanteId) {
		return kantenMappingRepository.getDlmKanteIds(radNETZKanteId);
	}

	public List<Long> findRadNETZKanteIds(Long dLMKanteId) {
		Optional<KantenMapping> kantenMapping = kantenMappingRepository
			.findByGrundnetzKantenIdAndQuellsystem(dLMKanteId, QuellSystem.RadNETZ);
		return kantenMapping
			.map(mapping -> mapping.getAbgebildeteKanten().stream().map(MappedKante::getKanteId)
				.collect(Collectors.toList()))
			.orElseGet(List::of);
	}

	public Map<Long, List<Long>> getZuordnungen(List<Long> kanteIds) {
		Iterable<KantenMapping> findAllById = kantenMappingRepository.findAllById(kanteIds);
		Map<Long, List<Long>> zuordnungen = new HashMap<>();
		findAllById.forEach(kantenMapping -> {
			List<Long> abgebildeteKanteIds = kantenMapping.getAbgebildeteKanten().stream().map(MappedKante::getKanteId)
				.collect(Collectors.toList());
			zuordnungen.put(kantenMapping.getGrundnetzKantenId(), abgebildeteKanteIds);
		});
		return zuordnungen;
	}

	/**
	 * Geht die Zuordnungen anhand der Grundnetzkanten durch und löscht die Zuordnungen zu den RadNetzKanten. Gibt
	 * anschließen die IDs der betroffenen Grudnnetzkanten zurück (damit z.B. die ehemals gemappten Attribute davon
	 * gelösch werden können)
	 *
	 * @param dlmnetzKanteId
	 * @return
	 */
	@Deprecated
	public void loescheZuordnungDlm(Long dlmnetzKanteId, QuellSystem quellSystem) {
		kantenRepository.findById(dlmnetzKanteId)
			.ifPresent(Kante::resetKantenAttributeAusserDLM);
		kantenMappingRepository.deleteByGrundnetzKantenIdAndQuellsystem(dlmnetzKanteId, quellSystem);
	}

}
