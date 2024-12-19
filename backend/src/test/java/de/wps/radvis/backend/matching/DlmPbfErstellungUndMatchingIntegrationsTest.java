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

package de.wps.radvis.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.DlmMatchedGraphhopperTestdataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlmPbfErstellungUndMatchingIntegrationsTest {

	@TempDir
	public File temp;

	@Test
	void test() throws KeinMatchGefundenException, IOException {
		List<Kante> kanten = List.of(
			KanteTestDataProvider.withCoordinatesAndQuelle(400000, 5000000, 400100, 5000100, QuellSystem.DLM)
				.id(111111L)
				.build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(400100, 5000100, 400200, 5000400, QuellSystem.DLM)
				.id(222222L)
				.build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(400150, 5000150, 400250, 5000450, QuellSystem.DLM)
				.id(333333L)
				.build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(400250, 5000450, 400000, 5000000, QuellSystem.DLM)
				.id(444444L)
				.build()
		);

		DlmMatchingRepository dlmMatchingRepository = DlmMatchedGraphhopperTestdataProvider.reimportWithKanten(
			List.of(kanten), "testpbfundmatching", temp);

		OsmMatchResult result = dlmMatchingRepository.matchGeometry(
			GeometryTestdataProvider.createLineString(new Coordinate(400052, 5000050), new Coordinate(400082, 5000080)),
			"bike");

		assertThat(result.getOsmWayIds().stream().map(OsmWayId::getValue)).containsExactly(111111L);

		assertThat(result.getGeometrie().getCoordinates()).usingComparatorForType(
			GeometryTestdataProvider.LENIENT_COORDINATE_COMPARATOR, Coordinate.class
		).containsExactly(new Coordinate(400050, 5000050), new Coordinate(400080, 5000080));

		assertThatThrownBy(() -> dlmMatchingRepository.matchGeometry(
			GeometryTestdataProvider.createLineString(new Coordinate(400000, 5000000), new Coordinate(400100, 5000000)),
			"bike")).isInstanceOf(KeinMatchGefundenException.class);

		OsmMatchResult result2 = dlmMatchingRepository.matchGeometry(
			GeometryTestdataProvider.createLineString(new Coordinate(400052, 5000050), new Coordinate(400102, 5000100),
				new Coordinate(400152, 5000250)), "bike");

		assertThat(result2.getOsmWayIds().stream().map(OsmWayId::getValue)).containsExactly(111111L, 222222L);
		assertThat(result2.getGeometrie().buffer(2).contains(
			GeometryTestdataProvider.createLineString(new Coordinate(400050, 5000050), new Coordinate(400100, 5000100),
				new Coordinate(400150, 5000250)))).isTrue();
	}
}
