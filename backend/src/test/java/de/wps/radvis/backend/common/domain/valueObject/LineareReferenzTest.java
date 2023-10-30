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

package de.wps.radvis.backend.common.domain.valueObject;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.GeometryTestdataProvider;

class LineareReferenzTest {

	@Test
	void testeProjektion_simple() {
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(10, 10),
			new Coordinate(15, 10), new Coordinate(20, 10));

		LineareReferenz result = LineareReferenz.of(lineString, new Coordinate(17, 14));

		assertThat(result.getAbschnittsmarke()).isCloseTo(0.7, Offset.offset(0.001));
	}

	@Test
	void testeProjektion() {
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(10, 10),
			new Coordinate(15, 10), new Coordinate(20, 15));

		LineareReferenz result = LineareReferenz.of(lineString, new Coordinate(17, 14));

		assertThat(result.getAbschnittsmarke()).isCloseTo(0.75, Offset.offset(0.03));
	}
}