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

import java.io.IOException;
import java.util.Optional;

import org.springframework.data.util.Lazy;

import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.RamUsageUtility;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.matching.domain.service.GraphhopperUpdateService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlmPbfErstellungsJob extends AbstractJob {
	private final DlmPbfErstellungService dlmPbfErstellungService;
	private final Lazy<GraphhopperUpdateService> graphhopperUpdateService;

	public DlmPbfErstellungsJob(
		DlmPbfErstellungService dlmPbfErstellungService,
		JobExecutionDescriptionRepository repository,
		Lazy<GraphhopperUpdateService> graphhopperUpdateService) {
		super(repository);
		this.dlmPbfErstellungService = dlmPbfErstellungService;
		this.graphhopperUpdateService = graphhopperUpdateService;
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		RamUsageUtility.logCurrentRamUsage("Vor pbf Erstellung");
		try {
			dlmPbfErstellungService.erstelleDlmPbf();
		} catch (IOException e) {
			log.error(
				"Fehler beim Erstellen der DlmPbf Datei. Die vorherige Datei wird beibehalten.", e);
		}
		RamUsageUtility.logCurrentRamUsage("Nach pbf Erstellung");
		// Graphhopper cache neu erstellen und currentGraphhopper austauschen, sodass die hier neu erstellte pbf
		// auch verwendet wird
		this.graphhopperUpdateService.get().update();
		return Optional.empty();
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Erstellt die PBF-Datei vom DLM-Netz, die für alle Routing- und Matching-Funktionen gebraucht wird.",
			"Liest Daten aus DB und legt eine PBF-Datei auf die Festplatte.",
			"Sollte nach Netz-verändernden Jobs laufen (insb. nach dem DLM-Reimport).",
			JobExecutionDurationEstimate.LONG
		);
	}
}
