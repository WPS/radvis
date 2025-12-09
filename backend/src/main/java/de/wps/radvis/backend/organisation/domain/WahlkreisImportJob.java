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

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.organisation.domain.entity.Wahlkreis;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WahlkreisImportJob extends AbstractJob {

	// Achtung! Unterscheidet sich von dem Klassennamen, weil wir damit verhindern,
	// dass der Job ungewollt erneut ausgefuehrt wird.
	protected static final String JOB_NAME = "WahlkreisImportJob";

	private final WahlkreisRepository wahlkreisRepository;

	private final File wahlkreisFile;

	private final ShapeFileRepository shapeFileRepository;

	public WahlkreisImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		WahlkreisRepository wahlkreisRepository,
		ShapeFileRepository shapeFileRepository, File wahlkreisFile) {
		super(jobExecutionDescriptionRepository);
		this.wahlkreisRepository = wahlkreisRepository;
		this.wahlkreisFile = wahlkreisFile;
		this.shapeFileRepository = shapeFileRepository;
	}

	@Override
	public String getName() {
		return JOB_NAME;
	}

	@Override
	@Transactional
	public JobExecutionDescription run() {
		JobExecutionDescription description = super.run();
		return description;
	}

	@Override
	@Transactional
	public JobExecutionDescription run(boolean force) {
		JobExecutionDescription description = super.run(force);
		return description;
	}

	@Override
	protected Optional<JobStatistik> doRun() {

		wahlkreisRepository.deleteAll();
		try (Stream<SimpleFeature> stream = shapeFileRepository.readShape(wahlkreisFile)) {
			stream.forEach(simpleFeature -> {
				Wahlkreis wahlkreis = getWahlkreisFromSimpleFeature(simpleFeature);
				wahlkreisRepository.save(wahlkreis);
			});

		} catch (IOException | ShapeProjectionException e) {
			throw new RuntimeException(e);
		}

		return Optional.empty();
	}

	private Wahlkreis getWahlkreisFromSimpleFeature(SimpleFeature simpleFeature) {

		MultiPolygon geometry = (MultiPolygon) simpleFeature.getDefaultGeometry();

		return new Wahlkreis(simpleFeature.getAttribute("WK Name").toString(),
			Integer.valueOf(simpleFeature.getAttribute("Nummer").toString()),
			geometry);
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Importiert Wahlkreise aus der konfigurierten ShapeFile.",
			"Bestehende Wahlkreise werden gelöscht und neu importiert.",
			"",
			JobExecutionDurationEstimate.UNKNOWN
		);
	}
}
