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

package de.wps.radvis.backend.administration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.administration.domain.AdministrationService;
import de.wps.radvis.backend.administration.schnittstelle.BenutzerGuard;
import de.wps.radvis.backend.administration.schnittstelle.OrganisationGuard;
import de.wps.radvis.backend.administration.schnittstelle.command.SaveBenutzerCommandConverter;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import lombok.NonNull;

@Configuration
public class AdministrationConfiguration {

	private final BenutzerService benutzerService;
	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final VerwaltungseinheitRepository verwaltungseinheitRepository;
	private final OrganisationRepository organisationRepository;

	public AdministrationConfiguration(
		@NonNull BenutzerService benutzerService,
		@NonNull BenutzerResolver benutzerResolver,
		@NonNull VerwaltungseinheitResolver verwaltungseinheitResolver,
		VerwaltungseinheitRepository verwaltungseinheitRepository, OrganisationRepository organisationRepository) {
		this.organisationRepository = organisationRepository;
		this.verwaltungseinheitRepository = verwaltungseinheitRepository;
		this.benutzerService = benutzerService;
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
	}

	@Bean
	public SaveBenutzerCommandConverter saveBenutzerCommandConverter() {
		return new SaveBenutzerCommandConverter(verwaltungseinheitResolver);
	}

	@Bean
	public BenutzerGuard benutzerGuard() {
		return new BenutzerGuard(benutzerService, benutzerResolver);
	}

	@Bean
	public OrganisationGuard organisationGuard() {
		return new OrganisationGuard(benutzerResolver, benutzerService,
			administrationService(), organisationRepository);
	}

	@Bean
	public AdministrationService administrationService() {
		return new AdministrationService(verwaltungseinheitRepository, organisationRepository);
	}
}
