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

package de.wps.radvis.backend.fahrradzaehlstelle.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.Channel;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.FahrradzaehlDatenEintrag;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ArtDerAuswertung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelBezeichnung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ChannelId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.DatenEintrag;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleAuswertung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstand;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstatus;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zeitstempel;

class FahrradzaehlstelleServiceTest {

	FahrradzaehlstelleService fahrradzaehlstelleService;
	FahrradzaehlstelleRepository fahrradzaehlstelleRepository;

	@BeforeEach
	public void setup() {
		fahrradzaehlstelleRepository = mock(FahrradzaehlstelleRepository.class);
		fahrradzaehlstelleService = new FahrradzaehlstelleService(fahrradzaehlstelleRepository);
	}

	@Test
	public void test_auswertungProStunde() {
		when(fahrradzaehlstelleRepository.getChannels(any())).thenReturn(createChannelsWithData(
			new HashMap<>() {{
				put(Zeitstempel.of("2000-01-01T01:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T02:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T03:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T04:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T05:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T06:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T07:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T08:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T09:00:00+0100"), 9999L);

				put(Zeitstempel.of("2000-01-02T01:00:00+0100"), 10L);
				put(Zeitstempel.of("2000-01-02T02:00:00+0100"), 20L);
				put(Zeitstempel.of("2000-01-02T03:00:00+0100"), 30L);
				put(Zeitstempel.of("2000-01-02T04:00:00+0100"), 40L);
				put(Zeitstempel.of("2000-01-02T05:00:00+0100"), 50L);
				put(Zeitstempel.of("2000-01-02T06:00:00+0100"), 60L);
				put(Zeitstempel.of("2000-01-02T07:00:00+0100"), 70L);
				put(Zeitstempel.of("2000-01-02T08:00:00+0100"), 80L);
				put(Zeitstempel.of("2000-01-02T09:00:00+0100"), 90L);
				// = 450

				put(Zeitstempel.of("2000-01-03T01:00:00+0100"), 20L);
				put(Zeitstempel.of("2000-01-03T02:00:00+0100"), 40L);
				put(Zeitstempel.of("2000-01-03T03:00:00+0100"), 60L);
				put(Zeitstempel.of("2000-01-03T04:00:00+0100"), 80L);
				put(Zeitstempel.of("2000-01-03T05:00:00+0100"), 100L);
				put(Zeitstempel.of("2000-01-03T06:00:00+0100"), 120L);
				put(Zeitstempel.of("2000-01-03T07:00:00+0100"), 140L);
				put(Zeitstempel.of("2000-01-03T08:00:00+0100"), 160L);
				put(Zeitstempel.of("2000-01-03T09:00:00+0100"), 180L);
				// = 900

				put(Zeitstempel.of("2000-01-04T01:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T02:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T03:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T04:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T05:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T06:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T07:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T08:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T09:00:00+0100"), 9999L);
			}},
			new HashMap<>() {{
				put(Zeitstempel.of("2000-01-01T01:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T02:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T03:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T04:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T05:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T06:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T07:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T08:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-01T09:00:00+0100"), 9999L);

				put(Zeitstempel.of("2000-01-02T01:00:00+0100"), 50L);
				put(Zeitstempel.of("2000-01-02T01:30:00+0100"), 50L);
				put(Zeitstempel.of("2000-01-02T02:00:00+0100"), 200L);
				put(Zeitstempel.of("2000-01-02T03:00:00+0100"), 300L);
				put(Zeitstempel.of("2000-01-02T04:00:00+0100"), 400L);
				put(Zeitstempel.of("2000-01-02T05:00:00+0100"), 500L);
				put(Zeitstempel.of("2000-01-02T06:00:00+0100"), 600L);
				put(Zeitstempel.of("2000-01-02T07:00:00+0100"), 700L);
				put(Zeitstempel.of("2000-01-02T08:00:00+0100"), 800L);
				put(Zeitstempel.of("2000-01-02T09:00:00+0100"), 900L);
				// = 4500

				put(Zeitstempel.of("2000-01-03T01:00:00+0100"), 200L);
				put(Zeitstempel.of("2000-01-03T02:00:00+0100"), 400L);
				put(Zeitstempel.of("2000-01-03T03:00:00+0100"), 600L);
				put(Zeitstempel.of("2000-01-03T04:00:00+0100"), 800L);
				put(Zeitstempel.of("2000-01-03T05:00:00+0100"), 1000L);
				put(Zeitstempel.of("2000-01-03T06:00:00+0100"), 1200L);
				put(Zeitstempel.of("2000-01-03T07:00:00+0100"), 1400L);
				put(Zeitstempel.of("2000-01-03T08:00:00+0100"), 1600L);
				put(Zeitstempel.of("2000-01-03T09:00:00+0100"), 1800L);
				// = 9000

				put(Zeitstempel.of("2000-01-04T01:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T02:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T03:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T04:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T05:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T06:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T07:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T08:00:00+0100"), 9999L);
				put(Zeitstempel.of("2000-01-04T09:00:00+0100"), 9999L);
			}}

		));

		FahrradzaehlstelleAuswertung auswertung = fahrradzaehlstelleService.getAuswertung(
			List.of(1L),
			Instant.parse("2000-01-02T00:00:00Z"),
			Instant.parse("2000-01-03T00:00:00Z"),
			ArtDerAuswertung.DURCHSCHNITT_PRO_STUNDE
		);

		assertThat(auswertung.getDaten()).containsExactly(
			new DatenEintrag("1h", (110 + 220) / 2.),
			new DatenEintrag("2h", (220 + 440) / 2.),
			new DatenEintrag("3h", (330 + 660) / 2.),
			new DatenEintrag("4h", (440 + 880) / 2.),
			new DatenEintrag("5h", (550 + 1100) / 2.),
			new DatenEintrag("6h", (660 + 1320) / 2.),
			new DatenEintrag("7h", (770 + 1540) / 2.),
			new DatenEintrag("8h", (880 + 1760) / 2.),
			new DatenEintrag("9h", (990 + 1980) / 2.)
		);

		assertThat(auswertung.getDurchschnitt()).isEqualTo((450 + 900 + 4500 + 9000) / 18.);
		assertThat(auswertung.getGesamtsumme()).isEqualTo(450 + 900 + 4500 + 9000);
		assertThat(auswertung.getSpitze()).isEqualTo("03.01.00 09:00");
		assertThat(auswertung.getSpitzenwert()).isEqualTo(1980);
	}

	@Test
	public void test_auswertungProWochentag() {

		when(fahrradzaehlstelleRepository.getChannels(any())).thenReturn(createChannelsWithData(
			new HashMap<>() {
				{
					put(Zeitstempel.of("2001-01-01T01:00:00+0100"), 5L);
					put(Zeitstempel.of("2001-01-01T05:00:00+0100"), 5L);

					put(Zeitstempel.of("2001-01-02T01:00:00+0100"), 20L);
					put(Zeitstempel.of("2001-01-03T01:00:00+0100"), 30L);
					put(Zeitstempel.of("2001-01-04T01:00:00+0100"), 40L);
					put(Zeitstempel.of("2001-01-05T01:00:00+0100"), 50L);
					put(Zeitstempel.of("2001-01-06T01:00:00+0100"), 60L);
					put(Zeitstempel.of("2001-01-07T01:00:00+0100"), 70L);
					// = 280

					put(Zeitstempel.of("2001-01-08T01:00:00+0100"), 40L);
					put(Zeitstempel.of("2001-01-08T23:00:00+0100"), 40L);

					put(Zeitstempel.of("2001-01-09T01:00:00+0100"), 90L);
					put(Zeitstempel.of("2001-01-10T01:00:00+0100"), 100L);
					put(Zeitstempel.of("2001-01-11T01:00:00+0100"), 110L);
					put(Zeitstempel.of("2001-01-12T01:00:00+0100"), 120L);
					put(Zeitstempel.of("2001-01-13T01:00:00+0100"), 130L);
					put(Zeitstempel.of("2001-01-14T01:00:00+0100"), 140L);
					// = 770

					put(Zeitstempel.of("2001-01-15T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2001-01-16T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2001-01-17T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2001-01-18T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2001-01-19T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2001-01-20T01:00:00+0100"), 9999L);
				}
			},
			new HashMap<>() {
				{
					put(Zeitstempel.of("2001-01-01T01:00:00+0100"), 100L);
					put(Zeitstempel.of("2001-01-02T01:00:00+0100"), 200L);
					put(Zeitstempel.of("2001-01-03T01:00:00+0100"), 300L);
					put(Zeitstempel.of("2001-01-04T01:00:00+0100"), 400L);
					put(Zeitstempel.of("2001-01-05T01:00:00+0100"), 500L);
					put(Zeitstempel.of("2001-01-06T01:00:00+0100"), 600L);
					put(Zeitstempel.of("2001-01-07T01:00:00+0100"), 700L);
					// = 2800

					put(Zeitstempel.of("2001-01-08T01:00:00+0100"), 800L);
					put(Zeitstempel.of("2001-01-09T01:00:00+0100"), 900L);
					put(Zeitstempel.of("2001-01-10T01:00:00+0100"), 1000L);
					put(Zeitstempel.of("2001-01-11T01:00:00+0100"), 1100L);
					put(Zeitstempel.of("2001-01-12T01:00:00+0100"), 1200L);
					put(Zeitstempel.of("2001-01-13T01:00:00+0100"), 1300L);
					put(Zeitstempel.of("2001-01-14T01:00:00+0100"), 1400L);
					// = 7700

					put(Zeitstempel.of("2001-01-15T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2001-01-16T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2001-01-17T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2001-01-18T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2001-01-19T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2001-01-20T01:00:00+0100"), 9999L);
				}
			}
		));

		FahrradzaehlstelleAuswertung auswertung = fahrradzaehlstelleService.getAuswertung(
			List.of(1L),
			Instant.parse("2001-01-01T00:00:00Z"),
			Instant.parse("2001-01-14T00:00:00Z"),
			ArtDerAuswertung.DURCHSCHNITT_PRO_WOCHENTAG
		);

		assertThat(auswertung.getDaten()).containsExactly(
			new DatenEintrag("Mo.", (110 + 880) / 2.),
			new DatenEintrag("Di.", (220 + 990) / 2.),
			new DatenEintrag("Mi.", (330 + 1100) / 2.),
			new DatenEintrag("Do.", (440 + 1210) / 2.),
			new DatenEintrag("Fr.", (550 + 1320) / 2.),
			new DatenEintrag("Sa.", (660 + 1430) / 2.),
			new DatenEintrag("So.", (770 + 1540) / 2.)
		);

		assertThat(auswertung.getDurchschnitt()).isEqualTo((280 + 770 + 2800 + 7700) / 14.);
		assertThat(auswertung.getGesamtsumme()).isEqualTo(280 + 770 + 2800 + 7700);
		assertThat(auswertung.getSpitze()).isEqualTo("14.01.01");
		assertThat(auswertung.getSpitzenwert()).isEqualTo(1540);
	}

	@Test
	public void test_auswertungProMonat() {

		when(fahrradzaehlstelleRepository.getChannels(any())).thenReturn(createChannelsWithData(
			new HashMap<>() {
				{
					put(Zeitstempel.of("2001-01-31T01:00:00+0100"), 10L);

					put(Zeitstempel.of("2001-02-01T01:00:00+0100"), 7L);
					put(Zeitstempel.of("2001-02-01T05:00:00+0100"), 3L);
					put(Zeitstempel.of("2001-02-02T01:00:00+0100"), 5L);
					put(Zeitstempel.of("2001-02-03T01:00:00+0100"), 5L);

					put(Zeitstempel.of("2001-03-08T01:00:00+0100"), 15L);
					put(Zeitstempel.of("2001-03-08T23:00:00+0100"), 15L);

					put(Zeitstempel.of("2001-04-09T01:00:00+0100"), 40L);
					put(Zeitstempel.of("2001-05-10T01:00:00+0100"), 50L);
					put(Zeitstempel.of("2001-06-11T01:00:00+0100"), 60L);
					put(Zeitstempel.of("2001-07-12T01:00:00+0100"), 70L);
					put(Zeitstempel.of("2001-08-13T01:00:00+0100"), 80L);
					put(Zeitstempel.of("2001-09-14T01:00:00+0100"), 90L);
					put(Zeitstempel.of("2001-10-14T01:00:00+0100"), 100L);
					put(Zeitstempel.of("2001-11-14T01:00:00+0100"), 110L);
					put(Zeitstempel.of("2001-12-14T01:00:00+0100"), 120L);
					// = 780

					put(Zeitstempel.of("2002-01-09T01:00:00+0100"), 130L);
					put(Zeitstempel.of("2002-02-10T01:00:00+0100"), 140L);
					put(Zeitstempel.of("2002-03-11T01:00:00+0100"), 150L);
					put(Zeitstempel.of("2002-04-09T01:00:00+0100"), 160L);
					put(Zeitstempel.of("2002-05-10T01:00:00+0100"), 170L);
					put(Zeitstempel.of("2002-06-11T01:00:00+0100"), 180L);
					put(Zeitstempel.of("2002-07-12T01:00:00+0100"), 190L);
					put(Zeitstempel.of("2002-08-13T01:00:00+0100"), 200L);
					put(Zeitstempel.of("2002-09-14T01:00:00+0100"), 210L);
					put(Zeitstempel.of("2002-10-14T01:00:00+0100"), 220L);
					put(Zeitstempel.of("2002-11-14T01:00:00+0100"), 230L);
					put(Zeitstempel.of("2002-12-14T01:00:00+0100"), 240L);
					// = 2220

					put(Zeitstempel.of("2003-01-01T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2003-02-16T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2003-03-17T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2003-04-18T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2003-05-19T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2003-06-20T01:00:00+0100"), 9999L);
				}
			},
			new HashMap<>() {
				{
					put(Zeitstempel.of("2001-01-01T01:00:00+0100"), 100L);

					put(Zeitstempel.of("2001-02-01T01:00:00+0100"), 70L);
					put(Zeitstempel.of("2001-02-01T05:00:00+0100"), 30L);
					put(Zeitstempel.of("2001-02-02T01:00:00+0100"), 50L);
					put(Zeitstempel.of("2001-02-03T01:00:00+0100"), 50L);

					put(Zeitstempel.of("2001-03-08T01:00:00+0100"), 150L);
					put(Zeitstempel.of("2001-03-08T23:00:00+0100"), 150L);

					put(Zeitstempel.of("2001-04-19T01:00:00+0100"), 400L);
					put(Zeitstempel.of("2001-05-20T01:00:00+0100"), 500L);
					put(Zeitstempel.of("2001-06-21T01:00:00+0100"), 600L);
					put(Zeitstempel.of("2001-07-22T01:00:00+0100"), 700L);
					put(Zeitstempel.of("2001-08-23T01:00:00+0100"), 800L);
					put(Zeitstempel.of("2001-09-24T01:00:00+0100"), 900L);
					put(Zeitstempel.of("2001-10-24T01:00:00+0100"), 1000L);
					put(Zeitstempel.of("2001-11-24T01:00:00+0100"), 1100L);
					put(Zeitstempel.of("2001-12-24T01:00:00+0100"), 1200L);
					// = 7800

					put(Zeitstempel.of("2002-01-29T01:00:00+0100"), 1300L);
					put(Zeitstempel.of("2002-02-20T01:00:00+0100"), 1400L);
					put(Zeitstempel.of("2002-03-21T01:00:00+0100"), 1500L);
					put(Zeitstempel.of("2002-04-19T01:00:00+0100"), 1600L);
					put(Zeitstempel.of("2002-05-20T01:00:00+0100"), 1700L);
					put(Zeitstempel.of("2002-06-21T01:00:00+0100"), 1800L);
					put(Zeitstempel.of("2002-07-22T01:00:00+0100"), 1900L);
					put(Zeitstempel.of("2002-08-23T01:00:00+0100"), 2000L);
					put(Zeitstempel.of("2002-09-24T01:00:00+0100"), 2100L);
					put(Zeitstempel.of("2002-10-24T01:00:00+0100"), 2200L);
					put(Zeitstempel.of("2002-11-24T01:00:00+0100"), 2300L);
					put(Zeitstempel.of("2002-12-24T01:00:00+0100"), 2400L);
					// = 22200

					put(Zeitstempel.of("2003-01-01T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2003-02-16T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2003-03-17T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2003-04-18T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2003-05-19T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2003-06-20T01:00:00+0100"), 9999L);
				}
			}
		));

		FahrradzaehlstelleAuswertung auswertung = fahrradzaehlstelleService.getAuswertung(
			List.of(1L),
			Instant.parse("2001-01-01T00:00:00Z"),
			Instant.parse("2002-12-31T00:00:00Z"),
			ArtDerAuswertung.DURCHSCHNITT_PRO_MONAT
		);

		assertThat(auswertung.getDaten()).containsExactly(
			new DatenEintrag("Jan.", (110 + 1430) / 2.),
			new DatenEintrag("Feb.", (220 + 1540) / 2.),
			new DatenEintrag("März", (330 + 1650) / 2.),
			new DatenEintrag("Apr.", (440 + 1760) / 2.),
			new DatenEintrag("Mai", (550 + 1870) / 2.),
			new DatenEintrag("Juni", (660 + 1980) / 2.),
			new DatenEintrag("Juli", (770 + 2090) / 2.),
			new DatenEintrag("Aug.", (880 + 2200) / 2.),
			new DatenEintrag("Sept.", (990 + 2310) / 2.),
			new DatenEintrag("Okt.", (1100 + 2420) / 2.),
			new DatenEintrag("Nov.", (1210 + 2530) / 2.),
			new DatenEintrag("Dez.", (1320 + 2640) / 2.)
		);

		assertThat(auswertung.getDurchschnitt()).isEqualTo((780 + 2220 + 7800 + 22200) / 24.);
		assertThat(auswertung.getGesamtsumme()).isEqualTo(780 + 2220 + 7800 + 22200);
		assertThat(auswertung.getSpitze()).isEqualTo("Dez. 2002");
		assertThat(auswertung.getSpitzenwert()).isEqualTo(2640);
	}

	@Test
	public void test_auswertungProJahr() {

		when(fahrradzaehlstelleRepository.getChannels(any())).thenReturn(createChannelsWithData(
			new HashMap<>() {
				{
					put(Zeitstempel.of("2001-01-31T01:00:00+0100"), 10L);

					put(Zeitstempel.of("2002-02-01T01:00:00+0100"), 7L);
					put(Zeitstempel.of("2002-02-01T05:00:00+0100"), 3L);
					put(Zeitstempel.of("2002-02-02T01:00:00+0100"), 5L);
					put(Zeitstempel.of("2002-02-03T01:00:00+0100"), 5L);

					put(Zeitstempel.of("2003-03-08T01:00:00+0100"), 15L);
					put(Zeitstempel.of("2003-03-08T23:00:00+0100"), 15L);

					put(Zeitstempel.of("2004-04-09T01:00:00+0100"), 40L);
					put(Zeitstempel.of("2005-05-10T01:00:00+0100"), 50L);
					put(Zeitstempel.of("2006-06-11T01:00:00+0100"), 60L);
					put(Zeitstempel.of("2007-07-12T01:00:00+0100"), 70L);
					put(Zeitstempel.of("2008-08-13T01:00:00+0100"), 80L);
					put(Zeitstempel.of("2009-09-14T01:00:00+0100"), 90L);
					put(Zeitstempel.of("2010-10-14T01:00:00+0100"), 100L);
					put(Zeitstempel.of("2011-11-14T01:00:00+0100"), 110L);
					put(Zeitstempel.of("2012-12-14T01:00:00+0100"), 120L);
					// = 780

					put(Zeitstempel.of("2013-01-01T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2014-02-16T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2000-03-17T01:00:00+0100"), 9999L);
				}
			},
			new HashMap<>() {
				{
					put(Zeitstempel.of("2001-01-01T01:00:00+0100"), 100L);

					put(Zeitstempel.of("2002-02-01T01:00:00+0100"), 70L);
					put(Zeitstempel.of("2002-02-01T05:00:00+0100"), 30L);
					put(Zeitstempel.of("2002-02-02T01:00:00+0100"), 50L);
					put(Zeitstempel.of("2002-02-03T01:00:00+0100"), 50L);

					put(Zeitstempel.of("2003-03-08T01:00:00+0100"), 150L);
					put(Zeitstempel.of("2003-03-08T23:00:00+0100"), 150L);

					put(Zeitstempel.of("2004-04-19T01:00:00+0100"), 400L);
					put(Zeitstempel.of("2005-05-20T01:00:00+0100"), 500L);
					put(Zeitstempel.of("2006-06-21T01:00:00+0100"), 600L);
					put(Zeitstempel.of("2007-07-22T01:00:00+0100"), 700L);
					put(Zeitstempel.of("2008-08-23T01:00:00+0100"), 800L);
					put(Zeitstempel.of("2009-09-24T01:00:00+0100"), 900L);
					put(Zeitstempel.of("2010-10-24T01:00:00+0100"), 1000L);
					put(Zeitstempel.of("2011-11-24T01:00:00+0100"), 1100L);
					put(Zeitstempel.of("2012-12-24T01:00:00+0100"), 1200L);
					// = 7800

					put(Zeitstempel.of("2013-04-18T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2014-05-19T01:00:00+0100"), 9999L);
					put(Zeitstempel.of("2000-06-20T01:00:00+0100"), 9999L);
				}
			}
		));

		FahrradzaehlstelleAuswertung auswertung = fahrradzaehlstelleService.getAuswertung(
			List.of(1L),
			Instant.parse("2001-01-01T00:00:00Z"),
			Instant.parse("2012-12-31T00:00:00Z"),
			ArtDerAuswertung.SUMME_PRO_JAHR
		);

		assertThat(auswertung.getDaten()).containsExactly(
			new DatenEintrag("2001", 110.),
			new DatenEintrag("2002", 220.),
			new DatenEintrag("2003", 330.),
			new DatenEintrag("2004", 440.),
			new DatenEintrag("2005", 550.),
			new DatenEintrag("2006", 660.),
			new DatenEintrag("2007", 770.),
			new DatenEintrag("2008", 880.),
			new DatenEintrag("2009", 990.),
			new DatenEintrag("2010", 1100.),
			new DatenEintrag("2011", 1210.),
			new DatenEintrag("2012", 1320.)
		);

		assertThat(auswertung.getDurchschnitt()).isEqualTo((780 + 7800) / 12.);
		assertThat(auswertung.getGesamtsumme()).isEqualTo(780 + 7800);
		assertThat(auswertung.getSpitze()).isEqualTo("2012");
		assertThat(auswertung.getSpitzenwert()).isEqualTo(1320);
	}

	@SafeVarargs
	private List<Channel> createChannelsWithData(Map<Zeitstempel, Long>... channelData) {
		List<Channel> result = new ArrayList<>();
		long i = 0;
		for (Map<Zeitstempel, Long> channelDatum : channelData) {
			Channel channel = Channel.builder()
				.channelId(ChannelId.of(i))
				.channelBezeichnung(ChannelBezeichnung.of(String.valueOf(i)))
				.fahrradzaehlDaten(channelDatum.entrySet().stream().collect(Collectors.toMap(
					Map.Entry::getKey,
					entry -> FahrradzaehlDatenEintrag.builder()
						.zaehlstatus(Zaehlstatus.of(0))
						.zaehlstand(Zaehlstand.of(entry.getValue()))
						.build()
				)))
				.build();
			i++;
			result.add(channel);
		}
		return result;
	}
}