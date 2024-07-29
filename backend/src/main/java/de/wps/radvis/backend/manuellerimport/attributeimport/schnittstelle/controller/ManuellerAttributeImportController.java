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

package de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.LineString;
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
import de.wps.radvis.backend.common.domain.exception.ShapeZipInvalidException;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportSession;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.service.ManuellerAttributeImportService;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.ImportierbaresAttribut;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.FeatureMappingToGeoJsonConverter;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.KonfliktToGeoJsonConverter;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.command.DeleteMappedGrundnetzkanteCommand;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.command.StartAttributeImportSessionCommand;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.command.UpdateFeatureMappingCommand;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.command.ValidateAttributeCommand;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.view.AttributeImportSessionView;
import de.wps.radvis.backend.manuellerimport.common.domain.exception.ManuellerImportNichtMoeglichException;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/import/attribute/")
@Validated
public class ManuellerAttributeImportController {
	private final ManuellerImportService manuellerImportService;
	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final ManuellerAttributeImportService manuellerAttributeImportService;
	private final FeatureMappingToGeoJsonConverter featureMappingToGeoJsonConverter;
	private final KonfliktToGeoJsonConverter konfliktToGeoJsonConverter;
	private final ManuellerAttributeImportGuard manuellerAttributeImportGuard;

	public ManuellerAttributeImportController(
		@NonNull ManuellerImportService manuellerImportService,
		@NonNull BenutzerResolver benutzerResolver,
		@NonNull VerwaltungseinheitResolver verwaltungseinheitResolver,
		@NonNull ManuellerAttributeImportService manuellerAttributeImportService,
		@NonNull FeatureMappingToGeoJsonConverter featureMappingToGeoJsonConverter,
		@NonNull KonfliktToGeoJsonConverter konfliktToGeoJsonConverter,
		@NonNull ManuellerAttributeImportGuard manuellerAttributeImportGuard) {
		this.manuellerImportService = manuellerImportService;
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
		this.manuellerAttributeImportService = manuellerAttributeImportService;
		this.featureMappingToGeoJsonConverter = featureMappingToGeoJsonConverter;
		this.konfliktToGeoJsonConverter = konfliktToGeoJsonConverter;
		this.manuellerAttributeImportGuard = manuellerAttributeImportGuard;
	}

	@GetMapping(path = "session")
	public AttributeImportSessionView getImportSession(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		return manuellerAttributeImportService.getAttributeImportSession(benutzer)
			.map(AttributeImportSessionView::new)
			.orElse(null);
	}

	@PostMapping(path = "start-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public void startAttributeImportSession(Authentication authentication,
		@RequestPart StartAttributeImportSessionCommand command, @RequestPart MultipartFile file)
		throws ManuellerImportNichtMoeglichException, IOException {
		manuellerAttributeImportGuard.startAttributeImportSession(authentication, command, file);

		assertKorrekteAttribute(command.getAttribute());

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		if (manuellerImportService.importSessionExists(benutzer)) {
			throw new RuntimeException("Es existiert bereits eine Session");
		}

		Verwaltungseinheit organisation = verwaltungseinheitResolver.resolve(command.getOrganisation());

		File shpDirectory = manuellerImportService.unzipAndValidateShape(file.getBytes());

		AttributeImportSession importSession = new AttributeImportSession(benutzer, organisation,
			command.getAttribute(), command.getAttributeImportFormat());

		manuellerImportService.saveImportSession(importSession);

		// async verarbeitung anstoßen
		this.manuellerAttributeImportService.runAutomatischeAbbildung(importSession, shpDirectory);
	}

	/**
	 * Da Trennstreifen und Radverkehrsführung eng zusammen hängen, müssen diese immer zusammen übernommen werden.
	 */
	private void assertKorrekteAttribute(List<String> attribute) {
		List<String> trennstreifenAttribute = List.of("sts_f_l", "sts_t_l", "sts_b_l", "sts_f_r", "sts_t_r",
			"sts_b_r");

		boolean trennstreifenUebernehmen = attribute.stream()
			.anyMatch(attribut -> trennstreifenAttribute.contains(attribut.toLowerCase()));

		boolean fuehrungsformUebernehmen = attribute.stream()
			.anyMatch(attribut -> attribut.equalsIgnoreCase("radverkehr"));

		if (trennstreifenUebernehmen && !attribute.containsAll(trennstreifenAttribute)) {
			throw new RuntimeException("Es können nur alle Trennstreifen-Attribute auf einmal übernommen werden.");
		}

		if (fuehrungsformUebernehmen && !trennstreifenUebernehmen) {
			throw new RuntimeException(
				"Bei zu übernehmender Radverkehrsführung müssen auch die Trennstreifen-Attribute übernommen werden");
		}

		if (!fuehrungsformUebernehmen && trennstreifenUebernehmen) {
			throw new RuntimeException(
				"Bei zu übernehmenden Trennstreifen-Attributen muss auch die Radverkehrsführung übernommen werden");
		}
	}

	@PostMapping(path = "validate-attribute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public List<ImportierbaresAttribut> validateAttribute(Authentication authentication,
		@Valid @RequestPart ValidateAttributeCommand command, @RequestPart MultipartFile file)
		throws ManuellerImportNichtMoeglichException {
		manuellerAttributeImportGuard.validateAttribute(authentication, command, file);
		try {
			return manuellerAttributeImportService.validateAttribute(
				file.getBytes(), command.getAttributeImportFormat());
		} catch (IOException e) {
			log.error("I/O-Exception beim Validieren der Attribute", e);
			throw new ManuellerImportNichtMoeglichException("Die hochgeladene Zip-Datei ist fehlerhaft.", e);
		} catch (ShapeZipInvalidException e) {
			throw new ManuellerImportNichtMoeglichException(e);
		}
	}

	@PostMapping(path = "execute-uebernehmen")
	public void executeAttributeUebernehmen(Authentication authentication) {
		manuellerAttributeImportGuard.executeAttributeUebernehmen(authentication);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		AttributeImportSession session = this.manuellerAttributeImportService.getAttributeImportSession(benutzer).get();
		// async verarbeitung anstoßen
		this.manuellerAttributeImportService.runUpdate(session);
	}

	@PostMapping(path = "bearbeitung-abschliessen")
	public void bearbeitungAbschliessen(Authentication authentication) {
		manuellerAttributeImportGuard.bearbeitungAbschliessen(authentication);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		AttributeImportSession session = this.manuellerAttributeImportService.getAttributeImportSession(benutzer).get();
		this.manuellerAttributeImportService.bearbeitungAbschliessen(session);
	}

	@GetMapping(path = "feature-mappings")
	public FeatureCollection getFeatureMappings(Authentication authentication) {
		manuellerAttributeImportGuard.getFeatureMappings(authentication);
		return featureMappingToGeoJsonConverter.convert(
			this.manuellerAttributeImportService.getAttributeImportSession(
				benutzerResolver.fromAuthentication(authentication)).get().getFeatureMappings());
	}

	@GetMapping(path = "konflikt-protokolle")
	public FeatureCollection getKonfliktprotokolle(Authentication authentication) {
		manuellerAttributeImportGuard.getKonfliktprotokolle(authentication);
		return this.konfliktToGeoJsonConverter.convert(
			this.manuellerAttributeImportService.getAttributeImportSession(
				benutzerResolver.fromAuthentication(authentication)).get().getAttributeImportKonfliktProtokoll()
				.getKantenKonfliktProtokolle());
	}

	@PostMapping(path = "delete-mapped-grundnetzkanten")
	public FeatureCollection deleteMappedGrundnetzkante(Authentication authentication,
		@RequestBody List<@Valid DeleteMappedGrundnetzkanteCommand> commands) {
		manuellerAttributeImportGuard.deleteMappedGrundnetzkante(authentication, commands);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		AttributeImportSession session = this.manuellerAttributeImportService.getAttributeImportSession(benutzer).get();
		List<FeatureMapping> modifiedFeatureMappings = commands
			.stream().map((command) -> session
				.deleteMappedGrundnetzkanteFromFeatureMapping(command.getFeatureMappingId(),
					command.getKanteId()))
			.toList();
		return featureMappingToGeoJsonConverter.convert(modifiedFeatureMappings);
	}

	@PostMapping(path = "update-feature-mapping")
	public Feature updateFeatureMapping(Authentication authentication,
		@RequestBody @Valid UpdateFeatureMappingCommand command) {
		manuellerAttributeImportGuard.updateFeatureMapping(authentication, command);
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		AttributeImportSession session = this.manuellerAttributeImportService.getAttributeImportSession(benutzer).get();

		FeatureMapping updatedFeaturemapping = this.manuellerAttributeImportService.updateFeatureMapping(session,
			command.getFeaturemappingID(),
			(LineString) command.getUpdatedLinestring());

		return featureMappingToGeoJsonConverter.convertFeatureMappingToFeature(updatedFeaturemapping);
	}

}
