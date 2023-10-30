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

package de.wps.radvis.backend.manuellerimport.common.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ImportLogEintragTest {

	@Test
	public void equals_true() {
		// Arrange
		ImportLogEintrag importFehler1 = ImportLogEintrag.ofError("Fehler 1");
		ImportLogEintrag importFehler2 = ImportLogEintrag.ofError("Fehler 1");

		// Act
		boolean result = importFehler1.equals(importFehler2);

		// Assert
		assertThat(result).isTrue();
	}

	@Test
	public void equals_false() {
		// Arrange
		ImportLogEintrag importFehler1 = ImportLogEintrag.ofError("Fehler 1");
		ImportLogEintrag importFehler2 = ImportLogEintrag.ofError("Fehler 2");

		// Act
		boolean result = importFehler1.equals(importFehler2);

		// Assert
		assertThat(result).isFalse();
	}

	@Test
	public void hashcode_true() {
		// Arrange
		ImportLogEintrag importFehler1 = ImportLogEintrag.ofError("Fehler 1");
		ImportLogEintrag importFehler2 = ImportLogEintrag.ofError("Fehler 1");

		// Act
		boolean result = importFehler1.hashCode() == importFehler2.hashCode();

		// Assert
		assertThat(result).isTrue();
	}

	@Test
	public void hashcode_false() {
		// Arrange
		ImportLogEintrag importFehler1 = ImportLogEintrag.ofError("Fehler 1");
		ImportLogEintrag importFehler2 = ImportLogEintrag.ofError("Fehler 2");

		// Act
		boolean result = importFehler1.hashCode() == importFehler2.hashCode();

		// Assert
		assertThat(result).isFalse();
	}
}
