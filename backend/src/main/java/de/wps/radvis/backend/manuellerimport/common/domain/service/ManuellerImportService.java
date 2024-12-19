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

package de.wps.radvis.backend.manuellerimport.common.domain.service;

import static org.valid4j.Assertive.ensure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.springframework.context.event.EventListener;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.BatchedCollectionIterator;
import de.wps.radvis.backend.common.domain.exception.ShapeZipInvalidException;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.exception.ManuellerImportNichtMoeglichException;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ImportSessionRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.netz.domain.event.GrundnetzAktualisiertEvent;
import de.wps.radvis.backend.netz.domain.event.KantenDeletedEvent;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeEncodingException;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeProjectionException;
import de.wps.radvis.backend.shapetransformation.domain.exception.ShapeUnreadableException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManuellerImportService {

	private final ManuellerImportFehlerRepository manuellerImportFehlerRepository;
	private final ImportSessionRepository importSessionRepository;
	private ShapeZipService shapeZipService;
	private ShapeFileRepository shapeFileRepository;

	public ManuellerImportService(@NonNull ImportSessionRepository importSessionRepository,
		ShapeZipService shapeZipService, ShapeFileRepository shapeFileRepository,
		ManuellerImportFehlerRepository manuellerImportFehlerRepository) {
		this.shapeFileRepository = shapeFileRepository;
		this.shapeZipService = shapeZipService;
		this.importSessionRepository = importSessionRepository;
		this.manuellerImportFehlerRepository = manuellerImportFehlerRepository;
	}

	public <T extends AbstractImportSession> Optional<T> findImportSessionFromBenutzer(Benutzer benutzer,
		Class<T> sessionClass) {
		return importSessionRepository.find(benutzer, sessionClass);
	}

	public boolean importSessionExists(Benutzer benutzer) {
		return importSessionRepository.exists(benutzer);
	}

	public void saveImportSession(AbstractImportSession importSession) {
		log.info("ImportSession gestartet. SessionTyp/Benutzer-Id: {}/{}",
			importSession.getClass().getSimpleName(),
			importSession.getBenutzer().getId());
		importSessionRepository.save(importSession);
	}

	public File unzipAndValidateShape(byte[] zip) throws ManuellerImportNichtMoeglichException {
		File shpDirectory = null;

		try {
			shpDirectory = shapeZipService.unzip(zip);
		} catch (IOException e) {
			log.error("Die hochgeladene Zip-Datei ist fehlerhaft.", e);
			throw new ManuellerImportNichtMoeglichException("Die hochgeladene Zip-Datei ist fehlerhaft.", e);
		} catch (ShapeZipInvalidException e) {
			throw new ManuellerImportNichtMoeglichException(e);
		}

		try {
			shapeFileRepository.validate(shpDirectory);
		} catch (ShapeEncodingException | ShapeUnreadableException | ShapeProjectionException e) {
			shapeZipService.deleteUploadedFiles(shpDirectory);
			throw new ManuellerImportNichtMoeglichException(e);
		} catch (IOException e) {
			shapeZipService.deleteUploadedFiles(shpDirectory);
			throw new ManuellerImportNichtMoeglichException("Die hochgeladene Zip-Datei ist fehlerhaft.", e);
		}

		ensure(shpDirectory != null);
		return shpDirectory;
	}

	public void deleteIfExists(Benutzer benutzer) {
		if (importSessionExists(benutzer)) {
			importSessionRepository.delete(benutzer);
			log.info("ImportSession beendet. Benutzer-Id: {}", benutzer.getId());
		}
	}

	@EventListener
	public void onGrundnetzAktualisiert(GrundnetzAktualisiertEvent grundnetzAktualisiertEvent) {
		log.info("Clearing Importsessions");
		this.importSessionRepository.clear();
	}

	@EventListener
	public void onKantenDeleted(KantenDeletedEvent event) {
		log.debug("Ändere Netzbezug für Manueller Import Fehler");

		List<ManuellerImportFehler> manuelleImportFehler = new ArrayList<>();

		// Batching, da Hibernate/Postgres nur eine gewisse Anzahl an Parametern in "someId IN (...)"-Queries zulässt.
		BatchedCollectionIterator.iterate(
			event.getKantenIds(),
			1000,
			(kantenIdBatch, startIndex, endIndex) -> {
				log.debug("Verarbeite Manueller Import Fehler für Kanten-Batch {} bis {}", startIndex, endIndex);
				manuelleImportFehler.addAll(this.manuellerImportFehlerRepository.findAllByKanteId(kantenIdBatch));
			}
		);

		Map<Long, List<ManuellerImportFehler>> manuellerImportFehlerMap = manuelleImportFehler.stream()
			.collect(Collectors.groupingBy(mif -> mif.getKante().get().getId(), HashMap::new, Collectors.toCollection(
				ArrayList::new)));

		log.debug("Ändere Netzbezug für {} Manueller Import Fehler mit IDs {}", manuelleImportFehler.size(),
			manuelleImportFehler.stream().map(f -> f.getId() + "").collect(Collectors.joining(", ")));

		for (int i = 0; i < event.getKantenIds().size(); i++) {
			Long kanteId = event.getKantenIds().get(i);

			if (manuellerImportFehlerMap.containsKey(kanteId)) {
				Geometry geometry = event.getGeometries().get(i);
				manuellerImportFehlerMap.get(kanteId).forEach(importFehler -> importFehler.removeDeletedKante(
					geometry));
			}
		}
		this.manuellerImportFehlerRepository.saveAll(manuelleImportFehler);

		log.debug("Netzbezugänderung beendet");
	}
}
