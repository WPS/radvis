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

package de.wps.radvis.backend.massnahme.domain.valueObject;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

class MassnahmeKonzeptIDTest {

	@Test
	void testeIdMitValidenSonderzeichen() {
		assertDoesNotThrow(() -> {
			MassnahmeKonzeptID.of("BC_027.1");
			MassnahmeKonzeptID.of("BC 027.1");
		});
	}

	@Test
	void testeCarriageReturnAndLineFeed() {
		assertThatThrownBy(() ->
			MassnahmeKonzeptID.of("BC_027.1" + (char) 13)
		).isInstanceOf(RequireViolation.class);
		assertThatThrownBy(() ->
			MassnahmeKonzeptID.of("BC 027.1" + (char) 10)
		).isInstanceOf(RequireViolation.class);
	}
}