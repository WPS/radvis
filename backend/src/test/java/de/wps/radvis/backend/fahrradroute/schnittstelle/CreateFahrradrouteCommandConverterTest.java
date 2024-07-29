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

package de.wps.radvis.backend.fahrradroute.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisherSensitiveTest;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import lombok.Getter;
import lombok.Setter;

public class CreateFahrradrouteCommandConverterTest implements RadVisDomainEventPublisherSensitiveTest {

	@Mock
	private BenutzerResolver benutzerResolver;
	@Mock
	private KanteResolver kanteResolver;

	@Getter
	@Setter
	private MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	private CreateFahrradrouteCommandConverter commandConverter;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		commandConverter = new CreateFahrradrouteCommandConverter(benutzerResolver, kanteResolver);
	}

	@Test
	public void convert() {
		// Arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(0, 1)))
			.build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(0, 1), new Coordinate(0, 2)))
			.build();

		when(kanteResolver.getKante(kante1.getId())).thenReturn(kante1);
		when(kanteResolver.getKante(kante2.getId())).thenReturn(kante2);

		LineString stuetzpunkteGeometry = GeometryTestdataProvider.createLineString(
			kante1.getGeometry().getCoordinates()[0],
			kante1.getGeometry().getCoordinates()[1], kante2.getGeometry().getCoordinates()[1]);

		CreateFahrradrouteCommand command = new CreateFahrradrouteCommand(
			"name",
			"beschreibung",
			Kategorie.LANDESRADFERNWEG,
			stuetzpunkteGeometry,
			List.of(kante1.getId(), kante2.getId()),
			(org.geojson.LineString) GeoJsonConverter.createGeoJsonGeometry(stuetzpunkteGeometry),
			List.of(
				new LinearReferenzierteProfilEigenschaftenCommand(BelagArt.ASPHALT, Radverkehrsfuehrung.SCHUTZSTREIFEN,
					0, 1)),
			GraphhopperRoutingRepository.DEFAULT_PROFILE_ID);

		Authentication authentication = Mockito.mock(Authentication.class);

		when(benutzerResolver.fromAuthentication(authentication))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().build());

		// Act
		Fahrradroute fahrradroute = commandConverter.convert(authentication, command);

		// Assert
		assertThat(fahrradroute.getName().getName()).isEqualTo(command.getName());
		assertThat(fahrradroute.getBeschreibung()).isEqualTo(command.getBeschreibung());
		assertThat(fahrradroute.getKategorie()).isEqualTo(command.getKategorie());

		assertThat(fahrradroute.getStuetzpunkte()).contains(command.getStuetzpunkte());
		assertThat(fahrradroute.getAbschnittsweiserKantenBezug()).hasSize(2);
		assertThat(fahrradroute.getAbschnittsweiserKantenBezug().get(0).getKante()).isEqualTo(kante1);
		assertThat(
			fahrradroute.getAbschnittsweiserKantenBezug().get(0).getLinearReferenzierterAbschnitt().getVonValue())
				.isEqualTo(0);
		assertThat(
			fahrradroute.getAbschnittsweiserKantenBezug().get(0).getLinearReferenzierterAbschnitt().getBisValue())
				.isEqualTo(1);
		assertThat(fahrradroute.getAbschnittsweiserKantenBezug().get(1).getKante()).isEqualTo(kante2);
		assertThat(
			fahrradroute.getAbschnittsweiserKantenBezug().get(1).getLinearReferenzierterAbschnitt().getVonValue())
				.isEqualTo(0);
		assertThat(
			fahrradroute.getAbschnittsweiserKantenBezug().get(1).getLinearReferenzierterAbschnitt().getBisValue())
				.isEqualTo(1);
		assertThat(fahrradroute.getNetzbezugLineString()).contains(stuetzpunkteGeometry);
		assertThat(fahrradroute.getLinearReferenzierteProfilEigenschaften()).containsExactly(
			new LinearReferenzierteProfilEigenschaften(
				FahrradrouteProfilEigenschaften.of(BelagArt.ASPHALT, Radverkehrsfuehrung.SCHUTZSTREIFEN),
				LinearReferenzierterAbschnitt.of(0, 1)));
	}
}
