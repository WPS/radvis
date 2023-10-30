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

package de.wps.radvis.backend.abfrage.extern.schnittstelle;

import org.geojson.FeatureCollection;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.netzfehler.domain.AnpassungswunschService;
import de.wps.radvis.backend.netzfehler.schnittstelle.AnpassungswunschToGeoJsonConverter;

@RestController
@RequestMapping("/api/extern/anpassungswuensche/")
public class AnpassungswunschExportController {

	private final AnpassungswunschService anpassungswunschService;
	private final AnpassungswunschToGeoJsonConverter anpassungswunschToGeoJsonConverter;

	public AnpassungswunschExportController(AnpassungswunschService anpassungswunschService,
		AnpassungswunschToGeoJsonConverter anpassungswunschToGeoJsonConverter) {
		this.anpassungswunschService = anpassungswunschService;
		this.anpassungswunschToGeoJsonConverter = anpassungswunschToGeoJsonConverter;
	}

	@GetMapping
	public FeatureCollection getGeoJson() {
		return anpassungswunschToGeoJsonConverter.convertAnpassungswuensche(anpassungswunschService.findAllOpenDlm());
	}
}
