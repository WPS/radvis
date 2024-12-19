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
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;

class GeschwindigkeitAttributGruppeTest {
	@Test
	void insert_splitSurroundingLR() {
		// arrange
		GeschwindigkeitAttributGruppe attributGruppe = GeschwindigkeitsAttributeTestDataProvider
			.gruppeWithGrundnetzDefaultwerte().geschwindigkeitAttribute(List.of(
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build()))
			.build();

		// act
		attributGruppe
			.insert(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build());

		// assert
		assertThat(attributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.3)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build());
	}

	@Test
	void insert_splitLRStartAndEnd() {
		// arrange
		GeschwindigkeitAttributGruppe attributGruppe = GeschwindigkeitsAttributeTestDataProvider
			.gruppeWithGrundnetzDefaultwerte().geschwindigkeitAttribute(List.of(
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.5)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.5, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build()))
			.build();

		// act
		attributGruppe
			.insert(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH).build());

		// assert
		assertThat(attributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.3)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build());
	}

	@Test
	void insert_worksAtLinestringEndings() {
		// arrange
		GeschwindigkeitAttributGruppe attributGruppe = GeschwindigkeitsAttributeTestDataProvider
			.gruppeWithGrundnetzDefaultwerte().geschwindigkeitAttribute(List.of(
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.5)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.5, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build()))
			.build();

		// act
		attributGruppe
			.insert(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH).build());

		// assert
		assertThat(attributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_40_KMH).build());
	}

	@Test
	void insert_defragment() {
		// arrange
		GeschwindigkeitAttributGruppe attributGruppe = GeschwindigkeitsAttributeTestDataProvider
			.gruppeWithGrundnetzDefaultwerte().geschwindigkeitAttribute(List.of(
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.5)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.5, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build()))
			.build();

		// act
		attributGruppe
			.insert(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build());

		// assert
		assertThat(attributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.6)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH).build());
	}

	@Test
	void mergeSegmentsKleinerAls_noMatchingSegment_doesNothing() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.4)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.4, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();
		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.39));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrderElementsOf(geschwindigkeitAttribute);
	}

	@Test
	void mergeSegmentsKleinerAls_segmentAtBeginning_mergeRight() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.2, 0.6)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.6)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
	}

	@Test
	void mergeSegmentsKleinerAls_mergeAll() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.2, 0.6).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 1).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(1.0));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1).build());
	}

	@Test
	void mergeSegmentsKleinerAls_segmentAtEnd_mergeLeft() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.4)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.4, 0.8)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.8, 1).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.4)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.4, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
	}

	@Test
	void mergeSegmentsKleinerAls_segmentInTheMiddle_mergeRight() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.4)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.4, 0.6).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.4)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.4, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
	}

	@Test
	void mergeSegmentsKleinerAls_konsekutivSegments_mergeBoth() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.1).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.1, 0.2).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.2, 0.4)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.4, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoSegments_mergeLeftAndRight() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.2, 0.8).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.8, 1).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoSegments_mergeRight() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.1).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.1, 0.5)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.5, 0.6).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.2));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.5)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
				GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.5, 1)
					.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoMiniSegmentsAtEnd_mergeTowardsBigSegment() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.2, 0.6)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 0.8)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.8, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactly(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build());
	}

	@Test
	void mergeSegmentsKleinerAls_onlyMiniSegments_mergeAll() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.3).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.5));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactly(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoMiniSegmentsAtStart_mergeTowardsBigSegment() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.2, 0.4)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.4, 0.8)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.8, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactly(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH).build());
	}

	@Test
	void mergeSegmentsKleinerAls_onlyOneSegment_doesNothing() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List
			.of(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactlyInAnyOrder(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1).build());
	}

	@Test
	void mergeSegmentsKleinerAls_segmentInTheMiddle_mergeUndFasseZusammen() {
		// arrange
		List<GeschwindigkeitAttribute> geschwindigkeitAttribute = List.of(
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 0.4)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.4, 0.6)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_100_KMH).build(),
			GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0.6, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build());
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = GeschwindigkeitAttributGruppe.builder()
			.geschwindigkeitAttribute(geschwindigkeitAttribute).build();

		// act
		geschwindigkeitAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(geschwindigkeitAttributGruppe.getGeschwindigkeitAttribute())
			.containsExactly(GeschwindigkeitsAttributeTestDataProvider.withLineareReferenz(0, 1)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN).build());
	}
}
