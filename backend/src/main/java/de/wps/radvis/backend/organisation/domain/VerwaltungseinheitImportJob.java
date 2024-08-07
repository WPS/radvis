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

package de.wps.radvis.backend.organisation.domain;

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.IOException;
import java.util.Optional;

import org.springframework.data.util.Lazy;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerwaltungseinheitImportJob extends AbstractJob {

	// Achtung! Unterscheidet sich von dem Klassennamen, weil wir damit verhindern,
	// dass der Job ungewollt erneut ausgefuehrt wird.
	protected static final String JOB_NAME = "OrganisationenImportJob";

	private final Lazy<VerwaltungseinheitImportRepository> verwaltungseinheitImportRepositorySupplier;
	private final OrganisationRepository organisationRepository;
	private final GebietskoerperschaftRepository gebietskoerperschaftRepository;

	public VerwaltungseinheitImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		Lazy<VerwaltungseinheitImportRepository> verwaltungseinheitImportRepository,
		OrganisationRepository organisationRepository, GebietskoerperschaftRepository gebietskoerperschaftRepository) {
		super(jobExecutionDescriptionRepository);
		require(verwaltungseinheitImportRepository, notNullValue());

		this.verwaltungseinheitImportRepositorySupplier = verwaltungseinheitImportRepository;
		this.organisationRepository = organisationRepository;
		this.gebietskoerperschaftRepository = gebietskoerperschaftRepository;
	}

	@Override
	public String getName() {
		return JOB_NAME;
	}

	@Override
	@Transactional
	public JobExecutionDescription run() {
		return super.run();
	}

	@Override
	@Transactional
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	public boolean isRepeatable() {
		return false;
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		try {
			gebietskoerperschaftRepository.saveAll(
				verwaltungseinheitImportRepositorySupplier.get().getGebietskoerperschaften());

			organisationRepository.saveAll(
				verwaltungseinheitImportRepositorySupplier.get().getCustomAdditionalOrganisationen());

			log.info("Organisationen wurden erfolgreich in die Datenbank geschrieben.");
		} catch (IOException e) {
			log.error("Fehler beim Einlesen der Organisationen");
			throw new RuntimeException(e);
		}

		return Optional.empty();
	}
}
