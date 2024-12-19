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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.leihstation.domain.entity.LeihstationMobiDataImportStatistik;
import de.wps.radvis.backend.leihstation.domain.valueObject.Anzahl;
import de.wps.radvis.backend.leihstation.domain.valueObject.ExterneLeihstationenId;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationStatus;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithFehlercode(Fehlercode.LEIHSTATIONEN_IMPORT)
public class LeihstationMobiDataImportJob extends AbstractJob {
	public static final String JOB_NAME = "LeihstationMobiDataImportJob";

	private final LeihstationRepository leihstationRepository;
	private final VerwaltungseinheitService verwaltungseinheitService;
	private final GeoJsonImportRepository geoJsonImportRepository;
	private final String mobiDataLeihstationenGeoJsonWFSUrl;

	static final String BIKESTATION_COUNT_KEY = "num_vehicles_available";
	static final String BIKESTATION_ID_KEY = "station_id";

	public LeihstationMobiDataImportJob(
		JobExecutionDescriptionRepository repository,
		VerwaltungseinheitService verwaltungseinheitService,
		LeihstationRepository leihstationRepository,
		GeoJsonImportRepository geoJsonImportRepository, String mobiDataLeihstationenGeoJsonWFSUrl) {
		super(repository);
		this.verwaltungseinheitService = verwaltungseinheitService;
		this.leihstationRepository = leihstationRepository;
		this.geoJsonImportRepository = geoJsonImportRepository;
		this.mobiDataLeihstationenGeoJsonWFSUrl = mobiDataLeihstationenGeoJsonWFSUrl;
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
		URL url;
		try {
			url = URI.create(mobiDataLeihstationenGeoJsonWFSUrl).toURL();
		} catch (MalformedURLException e) {
			log.error("Leihstation Url {} ist ungültig", mobiDataLeihstationenGeoJsonWFSUrl, e);
			throw new RuntimeException(e);
		}

		String fileContentAsString = null;
		try {
			fileContentAsString = geoJsonImportRepository.getFileContentAsString(url);
		} catch (IOException e) {
			log.error("Geojson konnte von Leihstation Url {} nicht geladen werden", mobiDataLeihstationenGeoJsonWFSUrl,
				e);
			throw new RuntimeException(e);
		}

		List<SimpleFeature> wfsFeatures = null;
		try {
			wfsFeatures = geoJsonImportRepository.getSimpleFeatures(
				fileContentAsString);
		} catch (ReadGeoJSONException e) {
			log.error("Leihstation Features konnten nicht aus MobiDataBW WFS mit URL {} gelesen werden",
				mobiDataLeihstationenGeoJsonWFSUrl, e);
			throw new RuntimeException(e);
		}
		AtomicInteger anzahlGeupdated = new AtomicInteger();
		AtomicInteger anzahlneu = new AtomicInteger();
		AtomicInteger counter = new AtomicInteger();
		AtomicInteger anzahlFehlerhaft = new AtomicInteger();

		PreparedGeometry bawueGebiet = verwaltungseinheitService.getBundeslandBereichPrepared();

		Set<ExterneLeihstationenId> sollLeihstationen = wfsFeatures
			.stream()
			// wir wollen nur die Leistationen innerhalb Baden-Würtembergs
			.filter(
				importierteLeihstation -> bawueGebiet.intersects((Point) importierteLeihstation.getDefaultGeometry()))
			.map(importierteLeihstation -> {
				Object externeIdAttribute = importierteLeihstation.getAttribute(BIKESTATION_ID_KEY);
				if (Objects.isNull(externeIdAttribute)) {
					log.warn("Leihstation-Feature ohne externe Id wird übersprungen: {}", importierteLeihstation);
					return null;
				}
				String externeIdString = externeIdAttribute.toString();

				if (!ExterneLeihstationenId.isValid(externeIdString)) {
					log.warn("Leihstation-Feature mit ungültiger externer Id '{}' wird übersprungen: {}",
						externeIdString,
						importierteLeihstation);
					return null;
				}
				ExterneLeihstationenId externeId = ExterneLeihstationenId.of(externeIdString);

				Object anzahlFahrraederAttribute = importierteLeihstation.getAttribute(BIKESTATION_COUNT_KEY);
				if (Objects.isNull(anzahlFahrraederAttribute)) {
					log.warn("Leihstation-Feature ohne Angabe der Anzahl an Fahrrädern wird übersprungen: {}",
						importierteLeihstation);
					return null;
				}

				String anzahlFahrraederString = anzahlFahrraederAttribute.toString();
				if (!Anzahl.isValid(anzahlFahrraederString)) {
					log.warn(
						"Leihstation-Feature mit ungültiger Angabe der Anzahl an Fahrrädern '{}' wird übersprungen: {}",
						anzahlFahrraederString,
						importierteLeihstation);
					return null;
				}

				Anzahl anzahlFahrraeder = Anzahl.of(anzahlFahrraederString);

				if (Objects.isNull(importierteLeihstation.getDefaultGeometry())
					|| !(importierteLeihstation.getDefaultGeometry() instanceof Point)) {
					log.warn("Leihstation-Feature ohne Geometrie oder mit nicht-Punkt-Geometrie wird übersprungen: {}",
						importierteLeihstation);
					return null;
				}
				// Leistationen neu erstellen oder updaten und externe ID zurück liefern
				leihstationRepository.findByExterneIdAndQuellSystem(externeId, LeihstationQuellSystem.MOBIDATABW)
					.ifPresentOrElse(
						// Existierende Leistation updaten
						existierendeLeistation -> {
							existierendeLeistation.setGeometrie((Point) importierteLeihstation.getDefaultGeometry());
							existierendeLeistation.setAnzahlFahrraeder(anzahlFahrraeder);
							leihstationRepository.save(existierendeLeistation);
							anzahlGeupdated.getAndIncrement();
						},
						// Neue Leihstation hinzufügen
						() -> {
							Leihstation neueLeihstation = Leihstation.builder()
								.externeId(externeId)
								.geometrie((Point) importierteLeihstation.getDefaultGeometry())
								.anzahlFahrraeder(anzahlFahrraeder)
								/// diese Daten sind standard für aus MobiData importierte Leistationen:
								.quellSystem(LeihstationQuellSystem.MOBIDATABW)
								.status(LeihstationStatus.AKTIV)
								.freiesAbstellen(false)
								.betreiber("")
								.build();
							leihstationRepository.save(neueLeihstation);
							anzahlneu.getAndIncrement();
						});
				logProgress(counter, 200, "Leihstation");
				return externeId;
			})
			.filter(id -> {
				if (Objects.isNull(id)) {
					anzahlFehlerhaft.incrementAndGet();
					return false;
				} else {
					return true;
				}
			})
			// Externe IDs der neuen/geupdateten Stationen sammeln
			.collect(Collectors.toSet());

		// alte Leihstationen entfernen
		int anzahlGeloeschteMobiDataLeihstationen = leihstationRepository.deleteByExterneIdNotInAndQuellSystem(
			sollLeihstationen, LeihstationQuellSystem.MOBIDATABW);

		statistik.anzahlGeloescht = anzahlGeloeschteMobiDataLeihstationen;
		statistik.anzahlGeupdated = anzahlGeupdated.get();
		statistik.anzahlNeuErstellt = anzahlneu.get();
		statistik.anzahlLeihstationenAttributmappingFehlerhaft = anzahlFehlerhaft.get();

		log.info("JobStatistik: " + statistik);
		return Optional.of(statistik);
	}
}
