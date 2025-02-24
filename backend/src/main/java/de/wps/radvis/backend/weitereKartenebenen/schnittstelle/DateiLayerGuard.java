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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;

public class DateiLayerGuard {
	private final BenutzerResolver benutzerResolver;

	public DateiLayerGuard(BenutzerResolver benutzerResolver) {
		this.benutzerResolver = benutzerResolver;
	}

	public void create(Authentication authentication, CreateDateiLayerCommand command, MultipartFile file) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.DATEI_LAYER_VERWALTEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung DateiLayer zu erstellen.");
		}
	}

	public void delete(Authentication authentication, Long id) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.DATEI_LAYER_VERWALTEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung DateiLayer zu löschen.");
		}
	}

	public void deleteStyle(Authentication authentication, Long id) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.DATEI_LAYER_VERWALTEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung den Style eines DateiLayers zu löschen.");
		}
	}

	public void addOrChangeStyle(Authentication authentication, Long id) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.DATEI_LAYER_VERWALTEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung den Style eines DateiLayers zu ändern.");
		}
	}
}
