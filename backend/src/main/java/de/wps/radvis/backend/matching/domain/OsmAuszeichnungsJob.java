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

package de.wps.radvis.backend.matching.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.matching.domain.service.OsmAuszeichnungsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OsmAuszeichnungsJob extends AbstractJob {

	private final OsmAuszeichnungsService osmAuszeichnungsService;

	private final File osmBasisDaten;
	private final File osmAngereichertDaten;

	public OsmAuszeichnungsJob(JobExecutionDescriptionRepository repository,
		OsmAuszeichnungsService osmAuszeichnungsService,
		File osmBasisDaten, File osmAngereichertDaten) {
		super(repository);
		require(osmAuszeichnungsService, notNullValue());
		require(osmBasisDaten, notNullValue());
		require(osmAngereichertDaten, notNullValue());

		this.osmAuszeichnungsService = osmAuszeichnungsService;
		this.osmBasisDaten = osmBasisDaten;
		this.osmAngereichertDaten = osmAngereichertDaten;
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		JobStatistik statistik = null;

		try {
			statistik = osmAuszeichnungsService.reicherePbfAn(osmBasisDaten, osmAngereichertDaten);
		} catch (IOException e) {
			log.error(
				"Fehler beim Auszeichnen des RadNETZ in den osm.pbf-Daten. OsmBasisDaten-File: {}. OsmAngereichertDaten-File: {}",
				osmBasisDaten.getAbsolutePath(), osmAngereichertDaten.getAbsolutePath(), e);
		}

		return Optional.ofNullable(statistik);
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Reichert die OSM-PBF mit einigen der RadVIS-Attribute an. Geometrien werden nicht verändert.",
			"Es wird eine zweite PBF-Datei erzeugt, die zusätzliche OSM-Tags enthält.",
			"Es müssen vorher die Jobs " + OsmPbfDownloadJob.class.getName() + " und " + MatchNetzAufOSMJob.class
				.getName() + " gelaufen sein.",
			JobExecutionDurationEstimate.SHORT
		);
	}
}
