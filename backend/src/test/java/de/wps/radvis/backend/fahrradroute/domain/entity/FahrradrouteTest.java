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

package de.wps.radvis.backend.fahrradroute.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEvent;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteVarianteTestDataProvider;
import de.wps.radvis.backend.fahrradroute.domain.event.FahrradrouteCreatedEvent;
import de.wps.radvis.backend.fahrradroute.domain.event.FahrradrouteUpdatedEvent;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Tourenkategorie;
import de.wps.radvis.backend.matching.domain.repository.GraphhopperRoutingRepository;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class FahrradrouteTest {
	private MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;
	private ArgumentCaptor<RadVisDomainEvent> eventCaptor;

	@BeforeEach
	void setup() {
		eventCaptor = ArgumentCaptor.forClass(RadVisDomainEvent.class);
		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@Test
	void testNormalConstructor_shouldFireCreatedEvent() {
		// Act
		Fahrradroute fahrradroute = new Fahrradroute(
			FahrradrouteName.of("Cooooooler Name"),
			"Be|schrei|bung, die; Substantiv, feminin",
			Kategorie.LANDESRADFERNWEG,
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(),
			List.of(),
			GeometryTestdataProvider.createLineString(),
			GeometryTestdataProvider.createLineString(),
			new ArrayList<>(),
			GraphhopperRoutingRepository.DEFAULT_PROFILE_ID);

		// Assert
		assertThatCreatedEventFired(fahrradroute);
	}

	@Test
	void testFrontendConstructor_shouldFireCreatedEvent() {
		// Act
		Fahrradroute fahrradroute = new Fahrradroute(
			FahrradrouteName.of("Toller name"),
			"Tolle <b>BESCHREIBUNG</b>",
			Kategorie.LANDESRADFERNWEG,
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(),
			List.of(),
			KanteTestDataProvider.withDefaultValues().build().getGeometry(),
			KanteTestDataProvider.withDefaultValues().build().getGeometry(),
			new ArrayList<>(),
			GraphhopperRoutingRepository.DEFAULT_PROFILE_ID);

		// Assert
		assertThatCreatedEventFired(fahrradroute);
	}

	@Test
	void testToubizConstructor_shouldFireCreatedEvent() {
		// Act
		Fahrradroute fahrradroute = new Fahrradroute(ToubizId.of("123"),
			FahrradrouteTyp.RADVIS_ROUTE,
			FahrradrouteName.of("Toller name"),
			"Tolle <b>BESCHREIBUNG</b>",
			"tle krz beschrbng",
			"Tolle Info",
			Laenge.of(123),
			Tourenkategorie.GRAVEL_TOUR,
			Kategorie.LANDESRADFERNWEG,
			"https://tolle-homepage.de/diese-domain-ist-tatsächlich-noch-zu-haben",
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(),
			"mail@testRadvis.de",
			"TOL-LE License 2.0",
			LocalDateTime.now(),
			List.of(),
			"He who shall not be named",
			KanteTestDataProvider.withDefaultValues().build().getGeometry(),
			KnotenTestDataProvider.withDefaultValues().build().getPoint(),
			List.of(),
			KanteTestDataProvider.withDefaultValues().build().getGeometry(),
			List.of(),
			FahrradroutenMatchingAndRoutingInformation.builder().build());

		// Assert
		assertThatCreatedEventFired(fahrradroute);
	}

	@Test
	void testTfisConstructor_shouldFireCreatedEvent() {
		// Act
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues().buildTfisRoute();

		// Assert
		assertThatCreatedEventFired(fahrradroute);
	}

	@SuppressWarnings("unchecked")
	@Test
	void lrfwConstructor_defaultStuetzpunkte() {
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(10, 10))).build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(100, 10)))
			.build();
		Kante kante3 = KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(100, 10), new Coordinate(100, 100)))
			.build();
		// act
		Fahrradroute landesradfernweg = FahrradrouteTestDataProvider.withDefaultValues()
			.abschnittsweiserKantenBezug(List.of(
				new AbschnittsweiserKantenBezug(kante1, LinearReferenzierterAbschnitt.of(0.0, 1.0)),
				new AbschnittsweiserKantenBezug(kante2, LinearReferenzierterAbschnitt.of(0.0, 1.0)),
				new AbschnittsweiserKantenBezug(kante3, LinearReferenzierterAbschnitt.of(0.0, 1.0))))
			.netzbezugLineString(GeometryTestdataProvider.createLineString(
				new Coordinate(0, 0),
				new Coordinate(10, 10),
				new Coordinate(100, 10),
				new Coordinate(100, 100)))
			.buildLandesradfernweg();

		// assert
		assertThat(landesradfernweg.getStuetzpunkte()).isNotEmpty();
		assertThat(landesradfernweg.getStuetzpunkte().get()).isEqualTo(GeometryTestdataProvider
			.createLineString(
				new Coordinate(0, 0),
				new Coordinate(5, 5),
				new Coordinate(55, 10),
				new Coordinate(100, 55),
				new Coordinate(100, 100)));
	}

	@Test
	void lrfwConstructor_noLineString_noDefaultStuetzpunkte() {
		// act
		Fahrradroute landesradfernweg = FahrradrouteTestDataProvider.withDefaultValues().netzbezugLineString(null)
			.buildLandesradfernweg();

		// assert
		assertThat(landesradfernweg.getStuetzpunkte()).isEmpty();

	}

	@Test
	void testMerge_shouldFireUpdatedEvent() {
		// Arrange
		Fahrradroute fahrradroute1 = FahrradrouteTestDataProvider.withDefaultValues().build();
		Fahrradroute fahrradroute2 = FahrradrouteTestDataProvider.withDefaultValues().build();
		domainPublisherMock.reset();

		// Act
		fahrradroute1.merge(fahrradroute2);

		// Assert
		assertThatUpdatedEventFired(fahrradroute1);
	}

	@Test
	void removeKanteFromNetzbezug_dieKantenVonDerHauptrouteWirdEntfernt() {
		// arrange
		Kante kanteInHauptroute = KanteTestDataProvider.withDefaultValues().id(1L).build();

		Fahrradroute hauptroute = FahrradrouteTestDataProvider.onKante(kanteInHauptroute).id(42L)
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE).build();

		// act
		hauptroute.removeKantenFromNetzbezug(List.of(1L));

		// assert
		assertThat(hauptroute.getAbschnittsweiserKantenBezug()).isEmpty();
		assertDoesNotThrow(() -> hauptroute.removeKantenFromNetzbezug(List.of(404L)));
	}

	@Test
	void removeKanteFromNetzbezug_dieKantenVonDenVariantenWerdenAuchEntfernt() {
		// arrange
		// eine Kante in hauptroute
		Kante kanteInHauptroute = KanteTestDataProvider.withDefaultValues().id(1L).build();
		AbschnittsweiserKantenBezug hauptrouteKantenBezug = new AbschnittsweiserKantenBezug(kanteInHauptroute,
			LinearReferenzierterAbschnitt.of(0, 1));
		Fahrradroute fahrradroute = FahrradrouteTestDataProvider.onKante(kanteInHauptroute).id(1337L)
			.fahrradrouteTyp(FahrradrouteTyp.RADVIS_ROUTE).build();

		// zwei Varianten mit in Summe drei Kanten
		AbschnittsweiserKantenBezug varianteAKantenBezug1 = new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withDefaultValues().id(11L).build(), LinearReferenzierterAbschnitt.of(0, 1));
		AbschnittsweiserKantenBezug varianteAKantenBezug2 = new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withDefaultValues().id(12L).build(), LinearReferenzierterAbschnitt.of(0, 1));
		AbschnittsweiserKantenBezug varianteBKantenBezug = new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withDefaultValues().id(21L).build(), LinearReferenzierterAbschnitt.of(0, 1));
		List<AbschnittsweiserKantenBezug> bezuegeA = new ArrayList<>();
		bezuegeA.add(varianteAKantenBezug1);
		bezuegeA.add(varianteAKantenBezug2);
		List<AbschnittsweiserKantenBezug> bezuegeB = new ArrayList<>();
		bezuegeB.add(varianteBKantenBezug);
		fahrradroute.replaceFahrradrouteVarianten(List.of(FahrradrouteVarianteTestDataProvider.defaultTfis()
			.abschnittsweiserKantenBezug(bezuegeA).build(),
			FahrradrouteVarianteTestDataProvider.defaultTfis()
				.abschnittsweiserKantenBezug(bezuegeB).build()));

		// act
		// entferne die zweite Kante aus Variante A
		fahrradroute.removeKantenFromNetzbezug(List.of(12L));

		// assert
		assertThat(fahrradroute.getAbschnittsweiserKantenBezug()).containsExactly(hauptrouteKantenBezug);
		assertThat(fahrradroute.getVarianten()).extracting(FahrradrouteVariante::getAbschnittsweiserKantenBezug)
			.containsExactly(List.of(varianteAKantenBezug1), List.of(varianteBKantenBezug));
	}

	@Nested
	class ErsetzeKanteInNetzbezug {
		@Test
		void ersetzeKanteInNetzbezug() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante nichtZuErsetzendeKante = KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 100, 100, 100, QuellSystem.DLM)
				.id(2l)
				.build();
			Fahrradroute fahrradroute = FahrradrouteTestDataProvider.onKante(zuErsetzendeKante, nichtZuErsetzendeKante)
				.build();

			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 50, QuellSystem.DLM).id(3l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 50, 0, 100, QuellSystem.DLM).id(4l)
				.build();

			// act
			fahrradroute.ersetzeKanteInNetzbezug(zuErsetzendeKante, Set.of(ersatzKante1, ersatzKante2), 1.0);

			// assert
			assertThat(fahrradroute.getAbschnittsweiserKantenBezug()).extracting(a -> a.getKante())
				.containsExactlyInAnyOrder(nichtZuErsetzendeKante, ersatzKante1, ersatzKante2);
		}

		@Test
		void ersetzeKanteInNetzbezug_notContained_throws() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante nichtZuErsetzendeKante = KanteTestDataProvider
				.withCoordinatesAndQuelle(0, 100, 100, 100, QuellSystem.DLM)
				.id(2l)
				.build();
			Fahrradroute fahrradroute = FahrradrouteTestDataProvider.onKante(nichtZuErsetzendeKante)
				.build();

			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 50, QuellSystem.DLM).id(3l)
				.build();

			// act + assert
			assertThatThrownBy(
				() -> fahrradroute.ersetzeKanteInNetzbezug(zuErsetzendeKante, Set.of(ersatzKante1), 1.0));
		}

		@Test
		void ersetzeKanteInNetzbezug_lineareReferenzUebertragen() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.id(1l)
				.build();
			Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
				.abschnittsweiserKantenBezug(List
					.of(new AbschnittsweiserKantenBezug(zuErsetzendeKante, LinearReferenzierterAbschnitt.of(0.3, 0.7))))
				.build();

			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 50, QuellSystem.DLM).id(3l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 50, 0, 100, QuellSystem.DLM).id(4l)
				.build();

			// act
			fahrradroute.ersetzeKanteInNetzbezug(zuErsetzendeKante, Set.of(ersatzKante1, ersatzKante2), 1.0);

			// assert
			assertThat(fahrradroute.getAbschnittsweiserKantenBezug()).containsExactlyInAnyOrder(
				new AbschnittsweiserKantenBezug(ersatzKante1, LinearReferenzierterAbschnitt.of(0.6, 1)),
				new AbschnittsweiserKantenBezug(ersatzKante2, LinearReferenzierterAbschnitt.of(0, 0.4)));
		}

		@Test
		void ersetzeKanteInNetzbezug_lineStringChangeMoreThanTolerance_doesNothing() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 100, QuellSystem.DLM)
				.id(1l)
				.build();
			Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
				.abschnittsweiserKantenBezug(List
					.of(new AbschnittsweiserKantenBezug(zuErsetzendeKante, LinearReferenzierterAbschnitt.of(0, 1))))
				.build();

			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 0, 98, QuellSystem.DLM).id(3l)
				.build();

			// act
			fahrradroute.ersetzeKanteInNetzbezug(zuErsetzendeKante, Set.of(ersatzKante1), 1.0);

			// assert
			assertThat(fahrradroute.getAbschnittsweiserKantenBezug()).containsExactlyInAnyOrder(
				new AbschnittsweiserKantenBezug(zuErsetzendeKante, LinearReferenzierterAbschnitt.of(0, 1)));
		}

		@Test
		void ersetzeKanteInNetzbezug_doesNotUpdateLineString() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider
				.fromKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(1l)
						.build(),
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).id(2l)
						.build())
				.id(1l)
				.build();
			Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
				.abschnittsweiserKantenBezug(List
					.of(new AbschnittsweiserKantenBezug(zuErsetzendeKante, LinearReferenzierterAbschnitt.of(0, 1))))
				.netzbezugLineString(null)
				.build();

			Kante ersatzKante1 = KanteTestDataProvider.fromKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(3l)
					.build(),
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 50), QuellSystem.DLM).id(4l)
					.build())
				.id(3l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.fromKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 50), QuellSystem.DLM).id(4l)
					.build(),
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).id(5l)
					.build())
				.id(4l)
				.build();

			// act
			fahrradroute.ersetzeKanteInNetzbezug(zuErsetzendeKante, Set.of(ersatzKante1, ersatzKante2), 3.0);

			// assert
			assertThat(fahrradroute.getNetzbezugLineString()).isEmpty();
			assertThat(fahrradroute.getAbschnittsweiserKantenBezug()).containsExactlyInAnyOrder(
				new AbschnittsweiserKantenBezug(ersatzKante1, LinearReferenzierterAbschnitt.of(0, 1)),
				new AbschnittsweiserKantenBezug(ersatzKante2, LinearReferenzierterAbschnitt.of(0, 1)));
		}

		@Test
		void ersetzeKanteInNetzbezug_noZusammenhaengenderLineStringAfter_doesNothing() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider
				.fromKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(1l)
						.build(),
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).id(2l)
						.build())
				.id(1l)
				.build();
			Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
				.abschnittsweiserKantenBezug(List
					.of(new AbschnittsweiserKantenBezug(zuErsetzendeKante, LinearReferenzierterAbschnitt.of(0, 1))))
				.netzbezugLineString(zuErsetzendeKante.getGeometry())
				.build();

			// zwei Ersatzkanten, die topologisch nicht zusammenhängen
			Kante ersatzKante1 = KanteTestDataProvider.fromKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(3l)
					.build(),
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 50), QuellSystem.DLM).id(4l)
					.build())
				.id(3l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.fromKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 50), QuellSystem.DLM).id(5l)
					.build(),
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).id(6l)
					.build())
				.id(4l)
				.build();

			// act
			fahrradroute.ersetzeKanteInNetzbezug(zuErsetzendeKante, Set.of(ersatzKante1, ersatzKante2), 3.0);

			// assert
			assertThat(fahrradroute.getNetzbezugLineString()).isPresent();
			assertThat(fahrradroute.getAbschnittsweiserKantenBezug()).containsExactlyInAnyOrder(
				new AbschnittsweiserKantenBezug(zuErsetzendeKante, LinearReferenzierterAbschnitt.of(0, 1)));
		}

		@Test
		void ersetzeKanteInNetzbezug_noZusammenhaengenderLineStringBeforeAndAfter_updates() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider
				.fromKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(1l)
						.build(),
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).id(2l)
						.build())
				.id(1l)
				.build();
			Fahrradroute fahrradroute = FahrradrouteTestDataProvider.withDefaultValues()
				.abschnittsweiserKantenBezug(List
					.of(new AbschnittsweiserKantenBezug(zuErsetzendeKante, LinearReferenzierterAbschnitt.of(0, 1))))
				.netzbezugLineString(null)
				.build();

			Kante ersatzKante1 = KanteTestDataProvider.fromKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).id(3l)
					.build(),
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 50), QuellSystem.DLM).id(4l)
					.build())
				.id(3l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.fromKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 50), QuellSystem.DLM).id(5l)
					.build(),
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM).id(6l)
					.build())
				.id(4l)
				.build();

			// act
			fahrradroute.ersetzeKanteInNetzbezug(zuErsetzendeKante, Set.of(ersatzKante1, ersatzKante2), 3.0);

			// assert
			assertThat(fahrradroute.getNetzbezugLineString()).isEmpty();
			assertThat(fahrradroute.getAbschnittsweiserKantenBezug()).containsExactlyInAnyOrder(
				new AbschnittsweiserKantenBezug(ersatzKante1, LinearReferenzierterAbschnitt.of(0, 1)),
				new AbschnittsweiserKantenBezug(ersatzKante2, LinearReferenzierterAbschnitt.of(0, 1)));
		}

	}

	private void assertThatCreatedEventFired(Fahrradroute fahrradroute) {
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(eventCaptor.capture()), times(1));
		assertThat(eventCaptor.getValue()).isEqualTo(new FahrradrouteCreatedEvent(fahrradroute));
	}

	private void assertThatUpdatedEventFired(Fahrradroute fahrradroute) {
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(eventCaptor.capture()), times(1));
		assertThat(eventCaptor.getValue()).isEqualTo(new FahrradrouteUpdatedEvent(fahrradroute));
	}
}
