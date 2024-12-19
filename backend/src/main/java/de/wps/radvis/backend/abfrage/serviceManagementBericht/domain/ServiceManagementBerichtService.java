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

package de.wps.radvis.backend.abfrage.serviceManagementBericht.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceManagementBerichtService {

	private final BenutzerService benutzerService;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	private final CsvRepository csvRepository;

	private final int mindestGesamtLaengeInMetern;

	public ServiceManagementBerichtService(
		BenutzerService benutzerService,
		VerwaltungseinheitService verwaltungseinheitService,
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		CsvRepository csvRepository,
		int mindestGesamtLaengeInMetern
	) {
		this.benutzerService = benutzerService;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.jobExecutionDescriptionRepository = jobExecutionDescriptionRepository;
		this.csvRepository = csvRepository;
		this.mindestGesamtLaengeInMetern = mindestGesamtLaengeInMetern;
	}

	public Map<String, String> getFachlicheStatistiken() {
		Map<String, String> jsonMap = new HashMap<>();

		log.debug("Ermittle Anzahl aktive Benutzer");
		jsonMap.put("Aktive Benutzer", Long.toString(benutzerService.getAnzahlBenutzerAktiv()));
		log.debug("Ermittle Anzahl registrierte Benutzer insgesamt");
		jsonMap.put("Registrierte Benutzer insgesamt", Long.toString(benutzerService.getAnzahlBenutzerGesamt()));

		log.debug("Ermittle Anzahl Gemeinden mit mindestens einem aktiven Benutzer");
		jsonMap.put("Gemeinden mit mindestens einem aktiven Benutzer", Long.toString(
			verwaltungseinheitService.getAnzahlVerwaltungseinheitOfOrganisationsArtMitAktivenBenutzern(
				OrganisationsArt.GEMEINDE)));
		log.debug("Ermittle Anzahl Gemeinden insgesamt");
		jsonMap.put("Gemeinden insgesamt", Long.toString(
			verwaltungseinheitService.getAnzahlVerwaltungseinheitOfOrganisationsArt(OrganisationsArt.GEMEINDE)));

		log.debug("Ermittle Anzahl Kreise mit mindestens einem aktiven Benutzer");
		jsonMap.put("Kreise mit mindestens einem aktiven Benutzer", Long.toString(
			verwaltungseinheitService.getAnzahlVerwaltungseinheitOfOrganisationsArtMitAktivenBenutzern(
				OrganisationsArt.KREIS)));
		log.debug("Ermittle Anzahl Kreise insgesamt");
		jsonMap.put("Kreise insgesamt", Long.toString(
			verwaltungseinheitService.getAnzahlVerwaltungseinheitOfOrganisationsArt(OrganisationsArt.KREIS)));

		log.debug("Ermittle Anzahl Regierungsbezirke mit mindestens einem aktiven Benutzer");
		jsonMap.put("Regierungsbezirke mit mindestens einem aktiven Benutzer", Long.toString(
			verwaltungseinheitService.getAnzahlVerwaltungseinheitOfOrganisationsArtMitAktivenBenutzern(
				OrganisationsArt.REGIERUNGSBEZIRK)));
		log.debug("Ermittle Anzahl Regierungsbezirke insgesamt");
		jsonMap.put("Regierungsbezirke insgesamt", Long.toString(
			verwaltungseinheitService.getAnzahlVerwaltungseinheitOfOrganisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)));

		log.debug("Ermittle Anzahl Kreise mit mindestens " + mindestGesamtLaengeInMetern + "m Kreisnetz");
		jsonMap.put("Kreise mit mindestens " + mindestGesamtLaengeInMetern + "m Kreisnetz",
			verwaltungseinheitService.getAllKreiseWithKreisnetzGreaterOrEqual(mindestGesamtLaengeInMetern)
				.stream()
				.map(Verwaltungseinheit::getName)
				.toList()
				.toString());

		log.debug("Ermittle Anzahl Kommunen mit mindestens " + mindestGesamtLaengeInMetern + "m Kommunalnetz");
		jsonMap.put("Kommunen mit mindestens " + mindestGesamtLaengeInMetern + "m Kommunalnetz",
			verwaltungseinheitService.getAllKommunenWIthKommunalnetzGreaterOrEqual(mindestGesamtLaengeInMetern)
				.stream()
				.map(Verwaltungseinheit::getName)
				.toList()
				.toString());

		return jsonMap;
	}

	public byte[] getJobUebersicht(LocalDate startDate, LocalDate endDate, List<String> jobNameBlacklist) {
		LocalDateTime startDateTime = startDate.atStartOfDay();
		LocalDateTime endDateTime = endDate.atStartOfDay();

		List<JobExecutionDescription> allExecutions = jobExecutionDescriptionRepository
			.findByExecutionStartGreaterThanEqualAndExecutionStartLessThan(startDateTime, endDateTime);

		// Filterung nach Namen in der Blacklist muss nachträglich passieren, da die Blacklist leer sein darf,
		// aber SQL keine leeren IN-Klauseln erlaubt, auch nicht aus Methodennamen generiertes SQL!
		List<JobExecutionDescription> filteredExecutions = allExecutions.stream()
			.filter(e -> !jobNameBlacklist.contains(e.getName())).toList();

		if (filteredExecutions.isEmpty()) {
			return "Keine Daten zum Exportieren".getBytes(StandardCharsets.UTF_8);
		}

		List<Map<String, String>> rows = filteredExecutions.stream()
			.collect(Collectors.groupingBy(e -> String.join(":", // nach relevanten Feldern gruppieren
				e.getName(),
				String.valueOf(e.getExecutionStart().getYear()),
				String.valueOf(e.getExecutionStart().getMonthValue())),
				Collectors.counting())) // Einträge in den Gruppen zählen
			.entrySet().stream()
			.sorted(Map.Entry.comparingByKey()) // sortieren nach Name-Jahr-Monat
			.map(entry -> {
				// Zeile im CSV zusammensetzen
				String[] keyParts = entry.getKey().split(":");
				return Map.of(
					"Name", keyParts[0],
					"Jahr", keyParts[1],
					"Monat", keyParts[2],
					"Anzahl Einträge", String.valueOf(entry.getValue())
				);
			})
			.collect(Collectors.toList());

		List<String> header = List.of("Name", "Jahr", "Monat", "Anzahl Einträge");

		try {
			return csvRepository.write(CsvData.of(rows, header));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
