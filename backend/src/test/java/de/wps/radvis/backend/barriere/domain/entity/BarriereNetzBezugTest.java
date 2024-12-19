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

package de.wps.radvis.backend.barriere.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;

class BarriereNetzBezugTest {

	private final Set<AbschnittsweiserKantenSeitenBezug> testAbschnittsweiserKantenSeitenBezug = new HashSet<>();
	private final Set<PunktuellerKantenSeitenBezug> testPunktuellerKantenSeitenBezug = new HashSet<>();
	private final Set<Knoten> testKnotenBezug = new HashSet<>();

	private Kante testKante;
	private Knoten testKnoten;

	@BeforeEach
	void setUp() {
		testKante = KanteTestDataProvider.withDefaultValues().id(1L).build();
		testKnoten = KnotenTestDataProvider.withDefaultValues().id(10L).build();
		testAbschnittsweiserKantenSeitenBezug.add(new AbschnittsweiserKantenSeitenBezug(
			testKante,
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS));
		testPunktuellerKantenSeitenBezug.add(new PunktuellerKantenSeitenBezug(
			testKante, LineareReferenz.of(0.5), Seitenbezug.BEIDSEITIG));
		testKnotenBezug.add(testKnoten);
	}

	@Test
	void getDisplayGeometry_allesVorhanden_ersterKnotenVerwendet() {
		// arrange
		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(testAbschnittsweiserKantenSeitenBezug,
			testPunktuellerKantenSeitenBezug, testKnotenBezug);

		// act
		Optional<Point> displayGeometry = barriereNetzBezug.getDisplayGeometry();

		// assert
		assertThat(displayGeometry).isPresent();
		assertThat(displayGeometry.get().getCoordinate()).isEqualTo(testKnoten.getKoordinate());
	}

	@Test
	void getDisplayGeometry_allesBisAufKnotenVorhanden_ersterPunktuellerNetzbezugVerwendet() {
		// arrange
		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(testAbschnittsweiserKantenSeitenBezug,
			testPunktuellerKantenSeitenBezug, Set.of());

		// act
		Optional<Point> displayGeometry = barriereNetzBezug.getDisplayGeometry();

		// assert
		assertThat(displayGeometry).isPresent();
		assertThat(displayGeometry.get().getCoordinate()).isEqualTo(
			new LengthIndexedLine(testKante.getGeometry())
				.extractPoint(0.5 * testKante.getGeometry().getLength()));
	}

	@Test
	void getDisplayGeometry_nurKantenBezug_startpunktErsteKante() {
		// arrange
		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(testAbschnittsweiserKantenSeitenBezug,
			Set.of(), Set.of());

		// act
		Optional<Point> displayGeometry = barriereNetzBezug.getDisplayGeometry();

		// assert
		assertThat(displayGeometry).isPresent();
		assertThat(displayGeometry.get().getCoordinate()).isEqualTo(testKante.getVonKnoten().getKoordinate());
	}

	@Test
	void withoutKanten() {
		// arrange
		Kante zuEntfernendeKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
			.id(1l)
			.build();
		Kante zuEntfernendeKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 50, 100, 50, QuellSystem.DLM)
			.id(2l)
			.build();
		Kante bleibendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 100, 100, 100, QuellSystem.DLM)
			.id(3l)
			.build();

		AbschnittsweiserKantenSeitenBezug abschnittAufBleibenderKante = new AbschnittsweiserKantenSeitenBezug(
			bleibendeKante, LinearReferenzierterAbschnitt.of(0.3, 0.7),
			Seitenbezug.BEIDSEITIG);
		PunktuellerKantenSeitenBezug punktAufBleibenderKante = new PunktuellerKantenSeitenBezug(bleibendeKante,
			LineareReferenz.of(0.25),
			Seitenbezug.BEIDSEITIG);
		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(
			Set.of(
				new AbschnittsweiserKantenSeitenBezug(zuEntfernendeKante1, LinearReferenzierterAbschnitt.of(0.3, 0.7),
					Seitenbezug.BEIDSEITIG),
				new AbschnittsweiserKantenSeitenBezug(zuEntfernendeKante2, LinearReferenzierterAbschnitt.of(0.3, 0.7),
					Seitenbezug.BEIDSEITIG),
				abschnittAufBleibenderKante),
			Set.of(new PunktuellerKantenSeitenBezug(zuEntfernendeKante1, LineareReferenz.of(0.25),
				Seitenbezug.BEIDSEITIG),
				new PunktuellerKantenSeitenBezug(zuEntfernendeKante2, LineareReferenz.of(0.25),
					Seitenbezug.BEIDSEITIG),
				punktAufBleibenderKante),
			Collections.emptySet());

		// act
		BarriereNetzBezug result = barriereNetzBezug
			.withoutKanten(Set.of(zuEntfernendeKante1.getId(), zuEntfernendeKante2.getId()));

		// assert
		assertThat(result.getImmutableKantenPunktBezug()).containsExactly(punktAufBleibenderKante);
		assertThat(result.getImmutableKantenAbschnittBezug()).containsExactly(abschnittAufBleibenderKante);
	}

	@Test
	void withoutKnoten() {
		// arrange
		Knoten zuEntfernenderKnoten1 = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten zuEntfernenderKnoten2 = KnotenTestDataProvider.withDefaultValues().id(2l).build();
		Knoten bleibenderKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuEntfernenderKnoten1, zuEntfernenderKnoten2, bleibenderKnoten));

		// act
		BarriereNetzBezug result = barriereNetzBezug
			.withoutKnoten(Set.of(zuEntfernenderKnoten1.getId(), zuEntfernenderKnoten2.getId()));

		// assert
		assertThat(result.getImmutableKnotenBezug()).containsExactly(bleibenderKnoten);
	}

	@Test
	void withoutKanten_entferntLetzteKante() {
		// arrange
		Kante zuEntfernendeKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
			.id(1l)
			.build();

		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(
			Set.of(
				new AbschnittsweiserKantenSeitenBezug(zuEntfernendeKante1, LinearReferenzierterAbschnitt.of(0.3, 0.7),
					Seitenbezug.BEIDSEITIG)),
			Collections.emptySet(),
			Collections.emptySet());

		// act
		BarriereNetzBezug result = barriereNetzBezug.withoutKanten(Set.of(zuEntfernendeKante1.getId()));

		// assert
		assertThat(result.getImmutableKantenAbschnittBezug()).isEmpty();
	}

	@Test
	void withoutKnoten_letzterKnotenEntfernt_doesNotThrow() {
		// arrange
		Knoten zuEntfernenderKnoten1 = KnotenTestDataProvider.withDefaultValues().id(1l).build();

		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuEntfernenderKnoten1));

		// act + assert
		assertDoesNotThrow(() -> barriereNetzBezug.withoutKnoten(Set.of(zuEntfernenderKnoten1.getId())));
	}

	@Test
	void withKnotenErsetzt() {
		// arrange
		Knoten zuErsetzenderKnoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten knoten2 = KnotenTestDataProvider.withDefaultValues().id(2l).build();
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuErsetzenderKnoten, knoten2));

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		BarriereNetzBezug withKnotenErsetzt = barriereNetzBezug.withKnotenErsetzt(ersatzKnotenZuordnung);

		// assert
		assertThat(withKnotenErsetzt.getImmutableKnotenBezug()).contains(knoten2, ersatzKnoten);
		assertThat(withKnotenErsetzt.getImmutableKnotenBezug()).doesNotContain(zuErsetzenderKnoten);
	}

	@Test
	void withKnotenErsetzt_knotenNotInNetzbezug_doesNothing() {
		// arrange
		Knoten zuErsetzenderKnoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten knoten2 = KnotenTestDataProvider.withDefaultValues().id(2l).build();
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(knoten2));

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		BarriereNetzBezug withKnotenErsetzt = barriereNetzBezug.withKnotenErsetzt(ersatzKnotenZuordnung);

		// assert
		assertThat(withKnotenErsetzt.getImmutableKnotenBezug()).containsExactly(knoten2);
	}

	@Test
	void withKnotenErsetzt_ersatzKnotenAlreadyInNetzbezug_doesNothing() {
		// arrange
		Knoten zuErsetzenderKnoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(ersatzKnoten, zuErsetzenderKnoten));

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		BarriereNetzBezug withKnotenErsetzt = barriereNetzBezug.withKnotenErsetzt(ersatzKnotenZuordnung);

		// assert
		assertThat(withKnotenErsetzt.getImmutableKnotenBezug()).contains(zuErsetzenderKnoten);
	}

}