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

package de.wps.radvis.backend.quellimport.ttsib.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.quellimport.ttsib.domain.KeinMittelstreifenException;
import de.wps.radvis.backend.quellimport.ttsib.domain.valueObject.TtSibEinordnung;
import de.wps.radvis.backend.quellimport.ttsib.domain.valueObject.TtSibQuerschnittArt;

class TtSibQuerschnittTest {

	@Test
	void addStreifen_EsDarfNurEinenMStreifenGeben() {
		// arrange
		TtSibQuerschnitt ttSibQuerschnitt = new TtSibQuerschnitt();

		TtSibStreifen mitte0 = new TtSibStreifen(1, 2, TtSibEinordnung.MITTE, 0,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);
		TtSibStreifen links1 = new TtSibStreifen(3, 4, TtSibEinordnung.LINKS, 1,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);
		TtSibStreifen mitte2 = new TtSibStreifen(5, 6, TtSibEinordnung.MITTE, 2,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);

		// act
		ttSibQuerschnitt.addStreifen(mitte0);
		ttSibQuerschnitt.addStreifen(links1);

		// act & assert
		Assertions
			.assertThatThrownBy(() -> ttSibQuerschnitt.addStreifen(mitte2))
			.isInstanceOf(RequireViolation.class)
			.hasMessage("Es gibt bereits einen Streifen M");
	}

	@Test
	void addStreifen_StreifenEinordnungNummerSindWiderspruchfrei() {
		// arrange
		TtSibQuerschnitt ttSibQuerschnitt = new TtSibQuerschnitt();

		TtSibStreifen links1 = new TtSibStreifen(1, 2, TtSibEinordnung.LINKS, 1,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);
		TtSibStreifen links2 = new TtSibStreifen(3, 4, TtSibEinordnung.LINKS, 2,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);
		TtSibStreifen links2nochmal = new TtSibStreifen(5, 6, TtSibEinordnung.LINKS, 2,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);

		// act
		ttSibQuerschnitt.addStreifen(links1);
		ttSibQuerschnitt.addStreifen(links2);

		// act & assert
		Assertions
			.assertThatThrownBy(() -> ttSibQuerschnitt.addStreifen(links2nochmal))
			.isInstanceOf(RequireViolation.class)
			.hasMessage("Streifen mit TtSibEinordnung Links hat bereits die Nummer 2");
	}

	@Test
	void ermittleStreifenversatze_soll_streifenversatze_korrekt_ermitteln() throws Exception {
		// arrange
		TtSibStreifen mitte0 = new TtSibStreifen(4, 8, TtSibEinordnung.MITTE, 0,
			TtSibQuerschnittArt.RADWEG);
		TtSibStreifen links1 = new TtSibStreifen(3, 7, TtSibEinordnung.LINKS, 1,
			TtSibQuerschnittArt.SCHLITZRINNE);
		TtSibStreifen links2 = new TtSibStreifen(2, 6, TtSibEinordnung.LINKS, 2,
			TtSibQuerschnittArt.RADWEG_AUCH_RAD_UND_GEHWEG);
		TtSibStreifen links3 = new TtSibStreifen(6, 10, TtSibEinordnung.LINKS, 3,
			TtSibQuerschnittArt.RADWEG_AUCH_RAD_UND_GEHWEG);
		TtSibStreifen links4 = new TtSibStreifen(5, 11, TtSibEinordnung.LINKS, 4,
			TtSibQuerschnittArt.BANKETT);
		TtSibStreifen rechts1 = new TtSibStreifen(10, 20, TtSibEinordnung.RECHTS, 1,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);

		TtSibQuerschnitt ttSibQuerschnitt = new TtSibQuerschnitt();
		ttSibQuerschnitt.addStreifen(mitte0);
		ttSibQuerschnitt.addStreifen(links1);
		ttSibQuerschnitt.addStreifen(links2);
		ttSibQuerschnitt.addStreifen(links3);
		ttSibQuerschnitt.addStreifen(links4);
		ttSibQuerschnitt.addStreifen(rechts1);

		// act
		Set<TtSibTeilabschnitt.Radwegstreifenversatz> versatze = ttSibQuerschnitt.ermittleStreifenversatze();

		// assert
		assertThat(versatze)
			.hasSize(4)
			.contains(new TtSibTeilabschnitt.Radwegstreifenversatz(7, 14, TtSibEinordnung.RECHTS))
			.contains(new TtSibTeilabschnitt.Radwegstreifenversatz(0, 0, TtSibEinordnung.MITTE))
			.contains(new TtSibTeilabschnitt.Radwegstreifenversatz(6, 14, TtSibEinordnung.LINKS))
			.contains(new TtSibTeilabschnitt.Radwegstreifenversatz(10, 22, TtSibEinordnung.LINKS));
	}

	@Test
	void ermittleStreifenversatze_soll_leeres_Ergebnis_liefern_wenn_keine_radwegstreifen_vorhanden() throws Exception {
		// arrange
		TtSibStreifen mitte0 = new TtSibStreifen(4, 8, TtSibEinordnung.MITTE, 0,
			TtSibQuerschnittArt.GEHWEG);
		TtSibStreifen links1 = new TtSibStreifen(3, 7, TtSibEinordnung.LINKS, 1,
			TtSibQuerschnittArt.SCHLITZRINNE);
		TtSibStreifen links2 = new TtSibStreifen(2, 6, TtSibEinordnung.LINKS, 2,
			TtSibQuerschnittArt.BANKETT);

		TtSibQuerschnitt ttSibQuerschnitt = new TtSibQuerschnitt();
		ttSibQuerschnitt.addStreifen(mitte0);
		ttSibQuerschnitt.addStreifen(links1);
		ttSibQuerschnitt.addStreifen(links2);

		// act
		Set<TtSibTeilabschnitt.Radwegstreifenversatz> versatze = ttSibQuerschnitt.ermittleStreifenversatze();

		// assert
		assertThat(versatze).isEmpty();
	}

	@Test
	void ermittleStreifenversatze_soll_fehler_werfen_wenn_kein_mittelstreifen_vorhanden() {
		// arrange
		TtSibStreifen rechts0 = new TtSibStreifen(4, 8, TtSibEinordnung.RECHTS, 0,
			TtSibQuerschnittArt.GEHWEG);
		TtSibStreifen links1 = new TtSibStreifen(3, 7, TtSibEinordnung.LINKS, 1,
			TtSibQuerschnittArt.RADWEG);
		TtSibStreifen links2 = new TtSibStreifen(2, 6, TtSibEinordnung.LINKS, 2,
			TtSibQuerschnittArt.BANKETT);

		TtSibQuerschnitt ttSibQuerschnitt = new TtSibQuerschnitt();
		ttSibQuerschnitt.addStreifen(rechts0);
		ttSibQuerschnitt.addStreifen(links1);
		ttSibQuerschnitt.addStreifen(links2);

		// act & assert
		assertThatThrownBy(ttSibQuerschnitt::ermittleStreifenversatze)
			.isInstanceOf(KeinMittelstreifenException.class);
	}
}
