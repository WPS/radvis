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
import java.util.Optional;

import org.geojson.GeoJsonObject;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.HttpHeaders;
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
import de.wps.radvis.backend.common.domain.exception.CsvExportException;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportSession;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service.ManuellerMassnahmenImportService;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.MassnahmenImportNetzbezugAktualisierenCommandConverter;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.MassnahmenImportAttributeAuswaehlenCommand;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.MassnahmenImportMassnahmenAuswaehlenCommand;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.MassnahmenImportNetzbezugAktualisierenCommand;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.StartMassnahmenImportSessionCommand;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.view.MassnahmenImportProtokollStatsView;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.view.MassnahmenImportSessionView;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.view.MassnahmenImportZuordnungAttributfehlerView;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.view.MassnahmenImportZuordnungUeberpruefungView;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/import/massnahmen/")
@Validated
public class ManuellerMassnahmenImportController {

	private final ManuellerMassnahmenImportGuard manuellerMassnahmenImportGuard;
	private final BenutzerResolver benutzerResolver;
	private final ManuellerMassnahmenImportService manuellerMassnahmenImportService;
	private final ManuellerImportService manuellerImportService;
	private final MassnahmenImportNetzbezugAktualisierenCommandConverter massnahmenImportNetzbezugAktualisierenCommandConverter;

	public ManuellerMassnahmenImportController(
		@NonNull BenutzerResolver benutzerResolver,
		@NonNull ManuellerMassnahmenImportGuard manuellerMassnahmenImportGuard,
		@NonNull ManuellerMassnahmenImportService manuellerMassnahmenImportService,
		@NonNull ManuellerImportService manuellerImportService,
		@NonNull MassnahmenImportNetzbezugAktualisierenCommandConverter massnahmenImportNetzbezugAktualisierenCommandConverter) {
		this.manuellerMassnahmenImportGuard = manuellerMassnahmenImportGuard;
		this.benutzerResolver = benutzerResolver;
		this.manuellerMassnahmenImportService = manuellerMassnahmenImportService;
		this.manuellerImportService = manuellerImportService;
		this.massnahmenImportNetzbezugAktualisierenCommandConverter = massnahmenImportNetzbezugAktualisierenCommandConverter;
	}

	@GetMapping(path = "session")
	public MassnahmenImportSessionView getImportSession(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		return manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.map(MassnahmenImportSessionView::new)
			.orElse(null);
	}

	@GetMapping(path = "session/zuordnungen-ueberpruefung")
	public List<MassnahmenImportZuordnungUeberpruefungView> getZuordnungenUeberpruefungView(
		Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		return manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.map(session -> session.getZuordnungen().stream()
				.filter(massnahmenImportZuordnung -> massnahmenImportZuordnung.getMappingFehler().isEmpty())
				.map(zuordnung -> {
					Geometry originalGeometrie = (Geometry) zuordnung.getFeature().getDefaultGeometry();
					Optional<Geometry> netzbezugGeometrie = zuordnung.getNetzbezugGeometrie();

					// Muss mit unserem Converter zu GeoJSON konvertiert werden,
					// da es sich um eine GeometryCollection handeln kann,
					// die mehrere Geometrie-Typen gleichzeitig enthalten kann.
					GeoJsonObject originalGeometrieGeoJSON = GeoJsonConverter.createGeoJsonGeometry(originalGeometrie);
					GeoJsonObject netzbezugGeometrieGeoJSON = netzbezugGeometrie
						.map(GeoJsonConverter::createGeoJsonGeometry)
						.orElse(null);

					return new MassnahmenImportZuordnungUeberpruefungView(zuordnung, originalGeometrieGeoJSON,
						netzbezugGeometrieGeoJSON);
				})
				.toList())
			.orElse(null);
	}

	@GetMapping(path = "session/zuordnungen-attributfehler")
	public List<MassnahmenImportZuordnungAttributfehlerView> getZuordnungenAttributfehler(
		Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		return manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.map(session -> session.getZuordnungen().stream()
				.map(MassnahmenImportZuordnungAttributfehlerView::new).toList())
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

		manuellerMassnahmenImportService.ladeFeatures(importSession, file.getBytes());

		manuellerImportService.saveImportSession(importSession);
	}

	@PostMapping("attribute-auswaehlen")
	public void attributeAuswaehlen(Authentication authentication,
		@RequestBody @Valid MassnahmenImportAttributeAuswaehlenCommand command) {
		manuellerMassnahmenImportGuard.attributeAuswaehlen(authentication, command);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		MassnahmenImportSession session = manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.orElseThrow(() -> new RuntimeException("Es existiert keine Session."));

		manuellerMassnahmenImportService.attributeValidieren(session, command.attribute());
	}

	@PostMapping("netzbezuege-erstellen")
	public void netzbezuegeErstellen(Authentication authentication) {
		manuellerMassnahmenImportGuard.netzbezuegeErstellen(authentication);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		MassnahmenImportSession session = manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.orElseThrow(() -> new RuntimeException("Es existiert keine Session."));

		manuellerMassnahmenImportService.erstelleNetzbezuege(session);
	}

	@PostMapping("netzbezug-aktualisieren")
	public void netzbezugAktualisieren(Authentication authentication,
		@RequestBody @Valid MassnahmenImportNetzbezugAktualisierenCommand command) {
		manuellerMassnahmenImportGuard.netzbezugAktualisieren(authentication, command);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		MassnahmenImportSession session = manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.orElseThrow(() -> new RuntimeException("Es existiert keine Session."));

		Optional<MassnahmeNetzBezug> netzbezug = massnahmenImportNetzbezugAktualisierenCommandConverter.convert(
			command);
		manuellerMassnahmenImportService.aktualisiereNetzbezug(session, command.getMassnahmenImportZuordnungId(),
			netzbezug.orElse(null));
	}

	@PostMapping("save-massnahmen")
	public void saveMassnahmen(Authentication authentication,
		@RequestBody MassnahmenImportMassnahmenAuswaehlenCommand command) {
		manuellerMassnahmenImportGuard.saveMassnahmen(authentication, command);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		MassnahmenImportSession session = manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.orElseThrow(() -> new RuntimeException("Es existiert keine Session."));

		manuellerMassnahmenImportService.speichereMassnahmenDerZuordnungen(session, command.zuordnungenIds());
	}

	@GetMapping("protokoll-stats")
	public MassnahmenImportProtokollStatsView getProtokollStats(Authentication authentication) {
		manuellerMassnahmenImportGuard.getProtokollStats(authentication);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		return manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.map(MassnahmenImportProtokollStatsView::of)
			.orElse(null);
	}

	@GetMapping("download-fehlerprotokoll")
	public void downloadFehlerprotokoll(Authentication authentication, HttpServletResponse response)
		throws CsvExportException {
		manuellerMassnahmenImportGuard.downloadFehlerprotokoll(authentication);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		MassnahmenImportSession session = manuellerMassnahmenImportService.getMassnahmenImportSession(benutzer)
			.orElseThrow(() -> new RuntimeException("Es existiert keine Session."));

		try {
			byte[] protokoll = manuellerMassnahmenImportService.downloadFehlerprotokoll(session);

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
