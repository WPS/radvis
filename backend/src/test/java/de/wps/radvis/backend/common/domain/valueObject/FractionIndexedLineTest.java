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

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;

class FractionIndexedLineTest {

	@Test
	public void testGetFractionAtIndex_wgs84() {
		LineString lineStringUtm32 = GeometryTestdataProvider.createLineString(
			GeometryTestdataProvider.moveAllToValidBounds(
				new Coordinate(100, 100),
				new Coordinate(200, 200),
				new Coordinate(300, 200),
				new Coordinate(300, 300)
			)
		);

		LineString lineStringWGS84 = (LineString) CoordinateReferenceSystemConverterUtility.transformGeometry(
			lineStringUtm32, KoordinatenReferenzSystem.WGS84);

		FractionIndexedLine fractionIndexedLine = new FractionIndexedLine(lineStringWGS84);

		assertThat(fractionIndexedLine.getFractionAtIndex(0)).isEqualTo(0, Offset.offset(0.0000001));
		assertThat(fractionIndexedLine.getFractionAtIndex(1)).isEqualTo(141.421356 / lineStringUtm32.getLength(),
			Offset.offset(0.0000001));
		assertThat(fractionIndexedLine.getFractionAtIndex(2)).isEqualTo(241.421356 / lineStringUtm32.getLength(),
			Offset.offset(0.0000001));
		assertThat(fractionIndexedLine.getFractionAtIndex(3)).isEqualTo(341.421356 / lineStringUtm32.getLength(),
			Offset.offset(0.0000001));

	}

	@Test
	public void testGetFractionAtIndex_utm32() {
		LineString lineStringUtm32 = GeometryTestdataProvider.createLineString(
			GeometryTestdataProvider.moveAllToValidBounds(
				new Coordinate(100, 100),
				new Coordinate(200, 200),
				new Coordinate(300, 200),
				new Coordinate(300, 300)
			)
		);

		FractionIndexedLine fractionIndexedLine = new FractionIndexedLine(lineStringUtm32);

		assertThat(fractionIndexedLine.getFractionAtIndex(0)).isEqualTo(0, Offset.offset(0.0000001));
		assertThat(fractionIndexedLine.getFractionAtIndex(1)).isEqualTo(141.421356 / lineStringUtm32.getLength(),
			Offset.offset(0.0000001));
		assertThat(fractionIndexedLine.getFractionAtIndex(2)).isEqualTo(241.421356 / lineStringUtm32.getLength(),
			Offset.offset(0.0000001));
		assertThat(fractionIndexedLine.getFractionAtIndex(3)).isEqualTo(341.421356 / lineStringUtm32.getLength(),
			Offset.offset(0.0000001));

	}

}