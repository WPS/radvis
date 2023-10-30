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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.service;

import static org.valid4j.Assertive.require;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.scheduling.annotation.Async;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.exception.GeometryTypeMismatchException;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AutomatischerImportSchritt;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportSessionStatus;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity.NetzklasseImportSession;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManuellerNetzklassenImportService {

	private final ManuellerNetzklassenImportAbbildungsService manuellerNetzklassenImportAbbildungsService;

	private final ManuellerNetzklassenImportUebernahmeService manuellerNetzklassenImportUebernahmeService;
	private final ManuellerImportService manuellerImportService;
	private final ShapeZipService shapeZipService;
	private final ShapeFileRepository shapeFileRepository;
	private final ManuellerImportFehlerRepository manuellerImportFehlerRepository;

	public ManuellerNetzklassenImportService(
		ManuellerImportService manuellerImportService,
		ManuellerNetzklassenImportAbbildungsService manuellerNetzklassenImportAbbildungsService,
		ManuellerNetzklassenImportUebernahmeService manuellerNetzklassenImportUebernahmeService,
		ShapeZipService shapeZipService,
		ShapeFileRepository shapeFileRepository, ManuellerImportFehlerRepository manuellerImportFehlerRepository) {
		this.manuellerNetzklassenImportAbbildungsService = manuellerNetzklassenImportAbbildungsService;
		this.manuellerNetzklassenImportUebernahmeService = manuellerNetzklassenImportUebernahmeService;
		this.manuellerImportService = manuellerImportService;
		this.shapeFileRepository = shapeFileRepository;
		this.manuellerImportFehlerRepository = manuellerImportFehlerRepository;
		this.shapeZipService = shapeZipService;
	}

	public NetzklasseImportSession getNetzklassenImportSession(Benutzer benutzer) {
		Optional<AbstractImportSession> importSession = manuellerImportService.findImportSessionFromBenutzer(benutzer);

		if (importSession.isEmpty() || !(importSession.get() instanceof NetzklasseImportSession)) {
			throw new RuntimeException("Keine NetzklassenImportSession für den Benutzer verfügbar");
		}

		return (NetzklasseImportSession) importSession.get();
	}

	@Async
	@WithAuditing(context = AuditingContext.MANUELLER_NETZKLASSEN_IMPORT)
	@Transactional
	public void runUpdate(NetzklasseImportSession netzklasseImportSession) {
		require(netzklasseImportSession.getStatus() == ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
			"Eine Session darf nur nach der automatischen Abbildung und nur einmal ausgeführt werden");
		netzklasseImportSession.setStatus(ImportSessionStatus.UPDATE_EXECUTING);
		LocalDateTime importZeitpunkt = LocalDateTime.now();
		try {
			log.info("Starting uebernehmeNetzzugehoerigkeit");
			manuellerNetzklassenImportUebernahmeService.uebernehmeNetzzugehoerigkeit(netzklasseImportSession);
			List<ManuellerImportFehler> collect = netzklasseImportSession.getNichtGematchteFeatureLineStrings().stream()
				.map(
					ls -> new ManuellerImportFehler(
						ls,
						ImportTyp.NETZKLASSE_ZUWEISEN,
						importZeitpunkt,
						netzklasseImportSession.getBenutzer(),
						netzklasseImportSession.getOrganisation()))
				.collect(Collectors.toList());

			manuellerImportFehlerRepository.saveAll(collect);
			netzklasseImportSession.setStatus(ImportSessionStatus.UPDATE_DONE);
		} catch (OptimisticLockException e) {
			netzklasseImportSession.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
			throw new OptimisticLockException(
				"Kanten in der Organisation wurden während des Speicherns aus einer anderen Quelle verändert."
					+ " Bitte versuchen Sie es erneut.");
		} catch (Throwable e) {
			netzklasseImportSession.addLogEintrag(
				ImportLogEintrag.ofError("Es ist ein Unbekannter Fehler aufgetreten"));
			netzklasseImportSession.setStatus(ImportSessionStatus.UPDATE_DONE);
			throw e;
		} finally {
			log.info("runUpdate done");
		}
	}

	@Async
	public void runAutomatischeAbbildung(NetzklasseImportSession session, File shpDirectory) {
		session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_RUNNING);

		try (Stream<SimpleFeature> features = shapeFileRepository
			.readShape(shapeZipService.getShapeFileFromDirectory(shpDirectory)
				.orElseThrow(
					() -> new RuntimeException("Directory " + shpDirectory.getName() + " enthält keine .shp Datei")))) {

			Set<LineString> abzubildendeLineStrings = new HashSet<>();

			AtomicInteger numberOfNullGeometries = new AtomicInteger();
			Set<SimpleFeature> featuresInBereich = features
				.filter(simpleFeature -> {
					if (simpleFeature.getDefaultGeometry() == null) {
						numberOfNullGeometries.getAndIncrement();
						return false;
					} else {
						return true;
					}
				})
				.filter(feature -> session.getOrganisation().getBereich()
					.map(geo -> geo.intersects(CoordinateReferenceSystemConverterUtility.transformGeometry(
						(Geometry) feature.getDefaultGeometry(),
						KoordinatenReferenzSystem.ETRS89_UTM32_N)))
					.orElse(false)
				)
				.collect(Collectors.toSet());

			if (numberOfNullGeometries.get() > 0) {
				session.addLogEintrag(
					ImportLogEintrag.ofWarnung(
						"Shapefile enthält " + numberOfNullGeometries.get()
							+ " Features ohne Geometrien, welche aus diesem Grund nicht importiert werden konnten."));
			}

			if (featuresInBereich.isEmpty()) {
				session.addLogEintrag(
					ImportLogEintrag.ofWarnung(
						"Shapefile enthält keine Features im Bereich der Organisation " + session.getOrganisation()
							.getName() + ". Bei Fortführung des Imports wird die Netzklasse \""
							+ session.getNetzklasse() + "\" in diesem Bereich gelöscht!"));
			}

			featuresInBereich
				.forEach(f -> {
					try {
						abzubildendeLineStrings.add(manuellerNetzklassenImportAbbildungsService.extractLinestring(f));
					} catch (GeometryTypeMismatchException e) {
						session.addLogEintrag(
							ImportLogEintrag
								.ofWarnung("Feature " + f.getID() + " wird nicht importiert: " + e.getMessage()));
					}
				});

			this.ermittleKanteIdAbbildungFuerLineStrings(session, abzubildendeLineStrings);
		} catch (ShapeProjectionException e) {
			session.addLogEintrag(ImportLogEintrag.ofError(e.getMessage()));
			log.error("Fehler bei ManuellerNetzklasseImport", e);
		} catch (Throwable e) {
			session.addLogEintrag(ImportLogEintrag.ofError("Es ist ein unerwarteter Fehler aufgetreten."));
			log.error("Fehler bei ManuellerNetzklasseImport", e);
		} finally {
			if (shpDirectory != null) {
				shapeZipService.deleteUploadedFiles(shpDirectory);
			}
			session.setStatus(ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE);
		}
	}

	private void ermittleKanteIdAbbildungFuerLineStrings(NetzklasseImportSession netzklasseImportSession,
		Set<LineString> lineStrings) {
		Verwaltungseinheit organisation = netzklasseImportSession.getOrganisation();

		log.info("{} LineStrings aus der Shapefile befinden sich im Bereich der gewählten Organisation ({})",
			lineStrings.size(), organisation.getName());

		netzklasseImportSession
			.setAktuellerImportSchritt(AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ);

		// Abbildung auf das RadVis-Netz
		ManuellerNetzklassenImportAbbildungsService.MatchingErgebnis matchingErgebnis = manuellerNetzklassenImportAbbildungsService
			.findKantenFromLineStrings(
				lineStrings, netzklasseImportSession.getOrganisation());
		Set<Long> kanteIds = matchingErgebnis.matchedKanten;
		log.info("LineStrings auf {} Kanten abgebildet", kanteIds.size());
		netzklasseImportSession.getKanteIds().addAll(kanteIds);
		Set<LineString> nichtGematchteLineStrings = matchingErgebnis.nichtGematchteLineStrings;
		netzklasseImportSession.addNichtGematchteFeatureLineStrings(nichtGematchteLineStrings);

		netzklasseImportSession.setAktuellerImportSchritt(
			AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN);
	}

}
