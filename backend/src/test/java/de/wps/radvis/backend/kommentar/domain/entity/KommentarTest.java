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

package de.wps.radvis.backend.kommentar.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;

class KommentarTest {
	@Test
	public void testConstructorRequiresNotNullValues() {
		// Act & Assert
		assertThatThrownBy(() -> new Kommentar("moin", null)).isInstanceOf(
			RequireViolation.class);
		assertThatThrownBy(() -> new Kommentar(null, BenutzerTestDataProvider.defaultBenutzer().build()))
			.isInstanceOf(
				RequireViolation.class);
	}

	@Test
	public void testLengthRestriction() {
		// Arranger
		String okayLongString = StringUtils.repeat("e", 4000);
		String tooLongString = StringUtils.repeat("e", 4001);

		// Act & Assert
		assertThat(new Kommentar(okayLongString, BenutzerTestDataProvider.defaultBenutzer().build()))
			.isNotNull();
		assertThatThrownBy(
			() -> new Kommentar(tooLongString, BenutzerTestDataProvider.defaultBenutzer().build())).isInstanceOf(
				RequireViolation.class);
	}
}