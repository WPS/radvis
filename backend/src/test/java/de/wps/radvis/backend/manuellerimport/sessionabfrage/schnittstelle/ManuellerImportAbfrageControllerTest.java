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

package de.wps.radvis.backend.manuellerimport.sessionabfrage.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AttributeImportFormat;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity.NetzklasseImportSession;
import de.wps.radvis.backend.manuellerimport.sessionabfrage.schnittstelle.controller.ManuellerImportAbfrageController;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class ManuellerImportAbfrageControllerTest {
	@Mock
	private ManuellerImportService manuellerImportService;
	@Mock
	private BenutzerResolver benutzerResolver;
	private ManuellerImportAbfrageController manuellerImportAbfrageController;
	private Benutzer defaultBenutzer;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		manuellerImportAbfrageController = new ManuellerImportAbfrageController(manuellerImportService,
			benutzerResolver);
		defaultBenutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		when(benutzerResolver.fromAuthentication(any())).thenReturn(defaultBenutzer);
	}

	@Test
	public void existsSession_noSession() {
		// arrange
		when(manuellerImportService.findImportSessionFromBenutzer(defaultBenutzer)).thenReturn(Optional.empty());

		// act
		boolean existsSession = manuellerImportAbfrageController.existsSession(mock(Authentication.class), null);

		// assert
		assertThat(existsSession).isFalse();
	}

	@Test
	public void existsSession_noType_hasSession() {
		// arrange
		when(manuellerImportService.findImportSessionFromBenutzer(defaultBenutzer))
			.thenReturn(Optional.of(new AttributeImportSession(defaultBenutzer,
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), List.of(),
				AttributeImportFormat.LUBW)));

		// act
		boolean existsSession = manuellerImportAbfrageController.existsSession(mock(Authentication.class), null);

		// assert
		assertThat(existsSession).isTrue();
	}

	@Test
	public void existsSession_attribute_correctSessionType() {
		// arrange
		when(manuellerImportService.findImportSessionFromBenutzer(defaultBenutzer))
			.thenReturn(Optional.of(new AttributeImportSession(defaultBenutzer,
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), List.of(),
				AttributeImportFormat.LUBW)));

		// act
		boolean existsSession = manuellerImportAbfrageController.existsSession(mock(Authentication.class),
			ImportTyp.ATTRIBUTE_UEBERNEHMEN);

		// assert
		assertThat(existsSession).isTrue();
	}

	@Test
	public void existsSession_attribute_wrongSessionType() {
		// arrange
		when(manuellerImportService.findImportSessionFromBenutzer(defaultBenutzer))
			.thenReturn(Optional.of(new AttributeImportSession(defaultBenutzer,
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(), List.of(),
				AttributeImportFormat.LUBW)));

		// act
		boolean existsSession = manuellerImportAbfrageController.existsSession(mock(Authentication.class),
			ImportTyp.NETZKLASSE_ZUWEISEN);

		// assert
		assertThat(existsSession).isFalse();
	}

	@Test
	public void existsSession_netzklasse_correctSessionType() {
		// arrange
		when(manuellerImportService.findImportSessionFromBenutzer(defaultBenutzer))
			.thenReturn(Optional.of(new NetzklasseImportSession(defaultBenutzer,
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(),
				Netzklasse.KOMMUNALNETZ_ALLTAG)));

		// act
		boolean existsSession = manuellerImportAbfrageController.existsSession(mock(Authentication.class),
			ImportTyp.NETZKLASSE_ZUWEISEN);

		// assert
		assertThat(existsSession).isTrue();
	}

	@Test
	public void existsSession_netzklasse_wrongSessionType() {
		// arrange
		when(manuellerImportService.findImportSessionFromBenutzer(defaultBenutzer))
			.thenReturn(Optional.of(new NetzklasseImportSession(defaultBenutzer,
				VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(),
				Netzklasse.KOMMUNALNETZ_ALLTAG)));

		// act
		boolean existsSession = manuellerImportAbfrageController.existsSession(mock(Authentication.class),
			ImportTyp.ATTRIBUTE_UEBERNEHMEN);

		// assert
		assertThat(existsSession).isFalse();
	}
}
