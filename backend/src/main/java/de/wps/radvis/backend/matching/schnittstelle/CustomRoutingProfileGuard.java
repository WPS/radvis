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

package de.wps.radvis.backend.matching.schnittstelle;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;

public class CustomRoutingProfileGuard {

	private final BenutzerResolver benutzerResolver;

	public CustomRoutingProfileGuard(BenutzerResolver benutzerResolver) {
		this.benutzerResolver = benutzerResolver;
	}

	public void save(Authentication authentication, @RequestBody List<SaveCustomRoutingProfileCommand> commands) {
		Benutzer benutzer = this.benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.ROUTINGPROFILE_VERWALTEN)) {
			throw new AccessDeniedException(
				"Sie haben nicht das Recht, Routingprofile zu verwalten.");
		}
	}
}
