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

package de.wps.radvis.backend.benutzer.schnittstelle;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerIstNichtRegistriertException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/benutzer/reaktivierung")
@AllArgsConstructor
@Slf4j
public class BenutzerReaktivierungController {

	private final BenutzerResolver benutzerResolver;
	private final BenutzerService benutzerService;
	private final BenutzerReaktivierungGuard guard;

	@PostMapping("beantrage-reaktivierung")
	public BenutzerDetailView beantrageReaktivierung(Authentication authentication)
		throws BenutzerIstNichtRegistriertException {
		Benutzer reaktivierenderBenutzer = benutzerResolver.fromAuthentication(authentication);
		guard.beantrageReaktivierung(reaktivierenderBenutzer);
		return new BenutzerDetailView(benutzerService.beantrageReaktivierungFuerBenutzer(reaktivierenderBenutzer));
	}
}
