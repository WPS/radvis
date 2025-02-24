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

package de.wps.radvis.backend.common.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.GeometryTestdataProvider;

class FahrradrouteFilterTest {

	@Test
	void contains_doesNotUseSimpleBoundingBox() {
		FahrradrouteFilter fahrradrouteFilter = new FahrradrouteFilter(
			List.of(GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
				new Coordinate(100, 0), new Coordinate(100, 100))),
			10);

		assertThat(fahrradrouteFilter.contains(GeometryTestdataProvider.createPoint(new Coordinate(50, 50)))).isFalse();
	}

	@Test
	void contains_withinAbstand() {
		FahrradrouteFilter fahrradrouteFilter = new FahrradrouteFilter(
			List.of(GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
				new Coordinate(100, 0))),
			10);

		assertThat(fahrradrouteFilter.contains(GeometryTestdataProvider.createPoint(new Coordinate(0, 10)))).isTrue();
		assertThat(fahrradrouteFilter.contains(GeometryTestdataProvider.createPoint(new Coordinate(0, 11)))).isFalse();
	}

	@Test
	void contains_linstring() {
		FahrradrouteFilter fahrradrouteFilter = new FahrradrouteFilter(
			List.of(GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
				new Coordinate(100, 0))),
			10);

		// one point inside
		assertThat(fahrradrouteFilter
			.contains(GeometryTestdataProvider.createLineString(new Coordinate(0, 100), new Coordinate(0, 10),
				new Coordinate(100, 100))))
					.isTrue();
		// orthogonal - no point inside
		assertThat(fahrradrouteFilter
			.contains(GeometryTestdataProvider.createLineString(new Coordinate(50, 100), new Coordinate(50, -100))))
				.isTrue();
		// parallel, outside
		assertThat(fahrradrouteFilter
			.contains(GeometryTestdataProvider.createLineString(new Coordinate(0, 11), new Coordinate(100, 11))))
				.isFalse();
	}

	@Test
	void contains_collection() {
		FahrradrouteFilter fahrradrouteFilter = new FahrradrouteFilter(
			List.of(GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
				new Coordinate(100, 0))),
			10);

		// linestring inside
		assertThat(fahrradrouteFilter
			.contains(GeometryTestdataProvider.creatGeometryCollection(
				GeometryTestdataProvider.createLineString(new Coordinate(0, 100), new Coordinate(0, 10),
					new Coordinate(100, 100)),
				GeometryTestdataProvider.createPoint(new Coordinate(0, 50)))))
					.isTrue();
		// point inside
		assertThat(fahrradrouteFilter
			.contains(GeometryTestdataProvider.creatGeometryCollection(
				GeometryTestdataProvider.createLineString(new Coordinate(0, 100),
					new Coordinate(100, 100)),
				GeometryTestdataProvider.createPoint(new Coordinate(0, 10)))))
					.isTrue();
		// both outside
		assertThat(fahrradrouteFilter
			.contains(GeometryTestdataProvider.creatGeometryCollection(
				GeometryTestdataProvider.createLineString(new Coordinate(0, 100), new Coordinate(100, 100)),
				GeometryTestdataProvider.createPoint(new Coordinate(0, 50)))))
					.isFalse();
	}

	@Test
	void contains_endOfFahrradroute() {
		FahrradrouteFilter fahrradrouteFilter = new FahrradrouteFilter(
			List.of(GeometryTestdataProvider.createLineString(new Coordinate(11, 0),
				new Coordinate(100, 0))),
			10);

		assertThat(fahrradrouteFilter.contains(GeometryTestdataProvider.createPoint(new Coordinate(1, 0)))).isTrue();
		assertThat(fahrradrouteFilter.contains(GeometryTestdataProvider.createPoint(new Coordinate(0, 0)))).isFalse();
	}
}
