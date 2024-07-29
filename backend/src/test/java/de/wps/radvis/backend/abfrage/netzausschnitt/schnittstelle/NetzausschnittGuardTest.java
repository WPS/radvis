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

package de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class NetzausschnittGuardTest {

	NetzausschnittGuard netzausschnittGuard;

	@Mock
	private BenutzerResolver benutzerResolver;

	@Mock
	private Authentication authentication;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		netzausschnittGuard = new NetzausschnittGuard(benutzerResolver);
	}

	@Nested
	class WithAdminBenutzer {
		Benutzer benutzer;

		@BeforeEach
		void beforeEach() {
			benutzer = BenutzerTestDataProvider.admin(
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
					.organisationsArt(OrganisationsArt.BUNDESLAND)
					.name("Oscorp").build())
				.build();
			when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);
		}

		@Test
		void getNetzfehlerGeoJson_Admin() {
			// act + assert
			assertDoesNotThrow(() -> netzausschnittGuard.getNetzfehlerGeoJson(authentication, null));
		}

		@Test
		void getNetzfehlerGeoJsonFuerTyp_Admin() {
			// act + assert
			assertDoesNotThrow(() -> netzausschnittGuard.getNetzfehlerGeoJsonFuerTyp(authentication, null, null));
		}
	}

	@Nested
	class WithBetrachter {
		Benutzer benutzer;

		@BeforeEach
		void beforeEach() {
			benutzer = BenutzerTestDataProvider.defaultBenutzer()
				.rollen(Set.of(Rolle.RADVIS_BETRACHTER))
				.build();
			when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzer);
		}

		@Test
		void getNetzfehlerGeoJson_Betrachter() {
			// act + assert
			assertThrows(AccessDeniedException.class,
				() -> netzausschnittGuard.getNetzfehlerGeoJson(authentication, null));
		}

		@Test
		void getNetzfehlerGeoJsonFuerTyp_Betrachter() {
			// act + assert
			assertThrows(AccessDeniedException.class,
				() -> netzausschnittGuard.getNetzfehlerGeoJsonFuerTyp(authentication, null, null));
		}
	}

}
