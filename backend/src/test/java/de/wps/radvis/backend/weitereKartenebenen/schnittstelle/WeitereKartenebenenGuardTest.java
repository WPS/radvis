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

package de.wps.radvis.backend.weitereKartenebenen.schnittstelle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;

class WeitereKartenebenenGuardTest {
	private WeitereKartenebenenGuard weitereKartenebenenGuard;
	@Mock
	private BenutzerResolver benutzerResolver;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		weitereKartenebenenGuard = new WeitereKartenebenenGuard(benutzerResolver);
	}

	@Test
	void save_hatRechtDefaultLayerZuVerwalten_addDefault_doesNotThrow() {
		// arrange
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR)).build());
		SaveWeitereKartenebeneCommand saveCommand = SaveWeitereKartenebenenCommandTestDataProvider.defaultValue()
			.id(null).defaultLayer(true).build();

		// act + assert
		assertDoesNotThrow(() -> weitereKartenebenenGuard.save(mock(Authentication.class), List.of(saveCommand)));
	}

	@Test
	void save_hatKeinRechtDefaultLayerZuVerwalten_addDefault_throws() {
		// arrange
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADWEGE_ERFASSERIN)).build());
		SaveWeitereKartenebeneCommand saveCommand = SaveWeitereKartenebenenCommandTestDataProvider.defaultValue()
			.id(null).defaultLayer(true).build();

		// act + assert
		assertThrows(AccessDeniedException.class,
			() -> weitereKartenebenenGuard.save(mock(Authentication.class), List.of(saveCommand)));
	}

	@Test
	void save_hatKeinRechtDefaultLayerZuVerwalten_updateDefault_doesNotThrow() {
		// arrange
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADWEGE_ERFASSERIN)).build());
		SaveWeitereKartenebeneCommand saveCommand = SaveWeitereKartenebenenCommandTestDataProvider.defaultValue()
			.id(1l).defaultLayer(false).build();

		// act + assert
		assertDoesNotThrow(() -> weitereKartenebenenGuard.save(mock(Authentication.class), List.of(saveCommand)));
	}

	@Test
	void save_hatKeinRechtDefaultLayerZuVerwalten_addNonDefault_doesNotThrow() {
		// arrange
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADWEGE_ERFASSERIN)).build());
		SaveWeitereKartenebeneCommand saveCommand = SaveWeitereKartenebenenCommandTestDataProvider.defaultValue()
			.id(null).defaultLayer(false).build();

		// act + assert
		assertDoesNotThrow(() -> weitereKartenebenenGuard.save(mock(Authentication.class), List.of(saveCommand)));
	}
}
