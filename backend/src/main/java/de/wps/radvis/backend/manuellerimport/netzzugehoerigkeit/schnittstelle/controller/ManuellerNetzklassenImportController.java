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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.schnittstelle.controller;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.geojson.FeatureCollection;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.manuellerimport.common.domain.exception.ManuellerImportNichtMoeglichException;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity.NetzklasseImportSession;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.service.ManuellerNetzklassenImportService;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.schnittstelle.KnotenToGeoJsonConverter;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.schnittstelle.command.StartNetzklassenImportSessionCommand;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.schnittstelle.view.NetzklassenImportSessionView;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.service.SackgassenService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.NonNull;

@RestController
@RequestMapping("/api/import/netzklassen/")
@Validated
public class ManuellerNetzklassenImportController {

	private final ManuellerImportService manuellerImportService;
	private final ManuellerNetzklassenImportService manuellerNetzklassenImportService;
	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final SackgassenService sackgassenService;
	private final KnotenToGeoJsonConverter knotenToGeoJsonConverter;
	private final ManuellerNetzklassenImportGuard manuellerNetzklassenImportGuard;

	public ManuellerNetzklassenImportController(
		@NonNull ManuellerImportService manuellerImportService,
		@NonNull ManuellerNetzklassenImportService manuellerNetzklassenImportService,
		@NonNull BenutzerResolver benutzerResolver,
		@NonNull VerwaltungseinheitResolver verwaltungseinheitResolver,
		@NonNull SackgassenService sackgassenService,
		@NonNull KnotenToGeoJsonConverter knotenToGeoJsonConverter,
		@NonNull ManuellerNetzklassenImportGuard manuellerNetzklassenImportGuard) {
		this.manuellerImportService = manuellerImportService;
		this.manuellerNetzklassenImportService = manuellerNetzklassenImportService;
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
		this.sackgassenService = sackgassenService;
		this.knotenToGeoJsonConverter = knotenToGeoJsonConverter;
		this.manuellerNetzklassenImportGuard = manuellerNetzklassenImportGuard;
	}

	@GetMapping(path = "session")
	public NetzklassenImportSessionView getImportSession(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		return manuellerNetzklassenImportService.getNetzklassenImportSession(benutzer)
			.map(NetzklassenImportSessionView::new)
			.orElse(null);
	}

	@PostMapping(path = "start-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public void startNetzklassenImportSession(Authentication authentication,
		@RequestPart StartNetzklassenImportSessionCommand command, @RequestPart MultipartFile file)
		throws ManuellerImportNichtMoeglichException, IOException {
		manuellerNetzklassenImportGuard.startNetzklassenImportSession(authentication, command, file);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		if (manuellerImportService.importSessionExists(benutzer)) {
			throw new RuntimeException("Es existiert bereits eine Session");
		}
		Verwaltungseinheit organisation = verwaltungseinheitResolver.resolve(command.getOrganisation());

		File shpDirectory = manuellerImportService
			.unzipAndValidateShape(file.getBytes());

		NetzklasseImportSession importSession = new NetzklasseImportSession(benutzer,
			organisation,
			command.getNetzklasse());

		manuellerImportService.saveImportSession(importSession);

		// async verarbeitung anstoßen
		this.manuellerNetzklassenImportService.runAutomatischeAbbildung(importSession, shpDirectory);
	}

	@PostMapping(path = "bearbeitung-abschliessen")
	public void bearbeitungAbschliessen(Authentication authentication) {
		manuellerNetzklassenImportGuard.bearbeitungAbschliessen(authentication);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		NetzklasseImportSession session = this.manuellerNetzklassenImportService.getNetzklassenImportSession(benutzer)
			.get();
		this.manuellerNetzklassenImportService.bearbeitungAbschliessen(session);
	}

	@GetMapping(path = "execute-zuweisen")
	public void executeNetzklassenZuweisen(Authentication authentication) {
		manuellerNetzklassenImportGuard.executeNetzklassenZuweisen(authentication);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		NetzklasseImportSession session = manuellerNetzklassenImportService.getNetzklassenImportSession(benutzer).get();
		manuellerNetzklassenImportService.runUpdate(session);
	}

	@GetMapping(path = "sackgassen")
	public FeatureCollection getSackgassen(Authentication authentication) {
		manuellerNetzklassenImportGuard.getSackgassen(authentication);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		NetzklasseImportSession session = manuellerNetzklassenImportService.getNetzklassenImportSession(benutzer).get();

		Set<Knoten> knoten = sackgassenService.bestimmeSackgassenknotenVonKanteIdsInOrganisation(
			session.getKanteIds(),
			session.getOrganisation());

		return knotenToGeoJsonConverter.convertKnoten(knoten);
	}

	@PostMapping(path = "toggle-zugehoerigkeit/{kanteId}")
	public Set<Long> toggleNetzklassenzugehoerigkeit(
		Authentication authentication, @PathVariable("kanteId") Long kanteId) {
		manuellerNetzklassenImportGuard.toggleNetzklassenzugehoerigkeit(authentication, kanteId);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		NetzklasseImportSession session = manuellerNetzklassenImportService.getNetzklassenImportSession(benutzer).get();
		session.toggleNetzklassenzugehoerigkeit(kanteId);

		return session.getKanteIds();
	}

	@GetMapping(path = "kante-ids")
	public Set<Long> getKanteIdsMitNetzklasse(Authentication authentication) {
		manuellerNetzklassenImportGuard.getKanteIdsMitNetzklasse(authentication);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		NetzklasseImportSession session = manuellerNetzklassenImportService.getNetzklassenImportSession(benutzer).get();

		return session.getKanteIds();
	}

}
