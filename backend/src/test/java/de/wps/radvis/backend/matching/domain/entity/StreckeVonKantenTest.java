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

package de.wps.radvis.backend.matching.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;

class StreckeVonKantenTest {

	GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Test
	void testeKonstruktor() {
		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		StreckeVonKanten streckeVonKanten = new StreckeVonKanten(kante);
		assertThat(streckeVonKanten.getStrecke().equals(kante.getGeometry())).isTrue();
		assertThat(streckeVonKanten.getVonKnoten()).isEqualTo(kante.getVonKnoten());
		assertThat(streckeVonKanten.getNachKnoten()).isEqualTo(kante.getNachKnoten());
	}

	@Test
	void testeAddKanteAmEndeAnhaengen() {
		// Arrange
		Kante baseKante = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(100, 100),
				new Coordinate(130, 135),
				new Coordinate(150, 150),
				new Coordinate(200, 150),
			}))
			.vonKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).id(1L)
					.build())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 150), QuellSystem.DLM).id(2L)
					.build())
			.build();
		StreckeVonKanten streckeVonKanten = new StreckeVonKanten(baseKante);
		StreckeVonKanten streckeVonKanten2 = new StreckeVonKanten(baseKante);

		Kante neueKante = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(201, 150),
				new Coordinate(250, 160),
				new Coordinate(300, 160),
			}))
			.vonKnoten(baseKante.getNachKnoten())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(300, 160), QuellSystem.DLM).id(3L)
					.build())
			.build();

		Kante neueKanteReversed = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.geometry(neueKante.getGeometry().reverse())
			.vonKnoten(neueKante.getNachKnoten())
			.nachKnoten(neueKante.getVonKnoten())
			.build();

		// act
		streckeVonKanten.addKante(neueKante, false);
		streckeVonKanten2.addKante(neueKanteReversed, false);

		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactly(
			new Coordinate(100, 100),
			new Coordinate(130, 135),
			new Coordinate(150, 150),
			new Coordinate(200, 150),
			new Coordinate(201, 150),
			new Coordinate(250, 160),
			new Coordinate(300, 160)
		);
		assertThat(streckeVonKanten.getStrecke().getLength())
			.isEqualTo(baseKante.getGeometry().getLength() + neueKante.getGeometry().getLength() + 1.);
		assertThat(streckeVonKanten.getVonKnoten()).isEqualTo(baseKante.getVonKnoten());
		assertThat(streckeVonKanten.getNachKnoten()).isEqualTo(neueKante.getNachKnoten());

		assertThat(streckeVonKanten2.getStrecke().getCoordinates())
			.containsExactlyElementsOf(
				Arrays.stream(streckeVonKanten.getStrecke().getCoordinates()).collect(Collectors.toList()));
		assertThat(streckeVonKanten2.getStrecke().getLength())
			.isEqualTo(baseKante.getGeometry().getLength() + neueKante.getGeometry().getLength() + 1.);
		assertThat(streckeVonKanten2.getVonKnoten()).isEqualTo(baseKante.getVonKnoten());
		assertThat(streckeVonKanten2.getNachKnoten()).isEqualTo(neueKante.getNachKnoten());
	}

	@Test
	void testeAddKanteAmAnfangVorhaengen() {
		// Arrange
		Kante baseKante = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(100, 100),
				new Coordinate(130, 135),
				new Coordinate(150, 150),
				new Coordinate(200, 150),
			}))
			.vonKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).id(1L)
					.build())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 150), QuellSystem.DLM).id(2L)
					.build())
			.build();
		StreckeVonKanten streckeVonKanten = new StreckeVonKanten(baseKante);
		StreckeVonKanten streckeVonKanten2 = new StreckeVonKanten(baseKante);

		Kante neueKante = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(20, 40),
				new Coordinate(80, 100),
				new Coordinate(99, 100),
			}))
			.nachKnoten(baseKante.getVonKnoten())
			.vonKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 40), QuellSystem.DLM).id(3L)
					.build())
			.build();

		Kante neueKanteReversed = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.geometry(neueKante.getGeometry().reverse())
			.vonKnoten(neueKante.getNachKnoten())
			.nachKnoten(neueKante.getVonKnoten())
			.build();

		// act
		streckeVonKanten.addKante(neueKante, false);
		streckeVonKanten2.addKante(neueKanteReversed, false);

		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactly(
			new Coordinate(20, 40),
			new Coordinate(80, 100),
			new Coordinate(99, 100),
			new Coordinate(100, 100),
			new Coordinate(130, 135),
			new Coordinate(150, 150),
			new Coordinate(200, 150)
		);
		assertThat(streckeVonKanten.getStrecke().getLength())
			.isEqualTo(baseKante.getGeometry().getLength() + neueKante.getGeometry().getLength() + 1.);
		assertThat(streckeVonKanten.getVonKnoten()).isEqualTo(neueKante.getVonKnoten());
		assertThat(streckeVonKanten.getNachKnoten()).isEqualTo(baseKante.getNachKnoten());

		assertThat(streckeVonKanten2.getStrecke().getCoordinates())
			.containsExactlyElementsOf(
				Arrays.stream(streckeVonKanten.getStrecke().getCoordinates()).collect(Collectors.toList()));
		assertThat(streckeVonKanten2.getStrecke().getLength())
			.isEqualTo(baseKante.getGeometry().getLength() + neueKante.getGeometry().getLength() + 1.);
		assertThat(streckeVonKanten2.getVonKnoten()).isEqualTo(neueKante.getVonKnoten());
		assertThat(streckeVonKanten2.getNachKnoten()).isEqualTo(baseKante.getNachKnoten());
	}

	@Test
	void testeMergeOrderAndAddOrder() {
		List<Coordinate[]> streckeCoordinates1 = new ArrayList<>();
		streckeCoordinates1.add(new Coordinate[] {
			new Coordinate(20, 20),
			new Coordinate(30, 30)
		});
		streckeCoordinates1.add(new Coordinate[] {
			new Coordinate(30, 30),
			new Coordinate(39, 49)
		});
		streckeCoordinates1.add(new Coordinate[] {
			new Coordinate(30, 30),
			new Coordinate(40, 50)
		});
		streckeCoordinates1.add(new Coordinate[] {
			new Coordinate(40, 50),
			new Coordinate(50, 50)
		});

		List<Coordinate[]> streckeCoordinates2 = new ArrayList<>();
		streckeCoordinates2.add(new Coordinate[] {
			new Coordinate(50, 50),
			new Coordinate(60, 60)
		});
		streckeCoordinates2.add(new Coordinate[] {
			new Coordinate(60, 60),
			new Coordinate(70, 70)
		});
		streckeCoordinates2.add(new Coordinate[] {
			new Coordinate(70, 70),
			new Coordinate(80, 80)
		});
		streckeCoordinates2.add(new Coordinate[] {
			new Coordinate(80, 80),
			new Coordinate(90, 90)
		});

		AtomicLong knotenId = new AtomicLong();
		AtomicLong kanteId = new AtomicLong();

		List<Kante> strecke1 = createStreckeUeberCoordinates(streckeCoordinates1, null, null, knotenId, kanteId);
		List<Kante> strecke2 = createStreckeUeberCoordinates(streckeCoordinates2,
			strecke1.get(strecke1.size() - 1).getNachKnoten(), null, knotenId, kanteId);

		// act
		StreckeVonKanten streckeVonKanten1 = new StreckeVonKanten(strecke1.get(1));
		streckeVonKanten1.addKante(strecke1.get(2), false);
		streckeVonKanten1.addKante(strecke1.get(0), false);
		streckeVonKanten1.addKante(strecke1.get(3), false);

		StreckeVonKanten streckeVonKanten2 = new StreckeVonKanten(strecke2.get(2));
		streckeVonKanten2.addKante(strecke2.get(3), false);
		streckeVonKanten2.addKante(strecke2.get(1), false);
		streckeVonKanten2.addKante(strecke2.get(0), false);

		StreckeVonKanten merged = new StreckeVonKanten(strecke1.get(1));
		merged.addKante(strecke1.get(2), false);
		merged.addKante(strecke1.get(0), false);
		merged.addKante(strecke1.get(3), false);

		merged.merge(streckeVonKanten2);

		StreckeVonKanten mergedReversed = new StreckeVonKanten(strecke2.get(2));
		mergedReversed.addKante(strecke2.get(3), false);
		mergedReversed.addKante(strecke2.get(1), false);
		mergedReversed.addKante(strecke2.get(0), false);

		mergedReversed.merge(streckeVonKanten1);

		// assert
		assertThat(streckeVonKanten1.getKanten()).containsExactlyElementsOf(strecke1);
		assertThat(streckeVonKanten2.getKanten()).containsExactlyElementsOf(strecke2);

		List<Kante> gesamtStrecke = Stream.concat(strecke1.stream(), strecke2.stream()).collect(Collectors.toList());
		assertThat(merged.getKanten()).containsExactlyElementsOf(gesamtStrecke);
		assertEqualGeometryInOrderOrReverseOrder(gesamtStrecke, merged);
		assertThat(mergedReversed.getKanten()).containsExactlyElementsOf(gesamtStrecke);
		assertEqualGeometryInOrderOrReverseOrder(gesamtStrecke, mergedReversed);
	}

	@Nested
	class WithStreckeVonKantenTest {
		Kante kante1;
		Kante kante2;
		Kante kante3;
		StreckeVonKanten streckeVonKanten;

		@BeforeEach
		void beforeEach() {
			kante1 = KanteTestDataProvider.withDefaultValues()
				.id(1L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(100, 100),
					new Coordinate(130, 135),
					new Coordinate(150, 150),
					new Coordinate(200, 150),
				}))
				.vonKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).id(1L)
						.build())
				.nachKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 150), QuellSystem.DLM).id(2L)
						.build())
				.build();
			kante2 = KanteTestDataProvider.withDefaultValues()
				.id(2L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(200, 150),
					new Coordinate(250, 200),
					new Coordinate(300, 200),
				}))
				.vonKnoten(kante1.getNachKnoten())
				.nachKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(300, 200), QuellSystem.DLM).id(3L)
						.build())
				.build();
			kante3 = KanteTestDataProvider.withDefaultValues()
				.id(3L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(300, 200),
					new Coordinate(350, 250),
					new Coordinate(400, 250),
				}))
				.vonKnoten(kante2.getNachKnoten())
				.nachKnoten(
					KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(400, 250), QuellSystem.DLM).id(4L)
						.build())
				.build();
			streckeVonKanten = new StreckeVonKanten(kante1);
			streckeVonKanten.addKante(kante2, false);
			streckeVonKanten.addKante(kante3, false);
		}

		@Test
		void updateKante() {
			// Arrange
			final var kante = KanteTestDataProvider.withDefaultValues()
				.id(kante2.getId())
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(200, 150),
					new Coordinate(50, 200),
					new Coordinate(300, 200),
				}))
				.vonKnoten(kante2.getVonKnoten())
				.nachKnoten(kante3.getNachKnoten())
				.build();

			// Act
			streckeVonKanten.updateKanteInStrecke(kante);

			// Assert
			final var zweiteKante = streckeVonKanten.getKanten().get(1);
			assertThat(zweiteKante).isEqualTo(kante);
			assertThat(streckeVonKanten.getStrecke().getCoordinates()).contains(new Coordinate(50, 200));
		}

		@Test
		void updateKante_kanteNichtBereitsInStrecke() {
			// Arrange
			final var kante = KanteTestDataProvider.withDefaultValues()
				.id(777L)
				.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
					new Coordinate(200, 150),
					new Coordinate(50, 200),
					new Coordinate(300, 200),
				}))
				.vonKnoten(kante2.getVonKnoten())
				.nachKnoten(kante3.getNachKnoten())
				.build();

			// Act + Assert
			assertThatThrownBy(() -> streckeVonKanten.updateKanteInStrecke(kante)).isInstanceOf(RequireViolation.class);

			// nichts hinzugefügt oder entfernt
			assertThat(streckeVonKanten.getKanten()).hasSize(3);
			assertThat(streckeVonKanten.getStrecke().getCoordinates()).doesNotContain(new Coordinate(50, 200));
		}

		@Test
		void splitAt_Beginning() {
			// Act
			final var result = streckeVonKanten.splitAt(kante1);

			// Assert
			assertThat(result).isEmpty();
			assertThat(streckeVonKanten.getKanten()).containsExactly(kante2, kante3);
			assertThat(streckeVonKanten.getStrecke().getCoordinates()).hasSize(6);

			List<Coordinate> expectedCoordinates = new ArrayList<>();
			expectedCoordinates.addAll(Arrays.asList(kante2.getGeometry().getCoordinates()));
			expectedCoordinates.addAll(Arrays.asList(kante3.getGeometry().getCoordinates()));
			assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyElementsOf(expectedCoordinates);
		}

		@Test
		void splitAt_End() {
			// Act
			final var result = streckeVonKanten.splitAt(kante3);

			// Assert
			assertThat(result).isEmpty();
			assertThat(streckeVonKanten.getKanten()).containsExactly(kante1, kante2);
			assertThat(streckeVonKanten.getStrecke().getCoordinates()).hasSize(7);
			List<Coordinate> expectedCoordinates = new ArrayList<>();
			expectedCoordinates.addAll(Arrays.asList(kante1.getGeometry().getCoordinates()));
			expectedCoordinates.addAll(Arrays.asList(kante2.getGeometry().getCoordinates()));
			assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyElementsOf(expectedCoordinates);
		}

		@Test
		void splitAt_Center() {
			// Act
			final var result = streckeVonKanten.splitAt(kante2);

			// Assert
			assertThat(result).containsExactly(kante3);
			assertThat(streckeVonKanten.getKanten()).containsExactly(kante1);
			assertThat(streckeVonKanten.getStrecke().getCoordinates()).hasSize(4);
			assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactly(
				kante1.getGeometry().getCoordinates());
		}
	}

	@Test
	void reverse() {
		// arrange
		Kante baseKante = KanteTestDataProvider.withDefaultValues()
			.id(1L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(100, 100),
				new Coordinate(130, 135),
				new Coordinate(150, 150),
				new Coordinate(200, 150),
			}))
			.vonKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM).id(1L)
					.build())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(200, 150), QuellSystem.DLM).id(2L)
					.build())
			.build();

		Kante neueKante = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(201, 150),
				new Coordinate(250, 160),
				new Coordinate(300, 160),
			}))
			.vonKnoten(baseKante.getNachKnoten())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(300, 160), QuellSystem.DLM).id(3L)
					.build())
			.build();

		StreckeVonKanten streckeVonKanten = new StreckeVonKanten(baseKante);
		streckeVonKanten.addKante(neueKante, true);

		// act
		StreckeVonKanten reversedStrecke = streckeVonKanten.reverse();

		// assert
		assertThat(reversedStrecke.getStrecke().getCoordinates())
			.isEqualTo(streckeVonKanten.getStrecke().reverse().getCoordinates());
		assertThat(reversedStrecke.getVonKnoten()).isEqualTo(streckeVonKanten.getNachKnoten());
		assertThat(reversedStrecke.getNachKnoten()).isEqualTo(streckeVonKanten.getVonKnoten());
		assertThat(reversedStrecke.isVonKnotenEndpunkt()).isTrue();
		assertThat(reversedStrecke.isNachKnotenEndpunkt()).isFalse();
	}

	private List<Kante> createStreckeUeberCoordinates(List<Coordinate[]> coordinatesDerKanten, Knoten startKnoten,
		Knoten endKnoten, AtomicLong currentKnotenId, AtomicLong currentKanteId) {

		Knoten vorherigerKnoten = KnotenTestDataProvider
			.withCoordinateAndQuelle(coordinatesDerKanten.get(0)[0], QuellSystem.DLM)
			.id(currentKnotenId.incrementAndGet()).build();

		if (startKnoten != null) {
			vorherigerKnoten = startKnoten;
		}

		List<Kante> result = new ArrayList<>();
		for (int i = 0, coordinatesDerKantenSize = coordinatesDerKanten.size(); i < coordinatesDerKantenSize; i++) {
			Coordinate[] coordinates = coordinatesDerKanten.get(i);

			Knoten nachKnoten = KnotenTestDataProvider
				.withCoordinateAndQuelle(coordinates[coordinates.length - 1], QuellSystem.DLM)
				.id(currentKnotenId.incrementAndGet()).build();

			if (i == coordinatesDerKantenSize - 1 && endKnoten != null) {
				nachKnoten = endKnoten;
			}

			Kante next = KanteTestDataProvider.withDefaultValues()
				.id(currentKanteId.incrementAndGet())
				.geometry(GEO_FACTORY.createLineString(coordinates))
				.vonKnoten(vorherigerKnoten)
				.nachKnoten(nachKnoten).build();
			vorherigerKnoten = next.getNachKnoten();

			result.add(next);
		}

		return result;
	}

	private void assertEqualGeometryInOrderOrReverseOrder(List<Kante> expectedStrecke,
		StreckeVonKanten actualStrecke) {

		List<Coordinate> expectedCoordinates = new ArrayList<>();
		for (Kante kante : expectedStrecke) {
			expectedCoordinates.addAll(Arrays.asList(kante.getGeometry().getCoordinates()));
		}

		// Aufeinanderfolgende doppelte Koordinaten rausfiltern
		AtomicReference<Coordinate> previous = new AtomicReference<>(null);
		expectedCoordinates = expectedCoordinates.stream()
			.filter(coordinate -> !coordinate.equals(previous.getAndSet(coordinate)))
			.collect(Collectors.toList());

		LineString expectedGeometry = GEO_FACTORY.createLineString(expectedCoordinates.toArray(Coordinate[]::new));

		isEqualInOrderOrReverseOrder(actualStrecke, expectedGeometry);
	}

	private void isEqualInOrderOrReverseOrder(StreckeVonKanten streckeInResult, LineString expectedGeometry) {
		assertThat(streckeInResult.getStrecke().getCoordinates())
			.satisfiesAnyOf(
				s -> assertThat(s).containsExactlyElementsOf(
					Arrays.stream(expectedGeometry.getCoordinates()).collect(Collectors.toList())),
				s -> assertThat(s).containsExactlyElementsOf(
					Arrays.stream(expectedGeometry.reverse().getCoordinates()).collect(Collectors.toList())));
	}
}
