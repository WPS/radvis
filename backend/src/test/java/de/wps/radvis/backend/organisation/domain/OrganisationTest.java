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

package de.wps.radvis.backend.organisation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class OrganisationTest {
	@SuppressWarnings("unchecked")
	@Test
	public void getBereich_overlap() {
		MultiPolygon bereich1 = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		MultiPolygon bereich2 = GeometryTestdataProvider.createQuadratischerBereich(50, 50, 150, 150);
		Organisation organisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().zustaendigFuerBereichOf(
			Set.of(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1l).bereich(bereich1).build(),
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2l).bereich(bereich2).build()))
			.build();

		assertThat(organisation.getBereich()).isPresent();

		// wenn bereich1 zuerst genommen wird beim union
		Polygon polygonA = KoordinatenReferenzSystem.ETRS89_UTM32_N
			.getGeometryFactory().createPolygon(new Coordinate[] { new Coordinate(0, 0),
				new Coordinate(0, 100), new Coordinate(50, 100), new Coordinate(50, 150), new Coordinate(150, 150),
				new Coordinate(150, 50), new Coordinate(100, 50), new Coordinate(100, 0), new Coordinate(0, 0) });

		// wenn bereich2 zuerst genommen wird beim union
		Polygon polygonB = KoordinatenReferenzSystem.ETRS89_UTM32_N
			.getGeometryFactory().createPolygon(
				new Coordinate[] { new Coordinate(50, 100), new Coordinate(50, 150), new Coordinate(150, 150),
					new Coordinate(150, 50), new Coordinate(100, 50), new Coordinate(100, 0), new Coordinate(0, 0),
					new Coordinate(0, 100), new Coordinate(50, 100) });

		assertThat(organisation.getBereich().get().getGeometryN(0))
			.satisfiesAnyOf((p) -> assertThat(p).isEqualTo(polygonA), (p) -> assertThat(p).isEqualTo(polygonB));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getBereich_disjoint() {
		MultiPolygon bereich1 = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 40, 40);
		MultiPolygon bereich2 = GeometryTestdataProvider.createQuadratischerBereich(50, 50, 150, 150);
		Organisation organisation = VerwaltungseinheitTestDataProvider.defaultOrganisation().zustaendigFuerBereichOf(
			Set.of(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1l).bereich(bereich1).build(),
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2l).bereich(bereich2).build()))
			.build();

		assertThat(organisation.getBereich()).isPresent();

		assertThat(organisation.getBereich().get()).satisfiesAnyOf(mp -> {
			assertThat(((MultiPolygon) mp).getGeometryN(0)).isEqualTo(bereich1.getGeometryN(0));
			assertThat(((MultiPolygon) mp).getGeometryN(1)).isEqualTo(bereich2.getGeometryN(0));
		}, mp -> {
			assertThat(((MultiPolygon) mp).getGeometryN(0)).isEqualTo(bereich2.getGeometryN(0));
			assertThat(((MultiPolygon) mp).getGeometryN(1)).isEqualTo(bereich1.getGeometryN(0));
		});
	}

	@Test
	public void getBereich_empty() {
		Organisation organisation = VerwaltungseinheitTestDataProvider.defaultOrganisation()
			.zustaendigFuerBereichOf(Set.of()).build();
		assertThat(organisation.getBereich()).isEmpty();
	}
}
