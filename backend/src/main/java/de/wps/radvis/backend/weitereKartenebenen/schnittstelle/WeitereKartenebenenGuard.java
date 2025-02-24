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

import java.nio.file.AccessDeniedException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import jakarta.validation.Valid;

public class WeitereKartenebenenGuard {
	private final BenutzerResolver benutzerResolver;

	public WeitereKartenebenenGuard(BenutzerResolver benutzerResolver) {
		this.benutzerResolver = benutzerResolver;
	}

	public void save(Authentication authentication,
		@RequestBody List<@Valid SaveWeitereKartenebeneCommand> commands) throws AccessDeniedException {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		if (benutzer.hatRecht(Recht.WEITERE_KARTENEBENEN_ALS_DEFAULT_FESTLEGEN)) {
			return;
		}

		if (commands.stream().filter(c -> c.getId() == null).anyMatch(c -> c.isDefaultLayer())) {
			throw new AccessDeniedException("Sie sind nicht berechtigt, Layer für alle Nutzer hinzuzufügen.");
		}
	}
}
