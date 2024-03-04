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

package de.wps.radvis.backend.administration.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.exception.BenutzerIstNichtRegistriertException;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

class BenutzerGuardTest {

	@Mock
	BenutzerService benutzerService;

	@Mock
	BenutzerResolver benutzerResolver;

	BenutzerGuard benutzerGuard;

	Verwaltungseinheit bundesland;
	Verwaltungseinheit regierungsbezirk;
	Verwaltungseinheit gemeinde;
	Verwaltungseinheit andererRegierungsbezirk;
	Verwaltungseinheit andereGemeinde;

	Benutzer adminBundesland;
	Benutzer benutzerRegierungsbezirk;
	Benutzer benutzerAndererRegierungsbezirk;
	Benutzer benutzerGemeinde;
	Benutzer benutzerAndereGemeinde;
	Benutzer benutzerDerEditiertWird;

	@BeforeEach
	public void setUp() throws BenutzerIstNichtRegistriertException {
		MockitoAnnotations.openMocks(this);

		bundesland = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(101L).name("Bundesland")
			.organisationsArt(OrganisationsArt.BUNDESLAND).build();
		regierungsbezirk = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(102L)
			.name("Regierungsbezirk")
			.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK)
			.uebergeordneteOrganisation(bundesland).build();
		gemeinde = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(103L).name("Gemeinde")
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.uebergeordneteOrganisation(regierungsbezirk).build();
		andererRegierungsbezirk = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(104L)
			.name("Anderer Regierungsbezirk")
			.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK)
			.uebergeordneteOrganisation(bundesland).build();
		andereGemeinde = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(105L)
			.name("Andere Gemeinde")
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.uebergeordneteOrganisation(andererRegierungsbezirk).build();

		adminBundesland = BenutzerTestDataProvider.admin(bundesland).id(1L).build();
		benutzerRegierungsbezirk = BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(regierungsbezirk).id(2L)
			.build();
		benutzerAndererRegierungsbezirk = BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(
			andererRegierungsbezirk).id(3L).build();
		benutzerGemeinde = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(gemeinde).id(4L).build();
		benutzerAndereGemeinde = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(andereGemeinde).id(5L).build();

		when(benutzerService.findByOrganisationAndStatus(bundesland, BenutzerStatus.AKTIV))
			.thenReturn(List.of(adminBundesland));
		when(benutzerService.findByOrganisationAndStatus(regierungsbezirk, BenutzerStatus.AKTIV))
			.thenReturn(List.of(benutzerRegierungsbezirk));
		when(benutzerService.findByOrganisationAndStatus(gemeinde, BenutzerStatus.AKTIV))
			.thenReturn(List.of(benutzerGemeinde));
		when(benutzerService.findByOrganisationAndStatus(andererRegierungsbezirk, BenutzerStatus.AKTIV))
			.thenReturn(List.of(benutzerAndererRegierungsbezirk));
		when(benutzerService.findByOrganisationAndStatus(andereGemeinde, BenutzerStatus.AKTIV))
			.thenReturn(List.of(benutzerAndereGemeinde));

		when(benutzerService.getRadvisAdmins()).thenReturn(List.of(adminBundesland));

		when(benutzerService.ermittleVergaberechteFuerRolle(any())).thenCallRealMethod();

		benutzerDerEditiertWird = BenutzerTestDataProvider.defaultBenutzer().build();
		when(benutzerService.getBenutzer(1L)).thenReturn(benutzerDerEditiertWird);
		benutzerGuard = new BenutzerGuard(benutzerService, benutzerResolver);
	}

	@Test
	void notAllowedThrowsAccessDenied() {
		when(benutzerResolver.fromAuthentication(any())).thenReturn(BenutzerTestDataProvider
			.defaultBenutzer()
			.rollen(Set.of(Rolle.RADNETZ_QUALITAETSSICHERIN))
			.build());
		assertThrows(AccessDeniedException.class, () -> benutzerGuard.benutzerStatusAendern(null, 1L, 1L));

		when(benutzerResolver.fromAuthentication(any())).thenReturn(BenutzerTestDataProvider
			.defaultBenutzer()
			.rollen(Set.of(Rolle.LGL_MITARBEITERIN))
			.build());
		assertThrows(AccessDeniedException.class, () -> benutzerGuard.benutzerStatusAendern(null, 1L, 1L));

		when(benutzerResolver.fromAuthentication(any())).thenReturn(BenutzerTestDataProvider
			.defaultBenutzer()
			.rollen(Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN))
			.build());
		assertThrows(AccessDeniedException.class, () -> benutzerGuard.benutzerStatusAendern(null, 1L, 1L));
	}

	@Test
	void allowedHasAccess() {
		Benutzer admin = BenutzerTestDataProvider
			.defaultBenutzer()
			.rollen(Set.of(Rolle.RADVIS_ADMINISTRATOR))
			.build();
		Benutzer bearbeiterinAdmin = BenutzerTestDataProvider
			.defaultBenutzer()
			.rollen(Set.of(Rolle.KREISKOORDINATOREN))
			.build();

		when(benutzerService.findAdminaufSelberEbene(any())).thenReturn(List.of(admin));

		when(benutzerResolver.fromAuthentication(any())).thenReturn(admin);
		assertDoesNotThrow(() -> benutzerGuard.benutzerStatusAendern(null, 1L, 1L));

		when(benutzerResolver.fromAuthentication(any())).thenReturn(bearbeiterinAdmin);
		assertDoesNotThrow(() -> benutzerGuard.benutzerStatusAendern(null, 1L, 1L));
	}

	@Test
	void pruefeBearbeiterIstZustaendigFuerBenutzer_benutzerRegierungsbezirk_benutzerBundesland_Zustaendig() {
		assertTrue(this.benutzerGuard.pruefeBearbeiterIstAutorisiertFuerBenutzer(adminBundesland, adminBundesland));
	}

	@Test
	void pruefeBearbeiterIstZustaendigFuerBenutzer_benutzerRegierungsbezirk_benutzerGemeinde_Zustaendig() {
		assertTrue(this.benutzerGuard.pruefeBearbeiterIstAutorisiertFuerBenutzer(adminBundesland,
			benutzerGemeinde));
	}

	@Test
	void pruefeBearbeiterIstZustaendigFuerBenutzer_benutzerRegierungsbezirk_benutzerRegierungsbezirk_Zustaendig() {
		// arrange
		when(benutzerService.findAdminaufSelberEbene(benutzerRegierungsbezirk.getOrganisation())).thenReturn(
				List.of(benutzerRegierungsbezirk))
			.thenReturn(List.of(benutzerRegierungsbezirk));

		// act
		assertTrue(this.benutzerGuard.pruefeBearbeiterIstAutorisiertFuerBenutzer(benutzerRegierungsbezirk,
			benutzerRegierungsbezirk));
	}

	@Test
	void pruefeBearbeiterIstZustaendigFuerBenutzer_benutzerRegierungsbezirk_benutzergemeinde_Zustaendig() {
		// arrange
		when(benutzerService.findAdminaufSelberEbene(benutzerGemeinde.getOrganisation())).thenReturn(List.of());
		when(benutzerService.findAdminaufSelberEbene(
			benutzerGemeinde.getOrganisation().getUebergeordneteVerwaltungseinheit().get()))
			.thenReturn(List.of(benutzerRegierungsbezirk));

		// act
		assertTrue(this.benutzerGuard.pruefeBearbeiterIstAutorisiertFuerBenutzer(benutzerRegierungsbezirk,
			benutzerGemeinde));
	}

	@Test
	void pruefeBearbeiterIstZustaendigFuerBenutzer_benutzerRegierungsbezirk_benutzerGemeinde_NichtZustaendig() {
		// arrange
		when(benutzerService.findAdminaufSelberEbene(benutzerRegierungsbezirk.getOrganisation())).thenReturn(
			List.of());

		// assert
		assertThat(benutzerGuard.pruefeBearbeiterIstAutorisiertFuerBenutzer(benutzerGemeinde,
			benutzerRegierungsbezirk)).isFalse();
	}

	@Test
	void pruefeBearbeiterIstZustaendigFuerBenutzer_benutzerAndereGemeinde_benutzerGemeinde_NichtZustaendig() {
		// arrange
		when(benutzerService.findAdminaufSelberEbene(benutzerGemeinde.getOrganisation())).thenReturn(List.of());

		// assert
		assertThat(benutzerGuard.pruefeBearbeiterIstAutorisiertFuerBenutzer(benutzerAndereGemeinde,
			benutzerGemeinde)).isFalse();
	}

	@Test
	void pruefeBearbeiterIstZustaendigFuerBenutzer_benutzerRegierungsbezirk_benutzerAndererRegierungsbezirk_NichtZustaendig() {
		// arrange
		when(benutzerService.findAdminaufSelberEbene(
			benutzerAndererRegierungsbezirk.getOrganisation())).thenReturn(
			List.of());

		// assert
		assertThat(benutzerGuard.pruefeBearbeiterIstAutorisiertFuerBenutzer(benutzerRegierungsbezirk,
			benutzerAndererRegierungsbezirk)).isFalse();
	}

	@Test
	void pruefeBearbeiterIstZustaendigFuerBenutzer_benutzerRegierungsbezirk_benutzerAndereGemeinde_NichtZustaendig() {
		// arrange
		when(benutzerService.findAdminaufSelberEbene(benutzerAndereGemeinde.getOrganisation())).thenReturn(
			List.of());

		// assert
		assertThat(benutzerGuard.pruefeBearbeiterIstAutorisiertFuerBenutzer(benutzerRegierungsbezirk,
			benutzerAndereGemeinde)).isFalse();
	}

	@Test
	void testErmittleGeaenderterollen_eineHinzu() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.externerDienstleister(gemeinde).build();
		final Set<Rolle> neueRollen = Set.of(Rolle.EXTERNER_DIENSTLEISTER,
			Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN);

		// act
		Set<Rolle> result = benutzerGuard.ermittleGeaenderterollen(benutzer, neueRollen);

		// assert
		assertThat(result).containsExactlyInAnyOrder(
			Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN);
	}

	@Test
	void testErmittleGeaenderterollen_eineWeg() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.radwegeErfasserinKommuneKreisInaktiv(gemeinde)
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER, Rolle.RADVERKEHRSBEAUFTRAGTER))
			.build();
		final Set<Rolle> neueRollen = Set.of(Rolle.EXTERNER_DIENSTLEISTER);

		// act
		Set<Rolle> result = benutzerGuard.ermittleGeaenderterollen(benutzer, neueRollen);

		// assert
		assertThat(result).containsExactlyInAnyOrder(
			Rolle.RADVERKEHRSBEAUFTRAGTER);
	}

	@Test
	void testErmittleGeaenderterollen_eineHinzuUndEineWeg() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.externerDienstleister(gemeinde)
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER, Rolle.RADVERKEHRSBEAUFTRAGTER))
			.build();
		final Set<Rolle> neueRollen = Set.of(Rolle.EXTERNER_DIENSTLEISTER,
			Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN);

		// act
		Set<Rolle> result = benutzerGuard.ermittleGeaenderterollen(benutzer, neueRollen);

		// assert
		assertThat(result).containsExactlyInAnyOrder(
			Rolle.RADVERKEHRSBEAUFTRAGTER, Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN);
	}

	@Test
	void testErmittleGeaenderterollen_keineAenderung() {
		// arrange
		Benutzer benutzer = BenutzerTestDataProvider.externerDienstleister(gemeinde)
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER, Rolle.RADVERKEHRSBEAUFTRAGTER))
			.build();
		final Set<Rolle> neueRollen = Set.of(Rolle.EXTERNER_DIENSTLEISTER, Rolle.RADVERKEHRSBEAUFTRAGTER);

		// act
		Set<Rolle> result = benutzerGuard.ermittleGeaenderterollen(benutzer, neueRollen);

		// assert
		assertThat(result).isEmpty();
	}

	@Test
	void testDarfBenutzerRollenAendern_darf_hinzufuegen() {
		// arrange
		Verwaltungseinheit regierungsbezirk = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(102L)
			.name("Regierungsbezirk")
			.organisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)
			.uebergeordneteOrganisation(bundesland).build();

		Benutzer aktiverBenutzer = BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(regierungsbezirk)
			.build();
		Benutzer andererBenutzer = BenutzerTestDataProvider.radwegeErfasserinKommuneKreis(regierungsbezirk)
			.build();

		// act + assert
		assertThat(benutzerGuard.rollenDieDerBenutzerAenderWillAberNichtDarf(aktiverBenutzer,
			// RADNETZ_ERFASSERIN_REGIERUNGSBEZIRK kommt hinzu
			andererBenutzer, Set.of(Rolle.RADWEGE_ERFASSERIN, Rolle.RADVERKEHRSBEAUFTRAGTER))).isEmpty();
	}

	@Test
	void testDarfBenutzerRollenAendern_darf_entfernen() {
		// arrange
		Verwaltungseinheit regierungsbezirk = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(102L)
			.name("Regierungsbezirk")
			.organisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)
			.uebergeordneteOrganisation(bundesland).build();
		Benutzer aktiverBenutzer = BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(regierungsbezirk)
			.build();
		Benutzer andererBenutzer = BenutzerTestDataProvider.externerDienstleister(regierungsbezirk)
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER, Rolle.RADVERKEHRSBEAUFTRAGTER))
			.build();

		// act + assert
		assertThat(benutzerGuard.rollenDieDerBenutzerAenderWillAberNichtDarf(aktiverBenutzer,
			// RADNETZ_ERFASSERIN_REGIERUNGSBEZIRK wird entfernt
			andererBenutzer, Set.of(Rolle.EXTERNER_DIENSTLEISTER))).isEmpty();
	}

	@Test
	void testDarfBenutzerRollenAendern_darf_veraendern() {
		// arrange
		Verwaltungseinheit regierungsbezirk = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(102L)
			.name("Regierungsbezirk")
			.organisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)
			.uebergeordneteOrganisation(bundesland).build();

		Benutzer aktiverBenutzer = BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(regierungsbezirk)
			.build();

		Benutzer andererBenutzer = BenutzerTestDataProvider.externerDienstleister(regierungsbezirk)
			.rollen(Set.of(Rolle.EXTERNER_DIENSTLEISTER, Rolle.RADVERKEHRSBEAUFTRAGTER))
			.build();

		// act + assert
		assertThat(benutzerGuard.rollenDieDerBenutzerAenderWillAberNichtDarf(aktiverBenutzer,
			// RADNETZ_ERFASSERIN_REGIERUNGSBEZIRK wird durch KREISKOORDINATOREN ersetzt
			andererBenutzer, Set.of(Rolle.EXTERNER_DIENSTLEISTER, Rolle.KREISKOORDINATOREN))).isEmpty();
	}

	@Test
	void testDarfBenutzerRollenAendern_darfNicht_hinzufuegen() {
		// arrange
		Verwaltungseinheit regierungsbezirk = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(102L)
			.name("Regierungsbezirk")
			.organisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)
			.uebergeordneteOrganisation(bundesland).build();
		Benutzer aktiverBenutzer = BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(regierungsbezirk)
			.build();
		Benutzer andererBenutzer = BenutzerTestDataProvider.kreiskoordinator(regierungsbezirk).build();

		// act + assert
		assertThat(benutzerGuard.rollenDieDerBenutzerAenderWillAberNichtDarf(aktiverBenutzer,
			// RADNETZ_ERFASSERIN_REGIERUNGSBEZIRK wird durch KREISKOORDINATOREN ersetzt
			andererBenutzer, Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN, Rolle.KREISKOORDINATOREN)))
			.contains(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN);
	}

	@Test
	void testDarfBenutzerRollenAendern_darfNicht_entfernen() {
		// arrange
		Verwaltungseinheit regierungsbezirk = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(102L)
			.name("Regierungsbezirk")
			.organisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)
			.uebergeordneteOrganisation(bundesland).build();
		Benutzer aktiverBenutzer = BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(regierungsbezirk)
			.build();
		Benutzer andererBenutzer = BenutzerTestDataProvider.externerDienstleister(regierungsbezirk)
			.rollen(Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN, Rolle.KREISKOORDINATOREN))
			.build();

		// act + assert
		assertThat(benutzerGuard.rollenDieDerBenutzerAenderWillAberNichtDarf(aktiverBenutzer,
			// RADNETZ_ERFASSERIN_REGIERUNGSBEZIRK wird durch KREISKOORDINATOREN ersetzt
			andererBenutzer, Set.of(Rolle.KREISKOORDINATOREN))).contains(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN);
	}

	@Test
	void testDarfBenutzerRollenAendern_darfNicht_veraendern() {
		// arrange
		Verwaltungseinheit regierungsbezirk = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(102L)
			.name("Regierungsbezirk")
			.organisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)
			.uebergeordneteOrganisation(bundesland).build();
		Benutzer aktiverBenutzer = BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(regierungsbezirk)
			.build();
		Benutzer andererBenutzer = BenutzerTestDataProvider.externerDienstleister(regierungsbezirk)
			.rollen(Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN, Rolle.KREISKOORDINATOREN))
			.build();

		// act + assert
		assertThat(benutzerGuard.rollenDieDerBenutzerAenderWillAberNichtDarf(aktiverBenutzer,
			// RADNETZ_ERFASSERIN_REGIERUNGSBEZIRK wird durch KREISKOORDINATOREN ersetzt
			andererBenutzer, Set.of(Rolle.KREISKOORDINATOREN, Rolle.RADVERKEHRSBEAUFTRAGTER)))
			.contains(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN);
	}

	@Test
	void testDarfBenutzerRollenAendern_darf_keineAenderung() {
		// arrange
		Verwaltungseinheit regierungsbezirk = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(102L)
			.name("Regierungsbezirk")
			.organisationsArt(
				OrganisationsArt.REGIERUNGSBEZIRK)
			.uebergeordneteOrganisation(bundesland).build();
		Benutzer aktiverBenutzer = BenutzerTestDataProvider.radnetzErfasserinRegierungsbezirk(regierungsbezirk)
			.build();
		Benutzer andererBenutzer = BenutzerTestDataProvider.externerDienstleister(regierungsbezirk)
			.rollen(Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN, Rolle.KREISKOORDINATOREN))
			.build();

		// act + assert
		assertThat(benutzerGuard.rollenDieDerBenutzerAenderWillAberNichtDarf(aktiverBenutzer,
			// RADNETZ_ERFASSERIN_REGIERUNGSBEZIRK wird durch KREISKOORDINATOREN ersetzt
			andererBenutzer, Set.of(Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN, Rolle.EXTERNER_DIENSTLEISTER)))
			.isEmpty();
	}

}
