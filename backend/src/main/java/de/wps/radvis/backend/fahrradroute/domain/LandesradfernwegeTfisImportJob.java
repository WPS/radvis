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

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.fahrradroute.domain.entity.AbstractTfisImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute.FahrradrouteBuilder;
import de.wps.radvis.backend.fahrradroute.domain.entity.LandesradfernwegeTFISImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LandesradfernwegeTfisImportJob extends AbstractTFISRadroutenImportJob {
	// Dieser Job Name sollte sich nicht mehr aendern, weil Controller und DB Eintraege den Namen verwenden
	private static final String JOB_NAME = "LandesradfernwegeTfisImportJob";

	private final FahrradrouteRepository fahrradrouteRepository;

	public LandesradfernwegeTfisImportJob(
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		FahrradrouteRepository fahrradrouteRepository,
		ShapeFileRepository shapeFileRepository,
		KantenRepository kantenRepository,
		TfisImportService tfisImportService,
		Path tfisRadwegePath) {
		super(jobExecutionDescriptionRepository, tfisImportService, kantenRepository, shapeFileRepository,
			tfisRadwegePath);
		this.fahrradrouteRepository = fahrradrouteRepository;
	}

	@Override
	public String getName() {
		return LandesradfernwegeTfisImportJob.JOB_NAME;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.LANDESRADFERNWEGE_TFIS_IMPORT_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		LandesradfernwegeTFISImportStatistik importStatistik = new LandesradfernwegeTFISImportStatistik();
		importFromTfis(importStatistik);

		return Optional.of(importStatistik);
	}

	@Override
	protected Fahrradroute saveFahrradroute(FahrradrouteBuilder builder, AbstractTfisImportStatistik statistik) {
		Fahrradroute tfisRoute = builder.buildLandesradfernweg();
		return fahrradrouteRepository.save(tfisRoute);
	}

	@Override
	protected FahrradrouteBuilder initFahrradrouteBuilder(TfisId forTfisId, SimpleFeature first,
		AbstractTfisImportStatistik statistik) {
		return Fahrradroute.builder();
	}

	@Override
	protected Stream<SimpleFeature> filterFeaturesToImport(Stream<SimpleFeature> featureStream,
		AbstractTfisImportStatistik statistik) {
		return featureStream.filter(tfisImportService::isLandesradfernweg);
	}
}
