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

package de.wps.radvis.backend.basicAuthentication.schnittstelle;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthBenutzerRepository;
import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthPasswortService;
import de.wps.radvis.backend.basicAuthentication.domain.BenutzerBasicAuthView;
import de.wps.radvis.backend.basicAuthentication.domain.entity.BenutzerBasicAuth;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;

@RestController
@RequestMapping("/api/benutzerzugangsdaten")
public class BenutzerBasicAuthController {

	private final BenutzerResolver benutzerResolver;
	private final BasicAuthPasswortService basicAuthPasswortService;
	private final BasicAuthBenutzerRepository basicAuthBenutzerRepository;

	public BenutzerBasicAuthController(BenutzerResolver benutzerResolver,
		BasicAuthPasswortService basicAuthPasswortService,
		BasicAuthBenutzerRepository basicAuthBenutzerRepository) {
		this.benutzerResolver = benutzerResolver;
		this.basicAuthPasswortService = basicAuthPasswortService;
		this.basicAuthBenutzerRepository = basicAuthBenutzerRepository;
	}

	@GetMapping(path = "/generate")
	public BenutzerBasicAuthView generateBasicAuth(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (benutzer == null) {
			throw new AccessDeniedException(
				"Es kann nur ein Basic Auth Passwort für einen vorhandenen Benutzer generiert werden.");
		}

		String passwordPlain = basicAuthPasswortService.generateRandomPassword();
		String passwordHashed = basicAuthPasswortService.hashPassword(passwordPlain);
		basicAuthBenutzerRepository.save(new BenutzerBasicAuth(benutzer.getId(), passwordHashed));
		return new BenutzerBasicAuthView(benutzer.getBasicAuthAnmeldeName(), passwordPlain);
	}
}