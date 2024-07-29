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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.provider.MappedFeatureTestDataProvider;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedFeature;

class NormalisierteMappedFeaturesTest {

	@Test
	void getLineareReferenzenNormalizedAndSorted_featuresLeer_RueckgabeLeer() {
		// Arrange
		NormalisierteMappedFeatures normalisierteMappedFeatures = NormalisierteMappedFeatures.of(
			Collections.emptyList());

		// Act
		List<LinearReferenzierterAbschnitt> result = normalisierteMappedFeatures
			.getLineareReferenzenNormalizedAndSorted();

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void getLineareReferenzenNormalizedAndSorted_ueberlappendeFeatures_lineareReferenzenSindKorrektGeschnittenUndSortiert() {
		// Arrange
		NormalisierteMappedFeatures normalisierteMappedFeatures = NormalisierteMappedFeatures.of(
			List.of(
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.7))
					.build(),
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
					.build()));

		// Act
		List<LinearReferenzierterAbschnitt> result = normalisierteMappedFeatures
			.getLineareReferenzenNormalizedAndSorted();

		// Assert
		assertThat(result).containsExactly(
			LinearReferenzierterAbschnitt.of(0, 0.5),
			LinearReferenzierterAbschnitt.of(0.5, 0.7),
			LinearReferenzierterAbschnitt.of(0.7, 1));
	}

	@Test
	void getLineareReferenzenNormalizedAndSorted_featuresMitLueckeInDerMitte_lineareReferenzenSindKorrektGeschnittenUndSortiert() {
		// Arrange
		NormalisierteMappedFeatures normalisierteMappedFeatures = NormalisierteMappedFeatures.of(
			List.of(
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.3))
					.build(),
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
					.build()));

		// Act
		List<LinearReferenzierterAbschnitt> result = normalisierteMappedFeatures
			.getLineareReferenzenNormalizedAndSorted();

		// Assert
		assertThat(result).containsExactly(
			LinearReferenzierterAbschnitt.of(0, 0.3),
			LinearReferenzierterAbschnitt.of(0.3, 0.5),
			LinearReferenzierterAbschnitt.of(0.5, 1));
	}

	@Test
	void getLineareReferenzenNormalizedAndSorted_featuresMitLueckeAmAnfang_lineareReferenzenSindKorrektGeschnittenUndSortiert() {
		// Arrange
		NormalisierteMappedFeatures normalisierteMappedFeatures = NormalisierteMappedFeatures.of(
			List.of(
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.5))
					.build(),
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
					.build()));

		// Act
		List<LinearReferenzierterAbschnitt> result = normalisierteMappedFeatures
			.getLineareReferenzenNormalizedAndSorted();

		// Assert
		assertThat(result).containsExactly(
			LinearReferenzierterAbschnitt.of(0, 0.2),
			LinearReferenzierterAbschnitt.of(0.2, 0.5),
			LinearReferenzierterAbschnitt.of(0.5, 1));
	}

	@Test
	void getLineareReferenzenNormalizedAndSorted_featuresMitLueckeAmEnde_lineareReferenzenSindKorrektGeschnittenUndSortiert() {
		// Arrange
		NormalisierteMappedFeatures normalisierteMappedFeatures = NormalisierteMappedFeatures.of(
			List.of(
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.3))
					.build(),
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.3, 0.8))
					.build()));

		// Act
		List<LinearReferenzierterAbschnitt> result = normalisierteMappedFeatures
			.getLineareReferenzenNormalizedAndSorted();

		// Assert
		assertThat(result).containsExactly(
			LinearReferenzierterAbschnitt.of(0, 0.3),
			LinearReferenzierterAbschnitt.of(0.3, 0.8),
			LinearReferenzierterAbschnitt.of(0.8, 1));
	}

	@Test
	void getLineareReferenzenNormalizedAndSorted_featuresMitGleicherLinearerReferenz_lineareReferenzenSindKorrektGeschnittenUndSortiert() {
		// Arrange
		NormalisierteMappedFeatures normalisierteMappedFeatures = NormalisierteMappedFeatures.of(
			List.of(
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.3))
					.build(),
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.3))
					.build(),
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.3, 1))
					.build()));

		// Act
		List<LinearReferenzierterAbschnitt> result = normalisierteMappedFeatures
			.getLineareReferenzenNormalizedAndSorted();

		// Assert
		assertThat(result).containsExactly(
			LinearReferenzierterAbschnitt.of(0, 0.3),
			LinearReferenzierterAbschnitt.of(0.3, 1));
	}

	@Test
	void testGetFeaturesFor_lineareReferenzNichtVorhanden_schmeisstRequireViolation() {
		// Arrange
		NormalisierteMappedFeatures normalisierteMappedFeaturesLeer = NormalisierteMappedFeatures.of(
			Collections.emptyList());

		NormalisierteMappedFeatures normalisierteMappedFeaturesMitAnderenReferenzen = NormalisierteMappedFeatures.of(
			List.of(
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
					.build(),
				MappedFeatureTestDataProvider.withDefaultValues()
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
					.build()));

		// Act & Assert
		assertThatThrownBy(
			() -> normalisierteMappedFeaturesLeer.getFeaturesFor(LinearReferenzierterAbschnitt.of(0, 0.5)))
				.isInstanceOf(RequireViolation.class);

		assertThatThrownBy(
			() -> normalisierteMappedFeaturesMitAnderenReferenzen.getFeaturesFor(
				LinearReferenzierterAbschnitt.of(0.2, 0.5)))
					.isInstanceOf(RequireViolation.class);
	}

	@Test
	void testGetFeaturesFor_lineareReferenzVorhanden_liefertDieRichtigenFeatures() {
		// Arrange
		Map<String, Object> attribut1 = new HashMap<>();
		attribut1.put("attribut", "1");
		Map<String, Object> attribut2 = new HashMap<>();
		attribut2.put("attribut", "2");

		NormalisierteMappedFeatures normalisierteMappedFeatures = NormalisierteMappedFeatures.of(
			List.of(
				MappedFeatureTestDataProvider.withProperties(attribut1)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.6))
					.build(),
				MappedFeatureTestDataProvider.withProperties(attribut2)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 1))
					.build()));

		// Act
		List<LinearReferenzierterAbschnitt> lineareReferenzen = normalisierteMappedFeatures
			.getLineareReferenzenNormalizedAndSorted();

		assertThat(lineareReferenzen).containsExactly(
			LinearReferenzierterAbschnitt.of(0, 0.2),
			LinearReferenzierterAbschnitt.of(0.2, 0.4),
			LinearReferenzierterAbschnitt.of(0.4, 0.6),
			LinearReferenzierterAbschnitt.of(0.6, 1));

		List<MappedFeature> features0_02 = normalisierteMappedFeatures.getFeaturesFor(
			LinearReferenzierterAbschnitt.of(0, 0.2));
		List<MappedFeature> features02_04 = normalisierteMappedFeatures.getFeaturesFor(
			LinearReferenzierterAbschnitt.of(0.2, 0.4));
		List<MappedFeature> features04_06 = normalisierteMappedFeatures.getFeaturesFor(
			LinearReferenzierterAbschnitt.of(0.4, 0.6));
		List<MappedFeature> features06_1 = normalisierteMappedFeatures.getFeaturesFor(
			LinearReferenzierterAbschnitt.of(0.6, 1));

		// Assert

		assertThat(features0_02).isEmpty();
		assertThat(features02_04).extracting(MappedFeature::getProperties).extracting(map -> map.get("attribut"))
			.containsExactlyInAnyOrder("1");
		assertThat(features04_06).extracting(MappedFeature::getProperties).extracting(map -> map.get("attribut"))
			.containsExactlyInAnyOrder("1", "2");
		assertThat(features06_1).extracting(MappedFeature::getProperties).extracting(map -> map.get("attribut"))
			.containsExactlyInAnyOrder("2");
	}
}