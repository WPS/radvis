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

package de.wps.radvis.backend.furtKreuzung;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.furtKreuzung.domain.FurtKreuzungService;
import de.wps.radvis.backend.furtKreuzung.domain.repository.FurtKreuzungNetzBezugAenderungRepository;
import de.wps.radvis.backend.furtKreuzung.domain.repository.FurtKreuzungRepository;
import de.wps.radvis.backend.furtKreuzung.schnittstelle.FurtKreuzungGuard;
import de.wps.radvis.backend.furtKreuzung.schnittstelle.SaveFurtKreuzungCommandConverter;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.NonNull;

@Configuration
@EnableJpaRepositories
@EntityScan
public class FurtKreuzungConfiguration {
	private final NetzService netzService;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final BenutzerService benutzerService;
	private final FurtKreuzungRepository repository;
	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private CommonConfigurationProperties commonConfigurationProperties;

	@Autowired
	private FurtKreuzungNetzBezugAenderungRepository furtKreuzungNetzBezugAenderungRepository;

	public FurtKreuzungConfiguration(
		@NonNull NetzService netzService,
		@NonNull BenutzerResolver benutzerResolver,
		@NonNull BenutzerService benutzerService,
		@NonNull VerwaltungseinheitService verwaltungseinheitService,
		@NonNull VerwaltungseinheitResolver verwaltungseinheitResolver,
		@NonNull FurtKreuzungRepository repository, CommonConfigurationProperties commonConfigurationProperties) {
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.benutzerService = benutzerService;
		this.repository = repository;
		this.netzService = netzService;
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
	}

	@Bean
	public SaveFurtKreuzungCommandConverter createFurtKreuzungCommandConverter() {
		return new SaveFurtKreuzungCommandConverter(netzService, netzService,
			verwaltungseinheitResolver);
	}

	@Bean
	public FurtKreuzungService furtKreuzungService() {
		return new FurtKreuzungService(repository, verwaltungseinheitService, netzService,
			commonConfigurationProperties.getErlaubteAbweichungFuerKantenNetzbezugRematch(),
			furtKreuzungNetzBezugAenderungRepository,
			benutzerService);
	}

	@Bean
	public FurtKreuzungGuard furtKreuzungGuard() {
		return new FurtKreuzungGuard(benutzerResolver, verwaltungseinheitService);
	}
}
