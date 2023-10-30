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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.schnittstelle.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.schnittstelle.command.StartNetzklassenImportSessionCommand;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class ManuellerNetzklassenImportGuard {
	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitService verwaltungseinheitService;

	public ManuellerNetzklassenImportGuard(BenutzerResolver benutzerResolver,
		VerwaltungseinheitService verwaltungseinheitService) {
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitService = verwaltungseinheitService;
	}

	public void startNetzklassenImportSession(Authentication authentication,
		StartNetzklassenImportSessionCommand command, MultipartFile file) {
		authorizeManuellerNetzklassenImport(authentication);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		Verwaltungseinheit zuBearbeitendeOrganisation = verwaltungseinheitService.resolve(command.getOrganisation());

		if (!verwaltungseinheitService.istUebergeordnet(benutzer.getOrganisation(), zuBearbeitendeOrganisation)) {
			throw new AccessDeniedException("Die Organisation liegt nicht in Ihrem Zuständigkeitsbereich");
		}
	}

	public void executeNetzklassenZuweisen(Authentication authentication) {
		authorizeManuellerNetzklassenImport(authentication);
	}

	public void getSackgassen(Authentication authentication) {
		authorizeManuellerNetzklassenImport(authentication);
	}

	public void toggleNetzklassenzugehoerigkeit(Authentication authentication, Long kanteId) {
		authorizeManuellerNetzklassenImport(authentication);
		// Hier kein Check notwendig, ob die kanteId im Zustaendigkeitsbereich liegt, da in dem execute der session
		// sowieso alle Kanten herausgefiltert werden, die nicht in der Organisation liegen.
	}

	public void getKanteIdsMitNetzklasse(Authentication authentication) {
		authorizeManuellerNetzklassenImport(authentication);
	}

	private void authorizeManuellerNetzklassenImport(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Streckendaten zu importieren");
		}
	}
}
