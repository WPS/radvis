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

package de.wps.radvis.backend.integration.dlm;

import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeUebertragungService;
import de.wps.radvis.backend.integration.dlm.domain.AttributlueckenSchliessenJob;
import de.wps.radvis.backend.integration.dlm.domain.AttributlueckenSchliessenProblemRepository;
import de.wps.radvis.backend.integration.dlm.domain.AttributlueckenService;
import de.wps.radvis.backend.integration.dlm.domain.CreateKantenService;
import de.wps.radvis.backend.integration.dlm.domain.DLMAttributMapper;
import de.wps.radvis.backend.integration.dlm.domain.DLMInitialImportJob;
import de.wps.radvis.backend.integration.dlm.domain.DLMNetzbildungJob;
import de.wps.radvis.backend.integration.dlm.domain.DLMNetzbildungProtokollService;
import de.wps.radvis.backend.integration.dlm.domain.DLMNetzbildungService;
import de.wps.radvis.backend.integration.dlm.domain.DlmImportService;
import de.wps.radvis.backend.integration.dlm.domain.DlmReimportJob;
import de.wps.radvis.backend.integration.dlm.domain.FindKnotenFromIndexService;
import de.wps.radvis.backend.integration.dlm.domain.InitialPartitionenImportService;
import de.wps.radvis.backend.integration.dlm.domain.NetzkorrekturConfigurationProperties;
import de.wps.radvis.backend.integration.dlm.domain.VernetzungKorrekturJob;
import de.wps.radvis.backend.integration.dlm.domain.VernetzungService;
import de.wps.radvis.backend.matching.domain.repository.CustomDlmMatchingRepositoryFactory;
import de.wps.radvis.backend.matching.domain.service.CustomGrundnetzMappingServiceFactory;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenWithInitialStatesRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DlmRepository;
import jakarta.persistence.EntityManager;

@Configuration
@EnableJpaRepositories
@EntityScan
public class IntegrationDlmConfiguration {
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private NetzService netzService;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private DlmPbfErstellungService dlmPbfErstellungService;

	@Autowired
	private CustomDlmMatchingRepositoryFactory customDlmMatchingRepositoryFactory;

	@Autowired
	private DLMConfigurationProperties dlmConfigurationProperties;

	@Autowired
	private NetzkorrekturConfigurationProperties netzKorrekturConfigurationProperties;

	@Autowired
	private KnotenRepository knotenRepository;

	@Autowired
	private NetzfehlerRepository netzfehlerRepository;

	@Autowired
	private AttributlueckenSchliessenProblemRepository attributlueckenSchliessenProblemRepository;

	@Autowired
	private KantenWithInitialStatesRepository kantenWithInitialStatesRepository;

	@Autowired
	private ImportedFeaturePersistentRepository importedFeatureRepository;

	@Autowired
	private KantenAttributeUebertragungService kantenAttributeUebertragungService;

	@Autowired
	private DlmRepository dlmRepository;

	@Bean
	public FindKnotenFromIndexService topologieUpdateService() {
		return new FindKnotenFromIndexService();
	}

	@Bean
	public CreateKantenService createKantenService() {
		return new CreateKantenService(dlmAttributMapper(), netzService, topologieUpdateService());
	}

	@Bean
	public DlmReimportJob dlmReimportJob() {
		return new DlmReimportJob(jobExecutionDescriptionRepository,
			dlmPbfErstellungService,
			kantenAttributeUebertragungService,
			vernetzungService(),
			netzService,
			dlmImportService(),
			customDlmMatchingRepositoryFactory,
			customGrundnetzMappingServiceFactory());
	}

	@Bean
	public CustomGrundnetzMappingServiceFactory customGrundnetzMappingServiceFactory() {
		return new CustomGrundnetzMappingServiceFactory();
	}

	@Bean
	public DlmImportService dlmImportService() {
		return new DlmImportService(dlmRepository, netzService);
	}

	@Bean
	public VernetzungService vernetzungService() {
		return new VernetzungService(kantenRepository, knotenRepository, netzService);
	}

	@Bean
	public VernetzungKorrekturJob vernetzungKorrekturJob() {
		Envelope envelope = new Envelope(dlmConfigurationProperties.getExtentProperty().getMinX(),
			dlmConfigurationProperties.getExtentProperty().getMaxX(),
			dlmConfigurationProperties.getExtentProperty().getMinY(),
			dlmConfigurationProperties.getExtentProperty().getMaxY());
		return new VernetzungKorrekturJob(jobExecutionDescriptionRepository, kantenRepository, knotenRepository,
			entityManager, envelope, netzKorrekturConfigurationProperties.getVernetzungKorrekturPartitionBreiteInM(),
			vernetzungService(), netzService);
	}

	@Bean
	public InitialPartitionenImportService initialPartitionenImportService() {
		return new InitialPartitionenImportService(createKantenService(), dlmRepository, netzService);
	}

	@Bean
	public DLMInitialImportJob dlmInitialImportJob() {
		return new DLMInitialImportJob(
			jobExecutionDescriptionRepository,
			dlmRepository, netzService,
			entityManager, initialPartitionenImportService());
	}

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

	@Bean
	public AttributlueckenSchliessenJob attributlueckenSchliessenJob() {
		return new AttributlueckenSchliessenJob(
			jobExecutionDescriptionRepository,
			kantenWithInitialStatesRepository,
			netzService,
			attributlueckenService(),
			netzKorrekturConfigurationProperties.isAttributlueckeninGpkgSchreiben());
	}

	@Bean
	public AttributlueckenService attributlueckenService() {
		return new AttributlueckenService(
			kantenAttributeUebertragungService,
			attributlueckenSchliessenProblemRepository,
			netzService,
			netzKorrekturConfigurationProperties.getAttributlueckenMaximaleLaengeInM(),
			netzKorrekturConfigurationProperties.getAttributlueckenMaximaleKantenanzahl(),
			netzKorrekturConfigurationProperties.getMaximaleAnzahlAdjazenterAttribuierterKanten());
	}
}
