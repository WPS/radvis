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

package de.wps.radvis.backend.application;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;

import de.wps.radvis.backend.application.domain.InitialImportService;
import de.wps.radvis.backend.application.domain.RadVISInfoService;
import de.wps.radvis.backend.application.domain.ScheduleConfigurationProperties;
import de.wps.radvis.backend.application.schnittstelle.RadVisJobScheduler;
import de.wps.radvis.backend.application.schnittstelle.RequestLoggingInterceptor;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributProjektionsJob;
import de.wps.radvis.backend.integration.dlm.domain.DLMNetzbildungJob;
import de.wps.radvis.backend.integration.radnetz.domain.RadNETZNetzbildungJob;
import de.wps.radvis.backend.integration.radnetz.domain.RadNETZSackgassenJob;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungJob;
import de.wps.radvis.backend.matching.domain.DlmPbfErstellungsJob;
import de.wps.radvis.backend.matching.domain.MatchNetzAufDLMJob;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.quellimport.common.domain.GenericQuellImportJob;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMBasisQuellImportJob;
import de.wps.radvis.backend.quellimport.radnetz.domain.RadNETZQuellImportJob;

@Configuration
@EnableAsync
@EnableJpaRepositories
public class ApplicationConfiguration {
	@Autowired
	private List<AbstractJob> jobs;

	@Autowired
	private CommonConfigurationProperties commonConfigurationProperties;

	@Autowired
	private ScheduleConfigurationProperties scheduleConfigurationProperties;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final RadNETZQuellImportJob radnetzQuellImportJob;
	private final GenericQuellImportJob radwegeLglTuttlingenImportJob;
	private final GenericQuellImportJob radwegeDbImportJob;
	private final GenericQuellImportJob rvkEsslingenImportJob;
	private final GenericQuellImportJob bietigheimBissingenImportJob;
	private final DLMBasisQuellImportJob dlmBasisQuellImportJob;

	private final DLMNetzbildungJob dlmNetzbildungJob;
	private final RadNETZNetzbildungJob radNETZNetzbildungJob;
	private final RadwegeDBNetzbildungJob radwegeDBNetzbildungJob;
	private final NetzService netzService;
	private final ImportedFeaturePersistentRepository importedFeatureRepository;
	private final DlmPbfErstellungsJob dlmPbfErstellungsJob;
	private final RadNETZSackgassenJob radNETZSackgassenJob;
	private final ApplicationContext applicationContext;
	private final MatchNetzAufDLMJob radNetzMatchingAufDLMJob;
	private final MatchNetzAufDLMJob radwegeDBMatchingAufDLMJob;
	private final AttributProjektionsJob radNETZProjektionsJob;
	private final AttributProjektionsJob radwegeDBProjektionsJob;

	public ApplicationConfiguration(
		RadNETZQuellImportJob radnetzQuellImportJob,
		GenericQuellImportJob radwegeLglTuttlingenImportJob,
		GenericQuellImportJob radwegeDbImportJob,
		GenericQuellImportJob rvkEsslingenImportJob,
		GenericQuellImportJob bietigheimBissingenImportJob,
		DLMBasisQuellImportJob dlmBasisQuellImportJob,
		DLMNetzbildungJob dlmNetzbildungJob,
		RadNETZNetzbildungJob radNETZNetzbildungJob,
		RadwegeDBNetzbildungJob radwegeDBNetzbildungJob,
		NetzService netzService,
		@Autowired ImportedFeaturePersistentRepository importedFeaturesRepository,
		DlmPbfErstellungsJob dlmPbfErstellungsJob,
		RadNETZSackgassenJob radNETZSackgassenJob,
		ApplicationContext applicationContext, MatchNetzAufDLMJob matchRadwegeDbAufDLMJob,
		AttributProjektionsJob radNETZAttributProjektionsJob, AttributProjektionsJob radwegeDbAttributProjektionsJob,
		MatchNetzAufDLMJob matchRadNETZAufDLMJob) {

		this.radnetzQuellImportJob = radnetzQuellImportJob;
		this.radwegeLglTuttlingenImportJob = radwegeLglTuttlingenImportJob;
		this.radwegeDbImportJob = radwegeDbImportJob;
		this.rvkEsslingenImportJob = rvkEsslingenImportJob;
		this.bietigheimBissingenImportJob = bietigheimBissingenImportJob;
		this.dlmBasisQuellImportJob = dlmBasisQuellImportJob;
		this.dlmNetzbildungJob = dlmNetzbildungJob;
		this.radNETZNetzbildungJob = radNETZNetzbildungJob;
		this.radwegeDBNetzbildungJob = radwegeDBNetzbildungJob;
		this.netzService = netzService;
		this.importedFeatureRepository = importedFeaturesRepository;
		this.dlmPbfErstellungsJob = dlmPbfErstellungsJob;
		this.radNETZSackgassenJob = radNETZSackgassenJob;
		this.applicationContext = applicationContext;
		this.radNetzMatchingAufDLMJob = matchRadNETZAufDLMJob;
		this.radwegeDBMatchingAufDLMJob = matchRadwegeDbAufDLMJob;
		this.radNETZProjektionsJob = radNETZAttributProjektionsJob;
		this.radwegeDBProjektionsJob = radwegeDbAttributProjektionsJob;
	}

	@Bean(initMethod = "init")
	public InitialImportService initialImportService() {
		return new InitialImportService(
			radnetzQuellImportJob,
			radwegeLglTuttlingenImportJob,
			radwegeDbImportJob,
			rvkEsslingenImportJob,
			bietigheimBissingenImportJob,
			dlmBasisQuellImportJob,
			dlmNetzbildungJob,
			radNETZNetzbildungJob,
			radwegeDBNetzbildungJob,
			netzService,
			importedFeatureRepository,
			dlmPbfErstellungsJob,
			radNETZSackgassenJob,
			applicationContext,
			radNetzMatchingAufDLMJob,
			radwegeDBMatchingAufDLMJob,
			radNETZProjektionsJob,
			radwegeDBProjektionsJob);
	}

	@Bean
	public RadVISInfoService radVISInfoService() {
		return new RadVISInfoService(commonConfigurationProperties.getVersion(), jdbcTemplate);
	}

	@Bean
	public RadVisJobScheduler radVisJobScheduler() {
		return new RadVisJobScheduler(
			jobs,
			scheduleConfigurationProperties.getRadVisStartupJobSchedule(),
			scheduleConfigurationProperties.getRadVisNaechtlicherJobSchedule()
		);
	}

	@Bean
	public RequestLoggingInterceptor requestLoggingInterceptor() {
		return new RequestLoggingInterceptor();
	}
}
