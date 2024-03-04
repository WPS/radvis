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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
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
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service.ManuellerMassnahmenImportService;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.MassnahmenImportAttributeAuswaehlenCommand;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.StartMassnahmenImportSessionCommand;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.view.MassnahmenImportSessionView;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.view.MassnahmenImportZuordnungView;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/import/massnahmen/")
@Validated
public class ManuellerMassnahmenImportController {

	private final ManuellerMassnahmenImportGuard manuellerMassnahmenImportGuard;
	private final BenutzerResolver benutzerResolver;
	private final ManuellerMassnahmenImportService manuellerMassnahmenImportService;
	private final ManuellerImportService manuellerImportService;

	public ManuellerMassnahmenImportController(
		@NonNull BenutzerResolver benutzerResolver,
		@NonNull ManuellerMassnahmenImportGuard manuellerMassnahmenImportGuard,
		@NonNull ManuellerMassnahmenImportService manuellerMassnahmenImportService,
		@NonNull ManuellerImportService manuellerImportService, GeoJsonImportRepository geoJsonImportRepository) {
		this.manuellerMassnahmenImportGuard = manuellerMassnahmenImportGuard;
		this.benutzerResolver = benutzerResolver;
		this.manuellerMassnahmenImportService = manuellerMassnahmenImportService;
		this.manuellerImportService = manuellerImportService;
	}

	@GetMapping(path = "session")
	public MassnahmenImportSessionView getImportSession(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		return manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.map(MassnahmenImportSessionView::new)
			.orElse(null);
	}

	@GetMapping(path = "session/zuordnungen")
	public List<MassnahmenImportZuordnungView> getZuordnungen(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		return manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.map(session -> session.getZuordnungen().stream().map(MassnahmenImportZuordnungView::new).toList())
			.orElse(null);
	}

	@PostMapping(path = "start-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public void startMassnahmenImportSession(Authentication authentication,
		@RequestPart StartMassnahmenImportSessionCommand command, @RequestPart MultipartFile file) throws IOException {
		manuellerMassnahmenImportGuard.startMassnahmenImportSession(authentication, command, file);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		if (manuellerImportService.importSessionExists(benutzer)) {
			throw new RuntimeException("Es existiert bereits eine Session");
		}

		MassnahmenImportSession importSession = manuellerMassnahmenImportService.createSession(benutzer,
			command.getGebietskoerperschaften(), command.getKonzeptionsquelle(), command.getSollStandard());

		this.manuellerMassnahmenImportService.ladeFeatures(importSession, file.getBytes());

		manuellerImportService.saveImportSession(importSession);
	}

	@PostMapping("attribute-auswaehlen")
	public void attributeAuswaehlen(Authentication authentication,
		@RequestBody @Valid MassnahmenImportAttributeAuswaehlenCommand command) {
		manuellerMassnahmenImportGuard.attributeAuswaehlen(authentication, command);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		MassnahmenImportSession session = this.manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.orElseThrow(() -> new RuntimeException("Es existiert keine Session."));

		this.manuellerMassnahmenImportService.attributeValidieren(session, command.attribute());
	}
}
