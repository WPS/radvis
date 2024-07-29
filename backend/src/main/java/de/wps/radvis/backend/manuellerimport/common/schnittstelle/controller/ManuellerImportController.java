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

package de.wps.radvis.backend.manuellerimport.common.schnittstelle.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.manuellerimport.common.domain.exception.ManuellerImportNichtMoeglichException;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.schnittstelle.view.FileValidationResultView;
import lombok.NonNull;

@RestController
@RequestMapping("/api/import/common")
@Validated
public class ManuellerImportController {

	private final ManuellerImportService manuellerImportService;
	private final BenutzerResolver benutzerResolver;

	public ManuellerImportController(
		@NonNull ManuellerImportService manuellerImportService,
		@NonNull BenutzerResolver benutzerResolver) {
		this.manuellerImportService = manuellerImportService;
		this.benutzerResolver = benutzerResolver;
	}

	@GetMapping(path = "exists-session")
	public boolean existsSession(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return manuellerImportService.importSessionExists(benutzer);
	}

	@PostMapping(path = "validate-shapefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResultView validateShapefile(@RequestPart MultipartFile file) {
		try {
			manuellerImportService.unzipAndValidateShape(file.getBytes());
		} catch (ManuellerImportNichtMoeglichException e) {
			return FileValidationResultView.invalid(e.getMessage());
		} catch (IOException e) {
			return FileValidationResultView
				.invalid("Beim Prüfen des Shapefiles ist ein unerwarteter Fehler aufgetreten.");
		}

		return FileValidationResultView.valid();
	}

	@DeleteMapping(path = "delete-session")
	public void delete(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		manuellerImportService.deleteIfExists(benutzer);
	}
}
