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

import java.util.Optional;

import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaterializedViewsUpdateJob extends AbstractJob {

	private final NetzService netzService;
	private final MassnahmeRepository massnahmenRepository;

	public MaterializedViewsUpdateJob(
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		NetzService netzService,
		MassnahmeRepository massnahmenRepository) {
		super(jobExecutionDescriptionRepository);
		this.netzService = netzService;
		this.massnahmenRepository = massnahmenRepository;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	public JobExecutionDescription run() {
		return run(false);
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Transactional
	@Override
	protected Optional<JobStatistik> doRun() {
		netzService.refreshNetzMaterializedViews();
		log.info("Refreshing Maßnahmen Materialized View");
		massnahmenRepository.refreshMassnahmeMaterializedViews();
		return Optional.empty();
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Aktualisiert alle Materialized Views.",
			"Materialized Views sind aktualisiert. Tabellen und normale Views sind davon nicht betroffen.",
			"Sollte nach allen anderen Jobs ausgeführt werden, da die Materialized Views Daten aus nahezu allen größeren fachlichen Bereichen benötigen. Wichtig: Manche anderen Jobs benötigen jedoch bereits Netz-Daten aus diesen Materialized Views.",
			JobExecutionDurationEstimate.LONG);
	}
}
