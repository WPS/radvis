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

package de.wps.radvis.backend.quellimport.common.schnittstelle;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;

import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.Envelope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.RadvisViewController;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeatureMapView;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;

@RestController
@RequestMapping("/api/daten")
@RadvisViewController
public class ImportedFeatureController {

	private final ImportedFeatureToGeoJsonConverter importedFeatureToGeoJsonConverterService;
	private final ImportedFeaturePersistentRepository importedFeaturesRepository;

	public ImportedFeatureController(ImportedFeaturePersistentRepository importedFeaturesRepository,
		ImportedFeatureToGeoJsonConverter importedFeatureToGeoJsonConverterService) {
		require(importedFeaturesRepository, notNullValue());
		require(importedFeatureToGeoJsonConverterService, notNullValue());

		this.importedFeaturesRepository = importedFeaturesRepository;
		this.importedFeatureToGeoJsonConverterService = importedFeatureToGeoJsonConverterService;
	}

	@GetMapping("/layer/{key}")
	public FeatureCollection getGeoJsonForLayer(@PathVariable("key") String layerKey,
		@ModelAttribute("sichtbereich") Envelope sichtbereich) {

		QuellSystem quelle;
		Art art;

		switch (layerKey) {
		case "radnetz":
			quelle = QuellSystem.RadNETZ;
			art = Art.Strecke;
			break;
		case "radnetzmassnahmen":
			quelle = QuellSystem.RadNETZ;
			art = Art.Massnahme;
			break;
		case "tuttlingen":
			quelle = QuellSystem.LGL;
			art = Art.Strecke;
			break;
		case "radwegedb":
			quelle = QuellSystem.RadwegeDB;
			art = Art.Strecke;
			break;
		case "rvkesslingen":
			quelle = QuellSystem.RvkEsslingen;
			art = Art.Strecke;
			break;
		case "bietigheimBissingen":
			quelle = QuellSystem.BietigheimBissingen;
			art = Art.Strecke;
			break;
		case "gisgoeppingen":
			quelle = QuellSystem.GisGoeppingen;
			art = Art.Strecke;
			break;
		case "dlm":
			quelle = QuellSystem.DLM;
			art = Art.Strecke;
			break;
		default:
			throw new IllegalArgumentException("Für den Key " + layerKey + " wurden keine Features gefunden");
		}

		List<ImportedFeatureMapView> featuresInSichtbereich = importedFeaturesRepository
			.getFeaturesInBereich(quelle, art, sichtbereich);

		return importedFeatureToGeoJsonConverterService
			.convertImportedFeaturesToFeatureCollection(featuresInSichtbereich);
	}

	@GetMapping("/feature/{id}")
	public ImportedFeature getImportedFeatureById(@PathVariable("id") Long importedFeatureId) {
		ImportedFeature importedFeature = importedFeaturesRepository.findById(importedFeatureId).orElseThrow();
		if (importedFeature.getQuelle().equals(QuellSystem.RadNETZ)) {
			if (importedFeature.getAnteilProjiziert() != null) {
				importedFeature.addAttribut("Anteil Projiziert", importedFeature.getAnteilProjiziert());
			} else {
				importedFeature.addAttribut("Anteil Projiziert", 0.);
			}
		}
		return importedFeature;
	}
}
