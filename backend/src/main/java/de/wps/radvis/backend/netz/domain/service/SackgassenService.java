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

package de.wps.radvis.backend.netz.domain.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.transaction.Transactional;

public class SackgassenService {

	private final NetzService netzService;
	private KantenRepository kantenRepository;

	public SackgassenService(
		NetzService netzService, KantenRepository kantenRepository) {
		require(netzService, notNullValue());
		this.netzService = netzService;
		this.kantenRepository = kantenRepository;
	}

	@Transactional
	public Set<Knoten> bestimmeSackgassenknoten(Set<Kante> kanten) {
		Set<Knoten> sackgassenKnoten = new HashSet<>();
		HashMap<Knoten, Integer> knotenAufGrad = new HashMap<>();

		kanten.forEach(kante -> mergeKnoten(knotenAufGrad, kante));

		knotenAufGrad.forEach((knoten, grad) -> {
			if (grad <= 1) {
				sackgassenKnoten.add(knoten);
			}
		});

		return sackgassenKnoten;
	}

	@Transactional
	public Set<Knoten> bestimmeSackgassenknotenVonKanteIdsInOrganisation(Set<Long> kanteIds,
		Verwaltungseinheit organisation) {
		Set<Kante> kanten = netzService.getKantenInOrganisationsbereichEagerFetchKnoten(organisation)
			.filter(kante -> kanteIds.contains(kante.getId())).collect(Collectors.toSet());

		Set<Knoten> bestimmeSackgassenknoten = bestimmeSackgassenknoten(kanten);

		return bestimmeSackgassenknoten.stream()
			.filter(knoten -> organisation.getBereich().map(geo -> geo.contains(knoten.getPoint())).orElse(false))
			.collect(Collectors.toSet());
	}

	@Transactional
	public Set<Knoten> bestimmeSackgassenknotenVonKantenFuerNetzklasse(
		Set<Netzklasse> netzklassen) {
		return bestimmeSackgassenknoten(
			new HashSet<>(kantenRepository.getKantenForNetzklassenEagerFetchKnoten(netzklassen)));
	}

	private void mergeKnoten(Map<Knoten, Integer> knotenAufGrad, Kante kante) {
		Knoten von = kante.getVonKnoten();
		Knoten nach = kante.getNachKnoten();

		knotenAufGrad.merge(von, 1, Integer::sum);
		knotenAufGrad.merge(nach, 1, Integer::sum);
	}
}
