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
import java.util.Set;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.fahrradroute.domain.entity.AbstractTfisImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute.FahrradrouteBuilder;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradroutenTfisUpdateStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.LandesradfernwegeTFISImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * Für alle TFIS-Routen ohne durchgehende Geometrie (also bisher nicht gematched) wird ein erneuter Import und
 * Matching-Versuch unternommen. Das inkludiert auch LRFW.
 */
@WithFehlercode(Fehlercode.FAHRRADROUTE_TFIS_IMPORT)
public class FahrradroutenTfisUpdateJob extends AbstractTFISRadroutenImportJob {
	// Dieser Job Name sollte sich nicht mehr aendern, weil Controller und DB Eintraege den Namen verwenden
	public static final String JOB_NAME = "FahrradroutenTfisUpdateJob";

	private FahrradrouteRepository fahrradrouteRepository;

	public FahrradroutenTfisUpdateJob(JobExecutionDescriptionRepository repository,
		TfisImportService tfisImportService,
		KantenRepository kantenRepository,
		ShapeFileRepository shapeFileRepository,
		Path tfisRadwegePath,
		FahrradrouteRepository fahrradrouteRepository) {
		super(repository, tfisImportService, kantenRepository, shapeFileRepository, tfisRadwegePath);
		this.fahrradrouteRepository = fahrradrouteRepository;
	}

	@Override
	public String getName() {
		return FahrradroutenTfisUpdateJob.JOB_NAME;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.FAHRRADROUTE_TFISNETZBEZUG_UPDATE_JOB)
	public JobExecutionDescription run() {
		return super.run();
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.FAHRRADROUTE_TFISNETZBEZUG_UPDATE_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		LandesradfernwegeTFISImportStatistik importStatistik = new FahrradroutenTfisUpdateStatistik();
		importFromTfis(importStatistik);

		return Optional.of(importStatistik);
	}

	@Override
	protected Fahrradroute saveFahrradroute(FahrradrouteBuilder builder, AbstractTfisImportStatistik statistik) {
		// damit wir an die gesetzten Werte kommen, es muss kein LRFW sein, aber für den werden alle Attribute im
		// AbstractJob gesetzt
		Fahrradroute newFahrradroute = builder.buildLandesradfernweg();
		Fahrradroute existingFahrradroute = fahrradrouteRepository.findByTfisId(newFahrradroute.getTfisId())
			.orElseThrow();
		// Wir können hier nichts verschlechtern, da wir nur TFIS-Routen ohne NetzbezugLineString betrachten ->
		// s.filterFeaturesToImport
		existingFahrradroute.updateNetzbezug(
			newFahrradroute.getNetzbezugLineString(),
			newFahrradroute.getAbschnittsweiserKantenBezug(),
			newFahrradroute.getLinearReferenzierteProfilEigenschaften(),
			newFahrradroute.getOriginalGeometrie());
		log.info("Der Netzbezug wurde geupdated (AbschnittsweiserKantenBezug, NetzbezugLineString, Stuetzpunkte).");
		return fahrradrouteRepository.save(existingFahrradroute);
	}

	@Override
	protected FahrradrouteBuilder initFahrradrouteBuilder(TfisId forTfisId, SimpleFeature feature,
		AbstractTfisImportStatistik statistik) {
		return Fahrradroute.builder();
	}

	@Override
	protected Stream<SimpleFeature> filterFeaturesToImport(Stream<SimpleFeature> stream,
		AbstractTfisImportStatistik statistik) {
		Set<TfisId> ids = fahrradrouteRepository.findAllTfisIdsWithoutNetzbezugLineString();
		return stream.filter(s -> {
			return ids.contains(TfisId.of(TfisImportService.extractObjid(s)));
		});
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Importiert Farradrouten aus der TFIS-Datei von Platte und aktualisiert dessen Netzbezug-Geometrien und Profileigenschaften.",
			"TFIS-Fahrradrouten verändern sich im Netzbezug und ihren Attributen.",
			JobExecutionDurationEstimate.MEDIUM
		);
	}
}
