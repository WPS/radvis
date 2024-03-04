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

package de.wps.radvis.backend.weitereKartenebenen.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.DateiLayer;
import de.wps.radvis.backend.weitereKartenebenen.domain.exception.SldValidationException;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.DateiLayerRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.GeoserverRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.WeitereKartenebenenRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.DateiLayerFormat;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverDatastoreName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverLayerName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverStyleName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Name;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DateiLayerService {

	private final DateiLayerRepository dateiLayerRepository;
	private final WeitereKartenebenenRepository weitereKartenebenenRepository;
	private final GeoserverRepository geoserverRepository;

	public DateiLayer createDateiLayer(Name layerName, Quellangabe quellangabe, Benutzer benutzer,
		DateiLayerFormat dateiLayerFormat, MultipartFile file) {
		require(layerName, notNullValue());
		require(dateiLayerFormat, notNullValue());
		require(file, notNullValue());

		if (dateiLayerRepository.existsByName(layerName)) {
			throw new DateiLayerImportException(
				"Der DateiLayer mit dem Namen \"" + layerName
					+ "\" existiert bereits und kann somit nicht neu angelegt werden."
			);
		}

		GeoserverDatastoreName datastoreName = GeoserverDatastoreName.withTimestamp(layerName.getValue());
		GeoserverLayerName geoserverLayerName;
		try {
			geoserverLayerName = geoserverRepository.createDataStoreAndLayer(datastoreName, dateiLayerFormat, file);
		} catch (Exception e) {
			throw new RuntimeException("Anlegen des Datenspeichers und der Kartenebene fehlgeschlagen.", e);
		}

		return new DateiLayer(layerName, quellangabe, geoserverLayerName, datastoreName, benutzer, LocalDateTime.now(),
			dateiLayerFormat);
	}

	@Transactional
	public void deleteDateiLayer(Long id) throws EntityNotFoundException, IOException, InterruptedException {
		DateiLayer dateiLayer = dateiLayerRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Der angegebene DateiLayer konnte nicht gefunden werden."));

		weitereKartenebenenRepository.deleteAllByDateiLayerId(id);
		dateiLayerRepository.deleteById(id);

		// Erst in der DB, dann am Geoserver löschen. Sollte dieser Aufruf fehlschlagen wird obiges durch ein rollback
		// rückgängig gemacht. Schlägt obiges Löschen aus DB fehl, dann wird das hier gar nicht aufgerufen. Es entstehen
		// somit keine Datei-Leichen im Geoserver.
		geoserverRepository.removeDatastoreAndLayer(dateiLayer.getGeoserverDatastoreName());
		if (dateiLayer.hasStyle()) {
			geoserverRepository.deleteStyle(dateiLayer.getGeoserverStyleName());
		}
	}

	public void validateStyleForLayer(Long layerId, MultipartFile sldFile) throws IOException, InterruptedException {
		Optional<DateiLayer> dateiLayerOptional = dateiLayerRepository.findById(layerId);
		if (dateiLayerOptional.isEmpty()) {
			throw new EntityNotFoundException("Der angegebene DateiLayer konnte nicht gefunden werden.");
		}
		DateiLayer dateiLayer = dateiLayerOptional.get();

		GeoserverStyleName geoserverStyleName = null;
		try {
			try {
				geoserverStyleName = geoserverRepository.createStyle(
					GeoserverStyleName.withTimestamp(dateiLayer.getName().getValue()), sldFile
				);
			} catch (IOException | InterruptedException e) {
				log.error("Style für Datei-Layer {} konnte für die Validierung nicht angelegt werden",
					dateiLayer.getName(),
					e);
				throw new SldValidationException(
					"Style für Datei-Layer konnte für die Validierung nicht angelegt werden.",
					e);
			}

			if (Objects.isNull(geoserverStyleName)) {
				throw new SldValidationException(
					"Style für Datei-Layer konnte für die Validierung nicht angelegt werden.");
			}

			try {
				geoserverRepository.addStyleToLayer(dateiLayer.getGeoserverLayerName(), geoserverStyleName, false);
			} catch (IOException | InterruptedException e) {
				log.error(
					"Style für Datei-Layer {} konnte zwar angelegt, aber nicht beim Layer für die Validierung hinterlegt werden.",
					dateiLayer.getName(), e);
				throw new SldValidationException(
					"Style für Datei-Layer konnte zwar angelegt, aber nicht beim Layer für die Validierung hinterlegt werden.",
					e);
			}

			log.info("Style {} wurde an Layer {} für die Validierung gesetzt", geoserverStyleName,
				dateiLayer.getName());

			log.info("Validiere Style mittel WMS abruf");
			geoserverRepository.validateStyleForLayer(dateiLayer.getGeoserverLayerName(), geoserverStyleName);
		} finally {
			if (!Objects.isNull(geoserverStyleName)) {
				log.info("Lösche für die Validierung angelegten Style...");
				this.geoserverRepository.deleteStyle(geoserverStyleName);
			}
		}
	}

	@Transactional
	public void deleteStyle(Long layerId) throws IOException, InterruptedException {
		Optional<DateiLayer> dateiLayer = dateiLayerRepository.findById(layerId);
		if (dateiLayer.isEmpty()) {
			throw new EntityNotFoundException("Der angegebene DateiLayer konnte nicht gefunden werden.");
		}

		if (!dateiLayer.get().hasStyle()) {
			throw new EntityNotFoundException("Der angegebene DateiLayer hat keinen assozierten Style.");
		}

		// Wenn wir den Style nicht löschen können, kriegen wir eine Exception.
		// Dann wollen wir den Style am Layer auch nicht entfernen.
		geoserverRepository.deleteStyle(dateiLayer.get().getGeoserverStyleName());
		dateiLayer.get().removeStyle();
	}

	@Transactional
	public boolean changeOrAddStyleForLayer(Long id, MultipartFile sldFile)
		throws IOException, InterruptedException {
		Optional<DateiLayer> dateiLayer = dateiLayerRepository.findById(id);
		if (dateiLayer.isEmpty()) {
			throw new EntityNotFoundException("Der angegebene DateiLayer konnte nicht gefunden werden.");
		}
		if (dateiLayer.get().hasStyle()) {
			GeoserverStyleName oldGeoserverStyleName = dateiLayer.get().getGeoserverStyleName();
			if (addDefaultStyleForLayer(dateiLayer.get().getId(), sldFile)) {
				geoserverRepository.deleteStyle(oldGeoserverStyleName);
				return true;
			}
			return false;
		} else {
			return addDefaultStyleForLayer(id, sldFile);
		}
	}

	private boolean addDefaultStyleForLayer(Long layerId, MultipartFile sldFile) {
		Optional<DateiLayer> dateiLayerOptional = dateiLayerRepository.findById(layerId);
		if (dateiLayerOptional.isEmpty()) {
			throw new EntityNotFoundException("Der angegebene DateiLayer konnte nicht gefunden werden.");
		}

		DateiLayer dateiLayer = dateiLayerOptional.get();

		GeoserverStyleName geoserverStyleName;
		try {
			geoserverStyleName = geoserverRepository.createStyle(
				GeoserverStyleName.withTimestamp(dateiLayer.getName().getValue()), sldFile
			);
		} catch (IOException | InterruptedException e) {
			log.info("Style für Datei-Layer {} konnte nicht angelegt werden.", dateiLayer.getName(), e);
			return false;
		}

		if (Objects.isNull(geoserverStyleName)) {
			return false;
		}

		try {
			geoserverRepository.addStyleToLayer(dateiLayer.getGeoserverLayerName(), geoserverStyleName, true);
		} catch (IOException | InterruptedException e) {
			log.info(
				"Style für Datei-Layer {} konnte zwar angelegt, aber nicht beim Layer als default Style hinterlegt werden. Ursache: {}",
				dateiLayer.getName(), e.getMessage());
			return false;
		}

		log.info("Style {} wurde als Default an Layer {} gesetzt", geoserverStyleName, dateiLayer.getName());
		dateiLayer.setStyle(geoserverStyleName, sldFile.getOriginalFilename());
		return true;
	}
}