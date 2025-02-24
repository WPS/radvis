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

package de.wps.radvis.backend.netzfehler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.repository.FahrradrouteFilterRepository;
import de.wps.radvis.backend.konsistenz.pruefung.domain.KonsistenzregelVerletzungsRepository;
import de.wps.radvis.backend.netzfehler.domain.AnpassungswuenscheConfigurationProperties;
import de.wps.radvis.backend.netzfehler.domain.AnpassungswunschRepository;
import de.wps.radvis.backend.netzfehler.domain.AnpassungswunschService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerService;
import de.wps.radvis.backend.netzfehler.schnittstelle.AnpassungswunschGuard;
import de.wps.radvis.backend.netzfehler.schnittstelle.NetzfehlerGuard;
import de.wps.radvis.backend.netzfehler.schnittstelle.SaveAnpassungswunschCommandConverter;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import lombok.NonNull;

@Configuration
@EnableJpaRepositories
@EntityScan
public class NetzfehlerConfiguration {

	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final FahrradrouteFilterRepository fahrradrouteFilterRepository;
	private AnpassungswuenscheConfigurationProperties anpassungswuenscheConfigurationProperties;

	NetzfehlerConfiguration(@NonNull BenutzerResolver benutzerResolver,
		VerwaltungseinheitResolver verwaltungseinheitResolver,
		FahrradrouteFilterRepository fahrradrouteFilterRepository,
		AnpassungswuenscheConfigurationProperties anpassungswuenscheConfigurationProperties) {
		this.anpassungswuenscheConfigurationProperties = anpassungswuenscheConfigurationProperties;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
		this.benutzerResolver = benutzerResolver;
		this.fahrradrouteFilterRepository = fahrradrouteFilterRepository;
	}

	@Autowired
	private NetzfehlerRepository netzfehlerRepository;

	@Autowired
	private AnpassungswunschRepository anpassungswunschRepository;

	@Autowired
	private KonsistenzregelVerletzungsRepository konsistenzregelVerletzungsRepository;

	@Bean
	public NetzfehlerService netzfehlerService() {
		return new NetzfehlerService(netzfehlerRepository);
	}

	@Bean
	public AnpassungswunschService anpassungswunschService() {
		return new AnpassungswunschService(anpassungswunschRepository, konsistenzregelVerletzungsRepository,
			fahrradrouteFilterRepository, anpassungswuenscheConfigurationProperties.getDistanzZuFahrradrouteInMetern());
	}

	@Bean
	public SaveAnpassungswunschCommandConverter saveAnpassungswunschCommandConverter() {
		return new SaveAnpassungswunschCommandConverter(verwaltungseinheitResolver);
	}

	@Bean
	public AnpassungswunschGuard anpassungswunschGuard() {
		return new AnpassungswunschGuard(benutzerResolver);
	}

	@Bean
	public NetzfehlerGuard netzfehlerGuard() {
		return new NetzfehlerGuard(benutzerResolver);
	}
}
