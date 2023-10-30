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
import static de.wps.radvis.backend.benutzer.domain.valueObject.Recht.MANUELLES_MATCHING_ZUORDNEN_UND_BEARBEITEN;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;

public class NetzfehlerGuard {

	private final BenutzerResolver benutzerResolver;

	public NetzfehlerGuard(BenutzerResolver benutzerResolver) {
		this.benutzerResolver = benutzerResolver;
	}

	public void getNetzfehler(Authentication authentication, Long id) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(MANUELLES_MATCHING_ZUORDNEN_UND_BEARBEITEN)
			&& !benutzer.hatRecht(ANPASSUNGSWUENSCHE_BEARBEITEN)) {
			throw new AccessDeniedException("Benutzer ist nicht autorisiert Netzfehler abzufragen.");
		}
	}

	public void setNetzfehlerErledigt(Authentication authentication, Long id) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(MANUELLES_MATCHING_ZUORDNEN_UND_BEARBEITEN)) {
			throw new AccessDeniedException("Benutzer ist nicht autorisiert Netzfehler als erledigt zu markieren.");
		}
	}
}
