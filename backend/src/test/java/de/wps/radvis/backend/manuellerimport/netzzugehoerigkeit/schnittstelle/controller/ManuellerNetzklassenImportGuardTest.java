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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.schnittstelle.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.schnittstelle.command.StartNetzklassenImportSessionCommand;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class ManuellerNetzklassenImportGuardTest {
	ManuellerNetzklassenImportGuard manuellerNetzklassenImportGuard;

	@Mock
	BenutzerResolver benutzerResolver;
	@Mock
	VerwaltungseinheitService verwaltungseinheitService;

	@Mock
	Authentication authentication;

	private Verwaltungseinheit organisation;
	private Benutzer benutzerMitManuellerImportRecht;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		manuellerNetzklassenImportGuard = new ManuellerNetzklassenImportGuard(benutzerResolver,
			verwaltungseinheitService);

		organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(12345L).build();

		benutzerMitManuellerImportRecht = BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(organisation)
			.build();
	}

	@Test
	public void testStartNetzklassenImportSession_happyPath_keineException() {
		// arrange
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzerMitManuellerImportRecht);
		when(verwaltungseinheitService.resolve(12345L)).thenReturn(organisation);
		when(verwaltungseinheitService.istUebergeordnet(organisation, organisation)).thenReturn(true);

		StartNetzklassenImportSessionCommand command = StartNetzklassenImportSessionCommand.builder()
			.netzklasse(Netzklasse.RADSCHNELLVERBINDUNG)
			.organisation(organisation.getId())
			.build();

		// act + assert
		assertDoesNotThrow(
			() -> manuellerNetzklassenImportGuard.startNetzklassenImportSession(authentication, command, null));
	}

	@Test
	public void testStartNetzklassenImportSession_organisationNichtInZustaendigkeitsbereich_exception() {
		// arrange
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzerMitManuellerImportRecht);
		when(verwaltungseinheitService.resolve(12345L)).thenReturn(organisation);
		when(verwaltungseinheitService.istUebergeordnet(organisation, organisation)).thenReturn(false);

		StartNetzklassenImportSessionCommand command = StartNetzklassenImportSessionCommand.builder()
			.netzklasse(Netzklasse.RADSCHNELLVERBINDUNG)
			.organisation(organisation.getId())
			.build();

		// act + assert
		assertThatThrownBy(
			() -> manuellerNetzklassenImportGuard.startNetzklassenImportSession(authentication, command, null))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Die Organisation liegt nicht in Ihrem Zuständigkeitsbereich.");
	}

	@Test
	public void testStartNetzklassenImportSession_benutzerHatKeineImportBerechtigung_exception() {
		// arrange
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(
			BenutzerTestDataProvider.bearbeiterinVmRadnetzAdminInaktiv(organisation)
				.status(BenutzerStatus.AKTIV)
				.build());
		when(verwaltungseinheitService.resolve(12345L)).thenReturn(organisation);
		when(verwaltungseinheitService.istUebergeordnet(organisation, organisation)).thenReturn(false);

		StartNetzklassenImportSessionCommand command = StartNetzklassenImportSessionCommand.builder()
			.netzklasse(Netzklasse.RADSCHNELLVERBINDUNG)
			.organisation(organisation.getId())
			.build();

		// act + assert
		assertThatThrownBy(
			() -> manuellerNetzklassenImportGuard.startNetzklassenImportSession(authentication, command, null))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Sie haben nicht die Berechtigung Streckendaten zu importieren.");
	}

	@Test
	public void testStartNetzklassenImportSession_benutzerEditiertKreisnetzUndHatKeineBerechtigung_exception() {
		// arrange
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzerMitManuellerImportRecht);
		when(verwaltungseinheitService.resolve(12345L)).thenReturn(organisation);
		when(verwaltungseinheitService.istUebergeordnet(organisation, organisation)).thenReturn(true);

		StartNetzklassenImportSessionCommand command = StartNetzklassenImportSessionCommand.builder()
			.netzklasse(Netzklasse.KREISNETZ_ALLTAG)
			.organisation(organisation.getId())
			.build();

		// act + assert
		assertThatThrownBy(
			() -> manuellerNetzklassenImportGuard.startNetzklassenImportSession(authentication, command, null))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Sie sind nicht berechtigt, die Netzklasse Kreisnetz zu verändern.");
	}

	@Test
	public void testStartNetzklassenImportSession_benutzerEditiertKreisnetzUndHatBerechtigung_Keineexception() {
		// arrange
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(organisation)
			.rollen(Set.of(Rolle.RADWEGE_ERFASSERIN, Rolle.KREISNETZBEARBEITERIN))
			.build());
		when(verwaltungseinheitService.resolve(12345L)).thenReturn(organisation);
		when(verwaltungseinheitService.istUebergeordnet(organisation, organisation)).thenReturn(true);

		StartNetzklassenImportSessionCommand command = StartNetzklassenImportSessionCommand.builder()
			.netzklasse(Netzklasse.KREISNETZ_ALLTAG)
			.organisation(organisation.getId())
			.build();

		// act + assert
		assertDoesNotThrow(
			() -> manuellerNetzklassenImportGuard.startNetzklassenImportSession(authentication, command, null));
	}

	@Test
	public void testStartNetzklassenImportSession_benutzerEditiertRadNetzUndHatKeineBerechtigung_exception() {
		// arrange
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(benutzerMitManuellerImportRecht);
		when(verwaltungseinheitService.resolve(12345L)).thenReturn(organisation);
		when(verwaltungseinheitService.istUebergeordnet(organisation, organisation)).thenReturn(true);

		StartNetzklassenImportSessionCommand command = StartNetzklassenImportSessionCommand.builder()
			.netzklasse(Netzklasse.RADNETZ_ALLTAG)
			.organisation(organisation.getId())
			.build();

		// act + assert
		assertThatThrownBy(
			() -> manuellerNetzklassenImportGuard.startNetzklassenImportSession(authentication, command, null))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Sie sind nicht berechtigt, die Netzklasse RadNETZ zu verändern.");
	}

	@Test
	public void testStartNetzklassenImportSession_benutzerEditiertRadNetzUndHatBerechtigung_Keineexception() {
		// arrange
		when(benutzerResolver.fromAuthentication(authentication)).thenReturn(BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(organisation)
			.rollen(Set.of(Rolle.RADWEGE_ERFASSERIN, Rolle.RADNETZ_QUALITAETSSICHERIN))
			.build());
		when(verwaltungseinheitService.resolve(12345L)).thenReturn(organisation);
		when(verwaltungseinheitService.istUebergeordnet(organisation, organisation)).thenReturn(true);

		StartNetzklassenImportSessionCommand command = StartNetzklassenImportSessionCommand.builder()
			.netzklasse(Netzklasse.RADNETZ_ALLTAG)
			.organisation(organisation.getId())
			.build();

		// act + assert
		assertDoesNotThrow(
			() -> manuellerNetzklassenImportGuard.startNetzklassenImportSession(authentication, command, null));
	}
}
