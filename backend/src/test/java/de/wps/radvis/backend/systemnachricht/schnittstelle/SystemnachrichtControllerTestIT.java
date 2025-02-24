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

package de.wps.radvis.backend.systemnachricht.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.systemnachricht.SystemnachrichtConfiguration;
import de.wps.radvis.backend.systemnachricht.domain.SystemnachrichtRepository;
import de.wps.radvis.backend.systemnachricht.domain.SystemnachrichtService;
import de.wps.radvis.backend.systemnachricht.domain.entity.Systemnachricht;

@Tag("group4")
@ContextConfiguration(classes = SystemnachrichtConfiguration.class)
class SystemnachrichtControllerTestIT extends DBIntegrationTestIT {
	private SystemnachrichtController systemnachrichtController;
	@MockitoBean
	private BenutzerResolver benutzerResolverMock;
	@Autowired
	private SystemnachrichtRepository systemnachrichtRepository;

	@BeforeEach
	void setup() {
		systemnachrichtController = new SystemnachrichtController(new SystemnachrichtService(systemnachrichtRepository),
			new SystemnachrichtGuard(benutzerResolverMock));
	}

	@Test
	void testDelete_keinAdministrator_throws() {
		// arrange
		Set<Rolle> alleAusserAdmin = new HashSet<>(Set.of(Rolle.values()));
		alleAusserAdmin.remove(Rolle.RADVIS_ADMINISTRATOR);
		when(benutzerResolverMock.fromAuthentication(any()))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().rollen(alleAusserAdmin).build());

		// act + assert
		assertThrows(AccessDeniedException.class, () -> systemnachrichtController.delete(mock(Authentication.class)));
	}

	@Test
	void testDelete_administrator_deletes() {
		// arrange
		when(benutzerResolverMock.fromAuthentication(any()))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR)).build());
		systemnachrichtRepository.save(new Systemnachricht(LocalDate.now(), "Test"));

		// act
		systemnachrichtController.delete(mock(Authentication.class));

		// assert
		assertThat(systemnachrichtController.get()).isEmpty();
	}

	@Test
	void testCreate_keinAdministrator_throws() {
		// arrange
		Set<Rolle> alleAusserAdmin = new HashSet<>(Set.of(Rolle.values()));
		alleAusserAdmin.remove(Rolle.RADVIS_ADMINISTRATOR);
		when(benutzerResolverMock.fromAuthentication(any()))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().rollen(alleAusserAdmin).build());

		// act + assert
		assertThrows(AccessDeniedException.class, () -> systemnachrichtController.create(mock(Authentication.class),
			new CreateSystemnachrichtCommand("Blubb")));
	}

	@Test
	void testCreate_administrator_creates() {
		// arrange
		when(benutzerResolverMock.fromAuthentication(any()))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR)).build());
		systemnachrichtRepository.save(new Systemnachricht(LocalDate.now(), "Test"));

		// act
		systemnachrichtController.create(mock(Authentication.class), new CreateSystemnachrichtCommand("Blubb"));

		// assert
		Optional<SystemnachrichtView> result = systemnachrichtController.get();
		assertThat(result).isPresent();
		assertThat(result.get().getText()).isEqualTo("Blubb");
	}

	@Test
	void testGet() {
		Set<Rolle> alleAusserAdmin = new HashSet<>(Set.of(Rolle.values()));
		alleAusserAdmin.remove(Rolle.RADVIS_ADMINISTRATOR);
		when(benutzerResolverMock.fromAuthentication(any()))
			.thenReturn(BenutzerTestDataProvider.defaultBenutzer().rollen(alleAusserAdmin).build());
		LocalDate vom = LocalDate.now();
		systemnachrichtRepository.save(new Systemnachricht(vom, "Test"));

		// act
		Optional<SystemnachrichtView> result = systemnachrichtController.get();

		// assert
		assertThat(result).isPresent();
		assertThat(result.get().getText()).isEqualTo("Test");
		assertThat(result.get().getVom()).isEqualTo(vom);
	}

}
