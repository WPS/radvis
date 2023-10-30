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

package de.wps.radvis.backend.fahrradroute.schnittstelle.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.mockito.MockedStatic;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisherSensitiveTest;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation;
import lombok.Getter;
import lombok.Setter;

class FahrradrouteDetailViewTest implements RadVisDomainEventPublisherSensitiveTest {

	@Getter
	@Setter
	MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@Test
	public void fahrradrouteDetailView_kehrtwenden_empty() {
		// arrange
		MultiPoint kehrtwenden = GeometryTestdataProvider.createMultiPoint();
		MultiLineString abweichendeSegmente = null;
		FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation = new FahrradroutenMatchingAndRoutingInformation(
			null, kehrtwenden, abweichendeSegmente, false);

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.id(1L)
			.fahrradroutenMatchingAndRoutingInformation(fahrradroutenMatchingAndRoutingInformation)
			.build();

		// act
		FahrradrouteDetailView fahrradrouteDetailView = new FahrradrouteDetailView(fahrradroute, false, false);

		// assert
		assertThat(fahrradrouteDetailView.getKehrtwenden()).isEmpty();
	}

	@Test
	public void fahrradrouteDetailView_kehrtwenden() {
		// arrange
		MultiPoint kehrtwenden = GeometryTestdataProvider.createMultiPoint(
			new Coordinate(0, 0),
			new Coordinate(10, 10));
		MultiLineString abweichendeSegmente = null;
		FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation = new FahrradroutenMatchingAndRoutingInformation(
			null, kehrtwenden, abweichendeSegmente, false);

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.id(1L)
			.fahrradroutenMatchingAndRoutingInformation(fahrradroutenMatchingAndRoutingInformation)
			.build();

		// act
		FahrradrouteDetailView fahrradrouteDetailView = new FahrradrouteDetailView(fahrradroute, false, false);

		// assert
		assertThat(fahrradrouteDetailView.getKehrtwenden()).containsExactlyInAnyOrder(
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(0, 0)),
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(10, 10)));
	}
}