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

package de.wps.radvis.backend.servicestation.schnittstelle;

import org.locationtech.jts.geom.Geometry;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.servicestation.domain.ServicestationService;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationenQuellSystem;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ServicestationGuard {

	private final BenutzerResolver benutzerResolver;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final ServicestationService servicestationService;

	public void create(SaveServicestationCommand command, Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		assertHatRecht(aktiverBenutzer);
		assertIstImZustaendigkeitsbereich(command.getGeometrie(), aktiverBenutzer);
	}

	public void save(Long id, SaveServicestationCommand command, Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		Servicestation servicestation = servicestationService.loadForModification(id, command.getVersion());
		assertIstQuellSystemRadvis(servicestation);
		assertHatRecht(aktiverBenutzer);
		assertIstImZustaendigkeitsbereich(command.getGeometrie(), aktiverBenutzer);
	}

	public void delete(Long id, DeleteServicestationCommand command, Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Servicestation servicestation = servicestationService.loadForModification(id, command.getVersion());

		assertIstQuellSystemRadvis(servicestation);
		assertHatRecht(aktiverBenutzer);
		assertIstImZustaendigkeitsbereich(servicestation.getGeometrie(), aktiverBenutzer);
	}

	public boolean darfBenutzerBearbeiten(Authentication authentication, Servicestation servicestation) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		try {
			assertIstQuellSystemRadvis(servicestation);
			assertHatRecht(aktiverBenutzer);
			assertIstImZustaendigkeitsbereich(servicestation.getGeometrie(), aktiverBenutzer);

			return true;
		} catch (AccessDeniedException e) {
			return false;
		}
	}

	private void assertIstQuellSystemRadvis(Servicestation servicestation) {
		if (!servicestation.getQuellSystem().equals(ServicestationenQuellSystem.RADVIS)) {
			throw new AccessDeniedException("Nur Servicestationen mit QuellSystem " + ServicestationenQuellSystem.RADVIS
				+ " können bearbeitet, gelöscht oder erstellt werden");
		}
	}

	private void assertHatRecht(Benutzer aktiverBenutzer) {
		if (!aktiverBenutzer.getRechte()
			.contains(Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Servicestationen zu bearbeiten.");
		}
	}

	private void assertIstImZustaendigkeitsbereich(Geometry geometry, Benutzer aktiverBenutzer) {
		if (!zustaendigkeitsService.istImZustaendigkeitsbereich(geometry, aktiverBenutzer)) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Servicestationen in diesem Verwaltungsbereich zu bearbeiten.");
		}
	}
}
