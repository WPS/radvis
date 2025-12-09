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

import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import jakarta.transaction.Transactional;

public class RecreateFahrradrouteImportDiffViewJob extends AbstractJob {
	private static final String JOB_NAME = "RecreateFahrradrouteImportDiffViewJob";

	protected final FahrradrouteRepository fahrradrouteRepository;
	private final int anzahlTageImportprotokolle;
	private final int maximaleAnzahlKoordinatenFuerImportDiff;

	public RecreateFahrradrouteImportDiffViewJob(JobExecutionDescriptionRepository repository,
		FahrradrouteRepository fahrradrouteRepository, int anzahlTageImportprotokolle,
		int maximaleAnzahlKoordinatenFuerImportDiff) {
		super(repository);
		this.fahrradrouteRepository = fahrradrouteRepository;
		this.anzahlTageImportprotokolle = anzahlTageImportprotokolle;
		this.maximaleAnzahlKoordinatenFuerImportDiff = maximaleAnzahlKoordinatenFuerImportDiff;
	}

	@Override
	public String getName() {
		return RecreateFahrradrouteImportDiffViewJob.JOB_NAME;
	}

	@Transactional
	@Override
	protected Optional<JobStatistik> doRun() {
		fahrradrouteRepository.resetGeoserverFahrradrouteImportDiffMaterializedView(anzahlTageImportprotokolle,
			maximaleAnzahlKoordinatenFuerImportDiff);
		return Optional.empty();
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Erzeugt eine Materialized View, bzw. erzeugt diese neu, die einen geometrischen Diff zwischen importierten Fahrradrouten enthält. Hierbei werden die Auditing-Informationen der Fahrradrouten ausgelesen.",
			"",
			"Abhängigkeiten",
			JobExecutionDurationEstimate.UNKNOWN
		);
	}
}
