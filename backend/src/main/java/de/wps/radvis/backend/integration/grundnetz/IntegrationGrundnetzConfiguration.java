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

package de.wps.radvis.backend.integration.grundnetz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.integration.grundnetz.domain.DLMAttributMapper;
import de.wps.radvis.backend.integration.grundnetz.domain.DLMNetzbildungJob;
import de.wps.radvis.backend.integration.grundnetz.domain.DLMNetzbildungProtokollService;
import de.wps.radvis.backend.integration.grundnetz.domain.DLMNetzbildungService;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import jakarta.persistence.EntityManager;

@Configuration
public class IntegrationGrundnetzConfiguration {
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private NetzfehlerRepository netzfehlerRepository;

	@Autowired
	private NetzService netzService;

	@Autowired
	private ImportedFeaturePersistentRepository importedFeatureRepository;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Bean
	public DLMNetzbildungJob dlmNetzbildungJob() {
		return new DLMNetzbildungJob(importedFeatureRepository, dlmNetzbildungService(),
			jobExecutionDescriptionRepository);
	}

	public DLMNetzbildungService dlmNetzbildungService() {
		return new DLMNetzbildungService(dlmNetzbildungProtokollService(), netzService, entityManager);
	}

	@Bean
	public DLMNetzbildungProtokollService dlmNetzbildungProtokollService() {
		return new DLMNetzbildungProtokollService(netzfehlerRepository);
	}

	@Bean
	public DLMAttributMapper dlmAttributMapper() {
		return new DLMAttributMapper();
	}
}
