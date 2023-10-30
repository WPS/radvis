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

package de.wps.radvis.backend.benutzer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.schnittstelle.BenutzerResolverImpl;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.NonNull;

@Configuration
@EnableJpaRepositories
@EntityScan
public class BenutzerConfiguration {
	@Autowired
	private BenutzerRepository benutzerRepository;

	private final VerwaltungseinheitService verwaltungseinheitService;

	@Autowired
	private TechnischerBenutzerConfigurationProperties technischerBenutzerConfigurationProperties;

	public BenutzerConfiguration(@NonNull VerwaltungseinheitService verwaltungseinheitService) {
		this.verwaltungseinheitService = verwaltungseinheitService;
	}

	@Bean
	public BenutzerService benutzerService() {
		return new BenutzerService(benutzerRepository, verwaltungseinheitService,
			technischerBenutzerConfigurationProperties.getServiceBwId());
	}

	@Bean
	public BenutzerResolver benutzerResolver() {
		return new BenutzerResolverImpl();
	}
}
