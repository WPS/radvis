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

package de.wps.radvis.backend.netz.domain.valueObject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;

class KnotenAttributeTest {

	@Test
	void testGetterUndContructor_WerteVorhanden() {
		// arrange
		KnotenAttribute.KnotenAttributeBuilder attributeBuilder = KnotenAttribute.builder();
		attributeBuilder.kommentar(Kommentar.of("comment"));
		attributeBuilder.knotenForm(KnotenForm.QUERUNGSSTELLE_OHNE_SICHERUNG);

		KnotenAttribute attribute = attributeBuilder.build();

		// assert
		assertThat(attribute.getKommentar()).contains(Kommentar.of("comment"));
		assertThat(attribute.getKnotenForm()).contains(KnotenForm.QUERUNGSSTELLE_OHNE_SICHERUNG);
	}

	@Test
	void testGetterUndConstructor_OhneWerte() {
		// arrange
		KnotenAttribute attribute = KnotenAttribute.builder().build();

		// act + assert
		assertThat(attribute.getKommentar()).isEmpty();
		assertThat(attribute.getKnotenForm()).isEmpty();
	}

	@Test
	void testHasSameValues_gleicheAttributWerte_True() {
		// arrange
		KnotenAttribute.KnotenAttributeBuilder attributeBuilder = KnotenAttribute.builder();
		attributeBuilder.kommentar(Kommentar.of("comment"));
		attributeBuilder.knotenForm(KnotenForm.QUERUNGSSTELLE_OHNE_SICHERUNG);

		KnotenAttribute attribute = attributeBuilder.build();

		KnotenAttribute.KnotenAttributeBuilder otherAttributeBuilder = KnotenAttribute.builder();
		otherAttributeBuilder.kommentar(Kommentar.of("comment"));
		otherAttributeBuilder.knotenForm(KnotenForm.QUERUNGSSTELLE_OHNE_SICHERUNG);

		KnotenAttribute otherAttribute = otherAttributeBuilder.build();

		// act
		boolean result = attribute.equals(otherAttribute);

		// assert
		assertThat(result).isTrue();
	}

	@Test
	void testHasSameValues_False_Kommentar() {
		// arrange
		KnotenAttribute.KnotenAttributeBuilder attributeBuilder = KnotenAttribute.builder();
		KnotenAttribute.KnotenAttributeBuilder otherAttributeBuilder = KnotenAttribute.builder();

		attributeBuilder.kommentar(Kommentar.of("A"));
		otherAttributeBuilder.kommentar(Kommentar.of("B"));

		KnotenAttribute attribute = attributeBuilder.build();
		KnotenAttribute otherAttribute = otherAttributeBuilder.build();

		// act && assert
		assertThat(attribute).isNotEqualTo(otherAttribute);
	}

	@Test
	void testHasSameValues_True_EmptyAttribute() {
		// arrange
		KnotenAttribute attribute = KnotenAttribute.builder().build();
		KnotenAttribute otherAttribute = KnotenAttribute.builder().build();

		// act & assert
		assertThat(attribute).isEqualTo(otherAttribute);
	}

	@Test
	void testHasSameValue_mancheGleichMancheVerschieden_false() {
		// arrange
		KnotenAttribute.KnotenAttributeBuilder attributeBuilder = KnotenAttribute.builder();
		KnotenAttribute.KnotenAttributeBuilder otherAttributeBuilder = KnotenAttribute.builder();

		attributeBuilder.kommentar(Kommentar.of("comment"));
		attributeBuilder.knotenForm(KnotenForm.QUERUNGSSTELLE_OHNE_SICHERUNG);

		otherAttributeBuilder.kommentar(Kommentar.of("comment"));
		otherAttributeBuilder.knotenForm(KnotenForm.FAHRBAHNEINENGUNG);

		KnotenAttribute attribute = attributeBuilder.build();
		KnotenAttribute otherAttribute = otherAttributeBuilder.build();

		// act
		assertThat(attribute).isNotEqualTo(otherAttribute);
	}
}
