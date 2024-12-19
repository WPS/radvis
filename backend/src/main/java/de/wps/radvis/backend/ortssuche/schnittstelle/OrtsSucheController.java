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

package de.wps.radvis.backend.ortssuche.schnittstelle;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.ortssuche.domain.OrtssucheRepository;
import de.wps.radvis.backend.ortssuche.domain.entity.OrtsSucheErgebnis;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/ortssuche")
@WithFehlercode(Fehlercode.ORTSSUCHE)
public class OrtsSucheController {

	private final OrtssucheRepository ortssucheRepository;

	public OrtsSucheController(OrtssucheRepository ortssucheRepository) {
		this.ortssucheRepository = ortssucheRepository;
	}

	@GetMapping
	public List<OrtsSucheErgebnis> sucheOrt(@RequestParam("suchParameter") String suchString) {
		log.debug("Suche via BKG wird durchgeführt...");
		return ortssucheRepository.find(suchString);
	}
}
