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
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.transaction.Transactional;

import org.opengis.feature.simple.SimpleFeature;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.fahrradroute.domain.entity.AbstractTfisImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute.FahrradrouteBuilder;
import de.wps.radvis.backend.fahrradroute.domain.entity.RoutenTFISImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FahrradroutenTfisImportJob extends AbstractTFISRadroutenImportJob {
	// Dieser Job Name sollte sich nicht mehr aendern, weil Controller und DB Eintraege den Namen verwenden
	public static final String JOB_NAME = "FahrradroutenTfisImportJob";
	private final FahrradrouteRepository fahrradrouteRepository;
	private HashSet<TfisId> aktuelleTFISIDs;

	public FahrradroutenTfisImportJob(
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
		return FahrradroutenTfisImportJob.JOB_NAME;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.ROUTEN_TFIS_IMPORT_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		if (!FeatureTogglz.FAHRRADROUTE_JOBS.isActive()) {
			log.info(
				"Kein Import, da Fahrradrouten-Jobs über das FeatureToggle deaktiviert sind.");
			return Optional.empty();
		}

		RoutenTFISImportStatistik importStatistik = new RoutenTFISImportStatistik();
		aktuelleTFISIDs = new HashSet<>();

		importFromTfis(importStatistik);

		// Lösche fahrradrouten, deren TFIS_ID laut Quelle nicht mehr existiert
		importStatistik.deletedRoutes = fahrradrouteRepository.deleteAllByFahrradrouteTypAndTfisIdNotIn(
			FahrradrouteTyp.TFIS_ROUTE,
			aktuelleTFISIDs);

		return Optional.of(importStatistik);
	}

	@Override
	protected Fahrradroute saveFahrradroute(FahrradrouteBuilder builder, AbstractTfisImportStatistik statistik) {
		Fahrradroute fahrradroute;
		Long fahrradrouteId = builder.buildTfisRoute().getId();
		Optional<Fahrradroute> findById = fahrradrouteId != null ? fahrradrouteRepository.findById(fahrradrouteId)
			: Optional.empty();
		if (findById.isPresent()) {
			fahrradroute = fahrradrouteRepository.save(builder.build());
		} else {
			fahrradroute = fahrradrouteRepository.save(builder.buildTfisRoute());
		}

		this.aktuelleTFISIDs.add(fahrradroute.getTfisId());
		return fahrradroute;
	}

	@Override
	protected FahrradrouteBuilder initFahrradrouteBuilder(TfisId forTfisId, SimpleFeature first,
		AbstractTfisImportStatistik statistik) {
		Optional<Fahrradroute> fahrradroute = fahrradrouteRepository.findByTfisId(forTfisId);
		FahrradrouteBuilder builder;
		if (fahrradroute.isPresent()) {
			((RoutenTFISImportStatistik) statistik).updatedRoutes++;
			builder = fahrradroute.get().toBuilder();
		} else {
			((RoutenTFISImportStatistik) statistik).newRoutes++;
			builder = Fahrradroute.builder();
		}
		builder
			.kurzbeschreibung(tfisImportService.extractKurzbeschreibung(first))
			.kategorie(tfisImportService.extractKategorie(first))
			.beschreibung(tfisImportService.extractBeschreibung(first))
			.info(tfisImportService.extractInfo(first))
			.offizielleLaenge(tfisImportService.extractOffizielleLaenge(first));
		return builder;
	}

	@Override
	protected Stream<SimpleFeature> filterFeaturesToImport(Stream<SimpleFeature> featureStream,
		AbstractTfisImportStatistik statistik) {
		return featureStream
			.filter(simpleFeature -> !tfisImportService.isLandesradfernweg(simpleFeature))
			.filter(simpleFeature -> !tfisImportService.extractName(simpleFeature).startsWith("Landkreis"))
			.filter(simpleFeature -> !tfisImportService.extractName(simpleFeature).startsWith("Stadtkreis"));
	}
}
