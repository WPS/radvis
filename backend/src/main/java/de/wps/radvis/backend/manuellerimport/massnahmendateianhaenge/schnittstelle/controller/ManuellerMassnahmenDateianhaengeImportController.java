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
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.entity.MassnahmenDateianhaengeImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.domain.service.ManuellerMassnahmenDateianhaengeImportService;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.command.StartMassnahmenDateianhaengeImportSessionCommand;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.view.MassnahmenDateianhaengeImportSessionView;
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

		MassnahmenDateianhaengeImportSession importSession = manuellerMassnahmenDateianhaengeImportService.createSession(
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
		Optional<MassnahmenDateianhaengeImportSession> sessionOpt = manuellerMassnahmenDateianhaengeImportService.getMassnahmenDateianhaengeImportSession(
			benutzer);

		sessionOpt.ifPresent(session -> {
			session.setSchritt(MassnahmenDateianhaengeImportSession.DUPLIKATE_UEBERPRUEFEN);
			manuellerImportService.saveImportSession(session);
		});
	}
}
