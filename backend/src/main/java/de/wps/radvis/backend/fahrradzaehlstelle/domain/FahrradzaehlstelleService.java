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

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.util.Pair;

import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ArtDerAuswertung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.DatenEintrag;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.DurchschnittlicherZaehlstand;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleAuswertung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zaehlstand;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zeitstempel;
import jakarta.validation.constraints.NotNull;

public class FahrradzaehlstelleService {

	private final static DateTimeFormatter FORMATTER_STUNDE = DateTimeFormatter.ofPattern("H'h'");
	private final static DateTimeFormatter FORMATTER_WOCHENTAG = DateTimeFormatter.ofPattern("EEE")
		.localizedBy(Locale.GERMANY);
	private final static DateTimeFormatter FORMATTER_JAHR = DateTimeFormatter.ofPattern("yyyy");

	private final FahrradzaehlstelleRepository fahrradzaehlstelleRepository;

	public FahrradzaehlstelleService(FahrradzaehlstelleRepository fahrradzaehlstelleRepository) {
		this.fahrradzaehlstelleRepository = fahrradzaehlstelleRepository;
	}

	public FahrradzaehlstelleAuswertung getAuswertung(List<Long> channelIds,
		Instant startDate,
		Instant endDate,
		ArtDerAuswertung artDerAuswertung) {

		// Wir führen die Daten aller Channels in einer sortierten Map zusammen.
		Map<Zeitstempel, Zaehlstand> datenAllerChannels = fahrradzaehlstelleRepository.getChannels(channelIds)
			.stream()
			.map(channel -> channel.getZaehlstaendeInZeitraum(startDate, endDate))
			.reduce(new TreeMap<>(), (map1, map2) -> {
				map2.forEach((k, v) -> map1.merge(k, v, Zaehlstand::add));
				return map1;
			});

		// Wir quantiesieren die Daten (Aufsummieren aller Zaehlstaende einer Auswertungseineheit)
		TreeMap<Zeitstempel, Zaehlstand> quantisierteDaten = datenAllerChannels.entrySet()
			.stream()
			.collect(Collectors.toMap(
				entry -> {
					switch (artDerAuswertung) {
					case DURCHSCHNITT_PRO_STUNDE:
						return entry.getKey().amAnfangDerStunde();
					case DURCHSCHNITT_PRO_WOCHENTAG:
						return entry.getKey().amAnfangDesTages();
					case DURCHSCHNITT_PRO_MONAT:
						return entry.getKey().amAnfangDesMonats();
					case SUMME_PRO_JAHR:
						return entry.getKey().amAnfangDesJahres();
					default:
						throw new RuntimeException(
							"Art der Auswertung '" + artDerAuswertung + "' wird nicht unterstützt");
					}
				},
				Map.Entry::getValue,
				Zaehlstand::add,
				TreeMap::new));

		// Wir aggregieren die Daten über ein Mapping auf eine Referenzauswertungseinheit (außer bei SUMME_PRO_JAHR)
		// mittels Durchschnittsbildung
		TreeMap<Zeitstempel, DurchschnittlicherZaehlstand> aggregierteDaten = quantisierteDaten
			.entrySet()
			.stream()
			.collect(Collectors.toMap(
				entry -> {
					switch (artDerAuswertung) {
					case DURCHSCHNITT_PRO_STUNDE:
						return entry.getKey().referenzStunde();
					case DURCHSCHNITT_PRO_WOCHENTAG:
						return entry.getKey().referenzWochentag();
					case DURCHSCHNITT_PRO_MONAT:
						return entry.getKey().referenzMonat();
					case SUMME_PRO_JAHR:
						return entry.getKey(); // Hier bilden wir keinen Durchscnhitt!
					default:
						throw new RuntimeException(
							"Art der Auswertung '" + artDerAuswertung + "' wird nicht unterstützt");
					}
				},
				entry -> DurchschnittlicherZaehlstand.mitAnfangsWert(entry.getValue()),
				DurchschnittlicherZaehlstand::union,
				TreeMap::new));

		// Wir mappen die Zeistempel der Referenzauswertungseinheiten auf Strings & berechnen den Durchschnitt als Double
		List<DatenEintrag> daten = aggregierteDaten
			.entrySet()
			.stream()
			.map(entry -> new DatenEintrag(
				getShortStringRepresentationOfReferenzZeitstempel(artDerAuswertung, entry.getKey()),
				entry.getValue().getDurchschnittswert()))
			.collect(Collectors.toList());

		// Wir ermitteln weitere statistische Daten
		Long gesamtsumme = datenAllerChannels.values().stream().mapToLong(Zaehlstand::getValue).sum();
		Double durchschnitt = daten.stream().mapToDouble(DatenEintrag::getZaehlstand).average().orElse(0);
		Pair<String, Long> spitzeMitWert = quantisierteDaten.entrySet().stream()
			.max(Comparator.comparing(entry -> entry.getValue().getValue()))
			.map(entry -> Pair.of(getFullStringRepresentationOfZeitstempel(artDerAuswertung, entry.getKey()),
				entry.getValue().getValue()))
			.orElse(Pair.of("", 0L));

		return new FahrradzaehlstelleAuswertung(daten, gesamtsumme, durchschnitt, spitzeMitWert.getFirst(),
			spitzeMitWert.getSecond());
	}

	@NotNull
	private String getShortStringRepresentationOfReferenzZeitstempel(ArtDerAuswertung artDerAuswertung,
		Zeitstempel zeitstempel) {
		switch (artDerAuswertung) {
		case DURCHSCHNITT_PRO_STUNDE:
			return zeitstempel.toZonedDateTime().format(FORMATTER_STUNDE);
		case DURCHSCHNITT_PRO_WOCHENTAG:
			return zeitstempel.toZonedDateTime().format(FORMATTER_WOCHENTAG);
		case DURCHSCHNITT_PRO_MONAT:
			// Es gibt zwischen versch. JREs/SDKs Unterschiede bei der Stringrepräsentationen der Monate. Daher hier ein custom-Mapping
			return getMonthString(zeitstempel);
		case SUMME_PRO_JAHR:
			return zeitstempel.toZonedDateTime().format(FORMATTER_JAHR);
		default:
			throw new RuntimeException(
				"Art der Auswertung '" + artDerAuswertung + "' wird nicht unterstützt");
		}
	}

	@NotNull
	private String getFullStringRepresentationOfZeitstempel(ArtDerAuswertung artDerAuswertung,
		Zeitstempel zeitstempel) {
		switch (artDerAuswertung) {
		case DURCHSCHNITT_PRO_STUNDE:
			return zeitstempel.getDisplayText();
		case DURCHSCHNITT_PRO_WOCHENTAG:
			return zeitstempel.toZonedDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yy"));
		case DURCHSCHNITT_PRO_MONAT:
			// Es gibt zwischen versch. JREs/SDKs Unterschiede bei der Stringrepräsentationen der Monate. Daher hier ein custom-Mapping
			return getMonthString(zeitstempel) + " " + zeitstempel.toZonedDateTime().format(FORMATTER_JAHR);
		case SUMME_PRO_JAHR:
			return zeitstempel.toZonedDateTime().format(FORMATTER_JAHR);
		default:
			throw new RuntimeException(
				"Art der Auswertung '" + artDerAuswertung + "' wird nicht unterstützt");
		}
	}

	private String getMonthString(Zeitstempel key) {
		switch (key.toZonedDateTime().getMonthValue()) {
		case 1:
			return "Jan.";
		case 2:
			return "Feb.";
		case 3:
			return "März";
		case 4:
			return "Apr.";
		case 5:
			return "Mai";
		case 6:
			return "Juni";
		case 7:
			return "Juli";
		case 8:
			return "Aug.";
		case 9:
			return "Sept.";
		case 10:
			return "Okt.";
		case 11:
			return "Nov.";
		case 12:
			return "Dez.";
		default:
			throw new RuntimeException("Monatswert muss zwischen 1 und 12 liegen!");
		}
	}

}
