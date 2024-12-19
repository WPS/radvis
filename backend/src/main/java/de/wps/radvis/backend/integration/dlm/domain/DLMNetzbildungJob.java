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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.valid4j.Assertive.require;

import java.util.Optional;
import java.util.stream.Stream;

import org.hamcrest.Matchers;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DLMNetzbildungJob extends AbstractJob {

	private final ImportedFeaturePersistentRepository importedFeatureRepository;
	private final DLMNetzbildungService dlmNetzbildungService;

	public DLMNetzbildungJob(ImportedFeaturePersistentRepository importedFeatureRepository,
		DLMNetzbildungService dlmNetzbildungService,
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository) {
		super(jobExecutionDescriptionRepository);

		require(importedFeatureRepository, Matchers.notNullValue());
		require(dlmNetzbildungService, Matchers.notNullValue());

		this.importedFeatureRepository = importedFeatureRepository;
		this.dlmNetzbildungService = dlmNetzbildungService;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.NETZBILDUNG_DLM_JOB)
	public JobExecutionDescription run() {
		return super.run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.NETZBILDUNG_DLM_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {

		Stream<ImportedFeature> features = importedFeatureRepository.getAllByQuelleAndArt(QuellSystem.DLM,
			Art.Strecke);

		dlmNetzbildungService.bildeDLMBasisNetz(features);
		log.info("DLMNetzbildungJob abgeschlossen.");
		return Optional.empty();
	}
}
