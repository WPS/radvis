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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveFuehrungsformAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveFuehrungsformAttributeCommand;
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

	private Benutzer benutzer;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		netzGuard = new NetzGuard(netzService, zustaendigkeitsService, benutzerResolver);
		benutzer = BenutzerTestDataProvider
			.radwegeErfasserinKommuneKreis(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
			.build();
		when(benutzerResolver.fromAuthentication(eq(authentication))).thenReturn(benutzer);
	}

	@Test
	void RadNetzKlasseHinzugefuegtOderEntfernt_NichtsVeraendert_false() {

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ,
			Netzklasse.KOMMUNALNETZ_ALLTAG);

		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG,
			Netzklasse.RADNETZ_ZIELNETZ);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen).kanteId(1l)
			.build();

		when(netzService.getKante(Mockito.any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		assertDoesNotThrow(() -> netzGuard.authorizeRadNetzVerlegung(command.getKanteId(),
			command.getNetzklassen(), benutzer));
	}

	@Test
	void RadNetzKlasseHinzugefuegtOderEntfernt_AenderungenAberKeineRadNetzAenderungen_keinRAdnetzBeteiltigt_false() {

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KREISNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.KREISNETZ_FREIZEIT, Netzklasse.KOMMUNALNETZ_ALLTAG);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen).kanteId(1l)
			.build();

		when(netzService.getKante(Mockito.any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		assertDoesNotThrow(() -> netzGuard.authorizeRadNetzVerlegung(command.getKanteId(),
			command.getNetzklassen(), benutzer));
	}

	@Test
	void RadNetzKlasseHinzugefuegtOderEntfernt_AenderungenAberKeineRadNetzAenderungen_false() {

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.KREISNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG,
			Netzklasse.RADNETZ_ALLTAG);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.KREISNETZ_FREIZEIT, Netzklasse.RADNETZ_ALLTAG,
			Netzklasse.KOMMUNALNETZ_ALLTAG);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen).kanteId(1l)
			.build();

		when(netzService.getKante(Mockito.any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		assertDoesNotThrow(() -> netzGuard.authorizeRadNetzVerlegung(command.getKanteId(),
			command.getNetzklassen(), benutzer));
	}

	@Test
	void RadNetzKlasseHinzugefuegtOderEntfernt_RadNetzDurchRadNetzErsetzt_true() {

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen).kanteId(1l)
			.build();

		when(netzService.getKante(Mockito.any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		assertThrows(AccessDeniedException.class,
			() -> netzGuard.authorizeRadNetzVerlegung(command.getKanteId(),
				command.getNetzklassen(), benutzer));
	}

	@Test
	void RadNetzKlasseHinzugefuegtOderEntfernt_RadNetzEntfernt_true() {

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen).kanteId(1l)
			.build();

		when(netzService.getKante(Mockito.any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		assertThrows(AccessDeniedException.class,
			() -> netzGuard.authorizeRadNetzVerlegung(command.getKanteId(),
				command.getNetzklassen(), benutzer));
	}

	@Test
	void RadNetzKlasseHinzugefuegtOderEntfernt_RadNetzhinzugefuegt_true() {

		Set<Netzklasse> netzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG);
		Set<Netzklasse> neueNetzklassen = Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ);

		SaveKanteAttributeCommand command = SaveKanteAttributeCommand.builder()
			.netzklassen(neueNetzklassen).kanteId(1l)
			.build();

		when(netzService.getKante(Mockito.any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues()
				.kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(netzklassen).build())
				.build());

		assertThrows(AccessDeniedException.class,
			() -> netzGuard.authorizeRadNetzVerlegung(command.getKanteId(),
				command.getNetzklassen(), benutzer));
	}
}
