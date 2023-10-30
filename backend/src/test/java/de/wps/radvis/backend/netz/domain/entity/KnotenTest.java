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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;

class KnotenTest {

	@Test
	public void getKoordinate() {
		// arrange
		Coordinate koordinate = new Coordinate(10, 10, 0);
		Knoten knoten = KnotenTestDataProvider.withCoordinateAndQuelle(koordinate, QuellSystem.LGL).build();

		// act
		Coordinate result = knoten.getKoordinate();

		// assert
		assertEquals(koordinate, result);
	}

	@Test
	public void getQuelle() {
		// arrange
		QuellSystem quelle = QuellSystem.LGL;
		Coordinate koordinate = new Coordinate(10, 10, 0);
		Knoten knoten = KnotenTestDataProvider.withCoordinateAndQuelle(koordinate, quelle).build();

		// act
		QuellSystem result = knoten.getQuelle();

		// assert
		assertEquals(quelle, result);
	}
}