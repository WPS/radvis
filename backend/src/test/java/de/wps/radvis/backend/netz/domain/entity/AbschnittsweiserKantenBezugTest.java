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

package de.wps.radvis.backend.netz.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;

class AbschnittsweiserKantenBezugTest {

	@Test
	void istWertsemantisch() {
		AbschnittsweiserKantenBezug SAKB_1_vollstaendig = new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, 1));

		AbschnittsweiserKantenBezug SAKB_1_vollstaendig_copy = new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, 1));

		AbschnittsweiserKantenBezug SAKB_2_vollstaendig = new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withDefaultValues().id(2L).build(),
			LinearReferenzierterAbschnitt.of(0, 1));

		AbschnittsweiserKantenBezug SAKB_1_halb = new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, 0.5));

		assertThat(SAKB_1_vollstaendig).isEqualTo(SAKB_1_vollstaendig);
		assertThat(SAKB_1_vollstaendig).isEqualTo(SAKB_1_vollstaendig_copy);
		assertThat(SAKB_1_vollstaendig).isNotEqualTo(SAKB_2_vollstaendig);
		assertThat(SAKB_1_vollstaendig).isNotEqualTo(SAKB_1_halb);
	}

	@Test
	void intersection() {
		Kante kante1 = KanteTestDataProvider.withDefaultValues().id(1L).build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues().id(2L).build();

		AbschnittsweiserKantenBezug one = new AbschnittsweiserKantenBezug(kante1,
			LinearReferenzierterAbschnitt.of(0, 0.5));
		AbschnittsweiserKantenBezug two = new AbschnittsweiserKantenBezug(kante1,
			LinearReferenzierterAbschnitt.of(0.2, 0.8));
		AbschnittsweiserKantenBezug three = new AbschnittsweiserKantenBezug(kante1,
			LinearReferenzierterAbschnitt.of(0.5, 0.9));
		AbschnittsweiserKantenBezug four = new AbschnittsweiserKantenBezug(kante1,
			LinearReferenzierterAbschnitt.of(0.9, 1));

		AbschnittsweiserKantenBezug five = new AbschnittsweiserKantenBezug(kante2,
			LinearReferenzierterAbschnitt.of(0., 1));

		assertThat(one.intersection(one)).contains(one);
		assertThat(two.intersection(two)).contains(two);
		assertThat(three.intersection(three)).contains(three);
		assertThat(four.intersection(four)).contains(four);
		assertThat(five.intersection(five)).contains(five);

		assertThat(one.intersection(two)).contains(
			new AbschnittsweiserKantenBezug(kante1, LinearReferenzierterAbschnitt.of(0.2, 0.5)));
		assertThat(one.intersection(three)).isEmpty();
		assertThat(one.intersection(four)).isEmpty();
		assertThat(one.intersection(five)).isEmpty();
	}

	@Test
	void erstelleNetzbezugLineString_eineTeilstrecke() {
		// arrange
		AtomicLong kantenId = new AtomicLong(100);
		AtomicLong knotenId = new AtomicLong(10);
		Knoten startKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(90, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();

		List<Coordinate[]> streckenCoordinates = List.of(new Coordinate[] {
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
		}, new Coordinate[] {
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0),
		}, new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(60, 0),
			new Coordinate(70, 0),
		}, new Coordinate[] {
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0),
		});
		List<Kante> streckeUeberCoordinates = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates,
			startKnoten, endKnoten, knotenId,
			kantenId);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs = streckeUeberCoordinates.stream()
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());

		// act
		StreckeVonKanten streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			abschnittsweiserKantenBezugs).get();

		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyInAnyOrder(
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0),
			new Coordinate(60, 0),
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0)
		);
		assertThat(streckeVonKanten.getKanten()).containsExactlyElementsOf(streckeUeberCoordinates);
	}

	@Test
	void erstelleNetzbezugLineString_eineTeilstrecke_nichtInOrder() {
		// arrange
		AtomicLong kantenId = new AtomicLong(100);
		AtomicLong knotenId = new AtomicLong(10);
		Knoten startKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(90, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();

		List<Coordinate[]> streckenCoordinates = List.of(new Coordinate[] {
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
		}, new Coordinate[] {
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0),
		}, new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(60, 0),
			new Coordinate(70, 0),
		}, new Coordinate[] {
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0),
		});
		List<Kante> streckeUeberCoordinates = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates,
			startKnoten, endKnoten, knotenId,
			kantenId);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs = List.of(streckeUeberCoordinates.get(2),
				streckeUeberCoordinates.get(0), streckeUeberCoordinates.get(3), streckeUeberCoordinates.get(1))
			.stream().map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());

		// act
		StreckeVonKanten streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			abschnittsweiserKantenBezugs).get();

		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyInAnyOrder(
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0),
			new Coordinate(60, 0),
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0)
		);
		assertThat(streckeVonKanten.getKanten()).containsExactlyElementsOf(streckeUeberCoordinates);
	}

	// TODO dieser test laeuft im Moment nicht durch, da doppelte Strecken aussortiert werden
	@Disabled
	@Test
	void erstelleNetzbezugLineString_BigLoop_1KanteDoppelt() {
		// arrange
		AtomicLong knotenId = new AtomicLong(1L);
		List<Knoten> knoten = new ArrayList<>();
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(70, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(60, 10), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(90, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());

		AtomicLong kantenId = new AtomicLong(100);
		List<Kante> testKanten = List.of(
			KanteTestDataProvider.fromKnoten(knoten.get(0), knoten.get(1)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(1), knoten.get(2)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(2), knoten.get(3)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(3), knoten.get(1)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(2), knoten.get(4)).id(kantenId.getAndIncrement()).build()
		);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs = testKanten.stream()
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());
		abschnittsweiserKantenBezugs.add(4, abschnittsweiserKantenBezugs.get(1));

		// Als Liste:
		// act
		StreckeVonKanten streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			abschnittsweiserKantenBezugs).get();

		// assert
		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyInAnyOrder(
			new Coordinate(0, 0),
			new Coordinate(50, 0),
			new Coordinate(70, 0),
			new Coordinate(60, 10),
			new Coordinate(50, 0),
			new Coordinate(70, 0),
			new Coordinate(90, 0)
		);
		assertThat(streckeVonKanten.getKanten()).containsExactlyElementsOf(testKanten);
	}

	@Test
	void erstelleNetzbezugLineString_2kreisverkehre_inMiddle() {
		// arrange
		AtomicLong knotenId = new AtomicLong(1L);
		List<Knoten> knoten = new ArrayList<>();
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(70, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(80, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(90, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(60, 10), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(120, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(140, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());
		knoten.add(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(95, 10), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build());

		AtomicLong kantenId = new AtomicLong(100);
		List<Kante> testKanten = List.of(
			KanteTestDataProvider.fromKnoten(knoten.get(0), knoten.get(1)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(1), knoten.get(2)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(2), knoten.get(3)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(3), knoten.get(4)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(4), knoten.get(5)).id(kantenId.getAndIncrement()).build(),

			KanteTestDataProvider.fromKnoten(knoten.get(2), knoten.get(6)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(6), knoten.get(3)).id(kantenId.getAndIncrement()).build(),

			KanteTestDataProvider.fromKnoten(knoten.get(5), knoten.get(7)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(7), knoten.get(8)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(8), knoten.get(9)).id(kantenId.getAndIncrement()).build(),

			KanteTestDataProvider.fromKnoten(knoten.get(5), knoten.get(10)).id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnoten(knoten.get(10), knoten.get(7)).id(kantenId.getAndIncrement()).build()
		);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs = testKanten.stream()
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());

		// Als Liste:
		// act
		StreckeVonKanten streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			abschnittsweiserKantenBezugs).get();

		// assert
		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyInAnyOrder(
			new Coordinate(0, 0),
			new Coordinate(20, 0),
			new Coordinate(50, 0),
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0),
			new Coordinate(100, 0),
			new Coordinate(120, 0),
			new Coordinate(140, 0)
		);
	}

	@Test
	void erstelleNetzbezugLineString_Rundtour() {
		// arrange
		AtomicLong kantenId = new AtomicLong(100);
		AtomicLong knotenId = new AtomicLong(10);
		Knoten startKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten mittelKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 0),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();

		List<Kante> streckeUeberCoordinates = List.of(
			KanteTestDataProvider.fromKnotenUndQuelle(startKnoten, mittelKnoten, QuellSystem.DLM)
				.id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnotenUndQuelle(mittelKnoten, endKnoten, QuellSystem.DLM)
				.id(kantenId.getAndIncrement()).build(),
			KanteTestDataProvider.fromKnotenUndQuelle(endKnoten, startKnoten, QuellSystem.DLM)
				.id(kantenId.getAndIncrement()).build()
		);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs = streckeUeberCoordinates.stream()
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());

		// act
		StreckeVonKanten streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			abschnittsweiserKantenBezugs).get();

		// Resultierender Linestring soll ein Loop sein
		assertThat(streckeVonKanten.getStrecke().getCoordinates()[0])
			.isEqualTo(streckeVonKanten.getStrecke()
				.getCoordinates()[streckeVonKanten.getStrecke().getCoordinates().length - 1]);
	}

	@Test
	void erstelleNetzbezugLineString_mittlereKanteFehlt_nichtVerbunden() {
		// arrange
		AtomicLong kantenId = new AtomicLong(100);
		AtomicLong knotenId = new AtomicLong(3);
		Knoten startKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100.0, 100.0),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(500.0, 350.0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();

		List<Coordinate[]> streckenCoordinates = List.of(new Coordinate[] {
			new Coordinate(100, 100),
			new Coordinate(130, 135),
			new Coordinate(150, 150),
			new Coordinate(200, 150),
		}, new Coordinate[] {
			new Coordinate(200, 150),
			new Coordinate(250, 200),
			new Coordinate(300, 200),
		}, new Coordinate[] {
			new Coordinate(300, 200),
			new Coordinate(350, 250),
			new Coordinate(400, 300),
		}, new Coordinate[] {
			new Coordinate(400, 300),
			new Coordinate(450, 350),
			new Coordinate(500, 350),
		});

		List<Kante> streckeUeberCoordinates = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates,
			startKnoten, endKnoten, knotenId,
			kantenId);

		AbschnittsweiserKantenBezug one = new AbschnittsweiserKantenBezug(streckeUeberCoordinates.get(0),
			LinearReferenzierterAbschnitt.of(0, 1));
		AbschnittsweiserKantenBezug two = new AbschnittsweiserKantenBezug(streckeUeberCoordinates.get(1),
			LinearReferenzierterAbschnitt.of(0, 1));
		AbschnittsweiserKantenBezug four = new AbschnittsweiserKantenBezug(streckeUeberCoordinates.get(3),
			LinearReferenzierterAbschnitt.of(0, 1));

		// act
		Optional<StreckeVonKanten> streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			new ArrayList<>(Set.of(one, two, four)));

		// assert
		assertThat(streckeVonKanten).isEmpty();
	}

	@Test
	void erstelleNetzbezugLineString_kreuzungsKnoten_mitLoop() {
		// arrange
		AtomicLong kantenId = new AtomicLong(100);
		AtomicLong knotenId = new AtomicLong(10);
		Knoten startKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(90, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten mittelKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();

		List<Coordinate[]> streckenCoordinates1 = List.of(new Coordinate[] {
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
		}, new Coordinate[] {
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0),
		});
		List<Kante> streckeUeberCoordinates1 = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates1,
			startKnoten, mittelKnoten, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinatesLoop = List.of(new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(50, 10),
		}, new Coordinate[] {
			new Coordinate(50, 10),
			new Coordinate(55, 20),
		}, new Coordinate[] {
			new Coordinate(55, 20),
			new Coordinate(45, 10),
			new Coordinate(50, 0),
		});
		List<Kante> streckeUeberCoordinatesLoop = KanteTestDataProvider.createStreckeUeberCoordinates(
			streckenCoordinatesLoop,
			mittelKnoten, mittelKnoten, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinates2 = List.of(new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(60, 0),
			new Coordinate(70, 0),
		}, new Coordinate[] {
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0)
		});
		List<Kante> streckeUeberCoordinates2 = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates2,
			mittelKnoten, endKnoten, knotenId,
			kantenId);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs = Stream.concat(Stream.concat(
					streckeUeberCoordinates1.stream(),
					streckeUeberCoordinatesLoop.stream()),
				streckeUeberCoordinates2.stream())
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());

		// act
		StreckeVonKanten streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			abschnittsweiserKantenBezugs).get();

		// assert
		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyInAnyOrder(
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0),
			new Coordinate(50, 10),
			new Coordinate(55, 20),
			new Coordinate(45, 10),
			new Coordinate(50, 0),
			new Coordinate(60, 0),
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0)
		);
	}

	@Test
	void erstelleNetzbezugLineString_kreuzungsKnoten_mitWurmfortsatz() {
		// arrange
		AtomicLong kantenId = new AtomicLong(100);
		AtomicLong knotenId = new AtomicLong(10);
		Knoten startKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(90, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten mittelKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnotenWurmfortsatz1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 20),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();

		List<Coordinate[]> streckenCoordinates1 = List.of(new Coordinate[] {
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
		}, new Coordinate[] {
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0),
		});
		List<Kante> streckeUeberCoordinates1 = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates1,
			startKnoten, mittelKnoten, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinatesWurmfortsatz = List.of(new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(50, 10),
		}, new Coordinate[] {
			new Coordinate(50, 10),
			new Coordinate(50, 20),
		});
		List<Kante> streckeUeberCoordinatesWurmfortsatz = KanteTestDataProvider.createStreckeUeberCoordinates(
			streckenCoordinatesWurmfortsatz,
			mittelKnoten, endKnotenWurmfortsatz1, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinates2 = List.of(new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(60, 0),
			new Coordinate(70, 0),
		}, new Coordinate[] {
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0)
		});
		List<Kante> streckeUeberCoordinates2 = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates2,
			mittelKnoten, endKnoten, knotenId,
			kantenId);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs = Stream.concat(Stream.concat(
					streckeUeberCoordinates1.stream(),
					streckeUeberCoordinatesWurmfortsatz.stream()),
				streckeUeberCoordinates2.stream())
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());

		// act
		StreckeVonKanten streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			abschnittsweiserKantenBezugs).get();

		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyInAnyOrder(
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0),
			new Coordinate(50, 10),
			new Coordinate(50, 20),
			new Coordinate(50, 10),
			new Coordinate(50, 0),
			new Coordinate(60, 0),
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0)
		);
		assertThat(streckeVonKanten.getKanten()).extracting(AbstractEntity::getId).containsExactly(
			101L, 102L, 103L, 104L, 104L, 103L, 105L, 106L
		);
	}

	@Test
	void erstelleNetzbezugLineString_kreuzungsKnoten_mitWurmfortsatzUndLoop() {
		// arrange
		AtomicLong kantenId = new AtomicLong(100);
		AtomicLong knotenId = new AtomicLong(10);
		Knoten startKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten mittelKnotenWurm = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnotenWurmfortsatz1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 20),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(90, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();

		List<Coordinate[]> streckenCoordinates1 = List.of(
			new Coordinate[] {
				new Coordinate(0, 0),
				new Coordinate(10, 0),
				new Coordinate(20, 0),
				new Coordinate(30, 0)
			},
			new Coordinate[] {
				new Coordinate(30, 0),
				new Coordinate(40, 0),
				new Coordinate(50, 0),
			}
		);
		List<Kante> streckeUeberCoordinates1 = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates1,
			startKnoten, mittelKnotenWurm, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinatesWurmfortsatz = List.of(new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(50, 10),
		}, new Coordinate[] {
			new Coordinate(50, 10),
			new Coordinate(50, 20),
		});
		List<Kante> streckeUeberCoordinatesWurmfortsatz = KanteTestDataProvider.createStreckeUeberCoordinates(
			streckenCoordinatesWurmfortsatz,
			mittelKnotenWurm, endKnotenWurmfortsatz1, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinatesLoop = List.of(new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(50, 10),
		}, new Coordinate[] {
			new Coordinate(50, 10),
			new Coordinate(55, 20),
		}, new Coordinate[] {
			new Coordinate(55, 20),
			new Coordinate(45, 10),
			new Coordinate(50, 0),
		});
		List<Kante> streckeUeberCoordinatesLoop = KanteTestDataProvider.createStreckeUeberCoordinates(
			streckenCoordinatesLoop,
			mittelKnotenWurm, mittelKnotenWurm, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinates2 = List.of(new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(60, 0),
			new Coordinate(70, 0),
		}, new Coordinate[] {
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0)
		});
		List<Kante> streckeUeberCoordinates2 = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates2,
			mittelKnotenWurm, endKnoten, knotenId,
			kantenId);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs = Stream.concat(Stream.concat(Stream.concat(
						streckeUeberCoordinates1.stream(),
						streckeUeberCoordinatesWurmfortsatz.stream()),
					streckeUeberCoordinatesLoop.stream()),
				streckeUeberCoordinates2.stream())
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());

		// act
		StreckeVonKanten streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			abschnittsweiserKantenBezugs).get();

		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyInAnyOrder(
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
			new Coordinate(40, 0),

			new Coordinate(50, 0),

			new Coordinate(50, 10),
			new Coordinate(50, 20),
			new Coordinate(50, 10),

			new Coordinate(50, 0),

			new Coordinate(50, 10),
			new Coordinate(55, 20),
			new Coordinate(45, 10),

			new Coordinate(50, 0),

			new Coordinate(60, 0),
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0)
		);
	}

	@Test
	void erstelleNetzbezugLineString_kreuzungsKnoten_dreiStrecken() {
		// arrange
		AtomicLong kantenId = new AtomicLong(100);
		AtomicLong knotenId = new AtomicLong(10);
		Knoten startKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten mittelKnoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnotenWurmfortsatz1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(30, 20),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten mittelKnoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnotenWurmfortsatz2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 20),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten endKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(90, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();

		List<Coordinate[]> streckenCoordinates1 = List.of(new Coordinate[] {
			new Coordinate(0, 0),
			new Coordinate(10, 0)
		}, new Coordinate[] {
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
		});
		List<Kante> streckeUeberCoordinates1 = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates1,
			startKnoten, mittelKnoten1, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinates2 = List.of(new Coordinate[] {
			new Coordinate(30, 0),
			new Coordinate(40, 0)
		}, new Coordinate[] {
			new Coordinate(40, 0),
			new Coordinate(50, 0),
		});
		List<Kante> streckeUeberCoordinates2 = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates2,
			mittelKnoten1, mittelKnoten2, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinates3 = List.of(new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(60, 0),
			new Coordinate(70, 0),
		}, new Coordinate[] {
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0),
		});
		List<Kante> streckeUeberCoordinates3 = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates3,
			mittelKnoten2, endKnoten, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinatesWurmfortsatz = List.of(new Coordinate[] {
			new Coordinate(30, 0),
			new Coordinate(30, 10),
		}, new Coordinate[] {
			new Coordinate(30, 10),
			new Coordinate(30, 20),
		});
		List<Kante> streckeUeberCoordinatesWurmfortsatz1 = KanteTestDataProvider.createStreckeUeberCoordinates(
			streckenCoordinatesWurmfortsatz,
			mittelKnoten1, endKnotenWurmfortsatz1, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinatesWurmfortsatz2 = List.of(new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(50, 10),
		}, new Coordinate[] {
			new Coordinate(50, 10),
			new Coordinate(50, 20),
		});
		List<Kante> streckeUeberCoordinatesWurmfortsatz2 = KanteTestDataProvider.createStreckeUeberCoordinates(
			streckenCoordinatesWurmfortsatz2,
			mittelKnoten2, endKnotenWurmfortsatz2, knotenId,
			kantenId);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs = Stream.concat(
				Stream.concat(Stream.concat(Stream.concat(
							streckeUeberCoordinates1.stream(),
							streckeUeberCoordinatesWurmfortsatz1.stream()),
						streckeUeberCoordinates2.stream()),
					streckeUeberCoordinatesWurmfortsatz2.stream()),
				streckeUeberCoordinates3.stream())
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());

		// act
		StreckeVonKanten streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			abschnittsweiserKantenBezugs).get();

		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyInAnyOrder(
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),

			new Coordinate(30, 0),

			new Coordinate(30, 10),
			new Coordinate(30, 20),
			new Coordinate(30, 10),

			new Coordinate(30, 0),

			new Coordinate(40, 0),

			new Coordinate(50, 0),

			new Coordinate(50, 10),
			new Coordinate(50, 20),
			new Coordinate(50, 10),

			new Coordinate(50, 0),

			new Coordinate(60, 0),
			new Coordinate(70, 0),
			new Coordinate(80, 0),
			new Coordinate(90, 0)
		);
	}

	@Test
	void erstelleNetzbezugLineString_kreuzungsKnoten_mitLoopAmEnde() {
		// arrange
		AtomicLong kantenId = new AtomicLong(100);
		AtomicLong knotenId = new AtomicLong(10);
		Knoten startKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0),
				QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();
		Knoten mittelKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(50, 0), QuellSystem.DLM)
			.id(knotenId.getAndIncrement())
			.build();

		List<Coordinate[]> streckenCoordinates1 = List.of(new Coordinate[] {
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
		}, new Coordinate[] {
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0),
		});
		List<Kante> streckeUeberCoordinates1 = KanteTestDataProvider.createStreckeUeberCoordinates(streckenCoordinates1,
			startKnoten, mittelKnoten, knotenId,
			kantenId);

		List<Coordinate[]> streckenCoordinatesLoop = List.of(new Coordinate[] {
			new Coordinate(50, 0),
			new Coordinate(50, 10),
		}, new Coordinate[] {
			new Coordinate(50, 10),
			new Coordinate(55, 20),
		}, new Coordinate[] {
			new Coordinate(55, 20),
			new Coordinate(45, 10),
			new Coordinate(50, 0),
		});
		List<Kante> streckeUeberCoordinatesLoop = KanteTestDataProvider.createStreckeUeberCoordinates(
			streckenCoordinatesLoop,
			mittelKnoten, mittelKnoten, knotenId,
			kantenId);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezugs = Stream.concat(
				streckeUeberCoordinates1.stream(),
				streckeUeberCoordinatesLoop.stream())
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());

		// act
		StreckeVonKanten streckeVonKanten = AbschnittsweiserKantenBezug.erstelleNetzbezugLineString(
			abschnittsweiserKantenBezugs).get();

		// assert
		assertThat(streckeVonKanten.getStrecke().getCoordinates()).containsExactlyInAnyOrder(
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0),
			new Coordinate(50, 10),
			new Coordinate(55, 20),
			new Coordinate(45, 10),
			new Coordinate(50, 0)
		);
	}

	@SuppressWarnings("unchecked")
	private void assertInOrRevOrder(List<Coordinate> expectedCoordinates, List<Coordinate> actualCoordinates) {
		List<Coordinate> expectedCoordinatesReversed = new ArrayList<>(expectedCoordinates);
		Collections.reverse(expectedCoordinatesReversed);

		assertThat(actualCoordinates).satisfiesAnyOf(
			coords -> assertThat((List<Coordinate>) coords).containsExactlyElementsOf(expectedCoordinates),
			coords -> assertThat((List<Coordinate>) coords).containsExactlyElementsOf(expectedCoordinatesReversed)
		);
	}
}