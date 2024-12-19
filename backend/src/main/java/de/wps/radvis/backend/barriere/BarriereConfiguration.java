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

package de.wps.radvis.backend.barriere;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.barriere.domain.BarriereService;
import de.wps.radvis.backend.barriere.domain.repository.BarriereNetzBezugAenderungRepository;
import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.barriere.schnittstelle.BarriereGuard;
import de.wps.radvis.backend.barriere.schnittstelle.SaveBarriereCommandConverter;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.NonNull;

@Configuration
@EnableJpaRepositories
@EntityScan
public class BarriereConfiguration {
	private final NetzService netzService;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final BarriereRepository barriereRepository;
	private final BenutzerResolver benutzerResolver;
	private final BenutzerService benutzerService;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private CommonConfigurationProperties commonConfigurationProperties;

	@Autowired
	private BarriereNetzBezugAenderungRepository barriereNetzBezugAenderungRepository;

	public BarriereConfiguration(
		@NonNull NetzService netzService,
		@NonNull VerwaltungseinheitResolver verwaltungseinheitResolver,
		@NonNull BarriereRepository barriereRepository,
		@NonNull BenutzerResolver benutzerResolver,
		CommonConfigurationProperties commonConfigurationProperties,
		@NonNull BenutzerService benutzerService,
		@NonNull VerwaltungseinheitService verwaltungseinheitService) {
		this.commonConfigurationProperties = commonConfigurationProperties;
		this.netzService = netzService;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
		this.barriereRepository = barriereRepository;
		this.benutzerResolver = benutzerResolver;
		this.benutzerService = benutzerService;
		this.verwaltungseinheitService = verwaltungseinheitService;
	}

	@Bean
	public SaveBarriereCommandConverter createBarriereCommandConverter() {
		return new SaveBarriereCommandConverter(netzService, netzService,
			verwaltungseinheitResolver);
	}

	@Bean
	public BarriereService barriereService() {
		return new BarriereService(barriereRepository, verwaltungseinheitService, netzService,
			commonConfigurationProperties.getErlaubteAbweichungFuerKantenNetzbezugRematch(),
			barriereNetzBezugAenderungRepository,
			benutzerService);
	}

	@Bean
	public BarriereGuard barriereGuard() {
		return new BarriereGuard(benutzerResolver, verwaltungseinheitService);
	}
}
