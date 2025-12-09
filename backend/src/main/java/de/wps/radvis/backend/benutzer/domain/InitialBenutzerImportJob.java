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

package de.wps.radvis.backend.benutzer.domain;

import java.util.Optional;

import de.wps.radvis.backend.benutzer.domain.exception.BenutzerExistiertBereitsException;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.organisation.domain.valueObject.Mailadresse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InitialBenutzerImportJob extends AbstractJob {

	private final InitialAdminImportConfigurationProperties adminImportConfigurationProperties;
	private final TechnischerBenutzerConfigurationProperties technischerBenutzerConfigurationProperties;
	private final BenutzerService benutzerService;

	public InitialBenutzerImportJob(JobExecutionDescriptionRepository repository,
		InitialAdminImportConfigurationProperties adminImportConfigurationProperties,
		TechnischerBenutzerConfigurationProperties technischerBenutzerConfigurationProperties,
		BenutzerService benutzerService) {
		super(repository);
		this.adminImportConfigurationProperties = adminImportConfigurationProperties;
		this.technischerBenutzerConfigurationProperties = technischerBenutzerConfigurationProperties;
		this.benutzerService = benutzerService;
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		registriereInitialenAdmin();
		registriereTechnischenBenutzer();
		return Optional.empty();
	}

	private void registriereInitialenAdmin() {
		if (benutzerService.istBenutzerRegistriert(
			ServiceBwId.of(adminImportConfigurationProperties.getServiceBwId()))) {
			log.warn(
				"Der gegebene Erstadministrator aus der Konfiguration ist bereits registriert. Eine erneute Registrierung wird übersprungen.");
			return;
		}

		try {
			benutzerService.registriereAdministrator(
				Name.of(adminImportConfigurationProperties.getVorname()),
				Name.of(adminImportConfigurationProperties.getName()),
				ServiceBwId.of(adminImportConfigurationProperties.getServiceBwId()),
				Mailadresse.of(adminImportConfigurationProperties.getMailAdresse()));
		} catch (BenutzerExistiertBereitsException e) {
			log.error("Der gegebene Erstadministrator aus der Konfiguration ist bereits registriert.", e);
		}
	}

	private void registriereTechnischenBenutzer() {
		if (benutzerService.istBenutzerRegistriert(
			ServiceBwId.of(technischerBenutzerConfigurationProperties.getServiceBwId()))) {
			log.warn(
				"Der gegebene technische Benutzer aus der Konfiguration ist bereits registriert. Eine erneute Registrierung wird übersprungen.");
			return;
		}

		try {
			benutzerService.registriereTechnischenBenutzer(
				Name.of(technischerBenutzerConfigurationProperties.getVorname()),
				Name.of(technischerBenutzerConfigurationProperties.getName()),
				ServiceBwId.of(technischerBenutzerConfigurationProperties.getServiceBwId()),
				Mailadresse.of(technischerBenutzerConfigurationProperties.getMailAdresse()));
		} catch (BenutzerExistiertBereitsException e) {
			log.error("Der gegebene technische Benutzer aus der Konfiguration ist bereits registriert.", e);
		}
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Erstellt die wichtigsten Nutzer, die in der application.yml-Datei konfiguriert sind. Das heißt Administrator und technischer Benutzer. Existieren Nutzer mit der konfigurierten ServiceBW-ID bereits, werden diese ignoriert. Der Job kann daher beliebig häufig ausgeführt werden.",
			"Bei erster Ausführung oder nach Config-Änderungen werden ggf. Nutzer angelegt und registriert.",
			JobExecutionDurationEstimate.SHORT
		);
	}
}
