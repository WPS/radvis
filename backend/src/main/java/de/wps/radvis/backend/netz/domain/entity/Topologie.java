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

package de.wps.radvis.backend.netz.domain.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Topologie {
	public static Map<Knoten, List<Kante>> erstelleTopologieMapAusKanten(Collection<Kante> alleKanten) {
		Map<Knoten, List<Kante>> topologieMap = new HashMap<>();
		alleKanten.forEach(kante -> {
			topologieMap.merge(kante.getVonKnoten(), new ArrayList<>(List.of(kante)), (existingList, newList) -> {
				existingList.addAll(newList);
				return existingList;
			});
			topologieMap.merge(kante.getNachKnoten(), new ArrayList<>(List.of(kante)), (existingList, newList) -> {
				existingList.addAll(newList);
				return existingList;
			});
		});
		return topologieMap;
	}

	public static Map<Knoten, List<StreckeVonKanten>> erstelleTopologieMapAusStrecken(
		Collection<StreckeVonKanten> alleStrecken) {
		Map<Knoten, List<StreckeVonKanten>> topologischeMap = new HashMap<>();
		alleStrecken.forEach(strecke -> {
			topologischeMap
				.merge(strecke.getVonKnoten(), new ArrayList<>(List.of(strecke)), (existingList, newList) -> {
					existingList.addAll(newList);
					return existingList;
				});
			topologischeMap
				.merge(strecke.getNachKnoten(), new ArrayList<>(List.of(strecke)), (existingList, newList) -> {
					existingList.addAll(newList);
					return existingList;
				});
		});
		return topologischeMap;
	}

	// TODO vielleicht woanders hin verschieben
	public static void updateStreckenEndenStatus(List<StreckeVonKanten> strecken,
		Map<Knoten, List<StreckeVonKanten>> topologischeMap) {
		for (StreckeVonKanten strecke : strecken) {
			boolean vonKnotenGrad1 = topologischeMap.get(strecke.getVonKnoten()).size() == 1;
			strecke.setVonKnotenEndpunkt(vonKnotenGrad1);
			boolean nachKnotenGrad1 = topologischeMap.get(strecke.getNachKnoten()).size() == 1;
			strecke.setNachKnotenEndpunkt(nachKnotenGrad1);
		}
	}
}
