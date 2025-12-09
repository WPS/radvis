/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.netz.domain;

import java.util.HashMap;
import java.util.Optional;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.netz.domain.entity.KantenAuditingSetzenJobStatistik;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Setzt Auditing-Informationen an Kanten und deren Attributgruppen, wo noch keine Auditing-Informationen existieren.
 */
@Slf4j
public class KantenAuditingSetzenJob extends AbstractJob {

	private final NetzService netzService;
	private final BenutzerService benutzerService;

	public KantenAuditingSetzenJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		NetzService netzService, BenutzerService benutzerService) {
		super(jobExecutionDescriptionRepository);
		this.netzService = netzService;
		this.benutzerService = benutzerService;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.KANTEN_AUDITING_SETZEN_JOB)
	public JobExecutionDescription run() {
		return run(false);
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.KANTEN_AUDITING_SETZEN_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Transactional
	@Override
	protected Optional<JobStatistik> doRun() {
		KantenAuditingSetzenJobStatistik statistik = new KantenAuditingSetzenJobStatistik();

		HashMap<String, Integer> tableToNewEntities = netzService.addMissingAuditingEntries(
			benutzerService.getTechnischerBenutzer().getId());
		statistik.ergaenzteEintraegeProTabelle = tableToNewEntities;

		log.info("JobStatistik:\n{}", statistik.toPrettyJSON());

		return Optional.of(statistik);
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Ergänzt fehlende Auditing-Einträge für Kanten und ihre Attributgruppen.",
			"Auditing-Einträge sind ergänzt. Kanten mit Auditing-Einträgen werden übersprungen und nicht angefasst.",
			JobExecutionDurationEstimate.MEDIUM
		);
	}
}
