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

package de.wps.radvis.backend.netz.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
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
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteAttributeCommand;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class NetzGuardTest {

	NetzGuard netzGuard;

	@Mock
	NetzService netzService;
	@Mock
	ZustaendigkeitsService zustaendigkeitsService;
	@Mock
	BenutzerResolver benutzerResolver;
	@Mock
	private Authentication authentication;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		netzGuard = new NetzGuard(netzService, zustaendigkeitsService, benutzerResolver);
	}

	@Test
	void testSaveKanteAllgemein_keineEditRechte_wirftException() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer()
			.rollen(Set.of(Rolle.RADVIS_BETRACHTER))
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KREISNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.KREISNETZ_FREIZEIT, Netzklasse.RADNETZ_FREIZEIT);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		List<SaveKanteAttributeCommand> commands = List.of(command);

		when(netzService.getKante(any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		// act + assert
		assertThatThrownBy(
			() -> netzGuard.saveKanteAllgemein(authentication, commands))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Sie haben nicht die Berechtigung Kanten zu bearbeiten.");
	}

	@Test
	void testSaveKanteAllgemein_keineAenderungen_wirftKeineException() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer()
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER))
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KREISNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.KREISNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		List<SaveKanteAttributeCommand> commands = List.of(command);

		when(netzService.getKante(any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		// act + assert
		assertDoesNotThrow(() -> netzGuard.saveKanteAllgemein(authentication, commands));
	}

	@Test
	void testSaveKanteAllgemein_KeineRadNetzOderKreisnetzAenderungen_wirftKeineException() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer()
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER))
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_FREIZEIT);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADSCHNELLVERBINDUNG);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		List<SaveKanteAttributeCommand> commands = List.of(command);

		when(netzService.getKante(any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		// act + assert
		assertDoesNotThrow(() -> netzGuard.saveKanteAllgemein(authentication, commands));
	}

	@Test
	void testSaveKanteAllgemein_keineRadNetzRechteUndRadNetzHinzugefuegt_wirftException() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		List<SaveKanteAttributeCommand> commands = List.of(command);

		when(netzService.getKante(any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		// act + assert
		assertThatThrownBy(
			() -> netzGuard.saveKanteAllgemein(authentication, commands))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Sie sind nicht berechtigt, die Netzklasse RadNETZ zu verändern.");
	}

	@Test
	void testSaveKanteAllgemein_keineRadNetzRechteUndRadNetzGeloescht_wirftException() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		List<SaveKanteAttributeCommand> commands = List.of(command);

		when(netzService.getKante(any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		// act + assert
		assertThatThrownBy(
			() -> netzGuard.saveKanteAllgemein(authentication, commands))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Sie sind nicht berechtigt, die Netzklasse RadNETZ zu verändern.");
	}

	@Test
	void testSaveKanteAllgemein_hatRadNetzRechteUndRadNetzGeandert_wirftKeineException() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer()
			.rollen(Set.of(Rolle.RADVERKEHRSBEAUFTRAGTER))
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.RADSCHNELLVERBINDUNG, Netzklasse.RADNETZ_FREIZEIT);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		List<SaveKanteAttributeCommand> commands = List.of(command);

		when(netzService.getKante(any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		// act + assert
		assertDoesNotThrow(() -> netzGuard.saveKanteAllgemein(authentication, commands));
	}

	@Test
	void testSaveKanteAllgemein_keineKreisnetzRechteUndKreisnetzHinzugefuegt_wirftException() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		List<SaveKanteAttributeCommand> commands = List.of(command);

		when(netzService.getKante(any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		// act + assert
		assertThatThrownBy(
			() -> netzGuard.saveKanteAllgemein(authentication, commands))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Sie sind nicht berechtigt, die Netzklasse Kreisnetz zu verändern.");
	}

	@Test
	void testSaveKanteAllgemein_keineKreisnetzRechteUndKreisnetzGeloescht_wirftException() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer().build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		List<SaveKanteAttributeCommand> commands = List.of(command);

		when(netzService.getKante(any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		// act + assert
		assertThatThrownBy(
			() -> netzGuard.saveKanteAllgemein(authentication, commands))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessage("Sie sind nicht berechtigt, die Netzklasse Kreisnetz zu verändern.");
	}

	@Test
	void testSaveKanteAllgemein_hatKreisnetzRechteUndKreisnetzGeandert_wirftKeineException() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer()
			.rollen(Set.of(Rolle.RADWEGE_ERFASSERIN, Rolle.KREISNETZBEARBEITERIN))
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KREISNETZ_ALLTAG);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.RADSCHNELLVERBINDUNG, Netzklasse.KREISNETZ_FREIZEIT);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		List<SaveKanteAttributeCommand> commands = List.of(command);

		when(netzService.getKante(any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		// act + assert
		assertDoesNotThrow(() -> netzGuard.saveKanteAllgemein(authentication, commands));
	}

	@Test
	void testSaveKanteAllgemein_MehrereKantenGleichzeitig_wirftKeineException() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer()
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER))
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_FREIZEIT);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADSCHNELLVERBINDUNG);

		SaveKanteAttributeCommand command1 = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		SaveKanteAttributeCommand command2 = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen)
			.kanteId(1L)
			.build();

		List<SaveKanteAttributeCommand> commands = List.of(command1, command2);

		when(netzService.getKante(any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		// act + assert
		assertDoesNotThrow(() -> netzGuard.saveKanteAllgemein(authentication, commands));
	}

	@Test
	void testDeleteRadVisKante_adminFremdeRadVisKante_true() {
		// arrange
		Benutzer admin = BenutzerTestDataProvider.admin(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.id(8L)
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(admin);

		Kante kanteRadVis = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).id(2L).build();
		when(netzService.getKante(2L)).thenReturn(kanteRadVis);
		when(netzService.wurdeAngelegtVon(eq(kanteRadVis), eq(admin))).thenReturn(false);

		// act & assert
		assertDoesNotThrow(() -> netzGuard.deleteRadVISKanteById(2L, authentication));
		assertThat(netzGuard.isLoeschenErlaubt(kanteRadVis, admin)).isTrue();
	}

	@Test
	void testDeleteRadVisKante_adminDlmKante_false() {
		// arrange
		Benutzer admin = BenutzerTestDataProvider.admin(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.id(8L)
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(admin);

		Kante kanteDLM = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM).id(2L).build();
		when(netzService.getKante(2L)).thenReturn(kanteDLM);
		when(netzService.wurdeAngelegtVon(eq(kanteDLM), eq(admin))).thenReturn(false);

		// act & assert
		assertThatThrownBy(() -> netzGuard.deleteRadVISKanteById(2L, authentication)).isInstanceOf(
			AccessDeniedException.class);
		assertThat(netzGuard.isLoeschenErlaubt(kanteDLM, admin)).isFalse();
	}

	@Test
	void testDeleteRadVisKante_nonAdminDlmKante_false() {
		// arrange
		Benutzer admin = BenutzerTestDataProvider.kreiskoordinator(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.id(8L)
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(admin);

		Kante kanteDLM = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.DLM).id(2L).build();
		when(netzService.getKante(2L)).thenReturn(kanteDLM);
		when(netzService.wurdeAngelegtVon(eq(kanteDLM), eq(admin))).thenReturn(false);

		// act & assert
		assertThatThrownBy(() -> netzGuard.deleteRadVISKanteById(2L, authentication)).isInstanceOf(
			AccessDeniedException.class);
		assertThat(netzGuard.isLoeschenErlaubt(kanteDLM, admin)).isFalse();
	}

	@Test
	void testDeleteRadVisKante_nonAdminFremdeRadVisKante_false() {
		// arrange
		Benutzer kreiskoordinator = BenutzerTestDataProvider.kreiskoordinator(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.id(8L)
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(kreiskoordinator);

		Kante kanteRadVis = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).id(2L).build();
		when(netzService.getKante(2L)).thenReturn(kanteRadVis);
		when(netzService.wurdeAngelegtVon(eq(kanteRadVis), eq(kreiskoordinator))).thenReturn(false);

		// act & assert
		assertThatThrownBy(() -> netzGuard.deleteRadVISKanteById(2L, authentication)).isInstanceOf(
			AccessDeniedException.class);
		assertThat(netzGuard.isLoeschenErlaubt(kanteRadVis, kreiskoordinator)).isFalse();
	}

	@Test
	void testDeleteRadVisKante_nonAdminEigeneRadVisKante_true() {
		// arrange
		Benutzer kreiskoordinator = BenutzerTestDataProvider.kreiskoordinator(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.id(8L)
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(kreiskoordinator);

		Kante kanteRadVis = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).id(2L).build();
		when(netzService.getKante(2L)).thenReturn(kanteRadVis);
		when(netzService.wurdeAngelegtVon(eq(kanteRadVis), eq(kreiskoordinator))).thenReturn(true);

		// act & assert
		assertDoesNotThrow(() -> netzGuard.deleteRadVISKanteById(2L, authentication));
		assertThat(netzGuard.isLoeschenErlaubt(kanteRadVis, kreiskoordinator)).isTrue();
	}
}
