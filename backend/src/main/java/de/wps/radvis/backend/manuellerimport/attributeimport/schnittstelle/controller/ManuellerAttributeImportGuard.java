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

package de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.controller;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.command.DeleteMappedGrundnetzkanteCommand;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.command.StartAttributeImportSessionCommand;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.command.UpdateFeatureMappingCommand;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.command.ValidateAttributeCommand;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class ManuellerAttributeImportGuard {
	private final BenutzerResolver benutzerResolver;
	private final VerwaltungseinheitService verwaltungseinheitService;

	public ManuellerAttributeImportGuard(BenutzerResolver benutzerResolver,
		VerwaltungseinheitService verwaltungseinheitService) {
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitService = verwaltungseinheitService;
	}

	public void startAttributeImportSession(Authentication authentication,
		StartAttributeImportSessionCommand command, MultipartFile file) {
		authorizeManuellerAttributeImport(authentication);

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		Verwaltungseinheit zuBearbeitendeOrganisation = verwaltungseinheitService.resolve(command.getOrganisation());

		if (!verwaltungseinheitService.istUebergeordnet(benutzer.getOrganisation(), zuBearbeitendeOrganisation)) {
			throw new AccessDeniedException("Die Organisation liegt nicht in Ihrem Zuständigkeitsbereich");
		}
	}

	public void validateAttribute(Authentication authentication, ValidateAttributeCommand command, MultipartFile file) {
		authorizeManuellerAttributeImport(authentication);
	}

	public void executeAttributeUebernehmen(Authentication authentication) {
		authorizeManuellerAttributeImport(authentication);
	}

	public void getFeatureMappings(Authentication authentication) {
		authorizeManuellerAttributeImport(authentication);
	}

	public void getKonfliktprotokolle(Authentication authentication) {
		authorizeManuellerAttributeImport(authentication);
	}

	public void deleteMappedGrundnetzkante(Authentication authentication,
		List<DeleteMappedGrundnetzkanteCommand> commands) {
		authorizeManuellerAttributeImport(authentication);
		// Da hier nur ein Mapping entfernt wird, aber nichts an den eigentlichen Daten veraendert wird,
		// ist hier keine Ueberpruefung notwendig, ob die kanteIds im Zustaendigkeitsbereich liegen
	}

	public void updateFeatureMapping(Authentication authentication, UpdateFeatureMappingCommand command) {
		authorizeManuellerAttributeImport(authentication);
		// Ueberpruefung ob die kanteIds innerhalb des Zustaendigkeitsbereiches liegen an dieser Stelle nicht nötig, da im
		// Code zum updaten der FeatureMappings schon alle Kanten ausserhalb der eigenen Organisation rausgefiltert werden
	}

	private void authorizeManuellerAttributeImport(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Streckendaten zu importieren");
		}
	}

	public void bearbeitungAbschliessen(Authentication authentication) {
		authorizeManuellerAttributeImport(authentication);
	}
}
