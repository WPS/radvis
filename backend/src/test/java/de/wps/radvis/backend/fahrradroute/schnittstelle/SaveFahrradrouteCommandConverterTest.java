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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class SaveFahrradrouteCommandConverterTest {

	@Mock
	private VerwaltungseinheitResolver verwaltungseinheitResolver;

	@Mock
	private KanteResolver kanteResolver;

	SaveFahrradrouteCommandConverter commandConverter;

	MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		commandConverter = new SaveFahrradrouteCommandConverter(verwaltungseinheitResolver, kanteResolver);

		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@Test
	public void apply_radvisRoute() {
		// Arrange
		GeometryFactory etrs89Utm32NGeometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N
			.getGeometryFactory();
		LineString lineString = etrs89Utm32NGeometryFactory
			.createLineString(new Coordinate[] { new Coordinate(0, 1), new Coordinate(0, 2) });
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.geometry(lineString)
			.build();
		when(kanteResolver.getKanten(Set.of(1L))).thenReturn(List.of(kante));

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.id(1L)
			.build();

		SaveFahrradrouteVarianteCommand varianteCommand = SaveFahrradrouteVarianteCommandTestDataProvider
			.withDefaultValue()
			.kantenIDs(List.of(1L))
			.stuetzpunkte(lineString)
			.geometrie((org.geojson.LineString) GeoJsonConverter.createGeoJsonGeometry(lineString))
			.build();

		LineString commandLineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(20, 20),
			new Coordinate(100, 100));

		SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue()
			.id(fahrradroute.getId())
			.version(fahrradroute.getVersion())
			.routenVerlauf((org.geojson.LineString) GeoJsonConverter.createGeoJsonGeometry(commandLineString))
			.varianten(List.of(varianteCommand))
			.build();

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(command.getVerantwortlichId())
			.build();
		when(verwaltungseinheitResolver.resolve(command.getVerantwortlichId())).thenReturn(organisation);

		// Act
		commandConverter.apply(command, fahrradroute);

		// Assert
		assertThat(fahrradroute.getName().getName()).isEqualTo(command.getName());
		assertThat(fahrradroute.getKurzbeschreibung()).isEqualTo(command.getKurzbeschreibung());
		assertThat(fahrradroute.getBeschreibung()).isEqualTo(command.getBeschreibung());
		assertThat(fahrradroute.getOffizielleLaenge().get()).isEqualTo(command.getOffizielleLaenge());
		assertThat(fahrradroute.getTourenkategorie()).isEqualTo(command.getTourenkategorie());
		assertThat(fahrradroute.getHomepage()).isEqualTo(command.getHomepage());
		assertThat(fahrradroute.getVerantwortlich()).isPresent();
		assertThat(fahrradroute.getVerantwortlich().get().getId()).isEqualTo(command.getVerantwortlichId());
		assertThat(fahrradroute.getEmailAnsprechpartner()).isEqualTo(command.getEmailAnsprechpartner());
		assertThat(fahrradroute.getLizenz()).isEqualTo(command.getLizenz());
		assertThat(fahrradroute.getLizenzNamensnennung()).isEqualTo(command.getLizenzNamensnennung());
		assertThat(fahrradroute.getVarianten()).hasSize(1);
		assertThat(fahrradroute.getStuetzpunkte()).contains(command.getStuetzpunkte());
		assertThat(fahrradroute.getNetzbezugLineString()).contains(commandLineString);
		assertThat(fahrradroute.getAbschnittsweiserKantenBezug().get(0))
			.isEqualTo(new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)));
	}

	@Test
	public void apply_LRFW() {
		// Arrange
		GeometryFactory etrs89Utm32NGeometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N
			.getGeometryFactory();
		LineString lineString = etrs89Utm32NGeometryFactory
			.createLineString(new Coordinate[] { new Coordinate(0, 1), new Coordinate(0, 2) });
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.geometry(lineString)
			.build();
		when(kanteResolver.getKanten(Set.of(1L))).thenReturn(List.of(kante));

		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.id(1L)
			.build();

		SaveFahrradrouteVarianteCommand varianteCommand = SaveFahrradrouteVarianteCommandTestDataProvider
			.withDefaultValue()
			.kantenIDs(List.of(1L))
			.stuetzpunkte(lineString)
			.geometrie((org.geojson.LineString) GeoJsonConverter.createGeoJsonGeometry(lineString))
			.build();

		SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue()
			.id(fahrradroute.getId())
			.version(fahrradroute.getVersion())
			.varianten(List.of(varianteCommand))
			.build();

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(command.getVerantwortlichId())
			.build();
		when(verwaltungseinheitResolver.resolve(command.getVerantwortlichId())).thenReturn(organisation);

		// Act
		commandConverter.apply(command, fahrradroute);

		// Assert
		assertThat(fahrradroute.getVarianten()).hasSize(1);
		assertThat(fahrradroute.getStuetzpunkte()).contains(command.getStuetzpunkte());
		assertThat(fahrradroute.getNetzbezugLineString())
			.contains(GeoJsonConverter.create3DJtsLineStringFromGeoJson(command.getRoutenVerlauf(),
				KoordinatenReferenzSystem.ETRS89_UTM32_N));
		assertThat(fahrradroute.getAbschnittsweiserKantenBezug().get(0))
			.isEqualTo(new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)));
	}

	@Test
	public void apply_with_toubizId_allowed() {
		// Arrange
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.id(1L)
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.toubizId(ToubizId.of("originaleToubizId"))
			.build();

		SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue()
			.id(fahrradroute.getId())
			.version(fahrradroute.getVersion())
			.toubizId(ToubizId.of("neueToubizId"))
			.kategorie(Kategorie.LANDESRADFERNWEG)
			.build();
		when(kanteResolver.getKanten(Set.of(command.getId()))).thenReturn(
			List.of(KanteTestDataProvider.withDefaultValues().id(1L).build()));

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(command.getVerantwortlichId())
			.build();
		when(verwaltungseinheitResolver.resolve(command.getVerantwortlichId())).thenReturn(organisation);

		// Act
		commandConverter.apply(command, fahrradroute);

		// Assert
		assertThat(fahrradroute.getToubizId()).isEqualTo(ToubizId.of("neueToubizId"));
	}

	@Test
	public void apply_withToubizIdNotAllowedButSet_shouldThrowRequireViolation() {
		// Arrange
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
			.id(1L)
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE)
			.toubizId(ToubizId.of("originaleToubizId"))
			.build();

		Kante kante = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.build();
		when(kanteResolver.getKanten(Set.of(1L))).thenReturn(List.of(kante));

		ToubizId originaleToubizId = fahrradroute.getToubizId();

		SaveFahrradrouteCommand command = SaveFahrradrouteCommandTestDataProvider.withDefaultValue()
			.id(fahrradroute.getId())
			.version(fahrradroute.getVersion())
			.toubizId(ToubizId.of("neueToubizId"))
			.build();

		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.id(command.getVerantwortlichId())
			.build();
		when(verwaltungseinheitResolver.resolve(command.getVerantwortlichId())).thenReturn(organisation);

		// Act & Assert
		assertThatExceptionOfType(RequireViolation.class).isThrownBy(
			() -> commandConverter.apply(command, fahrradroute));
	}

}
