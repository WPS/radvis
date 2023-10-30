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

package de.wps.radvis.backend.leihstation.domain;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.transaction.Transactional;

import org.locationtech.jts.geom.prep.PreparedGeometry;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.entity.LeihstationMobiDataImportStatistik;
import de.wps.radvis.backend.leihstation.domain.entity.LeihstationMobidataWFSElement;
import de.wps.radvis.backend.leihstation.domain.valueObject.Anzahl;
import de.wps.radvis.backend.leihstation.domain.valueObject.ExterneLeihstationenId;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationStatus;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LeihstationMobiDataImportJob extends AbstractJob {
	public static final String JOB_NAME = "LeihstationMobiDataImportJob";

	private final LeihstationRepository leihstationRepository;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final LeihstationMobiDataWFSRepository wfsRepository;

	public LeihstationMobiDataImportJob(
		JobExecutionDescriptionRepository repository,
		VerwaltungseinheitService verwaltungseinheitService,
		LeihstationRepository leihstationRepository,
		LeihstationMobiDataWFSRepository leihstationMobiDataWFSRepository) {
		super(repository);
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.leihstationRepository = leihstationRepository;
		this.wfsRepository = leihstationMobiDataWFSRepository;
	}

	@Override
	public String getName() {
		return JOB_NAME;
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.LEIHSTATION_MOBIDATA_IMPORT)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.LEIHSTATION_MOBIDATA_IMPORT)
	protected Optional<JobStatistik> doRun() {
		log.info(this.getName() + " gestartet.");
		LeihstationMobiDataImportStatistik statistik = new LeihstationMobiDataImportStatistik();

		Stream<LeihstationMobidataWFSElement> wfsFeatures = wfsRepository.readBikeStationFeatures();
		AtomicInteger anzahlGeupdated = new AtomicInteger();
		AtomicInteger anzahlneu = new AtomicInteger();
		AtomicInteger counter = new AtomicInteger();

		PreparedGeometry bawueGebiet = verwaltungseinheitService.getBundeslandBereichPrepared();

		Set<ExterneLeihstationenId> sollLeihstationen = wfsFeatures
			// wir wollen nur die Leistationen innerhalb Baden-Würtembergs
			.filter(importierteLeihstation -> bawueGebiet.intersects(importierteLeihstation.getPosition()))
			.map(importierteLeihstation -> {
				// Leistationen neu erstellen oder updaten und externe ID zurück liefern
				leihstationRepository.findByExterneIdAndQuellSystem(importierteLeihstation.getId(),
						LeihstationQuellSystem.MOBIDATABW)
					.ifPresentOrElse(
						// Existierende Leistation updaten
						existierendeLeistation -> {
							existierendeLeistation.setGeometrie(importierteLeihstation.getPosition());
							existierendeLeistation.setAnzahlFahrraeder(
								Anzahl.of(importierteLeihstation.getAnzahlFahrraeder()));
							leihstationRepository.save(existierendeLeistation);
							anzahlGeupdated.getAndIncrement();
						},
						// Neue Leistation hinzufügen
						() -> {
							Leihstation neueLeihstation = Leihstation.builder()
								.externeId(importierteLeihstation.getId())
								.geometrie(importierteLeihstation.getPosition())
								.anzahlFahrraeder(Anzahl.of(importierteLeihstation.getAnzahlFahrraeder()))
								/// diese Daten sind standard für aus MobiData importierte Leistationen:
								.quellSystem(LeihstationQuellSystem.MOBIDATABW)
								.status(LeihstationStatus.AKTIV)
								.freiesAbstellen(false)
								.betreiber("")
								.build();
							leihstationRepository.save(neueLeihstation);
							anzahlneu.getAndIncrement();
						}
					);
				logProgress(counter, 200, "Leihstation");
				return importierteLeihstation.getId();
			})
			// Externe IDs der neuen/geupdateten Stationen sammeln
			.collect(Collectors.toSet());

		// alte Leihstationen entfernen
		int anzahlGeloeschteMobiDataLeihstationen = leihstationRepository.deleteByExterneIdNotInAndQuellSystem(
			sollLeihstationen, LeihstationQuellSystem.MOBIDATABW);

		statistik.anzahlGeloescht = anzahlGeloeschteMobiDataLeihstationen;
		statistik.anzahlGeupdated = anzahlGeupdated.get();
		statistik.anzahlNeuErstellt = anzahlneu.get();

		log.info("JobStatistik: " + statistik);
		return Optional.of(statistik);
	}
}
