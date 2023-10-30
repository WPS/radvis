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

package de.wps.radvis.backend.netzfehler.schnittstelle;

import static de.wps.radvis.backend.benutzer.domain.valueObject.Recht.ANPASSUNGSWUENSCHE_BEARBEITEN;
import static de.wps.radvis.backend.benutzer.domain.valueObject.Recht.ANPASSUNGSWUENSCHE_ERFASSEN;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;

public class AnpassungswunschGuard {

	private final BenutzerResolver benutzerResolver;

	public AnpassungswunschGuard(BenutzerResolver benutzerResolver) {
		this.benutzerResolver = benutzerResolver;
	}

	public void createAnpassungswunsch(Authentication authentication, SaveAnpassungswunschCommand command)
		throws AccessDeniedException {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(ANPASSUNGSWUENSCHE_ERFASSEN)) {
			throw new AccessDeniedException("Benutzer ist nicht autorisiert Anpassungswünsche anzulegen.");
		}
	}

	public void deleteAnpassungswunsch(Authentication authentication, Long id) throws AccessDeniedException {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(ANPASSUNGSWUENSCHE_ERFASSEN)) {
			throw new AccessDeniedException("Benutzer ist nicht autorisiert Anpassungswünsche zu entfernen.");
		}
	}

	public void updateAnpassungswunsch(Authentication authentication, Long id, SaveAnpassungswunschCommand command)
		throws AccessDeniedException {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(ANPASSUNGSWUENSCHE_BEARBEITEN)) {
			throw new AccessDeniedException("Benutzer ist nicht autorisiert Anpassungswünsche zu editieren.");
		}
	}
}
