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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;

class TopologieTest {
	@Test
	void erstelleTopologieMap_eineKanteProKnoten() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.vonKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(10L).build())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).id(20L).build())
			.build();

		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.vonKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 20), QuellSystem.DLM).id(30L).build())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 30), QuellSystem.DLM).id(40L).build())
			.build();

		// act
		Map<Knoten, List<Kante>> knotenListMap = Topologie.erstelleTopologieMapAusKanten(List.of(kante1, kante2));

		// assert
		assertThat(knotenListMap).containsEntry(kante1.getVonKnoten(), List.of(kante1));
		assertThat(knotenListMap).containsEntry(kante1.getNachKnoten(), List.of(kante1));
		assertThat(knotenListMap).containsEntry(kante2.getVonKnoten(), List.of(kante2));
		assertThat(knotenListMap).containsEntry(kante2.getNachKnoten(), List.of(kante2));
	}

	@Test
	void erstelleTopologieMap_zweiKantenProKnoten() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.vonKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(10L).build())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).id(20L).build())
			.build();

		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.vonKnoten(
				kante1.getVonKnoten())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 30), QuellSystem.DLM).id(40L).build())
			.build();

		// act
		Map<Knoten, List<Kante>> knotenListMap = Topologie.erstelleTopologieMapAusKanten(List.of(kante1, kante2));

		// assert
		assertThat(knotenListMap).containsEntry(kante1.getVonKnoten(), List.of(kante1, kante2));
		assertThat(knotenListMap).containsEntry(kante1.getNachKnoten(), List.of(kante1));
		assertThat(knotenListMap).containsEntry(kante2.getVonKnoten(), List.of(kante1, kante2));
		assertThat(knotenListMap).containsEntry(kante2.getNachKnoten(), List.of(kante2));
	}
}