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

package de.wps.radvis.backend.leihstation.schnittstelle;

import org.locationtech.jts.geom.Geometry;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.leihstation.domain.LeihstationService;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LeihstationGuard {

	private final BenutzerResolver benutzerResolver;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final LeihstationService leihstationService;

	public void create(SaveLeihstationCommand command, Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		assertHatRecht(aktiverBenutzer);
		assertIstImZustaendigkeitsbereich(command.getGeometrie(), aktiverBenutzer);
	}

	public void save(Long id, SaveLeihstationCommand command, Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		assertHatRecht(aktiverBenutzer);
		assertIstImZustaendigkeitsbereich(command.getGeometrie(), aktiverBenutzer);
	}

	public void delete(Long id, DeleteLeihstationCommand command, Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Leihstation leihstation = leihstationService.loadForModification(id, command.getVersion());

		assertHatRecht(aktiverBenutzer);
		assertIstImZustaendigkeitsbereich(leihstation.getGeometrie(), aktiverBenutzer);
	}

	public boolean darfBenutzerBearbeiten(Authentication authentication, Leihstation leihstation) {

		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		try {
			assertHatRecht(aktiverBenutzer);
			assertIstImZustaendigkeitsbereich(leihstation.getGeometrie(), aktiverBenutzer);

			return true;
		} catch (AccessDeniedException e) {
			return false;
		}
	}

	private void assertHatRecht(Benutzer aktiverBenutzer) {
		if (!aktiverBenutzer.getRechte()
			.contains(Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Leihstationen zu bearbeiten.");
		}
	}

	private void assertIstImZustaendigkeitsbereich(Geometry geometry, Benutzer aktiverBenutzer) {
		if (!zustaendigkeitsService.istImZustaendigkeitsbereich(geometry, aktiverBenutzer)) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Leihstationen in diesem Verwaltungsbereich zu bearbeiten.");
		}
	}
}
