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

package de.wps.radvis.backend.integration.grundnetzReimport;

import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.grundnetz.domain.DLMAttributMapper;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.CreateKantenService;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.DLMInitialImportJob;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.DLMReimportJob;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.ExecuteTopologischeUpdatesService;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.FindKnotenFromIndexService;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.InitialPartitionenImportService;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.UpdateKantenService;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.VernetzungKorrekturJob;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.VernetzungService;
import de.wps.radvis.backend.matching.domain.KanteUpdateElevationService;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMWFSImportRepository;
import jakarta.persistence.EntityManager;

@Configuration
public class GrundnetzReimportConfiguration {
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private NetzService netzService;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private DLMWFSImportRepository dlmImportRepository;

	@Autowired
	private KantenMappingRepository kantenMappingRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private DLMAttributMapper dlmAttributMapper;

	@Autowired
	private DLMConfigurationProperties dlmConfigurationProperties;

	@Autowired
	private KnotenRepository knotenRepository;

	@Autowired
	private KanteUpdateElevationService kanteUpdateElevationService;

	@Bean
	public FindKnotenFromIndexService topologieUpdateService() {
		return new FindKnotenFromIndexService();
	}

	@Bean
	public CreateKantenService createKantenService() {
		return new CreateKantenService(dlmAttributMapper, netzService, topologieUpdateService());
	}

	@Bean
	public UpdateKantenService updateKantenService() {
		return new UpdateKantenService(dlmAttributMapper, kantenRepository);
	}

	@Bean
	public ExecuteTopologischeUpdatesService updateAttributgruppenService() {
		return new ExecuteTopologischeUpdatesService(topologieUpdateService(), kantenMappingRepository);
	}

	@Bean
	public DLMReimportJob dlmReimportJob() {
		return new DLMReimportJob(
			jobExecutionDescriptionRepository,
			dlmImportRepository, netzService,
			updateKantenService(), createKantenService(), updateAttributgruppenService(),
			kantenMappingRepository,
			entityManager, vernetzungService(), kanteUpdateElevationService);
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
			entityManager, envelope, 20000.0, vernetzungService(), netzService);
	}

	@Bean
	public InitialPartitionenImportService initialPartitionenImportService() {
		return new InitialPartitionenImportService(createKantenService(), dlmImportRepository, netzService);
	}

	@Bean
	public DLMInitialImportJob dlmInitialImportJob() {
		return new DLMInitialImportJob(
			jobExecutionDescriptionRepository,
			dlmImportRepository, netzService,
			entityManager, initialPartitionenImportService());
	}
}
