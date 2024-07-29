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

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.fahrradroute.domain.entity.DRouteImportStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.DrouteId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Importiert D-Routen aus der konfigurierten Shapefile.
 *
 * Anforderungen an die Shapefile:
 * <ol>
 * <li>Die Geometrie ist ein zusammenhängender LineString pro D-Route.</li>
 * <li>Das Feld "name" enthält den Namen der D-Route.</li>
 * <li>Optional: Das Feld "id" enthält die Nummer der D-Route.</li>
 * </ol>
 */
@Slf4j
public class DRouteImportJob extends AbstractJob {
	// Dieser Job Name sollte sich nicht mehr aendern, weil Controller und DB Eintraege den Namen verwenden
	public static final String JOB_NAME = "DRouteImportJob";

	private final FahrradrouteRepository fahrradrouteRepository;
	private final ShapeFileRepository shapeFileRepository;
	private final Path dRoutenShapefilePath;

	public DRouteImportJob(
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		FahrradrouteRepository fahrradrouteRepository,
		ShapeFileRepository shapeFileRepository,
		Path dRoutenShapefilePath) {
		super(jobExecutionDescriptionRepository);
		this.fahrradrouteRepository = fahrradrouteRepository;
		this.shapeFileRepository = shapeFileRepository;
		this.dRoutenShapefilePath = dRoutenShapefilePath;
	}

	@Override
	public String getName() {
		return DRouteImportJob.JOB_NAME;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.DROUTE_IMPORT_JOB)
	public JobExecutionDescription run() {
		return super.run();
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.DROUTE_IMPORT_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		log.info(JOB_NAME + " gestartet");
		DRouteImportStatistik importStatistik = new DRouteImportStatistik();

		LocalDateTime importDate = LocalDateTime.now();
		try (Stream<SimpleFeature> simpleFeatureStream = this.shapeFileRepository.readShape(
			dRoutenShapefilePath.toFile())) {
			log.info("Extrahiere D-Routen aus {}", dRoutenShapefilePath.getFileName());

			simpleFeatureStream.forEach(feature -> {
				LineString geometry = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
					.createLineString(
						Arrays.stream(((Geometry) feature.getDefaultGeometry()).getCoordinates())
							.map(coordinate -> new Coordinate(coordinate.x, coordinate.y))
							.toArray(Coordinate[]::new));

				String drouteName = feature.getAttribute("name").toString();

				Fahrradroute fahrradroute = new Fahrradroute(
					FahrradrouteName.of(drouteName),
					this.getDRouteID(feature),
					geometry,
					KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
						.createPoint(geometry.getCoordinates()[0]),
					importDate
				);
				fahrradrouteRepository.save(fahrradroute);
				importStatistik.anzahlFahrradroutenErstellt++;
			});
		} catch (ShapeProjectionException | IOException e) {
			throw new RuntimeException(e);
		}

		log.info(importStatistik.toString());
		return Optional.of(importStatistik);
	}

	private DrouteId getDRouteID(SimpleFeature feature) {
		// Alte Datensätze haben keine "id" Felder, da wandeln wir händisch den Namen in die D-Routen ID um.
		Object routenIdObj = feature.getAttribute("id");
		if (routenIdObj != null) {
			String routenId = routenIdObj.toString().trim();
			if (!routenId.isEmpty()) {
				return DrouteId.of(routenId);
			}
		}

		String name = feature.getAttribute("name").toString();
		switch (name) {
		case "Saar-Mosel-Main":
			return DrouteId.of("5");
		case "Donauroute":
			return DrouteId.of("6");
		case "Rhein-Route":
			return DrouteId.of("8");
		case "Weser - Romantische Straße":
			return DrouteId.of("9");
		default:
			log.error("Could not match D-Route ID for D-Route with name: " + name);
			return null;
		}
	}
}
