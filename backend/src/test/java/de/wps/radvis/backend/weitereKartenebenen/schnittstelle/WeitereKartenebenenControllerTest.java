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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.weitereKartenebenen.domain.WeitereKartenebenenConfigurationProperties;
import de.wps.radvis.backend.weitereKartenebenen.domain.WeitereKartenebenenService;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.WeitereKartenebene;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.WeitereKartenebenenTestDataProvider;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.WeitereKartenebenenRepository;

class WeitereKartenebenenControllerTest {
	private WeitereKartenebenenController weitereKartenebenenController;
	@Mock
	private WeitereKartenebenenRepository weitereKartenebenenRepository;
	@Mock
	private BenutzerResolver benutzerResolver;
	@Mock
	private WeitereKartenebenenConfigurationProperties weitereKartenebenenConfigurationProperties;
	@Mock
	private WeitereKartenebenenService weitereKartenebenenService;
	@Mock
	private WeitereKartenebenenGuard weitereKartenebenenGuard;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		weitereKartenebenenController = new WeitereKartenebenenController(weitereKartenebenenRepository,
			benutzerResolver, weitereKartenebenenConfigurationProperties, weitereKartenebenenService,
			weitereKartenebenenGuard);
	}

	@Test
	void save_deleteDefaultLayer_hatKeinRechtDefaultLayerZuVerwalten_doesNotDelete() throws AccessDeniedException {
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADWEGE_ERFASSERIN)).build();
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(benutzer);
		when(weitereKartenebenenService.getAllForNutzer(any()))
			.thenReturn(List.of(
				WeitereKartenebenenTestDataProvider.defaultValue().defaultLayer(true).build()));

		// act
		weitereKartenebenenController.save(mock(Authentication.class), Collections.emptyList());

		// assert
		verify(weitereKartenebenenRepository, never()).delete(any());
	}

	@Test
	void save_deleteDefaultLayer_hatRechtDefaultLayerZuVerwalten_doesDelete() throws AccessDeniedException {
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR))
			.build();
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(benutzer);
		when(weitereKartenebenenService.getAllForNutzer(any()))
			.thenReturn(List.of(
				WeitereKartenebenenTestDataProvider.defaultValue().defaultLayer(true).build()));

		// act
		weitereKartenebenenController.save(mock(Authentication.class), Collections.emptyList());

		// assert
		verify(weitereKartenebenenRepository, times(1)).delete(any());
	}

	@Test
	void save_deleteNonDefaultLayer_hatKeinRechtDefaultLayerZuVerwalten_doesDelete() throws AccessDeniedException {
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADWEGE_ERFASSERIN)).build();
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(benutzer);
		when(weitereKartenebenenService.getAllForNutzer(any()))
			.thenReturn(List.of(
				WeitereKartenebenenTestDataProvider.defaultValue().defaultLayer(false)
					.build()));

		// act
		weitereKartenebenenController.save(mock(Authentication.class), Collections.emptyList());

		// assert
		verify(weitereKartenebenenRepository, times(1)).delete(any());
	}

	@Test
	void save_updateDefaultLayer_hatKeinRechtDefaultLayerZuVerwalten_doesNotUpdate() throws AccessDeniedException {
		SaveWeitereKartenebeneCommand command = SaveWeitereKartenebenenCommandTestDataProvider.defaultValue()
			.id(1l).defaultLayer(true).build();
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADWEGE_ERFASSERIN)).build();
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(benutzer);
		when(weitereKartenebenenService.getAllForNutzer(any()))
			.thenReturn(List.of(WeitereKartenebenenTestDataProvider.defaultValue().id(command.getId()).build()));
		WeitereKartenebene kartenEbeneToUpdateMock = mock(WeitereKartenebene.class);
		when(kartenEbeneToUpdateMock.isDefaultLayer()).thenReturn(true);
		when(weitereKartenebenenRepository.findById(command.getId()))
			.thenReturn(Optional.of(kartenEbeneToUpdateMock));

		// act
		weitereKartenebenenController.save(mock(Authentication.class), List.of(command));

		// assert
		verify(weitereKartenebenenRepository, never()).delete(any());
		verify(kartenEbeneToUpdateMock, never()).update(any(), any(), any(), any(), any(), any(), any(), any(),
			anyBoolean());
	}

	@Test
	void save_updateNonDefaultLayer_hatKeinRechtDefaultLayerZuVerwalten_doesUpdate() throws AccessDeniedException {
		SaveWeitereKartenebeneCommand command = SaveWeitereKartenebenenCommandTestDataProvider.defaultValue()
			.id(1l).defaultLayer(true).build();
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADWEGE_ERFASSERIN)).build();
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(benutzer);
		when(weitereKartenebenenService.getAllForNutzer(any()))
			.thenReturn(List.of(WeitereKartenebenenTestDataProvider.defaultValue().id(command.getId()).build()));
		WeitereKartenebene kartenEbeneToUpdateMock = mock(WeitereKartenebene.class);
		when(kartenEbeneToUpdateMock.isDefaultLayer()).thenReturn(false);
		when(weitereKartenebenenRepository.findById(command.getId()))
			.thenReturn(Optional.of(kartenEbeneToUpdateMock));

		// act
		weitereKartenebenenController.save(mock(Authentication.class), List.of(command));

		// assert
		verify(weitereKartenebenenRepository, never()).delete(any());
		verify(kartenEbeneToUpdateMock, times(1)).update(any(), any(), any(), any(), any(), any(), any(), any(),
			anyBoolean());
	}

	@Test
	void save_updateDefaultLayer_hatRechtDefaultLayerZuVerwalten_doesUpdate() throws AccessDeniedException {
		SaveWeitereKartenebeneCommand command = SaveWeitereKartenebenenCommandTestDataProvider.defaultValue()
			.id(1l).defaultLayer(true).build();
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR))
			.build();
		when(benutzerResolver.fromAuthentication(any()))
			.thenReturn(benutzer);
		when(weitereKartenebenenService.getAllForNutzer(any()))
			.thenReturn(List.of(WeitereKartenebenenTestDataProvider.defaultValue().id(command.getId()).build()));
		WeitereKartenebene kartenEbeneToUpdateMock = mock(WeitereKartenebene.class);
		when(kartenEbeneToUpdateMock.isDefaultLayer()).thenReturn(true);
		when(weitereKartenebenenRepository.findById(command.getId()))
			.thenReturn(Optional.of(kartenEbeneToUpdateMock));

		// act
		weitereKartenebenenController.save(mock(Authentication.class), List.of(command));

		// assert
		verify(weitereKartenebenenRepository, never()).delete(any());
		verify(kartenEbeneToUpdateMock, times(1)).update(any(), any(), any(), any(), any(), any(), any(), any(),
			anyBoolean());
	}

}
