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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
public class Zeitstempel implements Comparable<Zeitstempel> {
	public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

	@Getter
	@NonNull
	private final Long value;

	private Zeitstempel(Long value) {
		require(value, notNullValue());
		this.value = value;
	}

	private Zeitstempel(String value) {
		require(value, notNullValue());
		require(value.length() > 0, "Value darf nicht leer sein");

		ZonedDateTime zonedDateTime = ZonedDateTime.parse(value, dateTimeFormatter);
		this.value = zonedDateTime.toInstant().atOffset(zonedDateTime.getOffset()).toEpochSecond();
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Zeitstempel of(String value) {
		return new Zeitstempel(value);
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static Zeitstempel of(Long value) {
		return new Zeitstempel(value);
	}

	public YearMonth toYearMonth() {
		return YearMonth.from(this.toZonedDateTime());
	}

	public ZonedDateTime toZonedDateTime() {
		Instant instant = Instant.ofEpochSecond(this.value);
		return ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Paris"));
	}

	@Override
	public String toString() {
		return "Zeitstempel[EpochSecs: " + value + "; As zonedDateTime: " + toZonedDateTime().format(dateTimeFormatter)
			+ "]";
	}

	@JsonValue
	public String getDisplayText() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
		return formatter.format(toZonedDateTime().toLocalDateTime());
	}

	public Zeitstempel amAnfangDerStunde() {
		return amAnfangVonZeiteinheit(ChronoUnit.HOURS);
	}

	public Zeitstempel amAnfangDesTages() {
		return amAnfangVonZeiteinheit(ChronoUnit.DAYS);
	}

	public Zeitstempel amAnfangDesMonats() {
		return Zeitstempel.of(
			this.toZonedDateTime().with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS)
				.toEpochSecond());
	}

	public Zeitstempel amAnfangDesJahres() {
		return Zeitstempel.of(
			this.toZonedDateTime().with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS)
				.toEpochSecond());
	}

	private Zeitstempel amAnfangVonZeiteinheit(TemporalUnit temporalUnit) {
		return Zeitstempel.of(this.toZonedDateTime().truncatedTo(temporalUnit).toEpochSecond());
	}

	@Override
	public int compareTo(Zeitstempel o) {
		return value.compareTo(o.value);
	}

	public Zeitstempel referenzStunde() {
		ZonedDateTime stundeAmErstenErsten2000 = ZonedDateTime.of(2000, 1, 1, this.toZonedDateTime().getHour(), 0, 0, 0,
			ZoneId.of("Europe/Berlin"));
		return new Zeitstempel(stundeAmErstenErsten2000.toEpochSecond());
	}

	public Zeitstempel referenzWochentag() {
		ZonedDateTime ersterErster2000 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"));
		ZonedDateTime ersterSonntag2000 = ersterErster2000.with(
			TemporalAdjusters.dayOfWeekInMonth(1, DayOfWeek.SUNDAY));
		ZonedDateTime wochentaginReferenzWoche = ersterSonntag2000.plusDays(
			this.toZonedDateTime().getDayOfWeek().getValue());
		return new Zeitstempel(wochentaginReferenzWoche.toEpochSecond());
	}

	public Zeitstempel referenzMonat() {
		ZonedDateTime monatImReferenzJahr2000 =
			ZonedDateTime.of(2000, this.toZonedDateTime().getMonthValue(), 2, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"));
		return new Zeitstempel(monatImReferenzJahr2000.toEpochSecond());
	}
}
