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

package de.wps.radvis.backend.abstellanlage.schnittstelle;

import org.locationtech.jts.geom.Geometry;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.abstellanlage.domain.AbstellanlageService;
import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AbstellanlageGuard {

	private final BenutzerResolver benutzerResolver;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final AbstellanlageService abstellanlageService;

	public void create(SaveAbstellanlageCommand command, Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		assertHatRecht(aktiverBenutzer);
		assertIstImZustaendigkeitsbereich(command.getGeometrie(), aktiverBenutzer);
	}

	public void save(Long id, SaveAbstellanlageCommand command, Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Abstellanlage abstellanlage = abstellanlageService.loadForModification(id, command.getVersion());

		assertIstQuellSystemRadVIS(abstellanlage.getQuellSystem());
		assertHatRecht(aktiverBenutzer);
		assertIstImZustaendigkeitsbereich(command.getGeometrie(), aktiverBenutzer);
	}

	public void delete(Long id, DeleteAbstellanlageCommand command, Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Abstellanlage abstellanlage = abstellanlageService.loadForModification(id, command.getVersion());

		assertIstQuellSystemRadVIS(abstellanlage.getQuellSystem());
		assertHatRecht(aktiverBenutzer);
		assertIstImZustaendigkeitsbereich(abstellanlage.getGeometrie(), aktiverBenutzer);
	}

	public boolean darfBenutzerBearbeiten(Authentication authentication, Abstellanlage abstellanlage) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		try {
			assertIstQuellSystemRadVIS(abstellanlage.getQuellSystem());
			assertHatRecht(aktiverBenutzer);
			assertIstImZustaendigkeitsbereich(abstellanlage.getGeometrie(), aktiverBenutzer);

			return true;
		} catch (AccessDeniedException e) {
			return false;
		}
	}

	private void assertIstQuellSystemRadVIS(AbstellanlagenQuellSystem quellSystem) {
		if (!quellSystem.equals(AbstellanlagenQuellSystem.RADVIS)) {
			throw new AccessDeniedException("Es dürfen nur Abstellanlagen mit Quellsystem RadVIS bearbeitet werden.");
		}
	}

	private void assertHatRecht(Benutzer aktiverBenutzer) {
		if (!aktiverBenutzer.getRechte()
			.contains(Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Abstellanlagen zu bearbeiten.");
		}
	}

	private void assertIstImZustaendigkeitsbereich(Geometry geometry, Benutzer aktiverBenutzer) {
		if (!zustaendigkeitsService.istImZustaendigkeitsbereich(geometry, aktiverBenutzer)) {
			throw new AccessDeniedException(
				"Sie haben nicht die Berechtigung Abstellanlagen in diesem Verwaltungsbereich zu bearbeiten.");
		}
	}
}
