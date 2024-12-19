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

package de.wps.radvis.backend.abfrage.serviceManagementBericht.schnittstelle;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.abfrage.serviceManagementBericht.domain.ServiceManagementBerichtService;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("serviceManagementBericht")
public class ServiceManagementBerichtController {

	private final BenutzerResolver benutzerResolver;

	private final ServiceManagementBerichtGuard serviceManagementBerichtGuard;

	private final ServiceManagementBerichtService serviceManagementBerichtService;

	@GetMapping(value = "fachlicheStatistiken")
	public ResponseEntity<Map<String, String>> getFachlicheStatistiken(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		serviceManagementBerichtGuard.getFachlicheStatistiken(benutzer);

		Map<String, String> jsonMap = serviceManagementBerichtService.getFachlicheStatistiken();
		return ResponseEntity.ok(jsonMap);
	}

	@GetMapping(value = "jobUebersicht")
	public ResponseEntity<byte[]> getJobUebersicht(Authentication authentication,
		@RequestParam("startDate") LocalDate startDate,
		@RequestParam("endDate") LocalDate endDate,
		@RequestParam("blacklist") List<String> blacklist
	) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		serviceManagementBerichtGuard.getJobUebersicht(benutzer);

		HttpHeaders headers = new HttpHeaders();
		String dateiname = "job_uebersicht_" + startDate + "_bis_" + endDate + ".csv";
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + dateiname);
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		byte[] datei = serviceManagementBerichtService.getJobUebersicht(startDate, endDate, blacklist);

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
			.contentLength(datei.length)
			.headers(headers)
			.body(datei);
	}
}
