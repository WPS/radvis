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

package de.wps.radvis.backend.organisation.schnittstelle;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.organisation.domain.WahlkreisService;
import de.wps.radvis.backend.organisation.domain.dbView.WahlkreisView;

@RestController
@RequestMapping("/api/wahlkreise")
public class WahlkreisController {

	private final WahlkreisService wahlkreisService;

	public WahlkreisController(WahlkreisService wahlkreisService) {
		this.wahlkreisService = wahlkreisService;
	}

	@GetMapping("/get")
	public List<WahlkreisView> getAlleWahlkreise() {
		return wahlkreisService.getWahlkreise()
			.stream()
			.sorted(Comparator.comparing(WahlkreisView::getName))
			.collect(Collectors.toList());
	}
}
