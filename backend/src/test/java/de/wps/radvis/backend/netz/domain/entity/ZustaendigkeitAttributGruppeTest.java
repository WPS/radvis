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

import java.util.List;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;

class ZustaendigkeitAttributGruppeTest {

	@Test
	void insert_splitSurroundingLR() {
		// arrange
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("ErsterAbschnitt")).build()));

		// act
		zustaendigkeitAttributGruppe
			.insert(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.vereinbarungsKennung(VereinbarungsKennung.of("ZweiterAbschnitt")).build());

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.3)
					.vereinbarungsKennung(VereinbarungsKennung.of("ErsterAbschnitt")).build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.3, 0.6)
					.vereinbarungsKennung(VereinbarungsKennung.of("ZweiterAbschnitt")).build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("ErsterAbschnitt")).build());
	}

	@Test
	void insert_worksAtLinestringEndings() {
		// arrange
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.2)
				.vereinbarungsKennung(VereinbarungsKennung.of("ErsterAbschnitt")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("ZweiterAbschnitt")).build()));

		// act
		zustaendigkeitAttributGruppe
			.insert(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("DritterAbschnitt")).build());

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("DritterAbschnitt")).build());
	}

	@Test
	void insert_splitLRStartAndEnd() {
		// arrange
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.5)
				.vereinbarungsKennung(VereinbarungsKennung.of("ErsterAbschnitt")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("ZweiterAbschnitt")).build()));

		// act
		zustaendigkeitAttributGruppe
			.insert(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.vereinbarungsKennung(VereinbarungsKennung.of("DritterAbschnitt")).build());

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.3)
					.vereinbarungsKennung(VereinbarungsKennung.of("ErsterAbschnitt")).build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.3, 0.6)
					.vereinbarungsKennung(VereinbarungsKennung.of("DritterAbschnitt")).build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("ZweiterAbschnitt")).build());
	}

	@Test
	void insert_defragment() {
		// arrange
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.5)
				.vereinbarungsKennung(VereinbarungsKennung.of("ErsterAbschnitt")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("ZweiterAbschnitt")).build()));

		// act
		zustaendigkeitAttributGruppe
			.insert(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.vereinbarungsKennung(VereinbarungsKennung.of("ErsterAbschnitt")).build());

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.6)
					.vereinbarungsKennung(VereinbarungsKennung.of("ErsterAbschnitt")).build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("ZweiterAbschnitt")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_noMatchingSegment_doesNothing() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.4)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttribute);

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.39));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrderElementsOf(zustaendigkeitAttribute);
	}

	@Test
	void mergeSegmentsKleinerAls_segmentAtBeginning_mergeRight() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 0.6)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttribute);

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.6)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_mergeAll() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.2)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 0.6)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("3")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttribute);

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(1.0));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("3")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_segmentAtEnd_mergeLeft() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.4)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 0.8)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.8, 1).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttribute);

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.4)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_segmentInTheMiddle_mergeRight() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.4)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 0.6).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttribute);

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.4)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_segmentInTheMiddle_mergeUndFasseZusammen() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.4)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 0.6)
				.vereinbarungsKennung(VereinbarungsKennung.of("x")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttribute);

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactly(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_konsekutivSegments_mergeBoth() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.1)
				.vereinbarungsKennung(VereinbarungsKennung.of("0")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.1, 0.2)
				.vereinbarungsKennung(VereinbarungsKennung.of("x")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 0.55)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.55, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttribute);

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.55)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.55, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoSegments_mergeLeftAndRight() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.2)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 0.8)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.8, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("3")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttribute);

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoSegments_mergeRight() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.1)
				.vereinbarungsKennung(VereinbarungsKennung.of("0")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.1, 0.4)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 0.5)
				.vereinbarungsKennung(VereinbarungsKennung.of("x")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.5, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttribute);

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.2));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.4)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
				ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 1)
					.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoMiniSegmentsAtEnd_mergeTowardsBigSegment() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 0.6)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 0.8)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.8, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("3")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = ZustaendigkeitAttributGruppe.builder()
			.zustaendigkeitAttribute(zustaendigkeitAttribute).build();

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactly(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_onlyMiniSegments_mergeAll() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.3).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.6, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = ZustaendigkeitAttributGruppe.builder()
			.zustaendigkeitAttribute(zustaendigkeitAttribute).build();

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.5));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactly(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoMiniSegmentsAtStart_mergeTowardsBigSegment() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List.of(
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.2, 0.4)
				.vereinbarungsKennung(VereinbarungsKennung.of("1")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.4, 0.8)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build(),
			ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0.8, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("3")).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = ZustaendigkeitAttributGruppe.builder()
			.zustaendigkeitAttribute(zustaendigkeitAttribute).build();

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactly(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1)
				.vereinbarungsKennung(VereinbarungsKennung.of("2")).build());
	}

	@Test
	void mergeSegmentsKleinerAls_onlyOneSegment_doesNothing() {
		// arrange
		List<ZustaendigkeitAttribute> zustaendigkeitAttribute = List
			.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1).build());
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = new ZustaendigkeitAttributGruppe(
			zustaendigkeitAttribute);

		// act
		zustaendigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1).build());

	}
}
