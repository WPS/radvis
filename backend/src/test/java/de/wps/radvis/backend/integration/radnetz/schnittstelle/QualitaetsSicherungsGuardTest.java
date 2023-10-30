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

package de.wps.radvis.backend.integration.radnetz.schnittstelle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;

class QualitaetsSicherungsGuardTest {
	@Mock
	Authentication authentication;

	@Mock
	BenutzerResolver benutzerResolver;

	private QualitaetsSicherungsGuard qualitaetsSicherungsGuard;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		this.qualitaetsSicherungsGuard = new QualitaetsSicherungsGuard(this.benutzerResolver);
	}

	@org.junit.jupiter.api.Test
	void notAllowedThrowsAccessDenied() {
		// arrange
		Benutzer massnahmenVerantwortlicher = BenutzerTestDataProvider
			.defaultBenutzer()
			.rollen(Set.of(Rolle.LGL_MITARBEITERIN))
			.build();

		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(massnahmenVerantwortlicher);

		// act + assert
		assertThrows(AccessDeniedException.class,
			() -> qualitaetsSicherungsGuard.markAsQualitaetsgesichert(authentication, null));
	}

	@Test
	void allowedHasAccess() {
		// arrange
		Benutzer radnetzQualitaetssicherin = BenutzerTestDataProvider
			.defaultBenutzer()
			.rollen(Set.of(Rolle.RADNETZ_QUALITAETSSICHERIN))
			.build();

		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(radnetzQualitaetssicherin);

		// act + assert
		assertDoesNotThrow(
			() -> qualitaetsSicherungsGuard.markAsQualitaetsgesichert(authentication, null));
	}
}
