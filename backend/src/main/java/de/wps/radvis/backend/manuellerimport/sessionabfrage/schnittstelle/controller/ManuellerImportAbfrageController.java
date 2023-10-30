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

package de.wps.radvis.backend.manuellerimport.sessionabfrage.schnittstelle.controller;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.AbstractImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity.NetzklasseImportSession;
import de.wps.radvis.backend.manuellerimport.sessionabfrage.schnittstelle.view.AbstractImportSessionView;
import de.wps.radvis.backend.manuellerimport.sessionabfrage.schnittstelle.view.AttributeImportSessionView;
import de.wps.radvis.backend.manuellerimport.sessionabfrage.schnittstelle.view.NetzklassenImportSessionView;
import lombok.NonNull;

@RestController
@RequestMapping("/api/import/abfrage")
@Validated
public class ManuellerImportAbfrageController {

	private final ManuellerImportService manuellerImportService;
	private final BenutzerResolver benutzerResolver;

	public ManuellerImportAbfrageController(
		@NonNull ManuellerImportService manuellerImportService,
		@NonNull BenutzerResolver benutzerResolver) {
		this.manuellerImportService = manuellerImportService;
		this.benutzerResolver = benutzerResolver;
	}

	@GetMapping(path = "session")
	public AbstractImportSessionView getImportSession(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		AbstractImportSession abstractImportSession = manuellerImportService.findImportSessionFromBenutzer(benutzer)
			.orElseThrow(
				() -> new RuntimeException("Keine Statusabfrage möglich: Es existiert keine Importsession"));

		return abstractImportSession instanceof NetzklasseImportSession
			? new NetzklassenImportSessionView((NetzklasseImportSession) abstractImportSession)
			: new AttributeImportSessionView((AttributeImportSession) abstractImportSession);
	}

	@GetMapping(path = "exists-session")
	public boolean existsSession(Authentication authentication, @RequestParam(required = false) ImportTyp importTyp) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		final Optional<AbstractImportSession> importSession = manuellerImportService
			.findImportSessionFromBenutzer(benutzer);
		if (importSession.isEmpty()) {
			return false;
		}
		if (importTyp == null) {
			return true;
		}
		return (importTyp == ImportTyp.NETZKLASSE_ZUWEISEN && importSession.get() instanceof NetzklasseImportSession)
			|| (importTyp == ImportTyp.ATTRIBUTE_UEBERNEHMEN && importSession.get() instanceof AttributeImportSession);
	}
}
