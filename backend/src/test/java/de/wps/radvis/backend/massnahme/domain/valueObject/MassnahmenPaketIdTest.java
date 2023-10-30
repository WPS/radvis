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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MassnahmenPaketIdTest {

	@Test
	void isValid() {
		assertThat(MassnahmenPaketId.isValid("ADK 11.0")).isTrue();
		assertThat(MassnahmenPaketId.isValid("LÖ 107.2")).isTrue();
		assertThat(MassnahmenPaketId.isValid("LÖ 107.22")).isTrue();
		assertThat(MassnahmenPaketId.isValid("S 11.3")).isTrue();
		assertThat(MassnahmenPaketId.isValid("ADK 11.")).isFalse();
		assertThat(MassnahmenPaketId.isValid("ADK 11.3\n")).isFalse();
		assertThat(MassnahmenPaketId.isValid("BC 027.1\n"
			+ "BC 027.1")).isFalse();
		assertThat(MassnahmenPaketId.isValid("FN 2298.3")).isTrue();
		assertThat(MassnahmenPaketId.isValid("FN 206.1A")).isTrue();
		assertThat(MassnahmenPaketId.isValid("FN 206.1C")).isTrue();
		assertThat(MassnahmenPaketId.isValid("FN 206.1CC")).isFalse();
		assertThat(MassnahmenPaketId.isValid("KUEN 106.3")).isTrue();
	}
}