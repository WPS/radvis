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
import java.util.List;
import java.util.Optional;

import org.springframework.context.event.EventListener;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.exception.ZipFileRequiredFilesMissingException;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.event.PreDlmReimportJobEvent;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.exception.ManuellerImportNichtMoeglichException;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ImportSessionRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.netz.domain.event.KanteDeletedEvent;
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
		ManuellerImportFehlerRepository manuellerImportFehlerRepository
	) {
		this.shapeFileRepository = shapeFileRepository;
		this.shapeZipService = shapeZipService;
		this.importSessionRepository = importSessionRepository;
		this.manuellerImportFehlerRepository = manuellerImportFehlerRepository;
	}

	public Optional<AbstractImportSession> findImportSessionFromBenutzer(Benutzer benutzer) {
		return importSessionRepository.find(benutzer);
	}

	public void saveImportSession(AbstractImportSession importSession) {
		log.info("ImportSession gestartet. SessionTyp/Benutzer-Id: {}/{}",
			importSession.getClass().getSimpleName(),
			importSession.getBenutzer().getId());
		importSessionRepository.save(importSession);
	}

	public File unzipAndValidate(byte[] zip) throws ManuellerImportNichtMoeglichException {
		File shpDirectory = null;

		try {
			shpDirectory = shapeZipService.unzip(zip);
		} catch (IOException e) {
			throw new ManuellerImportNichtMoeglichException("Die hochgeladene Zip-Datei ist fehlerhaft.", e);
		} catch (ZipFileRequiredFilesMissingException e) {
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
		Optional<AbstractImportSession> importSession = importSessionRepository.find(benutzer);
		if (importSession.isPresent()) {
			importSessionRepository.delete(benutzer);
			log.info("ImportSession beendet. SessionTyp/Benutzer-Id: {}/{}",
				importSession.get().getClass().getSimpleName(),
				importSession.get().getBenutzer().getId());
		}
	}

	@EventListener
	public void onPreDlmReimport(PreDlmReimportJobEvent preDlmReimportJobDomainEvent) {
		log.info("Clearing Importsessions");
		this.importSessionRepository.clear();
	}

	@EventListener
	public void onKanteDeleted(KanteDeletedEvent kanteDeletedEvent) {
		List<ManuellerImportFehler> manuelleImportFehler = this.manuellerImportFehlerRepository.findAllByKanteId(
			kanteDeletedEvent.getKanteId());
		manuelleImportFehler.forEach(mif -> {
			mif.removeDeletedKante(kanteDeletedEvent.getGeometry());
			this.manuellerImportFehlerRepository.save(mif);
		});
	}
}
