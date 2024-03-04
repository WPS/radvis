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

package de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.controller;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.manuellerimport.massnahmendateianhaenge.schnittstelle.command.StartMassnahmenDateianhaengeImportSessionCommand;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ManuellerMassnahmenDateianhaengeImportGuard {

	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitService verwaltungseinheitService;

	public void startMassnahmenDateianhaengeImportSession(
		Authentication authentication,
		StartMassnahmenDateianhaengeImportSessionCommand command,
		MultipartFile file) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		authorizeManuellerMassnahmenImport(benutzer);

		if (benutzer.hatRecht(Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN)) {
			return;
		}

		List<Long> untergeordnetIds = verwaltungseinheitService.findAllUntergeordnetIds(
			benutzer.getOrganisation().getId());
		if (!untergeordnetIds.containsAll(command.gebietskoerperschaften())) {
			throw new AccessDeniedException(
				"Die Gebietskörperschaften liegen nicht alle in Ihrem Zuständigkeitsbereich.");
		}
	}

	public void continueAfterFehlerUeberpruefen(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		authorizeManuellerMassnahmenImport(benutzer);
	}

	private void authorizeManuellerMassnahmenImport(Benutzer benutzer) {
		if (Stream.of(
			Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
			Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN).noneMatch(benutzer::hatRecht)) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Maßnahmen zu bearbeiten oder zu erfassen");
		}
	}
}
