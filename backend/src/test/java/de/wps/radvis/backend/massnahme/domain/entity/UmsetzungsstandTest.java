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

package de.wps.radvis.backend.massnahme.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;

class UmsetzungsstandTest {

	@Test
	void isUmsetzungsstandBearbeitungGesperrt_nichtStorniertOderUmgesetzt_false() {
		// arrange
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
		Massnahme massnahme = MassnahmeTestDataProvider.withPflichtfelderAbPlanung()
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG).build();

		// act + assert
		assertThat(Umsetzungsstand.isUmsetzungsstandBearbeitungGesperrt(umsetzungsstand, massnahme)).isFalse();
	}

	@Test
	void isUmsetzungsstandBearbeitungGesperrt_aktualisierungAngefordert_false() {
		// arrange
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
		umsetzungsstand.fordereAktualisierungAn();
		Massnahme massnahme = MassnahmeTestDataProvider.withPflichtfelderAbPlanung()
			.umsetzungsstatus(Umsetzungsstatus.UMGESETZT).build();

		// act + assert
		assertThat(Umsetzungsstand.isUmsetzungsstandBearbeitungGesperrt(umsetzungsstand, massnahme)).isFalse();
	}

	@Test
	void isUmsetzungsstandBearbeitungGesperrt_keineAktualisierungAngefordertUndUmgesetzt_true() {
		// arrange
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
		Massnahme massnahme = MassnahmeTestDataProvider.withPflichtfelderAbPlanung()
			.umsetzungsstatus(Umsetzungsstatus.UMGESETZT).build();

		// act + assert
		assertThat(Umsetzungsstand.isUmsetzungsstandBearbeitungGesperrt(umsetzungsstand, massnahme)).isTrue();
	}

	@Test
	void isUmsetzungsstandBearbeitungGesperrt_keineAktualisierungAngefordertUndStorniert_true() {
		// arrange
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
		Massnahme massnahme = MassnahmeTestDataProvider.withPflichtfelderAbPlanung()
			.umsetzungsstatus(Umsetzungsstatus.STORNIERT).build();

		// act + assert
		assertThat(Umsetzungsstand.isUmsetzungsstandBearbeitungGesperrt(umsetzungsstand, massnahme)).isTrue();
	}
}
