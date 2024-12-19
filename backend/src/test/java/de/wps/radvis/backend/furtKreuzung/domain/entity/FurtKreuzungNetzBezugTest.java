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

package de.wps.radvis.backend.furtKreuzung.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

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

class FurtKreuzungNetzBezugTest {

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
		FurtKreuzungNetzBezug furtKreuzungNetzBezug = new FurtKreuzungNetzBezug(
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
		FurtKreuzungNetzBezug result = furtKreuzungNetzBezug
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

		FurtKreuzungNetzBezug furtKreuzungNetzBezug = new FurtKreuzungNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuEntfernenderKnoten1, zuEntfernenderKnoten2, bleibenderKnoten));

		// act
		FurtKreuzungNetzBezug result = furtKreuzungNetzBezug
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

		FurtKreuzungNetzBezug furtKreuzungNetzBezug = new FurtKreuzungNetzBezug(
			Set.of(
				new AbschnittsweiserKantenSeitenBezug(zuEntfernendeKante1, LinearReferenzierterAbschnitt.of(0.3, 0.7),
					Seitenbezug.BEIDSEITIG)),
			Collections.emptySet(),
			Collections.emptySet());

		// act
		FurtKreuzungNetzBezug result = furtKreuzungNetzBezug.withoutKanten(Set.of(zuEntfernendeKante1.getId()));

		// assert
		assertThat(result.getImmutableKantenAbschnittBezug()).isEmpty();
	}

	@Test
	void withoutKnoten_letzterKnotenEntfernt_doesNotThrow() {
		// arrange
		Knoten zuEntfernenderKnoten1 = KnotenTestDataProvider.withDefaultValues().id(1l).build();

		FurtKreuzungNetzBezug furtKreuzungNetzBezug = new FurtKreuzungNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuEntfernenderKnoten1));

		// act + assert
		assertDoesNotThrow(() -> furtKreuzungNetzBezug.withoutKnoten(Set.of(zuEntfernenderKnoten1.getId())));
	}

	@Test
	void withKnotenErsetzt() {
		// arrange
		Knoten zuErsetzenderKnoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten knoten2 = KnotenTestDataProvider.withDefaultValues().id(2l).build();
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		FurtKreuzungNetzBezug furtKreuzungNetzBezug = new FurtKreuzungNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuErsetzenderKnoten, knoten2));

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		FurtKreuzungNetzBezug withKnotenErsetzt = furtKreuzungNetzBezug.withKnotenErsetzt(ersatzKnotenZuordnung);

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

		FurtKreuzungNetzBezug furtKreuzungNetzBezug = new FurtKreuzungNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(knoten2));

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		FurtKreuzungNetzBezug withKnotenErsetzt = furtKreuzungNetzBezug.withKnotenErsetzt(ersatzKnotenZuordnung);

		// assert
		assertThat(withKnotenErsetzt.getImmutableKnotenBezug()).containsExactly(knoten2);
	}

	@Test
	void withKnotenErsetzt_ersatzKnotenAlreadyInNetzbezug_doesNothing() {
		// arrange
		Knoten zuErsetzenderKnoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		FurtKreuzungNetzBezug furtKreuzungNetzBezug = new FurtKreuzungNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(ersatzKnoten, zuErsetzenderKnoten));

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		FurtKreuzungNetzBezug withKnotenErsetzt = furtKreuzungNetzBezug.withKnotenErsetzt(ersatzKnotenZuordnung);

		// assert
		assertThat(withKnotenErsetzt.getImmutableKnotenBezug()).contains(zuErsetzenderKnoten);
	}

}
