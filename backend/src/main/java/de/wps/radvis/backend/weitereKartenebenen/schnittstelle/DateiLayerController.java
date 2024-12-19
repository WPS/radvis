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

package de.wps.radvis.backend.weitereKartenebenen.schnittstelle;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.schnittstelle.BenutzerView;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.common.schnittstelle.ErrorDetails;
import de.wps.radvis.backend.weitereKartenebenen.domain.DateiLayerImportException;
import de.wps.radvis.backend.weitereKartenebenen.domain.DateiLayerService;
import de.wps.radvis.backend.weitereKartenebenen.domain.WeitereKartenebenenConfigurationProperties;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.DateiLayer;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.DateiLayerRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/datei-layer")
@Validated
@Slf4j
@WithFehlercode(Fehlercode.DATEI_LAYER)
public class DateiLayerController {
	private final DateiLayerRepository dateiLayerRepository;
	private final BenutzerResolver benutzerResolver;
	private final DateiLayerGuard dateiLayerGuard;
	private final DateiLayerService dateiLayerService;
	private final MultipartProperties multipartProperties;
	private final WeitereKartenebenenConfigurationProperties weitereKartenebenenProperties;

	public DateiLayerController(
		DateiLayerRepository dateiLayerRepository,
		BenutzerResolver benutzerResolver,
		DateiLayerGuard dateiLayerGuard,
		DateiLayerService dateiLayerService,
		MultipartProperties multipartProperties,
		WeitereKartenebenenConfigurationProperties weitereKartenebenenProperties) {
		this.dateiLayerRepository = dateiLayerRepository;
		this.benutzerResolver = benutzerResolver;
		this.dateiLayerGuard = dateiLayerGuard;
		this.dateiLayerService = dateiLayerService;
		this.multipartProperties = multipartProperties;
		this.weitereKartenebenenProperties = weitereKartenebenenProperties;

	}

	@GetMapping("/max-file-size-mb")
	public Long getMaxFileSizeInMB() {
		return multipartProperties.getMaxFileSize().toMegabytes();
	}

	@GetMapping("/list")
	public List<DateiLayerView> list() {
		return StreamSupport.stream(dateiLayerRepository.findAll().spliterator(), false)
			.map(
				dl -> new DateiLayerView(dl.getId(), dl.getName(), dl.getGeoserverLayerName(), dl.getDateiLayerFormat(),
					dl.getQuellangabe(), new BenutzerView(dl.getBenutzer()), dl.getErstelltAm(),
					dl.getSldFilename()))
			.collect(Collectors.toList());
	}

	@PostMapping(path = "/create", consumes = {
		MediaType.MULTIPART_FORM_DATA_VALUE,
	})
	@Transactional
	public ResponseEntity<?> create(Authentication authentication,
		@RequestPart CreateDateiLayerCommand command,
		@RequestPart MultipartFile file) {
		dateiLayerGuard.create(authentication, command, file);

		if (file.getSize() > multipartProperties.getMaxFileSize().toBytes()) {
			throw new AccessDeniedException("Die hochgeladene Datei ist zu groß.");
		}

		if (command.getName() == null || command.getQuellangabe() == null || command.getFormat() == null) {
			throw new RuntimeException(
				"Es kann kein DateiLayer ohne Name, Quellangabe oder Dateiformat angelegt werden.");
		}
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		DateiLayer dateiLayer;
		try {
			dateiLayer = dateiLayerService.createDateiLayer(command.getName(), command.getQuellangabe(),
				benutzer, command.getFormat(), file);
		} catch (DateiLayerImportException e) {
			ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), e.getMessage(),
				e.getClass().getSimpleName(), e.getMessage());
			return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			throw new RuntimeException("Es ist ein unbekannter Fehler beim Erstellen eines DateiLayers aufgetreten.",
				e);
		}

		DateiLayer savedDateiLayer = dateiLayerRepository.save(dateiLayer);
		log.info("Es wurde erfolgreich ein neuer {} DateiLayer mit der id: {} angelegt.",
			savedDateiLayer.getName(), savedDateiLayer.getId());
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{id}/delete")
	public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id) {
		dateiLayerGuard.delete(authentication, id);

		try {
			dateiLayerService.deleteDateiLayer(id);
		} catch (Exception e) {
			throw new RuntimeException("Es ist ein unbekannter Fehler beim Entfernen eines DateiLayers aufgetreten.",
				e);
		}

		log.info("Es wurde ein DateiLayer mit der id: {} gelöscht.", id);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{layerId}/deleteStyle")
	public ResponseEntity<?> deleteStyle(Authentication authentication, @PathVariable Long layerId) {
		dateiLayerGuard.deleteStyle(authentication, layerId);

		try {
			dateiLayerService.deleteStyle(layerId);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(
				"Es ist ein unbekannter Fehler beim Entfernen eines DateiLayer-Styles aufgetreten.",
				e);
		}

		log.info("Es wurde der Style des DateiLayers mit der id: {} gelöscht.", layerId);
		return ResponseEntity.ok().build();
	}

	@PostMapping(path = "/{layerId}/addOrChangeStyle", consumes = {
		MediaType.MULTIPART_FORM_DATA_VALUE,
	})
	public ResponseEntity<?> addOrChangeStyle(Authentication authentication, @PathVariable Long layerId,
		@RequestPart MultipartFile sldFile) {
		dateiLayerGuard.addOrChangeStyle(authentication, layerId);

		if (sldFile.getSize() > weitereKartenebenenProperties.getMaxSldFileSize().toBytes()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Die hochgeladene SLD-Datei ist zu groß.");
		}

		try {
			dateiLayerService.validateStyleForLayer(layerId, sldFile);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(
				"Der Style konnte nicht erfolgreich validiert werden",
				e);
		}

		try {
			if (!dateiLayerService.changeOrAddStyleForLayer(layerId, sldFile)) {
				throw new RuntimeException();
			}
		} catch (Exception e) {
			throw new RuntimeException(
				"Es ist ein unbekannter Fehler beim Ändern/Hinzufügen des Styles eines DateiLayers aufgetreten.",
				e);
		}

		log.info("Es wurde der Style des DateiLayers mit der id: {} geändert.", layerId);
		return ResponseEntity.ok().build();
	}
}
