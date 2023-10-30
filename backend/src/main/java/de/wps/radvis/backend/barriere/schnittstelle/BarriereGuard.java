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

package de.wps.radvis.backend.barriere.schnittstelle;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class BarriereGuard {

	private final BenutzerResolver benutzerResolver;

	private final VerwaltungseinheitService verwaltungseinheitService;

	public BarriereGuard(BenutzerResolver benutzerResolver, VerwaltungseinheitService verwaltungseinheitService) {
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitService = verwaltungseinheitService;
	}

	public void createBarriere(Authentication authentication, Verwaltungseinheit organisationsBereich) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		if (!aktiverBenutzer.getRechte().contains(Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Barrieren anzulegen.");
		}
		if (!verwaltungseinheitService.istUebergeordnet(aktiverBenutzer.getOrganisation(), organisationsBereich)) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Barrieren in diesem Verwaltungsbereich anzulegen.");
		}
	}

	public void updateBarriere(Authentication authentication, Verwaltungseinheit organisationsBereich) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		if (!aktiverBenutzer.getRechte().contains(Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Barrieren zu bearbeiten.");
		}
		if (!verwaltungseinheitService.istUebergeordnet(aktiverBenutzer.getOrganisation(), organisationsBereich)) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Barrieren in diesem Verwaltungsbereich zu bearbeiten.");
		}
	}
}
