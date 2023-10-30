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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.NetzklassenStreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.StreckenEinerPartition;
import lombok.NonNull;

public class NetzklassenStreckenViewService extends StreckenViewAbstractService<NetzklassenStreckeVonKanten> {

	@Override
	public StreckenEinerPartition<NetzklassenStreckeVonKanten> mergeUnvollstaendigeStrecken(
		List<NetzklassenStreckeVonKanten> unvollstaendig) {
		Map<Knoten, List<NetzklassenStreckeVonKanten>> topologischeMap = new HashMap<>();

		StreckenEinerPartition<NetzklassenStreckeVonKanten> result = new StreckenEinerPartition<>();

		unvollstaendig.forEach(strecke -> {
			if (!strecke.isVonKnotenEndpunkt()) {
				topologischeMap
					.merge(strecke.getVonKnoten(), new ArrayList<>(List.of(strecke)), (existingList, newList) -> {
						existingList.addAll(newList);
						return existingList;
					});
			}
			if (!strecke.isNachKnotenEndpunkt()) {
				topologischeMap
					.merge(strecke.getNachKnoten(), new ArrayList<>(List.of(strecke)), (existingList, newList) -> {
						existingList.addAll(newList);
						return existingList;
					});
			}
		});

		topologischeMap.forEach(((knoten, strecken) -> {
			if (strecken.size() != 2) {
				return;
			}

			NetzklassenStreckeVonKanten strecke1 = strecken.get(0);
			NetzklassenStreckeVonKanten strecke2 = strecken.get(1);
			strecken.remove(strecke1);
			strecken.remove(strecke2);

			if (strecke1.passtAnStreckeRan(strecke2)) {

				strecke1.merge(strecke2);

				if (!strecke1.isVonKnotenEndpunkt()) {
					List<NetzklassenStreckeVonKanten> vonKnotenStrecken = topologischeMap.get(strecke1.getVonKnoten());
					vonKnotenStrecken.remove(strecke2);
					if (!vonKnotenStrecken.contains(strecke1)) {
						vonKnotenStrecken.add(strecke1);
					}
				}
				if (!strecke1.isNachKnotenEndpunkt()) {
					List<NetzklassenStreckeVonKanten> nachKnotenStrecken = topologischeMap
						.get(strecke1.getNachKnoten());
					nachKnotenStrecken.remove(strecke2);
					if (!nachKnotenStrecken.contains(strecke1)) {
						nachKnotenStrecken.add(strecke1);
					}
				}
				if (strecke1.abgeschlossen()) {
					result.vollstaendig.add(strecke1);
				}
			} else {
				if (knoten.equals(strecke1.getVonKnoten())) {
					strecke1.setzeVonKnotenAlsEndknoten();
				} else {
					strecke1.setzeNachKnotenAlsEndknoten();
				}
				if (knoten.equals(strecke1.getVonKnoten())) {
					strecke2.setzeVonKnotenAlsEndknoten();
				} else {
					strecke2.setzeNachKnotenAlsEndknoten();
				}
				if (strecke1.abgeschlossen()) {
					result.vollstaendig.add(strecke1);
				}
				if (strecke2.abgeschlossen()) {
					result.vollstaendig.add(strecke2);
				}
			}
		}));

		result.unvollstaendig.addAll(topologischeMap.values().stream().flatMap(List::stream).distinct()
			.collect(Collectors.toList()));
		return result;
	}

	@Override
	protected void sucheBisEndpunktOderPartitionsende(
		Map<Knoten, List<Kante>> topologischeMap,
		Set<Long> bereitsEingeordnet, Set<Knoten> endpunkteVonStrecken,
		NetzklassenStreckeVonKanten netzklassenStreckeVonKanten, boolean rueckwaerts) {
		Optional<Kante> next = getNextKanteInRichtung(topologischeMap, netzklassenStreckeVonKanten, rueckwaerts);
		while (next.isPresent() && !bereitsEingeordnet.contains(next.get().getId())) {
			Kante nextKante = next.get();
			if (!netzklassenStreckeVonKanten.passtAnStreckeRan(nextKante)) {
				// mark node endnode on strecke
				if (rueckwaerts) {
					endpunkteVonStrecken.add(netzklassenStreckeVonKanten.getVonKnoten());
					netzklassenStreckeVonKanten.setzeVonKnotenAlsEndknoten();
				} else {
					endpunkteVonStrecken.add(netzklassenStreckeVonKanten.getNachKnoten());
					netzklassenStreckeVonKanten.setzeNachKnotenAlsEndknoten();
				}
				break;
			}

			netzklassenStreckeVonKanten.addKante(nextKante,
				endpunkteVonStrecken.contains(nextKante.getVonKnoten()) || endpunkteVonStrecken
					.contains(nextKante.getNachKnoten()));
			bereitsEingeordnet.add(nextKante.getId());
			next = getNextKanteInRichtung(topologischeMap, netzklassenStreckeVonKanten, rueckwaerts);
		}
	}

	@Override
	protected NetzklassenStreckeVonKanten createStreckeTyp(@NonNull Kante startKante, boolean vonKnotenEndpunkt,
		boolean nachKnotenEndpunkt) {
		return new NetzklassenStreckeVonKanten(startKante, vonKnotenEndpunkt, nachKnotenEndpunkt);
	}
}
