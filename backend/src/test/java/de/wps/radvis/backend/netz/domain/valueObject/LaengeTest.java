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

class LaengeTest {

	@Test
	void doubleToString() {
		assertThat(Laenge.of(42d)).hasToString("42,00 m");
	}

	@Test
	void stringToString() {
		assertThat(Laenge.of("1337")).hasToString("1337,00 m");
	}

	@Test
	void nullInvalid() {
		assertThatExceptionOfType(NullPointerException.class).isThrownBy(
			() -> Laenge.of(null));
	}

	@Test
	void canCheckEqual() {
		Laenge a = Laenge.of(42d);
		Laenge b = Laenge.of("42d");
		Laenge c = Laenge.of(1337d);

		assertThat(a)
			.isEqualTo(b)
			.isNotEqualTo(c);
	}
}
