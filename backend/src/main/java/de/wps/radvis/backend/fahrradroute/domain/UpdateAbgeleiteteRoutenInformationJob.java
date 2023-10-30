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

package de.wps.radvis.backend.fahrradroute.domain;

import java.util.Optional;

import jakarta.transaction.Transactional;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.UpdateAbgeleiteteRoutenInfoStatistik;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateAbgeleiteteRoutenInformationJob extends AbstractJob {
	private final FahrradrouteService fahrradrouteService;

	public UpdateAbgeleiteteRoutenInformationJob(JobExecutionDescriptionRepository repository,
		FahrradrouteService fahrradrouteService) {
		super(repository);
		this.fahrradrouteService = fahrradrouteService;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.FAHRRADROUTE_ABGELEITETE_ROUTENINFO_UPDATE_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		if (!FeatureTogglz.FAHRRADROUTE_JOBS.isActive()) {
			log.info(
				"Abgeleitete Routeninfos werden nicht geupdated, da Fahrradrouten-Jobs über das FeatureToggle deaktiviert sind.");
			return Optional.empty();
		}
		UpdateAbgeleiteteRoutenInfoStatistik updateAbgeleiteteRoutenInfoStatistik = new UpdateAbgeleiteteRoutenInfoStatistik();
		fahrradrouteService.updateAbgeleiteteRoutenInformationVonRadvisUndTfis(updateAbgeleiteteRoutenInfoStatistik);
		log.info(updateAbgeleiteteRoutenInfoStatistik.toString());
		return Optional.of(updateAbgeleiteteRoutenInfoStatistik);
	}
}