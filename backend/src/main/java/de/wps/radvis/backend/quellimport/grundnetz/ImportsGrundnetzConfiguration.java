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

package de.wps.radvis.backend.quellimport.grundnetz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMBasisQuellImportJob;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMWFSImportRepository;

@Configuration
public class ImportsGrundnetzConfiguration {

	@Autowired
	private ImportedFeaturePersistentRepository importedFeatureRepository;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private CommonConfigurationProperties commonConfigurationProperties;

	@Autowired
	private DLMConfigurationProperties dlmConfigurationProperties;

	@Bean
	public DLMWFSImportRepository dlmwfsImportRepository() {
		return new DLMWFSImportRepository(commonConfigurationProperties, dlmConfigurationProperties);
	}

	@Bean
	public DLMBasisQuellImportJob dlmBasisQuellImportJob() {
		return new DLMBasisQuellImportJob(jobExecutionDescriptionRepository, importedFeatureRepository,
			dlmwfsImportRepository());
	}
}
