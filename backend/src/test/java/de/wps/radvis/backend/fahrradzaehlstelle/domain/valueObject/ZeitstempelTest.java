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

package de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;

import org.junit.jupiter.api.Test;

class ZeitstempelTest {

	@Test
	void zeitstempel_hinUndHerkonvertieren() {
		String isoDateString = "2023-04-01T02:00:00+0200";
		Zeitstempel zeitstempelFromIsoString = Zeitstempel.of(isoDateString);

		Long epochSecs = zeitstempelFromIsoString.getValue();
		Zeitstempel zeitstempelFromEpochSecs = Zeitstempel.of(epochSecs);

		assertThat(zeitstempelFromEpochSecs).isEqualTo(zeitstempelFromIsoString);
		assertThat(zeitstempelFromEpochSecs.toZonedDateTime()).isEqualTo(zeitstempelFromIsoString.toZonedDateTime());

		String zeitstempelFromIsoStringBackToString = zeitstempelFromIsoString.toZonedDateTime()
			.format(Zeitstempel.dateTimeFormatter);
		String zeitstempelFromEpochSecsToString = zeitstempelFromEpochSecs.toZonedDateTime()
			.format(Zeitstempel.dateTimeFormatter);

		assertThat(Zeitstempel.of(zeitstempelFromIsoStringBackToString)).isEqualTo(zeitstempelFromIsoString);
		assertThat(Zeitstempel.of(zeitstempelFromEpochSecsToString)).isEqualTo(zeitstempelFromIsoString);

		assertThat(zeitstempelFromIsoStringBackToString).isEqualTo(isoDateString);
		assertThat(zeitstempelFromEpochSecsToString).isEqualTo(isoDateString);
	}

	@Test
	void zeitstempel_amAnfangDerStunde() {
		Zeitstempel zeitstempel_vorherigeStunde = Zeitstempel.of("2003-01-01T00:59:59+0100").amAnfangDerStunde();
		Zeitstempel zeitstempel1 = Zeitstempel.of("2003-01-01T01:00:00+0100").amAnfangDerStunde();
		Zeitstempel zeitstempel2 = Zeitstempel.of("2003-01-01T01:30:00+0100").amAnfangDerStunde();
		Zeitstempel zeitstempel3 = Zeitstempel.of("2003-01-01T01:59:59+0100").amAnfangDerStunde();
		Zeitstempel zeitstempel_naechsteStunde = Zeitstempel.of("2003-01-01T02:00:00+0100").amAnfangDerStunde();

		Zeitstempel zeitstempel_naechsterTag = Zeitstempel.of("2003-01-02T01:30:00+0100").amAnfangDerStunde();

		assertThat(zeitstempel1.toZonedDateTime().getHour())
			.isEqualTo(1);
		assertThat(zeitstempel1)
			.isEqualTo(zeitstempel2)
			.isEqualTo(zeitstempel3);
		assertThat(zeitstempel1)
			.isNotEqualTo(zeitstempel_naechsteStunde)
			.isNotEqualTo(zeitstempel_vorherigeStunde)
			.isNotEqualTo(zeitstempel_naechsterTag);

	}

	@Test
	void zeitstempel_amAnfangDesTages() {
		Zeitstempel zeitstempel_vorherigerTag = Zeitstempel.of("2003-01-01T00:59:59+0100").amAnfangDesTages();
		Zeitstempel zeitstempel1 = Zeitstempel.of("2003-01-02T01:00:00+0100").amAnfangDesTages();
		Zeitstempel zeitstempel2 = Zeitstempel.of("2003-01-02T03:30:00+0100").amAnfangDesTages();
		Zeitstempel zeitstempel3 = Zeitstempel.of("2003-01-02T23:59:59+0100").amAnfangDesTages();
		Zeitstempel zeitstempel_naechsterTag = Zeitstempel.of("2003-01-03T02:00:00+0100").amAnfangDesTages();

		Zeitstempel zeitstempel_naechsterMonat = Zeitstempel.of("2003-02-02T01:30:00+0100").amAnfangDesTages();

		assertThat(zeitstempel1.toZonedDateTime().getDayOfMonth())
			.isEqualTo(2);
		assertThat(zeitstempel1)
			.isEqualTo(zeitstempel2)
			.isEqualTo(zeitstempel3);
		assertThat(zeitstempel1)
			.isNotEqualTo(zeitstempel_naechsterTag)
			.isNotEqualTo(zeitstempel_vorherigerTag)
			.isNotEqualTo(zeitstempel_naechsterMonat);

	}

	@Test
	void zeitstempel_amAnfangDesMonats() {
		Zeitstempel zeitstempel_vorherigerMonat = Zeitstempel.of("2003-01-01T00:59:59+0100").amAnfangDesMonats();
		Zeitstempel zeitstempel1 = Zeitstempel.of("2003-02-01T01:00:00+0100").amAnfangDesMonats();
		Zeitstempel zeitstempel2 = Zeitstempel.of("2003-02-01T03:30:00+0100").amAnfangDesMonats();
		Zeitstempel zeitstempel3 = Zeitstempel.of("2003-02-15T23:59:59+0100").amAnfangDesMonats();
		Zeitstempel zeitstempel_naechsterMonat = Zeitstempel.of("2003-03-01T02:00:00+0100").amAnfangDesMonats();

		Zeitstempel zeitstempel_naechstesJahr = Zeitstempel.of("2004-02-02T01:30:00+0100").amAnfangDesMonats();

		assertThat(zeitstempel1.toZonedDateTime().getMonthValue())
			.isEqualTo(2);
		assertThat(zeitstempel1)
			.isEqualTo(zeitstempel2)
			.isEqualTo(zeitstempel3);
		assertThat(zeitstempel1)
			.isNotEqualTo(zeitstempel_naechsterMonat)
			.isNotEqualTo(zeitstempel_vorherigerMonat)
			.isNotEqualTo(zeitstempel_naechstesJahr);

	}

	@Test
	void zeitstempel_amAnfangDesJahres() {
		Zeitstempel zeitstempel_vorherigesJahr = Zeitstempel.of("2002-12-31T00:59:59+0100").amAnfangDesJahres();
		Zeitstempel zeitstempel1 = Zeitstempel.of("2003-01-01T01:00:00+0100").amAnfangDesJahres();
		Zeitstempel zeitstempel2 = Zeitstempel.of("2003-06-01T03:30:00+0100").amAnfangDesJahres();
		Zeitstempel zeitstempel3 = Zeitstempel.of("2003-12-15T23:59:59+0100").amAnfangDesJahres();
		Zeitstempel zeitstempel_naechstesJahr = Zeitstempel.of("2004-02-02T01:30:00+0100").amAnfangDesJahres();

		assertThat(zeitstempel1.toZonedDateTime().getYear())
			.isEqualTo(2003);
		assertThat(zeitstempel1)
			.isEqualTo(zeitstempel2)
			.isEqualTo(zeitstempel3);
		assertThat(zeitstempel1)
			.isNotEqualTo(zeitstempel_vorherigesJahr)
			.isNotEqualTo(zeitstempel_naechstesJahr);

	}

	@Test
	void zeitstempel_referenzStunde() {
		Zeitstempel zeitstempel1 = Zeitstempel.of("2003-01-01T01:00:00+0100").referenzStunde();
		Zeitstempel zeitstempel2 = Zeitstempel.of("2003-01-02T01:00:00+0100").referenzStunde();
		Zeitstempel zeitstempel3 = Zeitstempel.of("2003-03-01T01:00:00+0100").referenzStunde();
		Zeitstempel zeitstempel_andereStunde = Zeitstempel.of("2003-01-01T02:00:00+0100").referenzStunde();

		assertThat(zeitstempel1.toZonedDateTime().getHour())
			.isEqualTo(1);
		assertThat(zeitstempel1)
			.isEqualTo(zeitstempel2)
			.isEqualTo(zeitstempel3);
		assertThat(zeitstempel1)
			.isNotEqualTo(zeitstempel_andereStunde);

	}

	@Test
	void zeitstempel_referenzWochentag() {
		Zeitstempel zeitstempel1 = Zeitstempel.of("2003-01-01T01:00:00+0100").referenzWochentag();
		Zeitstempel zeitstempel2 = Zeitstempel.of("2003-01-08T01:00:00+0100").referenzWochentag();
		Zeitstempel zeitstempel3 = Zeitstempel.of("2003-01-15T01:00:00+0100").referenzWochentag();
		Zeitstempel zeitstempel_andererWochentag = Zeitstempel.of("2003-01-02T01:00:00+0100").referenzStunde();

		assertThat(zeitstempel1.toZonedDateTime().getDayOfWeek())
			.isEqualTo(DayOfWeek.WEDNESDAY);
		assertThat(zeitstempel1)
			.isEqualTo(zeitstempel2)
			.isEqualTo(zeitstempel3);
		assertThat(zeitstempel1)
			.isNotEqualTo(zeitstempel_andererWochentag);

	}

	@Test
	void zeitstempel_referenzMonat() {
		Zeitstempel zeitstempel1 = Zeitstempel.of("2003-01-01T01:00:00+0100").referenzMonat();
		Zeitstempel zeitstempel2 = Zeitstempel.of("2004-01-01T01:00:00+0100").referenzMonat();
		Zeitstempel zeitstempel3 = Zeitstempel.of("2005-01-01T01:00:00+0100").referenzMonat();
		Zeitstempel zeitstempel_andererMonat = Zeitstempel.of("2003-02-01T01:00:00+0100").referenzMonat();

		assertThat(zeitstempel1.toZonedDateTime().getMonthValue())
			.isEqualTo(1);
		assertThat(zeitstempel1)
			.isEqualTo(zeitstempel2)
			.isEqualTo(zeitstempel3);
		assertThat(zeitstempel1)
			.isNotEqualTo(zeitstempel_andererMonat);

	}
}