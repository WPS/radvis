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

package de.wps.radvis.backend.abstellanlage;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageExporterService;
import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageImportService;
import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageRepository;
import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageService;
import de.wps.radvis.backend.abstellanlage.schnittstelle.AbstellanlageGuard;
import de.wps.radvis.backend.abstellanlage.schnittstelle.SaveAbstellanlageCommandConverter;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.AllArgsConstructor;

@Configuration
@EnableJpaRepositories
@EntityScan
@AllArgsConstructor
public class AbstellanlageConfiguration {
	private final AbstellanlageRepository abstellanlageRepository;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final CommonConfigurationProperties commonConfigurationProperties;
	private final BenutzerResolver benutzerResolver;

	@Bean
	public AbstellanlageGuard abstellanlageGuard() {
		return new AbstellanlageGuard(benutzerResolver, zustaendigkeitsService, abstellanlageService());
	}

	@Bean
	public AbstellanlageService abstellanlageService() {
		return new AbstellanlageService(abstellanlageRepository);
	}

	@Bean
	public AbstellanlageExporterService abstellanlageExporterService() {
		return new AbstellanlageExporterService(abstellanlageRepository);
	}

	@Bean
	public SaveAbstellanlageCommandConverter saveAbstellanlageCommandConverter() {
		return new SaveAbstellanlageCommandConverter(verwaltungseinheitService);
	}

	@Bean
	public AbstellanlageImportService abstellanlageImportService() {
		return new AbstellanlageImportService(abstellanlageRepository, verwaltungseinheitService,
			zustaendigkeitsService, commonConfigurationProperties.getBasisUrl());
	}
}
