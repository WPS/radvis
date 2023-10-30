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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class ZustaendigkeitsServiceTest {

	GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Mock
	OrganisationConfigurationProperties organisationConfigurationProperties;
	Benutzer benutzer;

	ZustaendigkeitsService service;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		when(organisationConfigurationProperties.getZustaendigkeitBufferInMeter()).thenReturn(500);
		service = new ZustaendigkeitsService(organisationConfigurationProperties);
	}

	@Test
	public void testeMultipolygonBufferWorksCorrectly() {
		MultiPolygon multiPolygonWithHoles = geometryFactory.createMultiPolygon(new Polygon[] {
			geometryFactory.createPolygon(geometryFactory.createLinearRing(
					new Coordinate[] { new Coordinate(10000, 10000), new Coordinate(10000, 20000),
						new Coordinate(20000, 20000), new Coordinate(20000, 10000), new Coordinate(10000, 10000) }),
				new LinearRing[] {
					geometryFactory.createLinearRing(
						new Coordinate[] { new Coordinate(13000, 13000), new Coordinate(13000, 17000),
							new Coordinate(17000, 17000), new Coordinate(17000, 13000),
							new Coordinate(13000, 13000) }) }
			),

			geometryFactory.createPolygon(geometryFactory.createLinearRing(
					new Coordinate[] { new Coordinate(30000, 10000), new Coordinate(30000, 20000),
						new Coordinate(40000, 20000), new Coordinate(40000, 10000),
						new Coordinate(30000, 10000) }),
				new LinearRing[] {
					geometryFactory.createLinearRing(
						new Coordinate[] { new Coordinate(33000, 13000), new Coordinate(33000, 17000),
							new Coordinate(37000, 17000), new Coordinate(37000, 13000),
							new Coordinate(33000, 13000) }) }
			)
		});

		benutzer.setOrganisation(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.bereich(multiPolygonWithHoles).id(10L).build());

		Kante kanteInHoleOutsideOfBuffer = KanteTestDataProvider.withDefaultValues().geometry(
			GeometryTestdataProvider.createLineString(new Coordinate(15000, 15000),
				new Coordinate(15010, 15010))).build();
		Kante kanteInHoleInBuffer = KanteTestDataProvider.withDefaultValues().geometry(
			GeometryTestdataProvider.createLineString(new Coordinate(13400, 13400),
				new Coordinate(13410, 13410))).build();

		Kante kanteZwischenPolygonsOutsideOfBuffer = KanteTestDataProvider.withDefaultValues().geometry(
			GeometryTestdataProvider.createLineString(new Coordinate(23000, 11000),
				new Coordinate(23010, 11010))).build();

		service.istImZustaendigkeitsbereich(kanteInHoleOutsideOfBuffer, benutzer);

		assertThat(service.istImZustaendigkeitsbereich(kanteInHoleOutsideOfBuffer, benutzer)).isFalse();
		assertThat(service.istImZustaendigkeitsbereich(kanteInHoleInBuffer, benutzer)).isTrue();
		assertThat(service.istImZustaendigkeitsbereich(kanteZwischenPolygonsOutsideOfBuffer, benutzer)).isFalse();
	}

}
