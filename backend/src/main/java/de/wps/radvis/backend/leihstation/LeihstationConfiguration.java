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

package de.wps.radvis.backend.leihstation;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.leihstation.domain.LeihstationExporterService;
import de.wps.radvis.backend.leihstation.domain.LeihstationImportService;
import de.wps.radvis.backend.leihstation.domain.LeihstationRepository;
import de.wps.radvis.backend.leihstation.domain.LeihstationService;
import de.wps.radvis.backend.leihstation.schnittstelle.LeihstationGuard;
import de.wps.radvis.backend.leihstation.schnittstelle.SaveLeihstationCommandConverter;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.AllArgsConstructor;

@Configuration
@EnableJpaRepositories
@EntityScan
@AllArgsConstructor
public class LeihstationConfiguration {
	private final LeihstationRepository repository;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final CommonConfigurationProperties commonConfigurationProperties;
	private final BenutzerResolver benutzerResolver;

	@Bean
	public LeihstationGuard leihstationGuard() {
		return new LeihstationGuard(benutzerResolver, zustaendigkeitsService, leihstationService());
	}

	@Bean
	public LeihstationService leihstationService() {
		return new LeihstationService(repository);
	}

	@Bean
	public LeihstationImportService leihstationImportService() {
		return new LeihstationImportService(repository, verwaltungseinheitService,
			zustaendigkeitsService, commonConfigurationProperties.getBasisUrl());
	}

	@Bean
	public SaveLeihstationCommandConverter saveLeihstationCommandConverter() {
		return new SaveLeihstationCommandConverter();
	}

	@Bean
	public LeihstationExporterService leihstationExporterService() {
		return new LeihstationExporterService(repository);
	}
}
