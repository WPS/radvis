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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

class StrassenNummerTest {
	@Test
	void canGetValue() {
		assertThat(StrassenNummer.of("12a")).hasToString("12a");
	}

	@Test
	void canCheckEqual() {
		StrassenNummer a = StrassenNummer.of("12a");
		StrassenNummer b = StrassenNummer.of("12a");
		StrassenNummer c = StrassenNummer.of("13b");

		assertThat(a)
			.isEqualTo(b)
			.isNotEqualTo(c);
	}

	@Test
	void nullInvalid() {
		assertThatExceptionOfType(NullPointerException.class).isThrownBy(
			() -> StrassenNummer.of(null));
	}
}