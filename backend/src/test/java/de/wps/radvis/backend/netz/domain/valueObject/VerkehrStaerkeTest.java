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

class VerkehrStaerkeTest {

	@Test
	void testToString() {
		assertThat(VerkehrStaerke.of(5)).hasToString("5 Fz/Tag");
	}

	@Test
	void testEquals() {
		VerkehrStaerke a = VerkehrStaerke.of(5);
		VerkehrStaerke b = VerkehrStaerke.of(5);
		VerkehrStaerke c = VerkehrStaerke.of(9);

		assertThat(a)
			.isEqualTo(b)
			.isNotEqualTo(c);
	}
}
