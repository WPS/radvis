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

package de.wps.radvis.backend.integration.radnetz;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.integration.radnetz.domain.RadNETZNachbearbeitungsRepository;
import de.wps.radvis.backend.integration.radnetz.domain.RadNETZNachbearbeitungsRepositoryImpl;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzAttributMapper;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungProtokollService;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungService;
import de.wps.radvis.backend.integration.radnetz.domain.repository.QualitaetsSicherungsRepository;
import de.wps.radvis.backend.integration.radnetz.schnittstelle.QualitaetsSicherungsGuard;
import de.wps.radvis.backend.integration.radnetz.schnittstelle.repositoryImpl.QuaelitaetsSicherungsRepositoryImpl;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import lombok.NonNull;

@Configuration
public class IntegrationRadNetzConfiguration {
	@Autowired
	private NetzfehlerRepository netzfehlerRepository;

	@PersistenceContext
	private EntityManager entityManager;

	private final NetzService netzService;
	private final BenutzerResolver benutzerResolver;

	IntegrationRadNetzConfiguration(@NonNull NetzService netzService,
		@NonNull BenutzerResolver benutzerResolver) {
		this.netzService = netzService;
		this.benutzerResolver = benutzerResolver;
	}

	@Bean
	public RadNETZNachbearbeitungsRepository radNETZNachbearbeitungsRepository() {
		return new RadNETZNachbearbeitungsRepositoryImpl();
	}

	@Bean
	public RadNetzNetzbildungService radNetzNetzbildungService() {
		return new RadNetzNetzbildungService(netzService, radNetzNetzbildungProtokollService(), radNetzAttributMapper(),
			entityManager);
	}

	@Bean
	public RadNetzAttributMapper radNetzAttributMapper() {
		return new RadNetzAttributMapper(radNetzNetzbildungProtokollService());
	}

	@Bean
	public RadNetzNetzbildungProtokollService radNetzNetzbildungProtokollService() {
		return new RadNetzNetzbildungProtokollService(netzfehlerRepository);
	}

	@Bean
	public QualitaetsSicherungsRepository qualitaetsSicherungsRepository() {
		return new QuaelitaetsSicherungsRepositoryImpl();
	}

	@Bean
	public QualitaetsSicherungsGuard qualitaetsSicherungsGuard() {
		return new QualitaetsSicherungsGuard(this.benutzerResolver);
	}
}
