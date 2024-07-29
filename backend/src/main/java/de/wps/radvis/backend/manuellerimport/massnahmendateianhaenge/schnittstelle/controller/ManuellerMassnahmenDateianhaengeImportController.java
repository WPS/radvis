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

package de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.exception.CsvExportException;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.service.ManuellerMassnahmenDateianhaengeImportService;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.command.SaveMassnahmenDateianhaengeCommand;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.command.StartMassnahmenDateianhaengeImportSessionCommand;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.converter.SaveMassnahmenDateianhaengeCommandConverter;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.view.MassnahmenDateianhaengeImportProtokollStatsView;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.view.MassnahmenDateianhaengeImportSessionView;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/import/massnahmen-dateianhaenge")
@Validated
@AllArgsConstructor
public class ManuellerMassnahmenDateianhaengeImportController {

	private final BenutzerResolver benutzerResolver;
	private final ManuellerMassnahmenDateianhaengeImportGuard manuellerMassnahmenDateianhaengeImportGuard;
	private final ManuellerMassnahmenDateianhaengeImportService manuellerMassnahmenDateianhaengeImportService;
	private final ManuellerImportService manuellerImportService;
	private final SaveMassnahmenDateianhaengeCommandConverter saveMassnahmenDateianhaengeCommandConverter;

	@GetMapping(path = "session")
	public MassnahmenDateianhaengeImportSessionView getImportSession(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		return manuellerMassnahmenDateianhaengeImportService.getMassnahmenDateianhaengeImportSession(benutzer)
			.map(MassnahmenDateianhaengeImportSessionView::new)
			.orElse(null);
	}

	@PostMapping(path = "start-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public void startMassnahmenDateianhaengeImportSession(Authentication authentication,
		@RequestPart StartMassnahmenDateianhaengeImportSessionCommand command, @RequestPart MultipartFile file) {
		manuellerMassnahmenDateianhaengeImportGuard.startMassnahmenDateianhaengeImportSession(authentication, command,
			file);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		if (manuellerImportService.importSessionExists(benutzer)) {
			throw new RuntimeException("Es existiert bereits eine Session");
		}

		MassnahmenDateianhaengeImportSession importSession = manuellerMassnahmenDateianhaengeImportService
			.createSession(
				benutzer, command.gebietskoerperschaften(), command.konzeptionsquelle(), command.sollStandard());

		File temporaryZipCopy = this.manuellerMassnahmenDateianhaengeImportService.createTemporaryZipCopy(importSession,
			file);
		this.manuellerMassnahmenDateianhaengeImportService.ladeDateien(importSession, temporaryZipCopy);

		manuellerImportService.saveImportSession(importSession);
	}

	@PostMapping("continue-checked-errors")
	public void continueAfterFehlerUeberpruefen(Authentication authentication) {
		manuellerMassnahmenDateianhaengeImportGuard.continueAfterFehlerUeberpruefen(authentication);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		Optional<MassnahmenDateianhaengeImportSession> sessionOpt = manuellerMassnahmenDateianhaengeImportService
			.getMassnahmenDateianhaengeImportSession(
				benutzer);

		sessionOpt.ifPresent(session -> {
			session.setSchritt(MassnahmenDateianhaengeImportSession.DUPLIKATE_UEBERPRUEFEN);
			manuellerImportService.saveImportSession(session);
		});
	}

	@PostMapping("save-selected-dateianhaenge")
	public void saveSelectedDateianhaengeToMassnahme(
		Authentication authentication,
		@RequestBody List<SaveMassnahmenDateianhaengeCommand> commands) {
		manuellerMassnahmenDateianhaengeImportGuard.saveSelectedDateianhaengeToMassnahme(authentication, commands);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		Optional<MassnahmenDateianhaengeImportSession> sessionOpt = manuellerMassnahmenDateianhaengeImportService
			.getMassnahmenDateianhaengeImportSession(
				benutzer);

		sessionOpt.ifPresent(session -> {
			session.setExecuting(true);
			saveMassnahmenDateianhaengeCommandConverter.applyCommandsToSession(session, commands);
			manuellerMassnahmenDateianhaengeImportService.saveSelectedDateianhaengeToMassnahme(session, benutzer);
			manuellerImportService.saveImportSession(session);
		});
	}

	@GetMapping("protokoll-stats")
	public MassnahmenDateianhaengeImportProtokollStatsView getProtokollStats(Authentication authentication) {
		manuellerMassnahmenDateianhaengeImportGuard.getProtokollStats(authentication);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return manuellerMassnahmenDateianhaengeImportService.getMassnahmenDateianhaengeImportSession(benutzer)
			.map(MassnahmenDateianhaengeImportProtokollStatsView::of)
			.orElse(null);
	}

	@GetMapping("download-fehlerprotokoll")
	public void downloadFehlerprotokoll(Authentication authentication, HttpServletResponse response)
		throws CsvExportException {
		manuellerMassnahmenDateianhaengeImportGuard.downloadFehlerprotokoll(authentication);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		MassnahmenDateianhaengeImportSession session = manuellerMassnahmenDateianhaengeImportService
			.getMassnahmenDateianhaengeImportSession(benutzer)
			.orElseThrow(() -> new RuntimeException("Es existiert keine Session."));

		try {
			byte[] protokoll = manuellerMassnahmenDateianhaengeImportService.downloadFehlerprotokoll(session);

			response.setContentType("text/csv;charset=utf-8");
			response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=import_protokoll.csv");
			response.getOutputStream().write(protokoll);
			response.flushBuffer();

		} catch (IOException e) {
			throw new CsvExportException("Protokoll kann aufgrund eines Fehlers nicht exportiert werden.");
		}
	}
}
