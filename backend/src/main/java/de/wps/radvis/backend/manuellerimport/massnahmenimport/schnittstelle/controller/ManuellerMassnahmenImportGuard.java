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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.controller;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.MassnahmenImportAttributeAuswaehlenCommand;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.MassnahmenImportNetzbezugAktualisierenCommand;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.MassnahmenImportMassnahmenAuswaehlenCommand;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.schnittstelle.command.StartMassnahmenImportSessionCommand;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;

public class ManuellerMassnahmenImportGuard {
	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitService verwaltungseinheitService;

	public ManuellerMassnahmenImportGuard(BenutzerResolver benutzerResolver,
		VerwaltungseinheitService verwaltungseinheitService) {
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitService = verwaltungseinheitService;
	}

	public void startMassnahmenImportSession(Authentication authentication, StartMassnahmenImportSessionCommand command,
		MultipartFile file) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		authorizeManuellerMassnahmenImport(benutzer);

		if (benutzer.hatRecht(Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN)) {
			return;
		}

		List<Long> untergeordnetIds = verwaltungseinheitService.findAllUntergeordnetIds(
			benutzer.getOrganisation().getId());
		if (!untergeordnetIds.containsAll(command.getGebietskoerperschaften())) {
			throw new AccessDeniedException(
				"Die Gebietskörperschaften liegen nicht alle in Ihrem Zuständigkeitsbereich.");
		}
	}

	private void authorizeManuellerMassnahmenImport(Benutzer benutzer) {
		if (Stream.of(
			Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN,
			Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN).noneMatch(benutzer::hatRecht)) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Maßnahmen zu bearbeiten oder zu erfassen");
		}
	}

	public void attributeAuswaehlen(Authentication authentication,
		MassnahmenImportAttributeAuswaehlenCommand command) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		authorizeManuellerMassnahmenImport(benutzer);
	}

	public void netzbezuegeErstellen(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		authorizeManuellerMassnahmenImport(benutzer);
	}

	public void netzbezugAktualisieren(Authentication authentication,
		MassnahmenImportNetzbezugAktualisierenCommand command) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		authorizeManuellerMassnahmenImport(benutzer);
	}

	public void saveMassnahmen(Authentication authentication,
		MassnahmenImportMassnahmenAuswaehlenCommand command) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		authorizeManuellerMassnahmenImport(benutzer);
	}

	public void getProtokollStats(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		authorizeManuellerMassnahmenImport(benutzer);
	}

	public void downloadFehlerprotokoll(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		authorizeManuellerMassnahmenImport(benutzer);
	}
}
