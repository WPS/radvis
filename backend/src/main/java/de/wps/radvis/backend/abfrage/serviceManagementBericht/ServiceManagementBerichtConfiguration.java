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

package de.wps.radvis.backend.abfrage.serviceManagementBericht;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.abfrage.serviceManagementBericht.domain.ServiceManagementBerichtConfigurationProperties;
import de.wps.radvis.backend.abfrage.serviceManagementBericht.domain.ServiceManagementBerichtService;
import de.wps.radvis.backend.abfrage.serviceManagementBericht.schnittstelle.ServiceManagementBerichtGuard;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.NonNull;

@Configuration
public class ServiceManagementBerichtConfiguration {

	@Autowired
	private BenutzerService benutzerService;

	@Autowired
	private VerwaltungseinheitService verwaltungseinheitService;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private CsvRepository csvRepository;

	private final ServiceManagementBerichtConfigurationProperties serviceManagementBerichtConfigurationProperties;

	public ServiceManagementBerichtConfiguration(
		@NonNull ServiceManagementBerichtConfigurationProperties serviceManagementBerichtConfigurationProperties
	) {
		this.serviceManagementBerichtConfigurationProperties = serviceManagementBerichtConfigurationProperties;
	}

	@Bean
	public ServiceManagementBerichtGuard serviceManagementBerichtGuard() {
		return new ServiceManagementBerichtGuard();
	}

	@Bean
	public ServiceManagementBerichtService serviceManagementBerichtService() {
		return new ServiceManagementBerichtService(
			benutzerService,
			verwaltungseinheitService,
			jobExecutionDescriptionRepository,
			csvRepository,
			serviceManagementBerichtConfigurationProperties.getMindestGesamtLaengeInMetern()
		);
	}
}
