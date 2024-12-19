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

package de.wps.radvis.backend.application.domain;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;

import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributProjektionsJob;
import de.wps.radvis.backend.integration.dlm.domain.DLMNetzbildungJob;
import de.wps.radvis.backend.integration.radnetz.domain.RadNETZNetzbildungJob;
import de.wps.radvis.backend.integration.radnetz.domain.RadNETZSackgassenJob;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungJob;
import de.wps.radvis.backend.matching.domain.DlmPbfErstellungsJob;
import de.wps.radvis.backend.matching.domain.MatchNetzAufDLMJob;
import de.wps.radvis.backend.matching.domain.MatchNetzAufOSMJob;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.quellimport.common.domain.GenericQuellImportJob;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMBasisQuellImportJob;
import de.wps.radvis.backend.quellimport.radnetz.domain.RadNETZQuellImportJob;

public class InitialImportService {

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
	private final ApplicationContext applicationContext;

	private MatchNetzAufOSMJob dlmMatchingAufOSMJob;

	private final DlmPbfErstellungsJob dlmPbfErstellungsJob;

	private final MatchNetzAufDLMJob radNetzMatchingAufDLMJob;
	private final MatchNetzAufDLMJob radwegeDBMatchingAufDLMJob;

	private final AttributProjektionsJob radNETZProjektionsJob;
	private final AttributProjektionsJob radwegeDBProjektionsJob;

	private final RadNETZSackgassenJob radNETZSackgassenJob;

	public InitialImportService(
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
		ImportedFeaturePersistentRepository importedFeaturesRepository,
		DlmPbfErstellungsJob dlmPbfErstellungsJob,
		RadNETZSackgassenJob radNETZSackgassenJob,
		ApplicationContext applicationContext, MatchNetzAufDLMJob radNetzMatchingAufDLMJob,
		MatchNetzAufDLMJob radwegeDBMatchingAufDLMJob, AttributProjektionsJob radNETZProjektionsJob,
		AttributProjektionsJob radwegeDBProjektionsJob) {

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
		this.radNetzMatchingAufDLMJob = radNetzMatchingAufDLMJob;
		this.radwegeDBMatchingAufDLMJob = radwegeDBMatchingAufDLMJob;
		this.radNETZProjektionsJob = radNETZProjektionsJob;
		this.radwegeDBProjektionsJob = radwegeDBProjektionsJob;
		this.radNETZSackgassenJob = radNETZSackgassenJob;
		this.applicationContext = applicationContext;
	}

	// Wird bei der Initialisierung des Beans ausgefuehrt
	public void init() {
		dlmNetzbildungJob.setInputSummarySupplier(dlmBasisQuellImportJob.asJobExecutionInputSummarySupplier());
		radNETZNetzbildungJob.setInputSummarySupplier(radnetzQuellImportJob.asJobExecutionInputSummarySupplier());

		dlmMatchingAufOSMJob = applicationContext.getBean(MatchNetzAufOSMJob.class);
		dlmMatchingAufOSMJob.setInputSummarySupplier(dlmMatchingAufOSMJob.asJobExecutionInputSummarySupplier());

		radNetzMatchingAufDLMJob.setInputSummarySupplier(radNETZNetzbildungJob.asJobExecutionInputSummarySupplier());
		radNETZProjektionsJob
			.setInputSummarySupplier(radNETZProjektionsJob.asJobExecutionInputSummarySupplier());

		radwegeDBMatchingAufDLMJob
			.setInputSummarySupplier(radwegeDBMatchingAufDLMJob.asJobExecutionInputSummarySupplier());
		radwegeDBProjektionsJob
			.setInputSummarySupplier(radwegeDBProjektionsJob.asJobExecutionInputSummarySupplier());
	}

	@Async
	public Future<Void> runJobs() {
		radnetzQuellImportJob.run();
		radwegeLglTuttlingenImportJob.run();
		radwegeDbImportJob.run();
		rvkEsslingenImportJob.run();
		bietigheimBissingenImportJob.run();
		dlmBasisQuellImportJob.run();

		importedFeatureRepository.buildIndex();

		dlmNetzbildungJob.run();
		radNETZNetzbildungJob.run();
		radwegeDBNetzbildungJob.run();

		dlmMatchingAufOSMJob.run();

		dlmPbfErstellungsJob.run();

		radNetzMatchingAufDLMJob.run();
		radwegeDBMatchingAufDLMJob.run();

		netzService.buildIndices();

		radNETZProjektionsJob.run();
		radNETZSackgassenJob.run();
		radwegeDBProjektionsJob.run();

		return new CompletableFuture<>();
	}

	@Async
	public Future<Void> runJobsFuerAttributprojektion() {
		radnetzQuellImportJob.run();
		dlmBasisQuellImportJob.run();
		radwegeDbImportJob.run();

		importedFeatureRepository.buildIndex();

		radNETZNetzbildungJob.run();
		dlmNetzbildungJob.run();
		radwegeDBNetzbildungJob.run();

		netzService.buildIndices();

		dlmPbfErstellungsJob.run();

		radNetzMatchingAufDLMJob.run();
		radwegeDBMatchingAufDLMJob.run();

		radNETZProjektionsJob.run();
		radNETZSackgassenJob.run();
		radwegeDBProjektionsJob.run();

		return new CompletableFuture<>();
	}
}
