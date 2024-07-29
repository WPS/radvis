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

package de.wps.radvis.backend.massnahme.domain.bezug;

import static de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug.fasseUeberlappendeBezuegeZusammen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;

class AbschnittsweiserKantenSeitenBezugTest {

	@Test
	void istWertsemantisch() {
		AbschnittsweiserKantenSeitenBezug SAKB_1_vollstaendig_beidseitig = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG);

		AbschnittsweiserKantenSeitenBezug SAKB_1_vollstaendig_beidseitig_copy = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG);

		AbschnittsweiserKantenSeitenBezug SAKB_2_vollstaendig_beidseitig = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(2L).build(),
			LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG);

		AbschnittsweiserKantenSeitenBezug SAKB_1_halb_beidseitig = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, 0.5), Seitenbezug.BEIDSEITIG);

		AbschnittsweiserKantenSeitenBezug SAKB_1_vollstaendig_linksseitig = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS);

		assertThat(SAKB_1_vollstaendig_beidseitig).isEqualTo(SAKB_1_vollstaendig_beidseitig);
		assertThat(SAKB_1_vollstaendig_beidseitig).isEqualTo(SAKB_1_vollstaendig_beidseitig_copy);
		assertThat(SAKB_1_vollstaendig_beidseitig).isNotEqualTo(SAKB_2_vollstaendig_beidseitig);
		assertThat(SAKB_1_vollstaendig_beidseitig).isNotEqualTo(SAKB_1_halb_beidseitig);
		assertThat(SAKB_1_vollstaendig_beidseitig).isNotEqualTo(SAKB_1_vollstaendig_linksseitig);
	}

	@Test
	void copyWithSeitenbezug() {
		AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG);
		AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(2L).build(),
			LinearReferenzierterAbschnitt.of(0.3, 0.8), Seitenbezug.LINKS);

		AbschnittsweiserKantenSeitenBezug oneCopy = one.withSeitenbezug(
			Seitenbezug.RECHTS);
		assertThat(oneCopy.getSeitenbezug()).isEqualTo(Seitenbezug.RECHTS);
		assertThat(oneCopy.getKante()).isEqualTo(one.getKante());
		assertThat(oneCopy.getLinearReferenzierterAbschnitt()).isEqualTo(one.getLinearReferenzierterAbschnitt());

		AbschnittsweiserKantenSeitenBezug twoCopy = two.withSeitenbezug(
			Seitenbezug.BEIDSEITIG);
		assertThat(twoCopy.getSeitenbezug()).isEqualTo(Seitenbezug.BEIDSEITIG);
		assertThat(twoCopy.getKante()).isEqualTo(two.getKante());
		assertThat(twoCopy.getLinearReferenzierterAbschnitt()).isEqualTo(two.getLinearReferenzierterAbschnitt());

	}

	@Test
	void intersection() {
		AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, .5), Seitenbezug.BEIDSEITIG);

		AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(.4, 1.), Seitenbezug.BEIDSEITIG);

		AbschnittsweiserKantenSeitenBezug three = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(.4, 1.), Seitenbezug.LINKS);

		AbschnittsweiserKantenSeitenBezug four = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(.4, 1.), Seitenbezug.RECHTS);

		AbschnittsweiserKantenSeitenBezug five = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(2L).build(),
			LinearReferenzierterAbschnitt.of(.4, 1.), Seitenbezug.RECHTS);

		assertThat(one.intersection(one)).contains(one);
		assertThat(two.intersection(two)).contains(two);
		assertThat(three.intersection(three)).contains(three);
		assertThat(four.intersection(four)).contains(four);
		assertThat(five.intersection(five)).contains(five);

		assertThat(two.intersection(three)).contains(two);
		assertThat(three.intersection(two)).contains(three);

		assertThat(two.intersection(four)).contains(two);
		assertThat(four.intersection(two)).contains(four);

		assertThat(three.intersection(four)).isEmpty();
		assertThat(four.intersection(three)).isEmpty();

		assertThat(five.intersection(one)).isEmpty();
		assertThat(five.intersection(two)).isEmpty();
		assertThat(five.intersection(three)).isEmpty();
		assertThat(five.intersection(four)).isEmpty();

		assertThat(one.intersection(five)).isEmpty();
		assertThat(two.intersection(five)).isEmpty();
		assertThat(three.intersection(five)).isEmpty();
		assertThat(four.intersection(five)).isEmpty();
	}

	@Test
	void ueberlappenSichBezuege_duplicatesUeberlappenSich() {
		AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, .5), Seitenbezug.BEIDSEITIG);
		assertThat(AbschnittsweiserKantenSeitenBezug.ueberlappenSichBezuege(List.of(one, one))).isTrue();
	}

	@Test
	void ueberlappenSichBezuege_kleineUeberlappung() {
		AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, .5), Seitenbezug.BEIDSEITIG);
		AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0.4, 1), Seitenbezug.BEIDSEITIG);
		assertThat(AbschnittsweiserKantenSeitenBezug.ueberlappenSichBezuege(List.of(one, two))).isTrue();
	}

	@Test
	void ueberlappenSichBezuege_beruecksichtigtKantenID() {
		AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, .5), Seitenbezug.LINKS);
		AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(2L).build(),
			LinearReferenzierterAbschnitt.of(0.4, 1), Seitenbezug.BEIDSEITIG);
		assertThat(AbschnittsweiserKantenSeitenBezug.ueberlappenSichBezuege(List.of(one, two))).isFalse();
	}

	@Test
	void ueberlappenSichBezuege_AdjacencyIstKeineUeberlappung() {
		AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0, .5), Seitenbezug.LINKS);
		AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(),
			LinearReferenzierterAbschnitt.of(0.5, 1), Seitenbezug.BEIDSEITIG);
		assertThat(AbschnittsweiserKantenSeitenBezug.ueberlappenSichBezuege(List.of(one, two))).isFalse();
	}

	@Nested
	public class FasseIntersectionsZusammen {
		@Test
		void enthaeltSich_fasstzusammen() {
			AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.8), Seitenbezug.BEIDSEITIG);
			AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.4, 0.8), Seitenbezug.BEIDSEITIG);
			AbschnittsweiserKantenSeitenBezug three = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.1, 1), Seitenbezug.BEIDSEITIG);
			assertThat(fasseUeberlappendeBezuegeZusammen(List.of(one, two, three))).containsExactly(
				one.copyWithLR(LinearReferenzierterAbschnitt.of(0.1, 1)));
		}

		@Test
		void enthaeltSich_fasstUeberschneidungenZusammen() {
			AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.6), Seitenbezug.BEIDSEITIG);
			AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.4, 0.8), Seitenbezug.BEIDSEITIG);
			assertThat(fasseUeberlappendeBezuegeZusammen(List.of(one, two))).containsExactly(
				one.copyWithLR(LinearReferenzierterAbschnitt.of(0.2, 0.8)));
			assertThat(fasseUeberlappendeBezuegeZusammen(List.of(two, one))).containsExactly(
				one.copyWithLR(LinearReferenzierterAbschnitt.of(0.2, 0.8)));
		}

		@Test
		void enthaeltSich_unterschiedlicheSeitenWirftException() {
			AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.8), Seitenbezug.LINKS);
			AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.4, 0.8), Seitenbezug.BEIDSEITIG);
			assertThatThrownBy(() -> fasseUeberlappendeBezuegeZusammen(List.of(one, two))).isInstanceOf(
				RequireViolation.class);
			assertThatThrownBy(() -> fasseUeberlappendeBezuegeZusammen(List.of(two, one))).isInstanceOf(
				RequireViolation.class);
		}

		@Test
		void enthaeltSich_unterschiedlicheKantenWirftException() {
			AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.8), Seitenbezug.LINKS);
			AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.4, 0.8), Seitenbezug.LINKS);
			assertThatThrownBy(() -> fasseUeberlappendeBezuegeZusammen(List.of(one, two))).isInstanceOf(
				RequireViolation.class);
			assertThatThrownBy(() -> fasseUeberlappendeBezuegeZusammen(List.of(two, one))).isInstanceOf(
				RequireViolation.class);
		}
	}

	@Nested
	class FiltereUeberlappungen {
		// TODO RAD-6596: andere Seitenbezüge testen bspw. mit  
		// 		@ParameterizedTest
		//		@EnumSource(value = Seitenbezug.class)
		//		void ueberlappungenVorhanden_filtert(Seitenbezug seitenbezug) {
		@Test
		void ueberlappungenVorhanden_filtert() {
			// Arrange
			AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.6), Seitenbezug.BEIDSEITIG);
			AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.4, 0.8), Seitenbezug.BEIDSEITIG);

			// Act
			Set<AbschnittsweiserKantenSeitenBezug> actual = AbschnittsweiserKantenSeitenBezug
				.fasseUeberlappendeBezuegeProKanteZusammen(
					Set.of(one, two)
				);

			// Assert
			assertThat(actual).containsExactly(one.copyWithLR(LinearReferenzierterAbschnitt.of(0.2, 0.8)));
		}

		@Test
		void keineUeberlappungenVorhanden_filtertNicht() {
			// Arrange
			AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.4), Seitenbezug.BEIDSEITIG);
			AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.BEIDSEITIG);

			// Act
			Set<AbschnittsweiserKantenSeitenBezug> actual = AbschnittsweiserKantenSeitenBezug
				.fasseUeberlappendeBezuegeProKanteZusammen(
					Set.of(one, two)
				);

			// Assert
			assertThat(actual).containsExactlyInAnyOrder(one, two);
		}

		@Test
		void verschiedeneIDs_filtertNicht() {
			// Arrange
			AbschnittsweiserKantenSeitenBezug one = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.6), Seitenbezug.BEIDSEITIG);
			AbschnittsweiserKantenSeitenBezug two = new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.4, 0.8), Seitenbezug.BEIDSEITIG);

			// Act
			Set<AbschnittsweiserKantenSeitenBezug> actual = AbschnittsweiserKantenSeitenBezug
				.fasseUeberlappendeBezuegeProKanteZusammen(
					Set.of(one, two)
				);

			// Assert
			assertThat(actual).containsExactlyInAnyOrder(one, two);
		}
	}
}
