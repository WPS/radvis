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

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteMatchingStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteNetzbezugResult;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation.FahrradroutenMatchingAndRoutingInformationBuilder;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DRouteMatchingJob extends AbstractJob {
	// Dieser Job Name sollte sich nicht mehr aendern, weil Controller und DB Eintraege den Namen verwenden
	public static final String JOB_NAME = "DRouteMatchingJob";

	private final FahrradrouteRepository fahrradrouteRepository;

	private final FahrradroutenMatchingService fahrradroutenMatchingService;

	public DRouteMatchingJob(JobExecutionDescriptionRepository repository,
		FahrradrouteRepository fahrradrouteRepository, FahrradroutenMatchingService fahrradroutenMatchingService) {
		super(repository);
		this.fahrradrouteRepository = fahrradrouteRepository;
		this.fahrradroutenMatchingService = fahrradroutenMatchingService;
	}

	@Override
	public String getName() {
		return DRouteMatchingJob.JOB_NAME;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.DROUTE_MATCHING_JOB)
	public JobExecutionDescription run() {
		return super.run();
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.DROUTE_MATCHING_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		log.info(JOB_NAME + " gestartet");
		FahrradrouteMatchingStatistik fahrradrouteMatchingStatistik = new FahrradrouteMatchingStatistik();

		fahrradrouteRepository.findAllByKategorie(Kategorie.D_ROUTE)
			.filter(f -> f.getNetzbezugLineString().isEmpty())
			.filter(f -> f.getOriginalGeometrie().isPresent())
			.filter(f -> f.getOriginalGeometrie().get().getGeometryType().equals(Geometry.TYPENAME_LINESTRING))
			.forEach(fahrradroute -> {
				log.info("Fahrradroute: {} mit der Id: {}", fahrradroute.getName(), fahrradroute.getId());
				FahrradroutenMatchingAndRoutingInformationBuilder fahrradroutenMatchingAndRoutingInformationBuilder = FahrradroutenMatchingAndRoutingInformation
					.builder();
				Optional<LineString> zuMatchendeGeometrieZugeschnitten = fahrradroutenMatchingService
					.schneideAnfangUndEndeAusserhalbBWsAb(
						(LineString) fahrradroute.getOriginalGeometrie().get());
				if (zuMatchendeGeometrieZugeschnitten.isEmpty()) {
					return;
				}
				Optional<FahrradrouteNetzbezugResult> fahrradrouteNetzbezugResult = fahrradroutenMatchingService
					.getFahrradrouteNetzbezugResult(
						zuMatchendeGeometrieZugeschnitten.get(), fahrradrouteMatchingStatistik,
						fahrradroutenMatchingAndRoutingInformationBuilder, false);
				if (fahrradrouteNetzbezugResult.isPresent()) {
					fahrradroute.updateNetzbezug(fahrradrouteNetzbezugResult.map(r -> r.getGeometry()),
						fahrradrouteNetzbezugResult.get().getAbschnittsweiserKantenBezug(),
						fahrradrouteNetzbezugResult.get().getProfilEigenschaften(),
						fahrradroute.getOriginalGeometrie());
					fahrradroute.setMatchingAndRoutingInformation(
						fahrradroutenMatchingAndRoutingInformationBuilder.build());
					fahrradrouteRepository.save(fahrradroute);
				}
			});

		log.info(fahrradrouteMatchingStatistik.toString());
		return Optional.empty();
	}
}
