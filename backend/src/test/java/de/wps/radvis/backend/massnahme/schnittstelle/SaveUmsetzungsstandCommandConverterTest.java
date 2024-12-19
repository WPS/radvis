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

package de.wps.radvis.backend.massnahme.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerAbweichungZumMassnahmenblatt;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerNichtUmsetzungDerMassnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.PruefungQualitaetsstandardsErfolgt;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class SaveUmsetzungsstandCommandConverterTest {

	@Mock
	private BenutzerResolver benutzerresolver;

	private SaveUmsetzungsstandCommandConverter commandConverter;

	private Benutzer testBenutzer;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		Verwaltungseinheit testOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(500L)
			.name("Coole Organisation")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();
		testBenutzer = BenutzerTestDataProvider.admin(testOrganisation).build();

		when(benutzerresolver.fromAuthentication(any())).thenReturn(testBenutzer);
		commandConverter = new SaveUmsetzungsstandCommandConverter(benutzerresolver);
	}

	@ParameterizedTest
	@EnumSource(
		value = GrundFuerNichtUmsetzungDerMassnahme.class, names = { "KAPAZITAETSGRUENDE" })
	@NullSource
	void convert_Umsetzungsstand(GrundFuerNichtUmsetzungDerMassnahme grundFuerNichtUmsetzungDerMassnahme) {
		// arrange
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
		LocalDateTime letzteAenderungInitial = LocalDateTime.now().minusDays(1);

		umsetzungsstand.update(true,
			letzteAenderungInitial,
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GrundFuerAbweichungZumMassnahmenblatt.SONSTIGER_GRUND,
			PruefungQualitaetsstandardsErfolgt.JA_STANDARDS_EINGEHALTEN,
			"Beschreibung abweichender Massnahme",
			20000L,
			GrundFuerNichtUmsetzungDerMassnahme.LAUT_VERKEHRSSCHAU_NICHT_ERFORDERLICH,
			"Anmerkung", Umsetzungsstatus.PLANUNG);

		SaveUmsetzungsstandCommand command = SaveUmsetzungsstandCommand.builder()
			.umsetzungGemaessMassnahmenblatt(false)
			.grundFuerAbweichungZumMassnahmenblatt(GrundFuerAbweichungZumMassnahmenblatt.RADNETZ_WURDE_VERLEGT)
			.pruefungQualitaetsstandardsErfolgt(PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH)
			.beschreibungAbweichenderMassnahme("neue Beschreibung abweichender Massnahme")
			.kostenDerMassnahme(30000L)
			.grundFuerNichtUmsetzungDerMassnahme(grundFuerNichtUmsetzungDerMassnahme)
			.anmerkung("neue Anmerkung")
			.build();

		// act
		Authentication authentication = mock(Authentication.class);
		commandConverter.apply(authentication, command, umsetzungsstand,
			MassnahmeTestDataProvider.withDefaultValues().umsetzungsstatus(Umsetzungsstatus.IDEE).build());

		// assert
		assertThat(umsetzungsstand.getUmsetzungsstandStatus()).isEqualTo(UmsetzungsstandStatus.AKTUALISIERT);
		assertThat(umsetzungsstand.isUmsetzungGemaessMassnahmenblatt()).isFalse();
		assertThat(umsetzungsstand.getGrundFuerAbweichungZumMassnahmenblatt()).isEqualTo(
			GrundFuerAbweichungZumMassnahmenblatt.RADNETZ_WURDE_VERLEGT);
		assertThat(umsetzungsstand.getPruefungQualitaetsstandardsErfolgt()).isEqualTo(
			PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH);
		assertThat(umsetzungsstand.getBeschreibungAbweichenderMassnahme()).isEqualTo(
			"neue Beschreibung abweichender Massnahme");
		assertThat(umsetzungsstand.getKostenDerMassnahme()).isEqualTo(30000L);
		assertThat(umsetzungsstand.getGrundFuerNichtUmsetzungDerMassnahme()).isEqualTo(
			grundFuerNichtUmsetzungDerMassnahme);
		assertThat(umsetzungsstand.getBenutzerLetzteAenderung()).isEqualTo(testBenutzer);
		assertThat(umsetzungsstand.getLetzteAenderung()).isAfter(letzteAenderungInitial);

	}

	@Test
	void convert_Umsetzungsstand_MassnahmeStoriniertOderUmgesetztUndKeineAktualisierungAngefordert_WirftRequireViolation() {
		// arrange
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
		LocalDateTime letzteAenderungInitial = LocalDateTime.now().minusDays(1);

		umsetzungsstand.update(true,
			letzteAenderungInitial,
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GrundFuerAbweichungZumMassnahmenblatt.SONSTIGER_GRUND,
			PruefungQualitaetsstandardsErfolgt.JA_STANDARDS_EINGEHALTEN,
			"Beschreibung abweichender Massnahme",
			20000L,
			GrundFuerNichtUmsetzungDerMassnahme.LAUT_VERKEHRSSCHAU_NICHT_ERFORDERLICH,
			"Anmerkung", Umsetzungsstatus.PLANUNG);

		SaveUmsetzungsstandCommand command = SaveUmsetzungsstandCommand.builder()
			.umsetzungGemaessMassnahmenblatt(false)
			.grundFuerAbweichungZumMassnahmenblatt(GrundFuerAbweichungZumMassnahmenblatt.RADNETZ_WURDE_VERLEGT)
			.pruefungQualitaetsstandardsErfolgt(PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH)
			.beschreibungAbweichenderMassnahme("neue Beschreibung abweichender Massnahme")
			.kostenDerMassnahme(30000L)
			.grundFuerNichtUmsetzungDerMassnahme(GrundFuerNichtUmsetzungDerMassnahme.NOCH_IN_PLANUNG_UMSETZUNG)
			.anmerkung("neue Anmerkung")
			.build();

		// act & assert
		Authentication authentication = mock(Authentication.class);
		assertThatThrownBy(
			() -> commandConverter.apply(authentication, command, umsetzungsstand,
				MassnahmeTestDataProvider.withDefaultValues().umsetzungsstatus(Umsetzungsstatus.STORNIERT).build()))
					.isInstanceOf(RequireViolation.class);
		assertThatThrownBy(
			() -> commandConverter.apply(authentication, command, umsetzungsstand,
				MassnahmeTestDataProvider.withDefaultValues().umsetzungsstatus(Umsetzungsstatus.UMGESETZT).build()))
					.isInstanceOf(RequireViolation.class);
	}

	@Test
	void convert_Umsetzungsstand_MassnahmeUmgesetztOderStorniertUndAktualisierungAngefordertUndKeinGrundAngegeben_WirftRequireViolation() {
		// arrange
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
		umsetzungsstand.fordereAktualisierungAn();

		SaveUmsetzungsstandCommand command = SaveUmsetzungsstandCommand.builder()
			.umsetzungGemaessMassnahmenblatt(false)
			.grundFuerAbweichungZumMassnahmenblatt(GrundFuerAbweichungZumMassnahmenblatt.RADNETZ_WURDE_VERLEGT)
			.pruefungQualitaetsstandardsErfolgt(PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH)
			.beschreibungAbweichenderMassnahme("neue Beschreibung abweichender Massnahme")
			.kostenDerMassnahme(30000L)
			.grundFuerNichtUmsetzungDerMassnahme(null)
			.anmerkung("neue Anmerkung")
			.build();

		// act & assert
		Authentication authentication = mock(Authentication.class);
		assertThatThrownBy(
			() -> commandConverter.apply(authentication, command, umsetzungsstand,
				MassnahmeTestDataProvider.withDefaultValues().umsetzungsstatus(Umsetzungsstatus.STORNIERT).build()))
					.isInstanceOf(RequireViolation.class);
		assertThatThrownBy(
			() -> commandConverter.apply(authentication, command, umsetzungsstand,
				MassnahmeTestDataProvider.withDefaultValues().umsetzungsstatus(Umsetzungsstatus.UMGESETZT).build()))
					.isInstanceOf(RequireViolation.class);
	}

	@TestFactory
	Stream<DynamicTest> convert_Umetzungsstand_MassnahmeStorniertOderUmgesetztAberAktualiserungAngefordert_wirdGespeichert() {
		return Stream.of(Umsetzungsstatus.STORNIERT, Umsetzungsstatus.UMGESETZT)
			.map(umsetzungsstatus -> DynamicTest.dynamicTest(
				"massnahme " + umsetzungsstatus + " aber aktualiserung angefordert: wird gespeichert", () -> {
					// arrange
					Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
					Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
						.id(100L)
						.build();

					LocalDateTime letzteAenderungInitial = LocalDateTime.now().minusDays(1);

					umsetzungsstand.update(true,
						letzteAenderungInitial,
						BenutzerTestDataProvider.defaultBenutzer().build(),
						GrundFuerAbweichungZumMassnahmenblatt.SONSTIGER_GRUND,
						PruefungQualitaetsstandardsErfolgt.JA_STANDARDS_EINGEHALTEN,
						"Beschreibung abweichender Massnahme",
						20000L,
						GrundFuerNichtUmsetzungDerMassnahme.LAUT_VERKEHRSSCHAU_NICHT_ERFORDERLICH,
						"Anmerkung", Umsetzungsstatus.PLANUNG);

					umsetzungsstand.fordereAktualisierungAn();

					SaveUmsetzungsstandCommand command = SaveUmsetzungsstandCommand.builder()
						.umsetzungGemaessMassnahmenblatt(false)
						.grundFuerAbweichungZumMassnahmenblatt(
							GrundFuerAbweichungZumMassnahmenblatt.RADNETZ_WURDE_VERLEGT)
						.pruefungQualitaetsstandardsErfolgt(PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH)
						.beschreibungAbweichenderMassnahme("neue Beschreibung abweichender Massnahme")
						.kostenDerMassnahme(30000L)
						.grundFuerNichtUmsetzungDerMassnahme(
							GrundFuerNichtUmsetzungDerMassnahme.NOCH_IN_PLANUNG_UMSETZUNG)
						.anmerkung("neue Anmerkung")
						.build();

					// act
					Authentication authentication = mock(Authentication.class);
					commandConverter.apply(authentication, command, umsetzungsstand,
						MassnahmeTestDataProvider.withDefaultValues().baulastZustaendiger(organisation)
							.umsetzungsstatus(umsetzungsstatus).build());

					// assert
					assertThat(umsetzungsstand.getUmsetzungsstandStatus()).isEqualTo(
						UmsetzungsstandStatus.AKTUALISIERT);
					assertThat(umsetzungsstand.isUmsetzungGemaessMassnahmenblatt()).isFalse();
					assertThat(umsetzungsstand.getGrundFuerAbweichungZumMassnahmenblatt()).isEqualTo(
						GrundFuerAbweichungZumMassnahmenblatt.RADNETZ_WURDE_VERLEGT);
					assertThat(umsetzungsstand.getPruefungQualitaetsstandardsErfolgt()).isEqualTo(
						PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH);
					assertThat(umsetzungsstand.getBeschreibungAbweichenderMassnahme()).isEqualTo(
						"neue Beschreibung abweichender Massnahme");
					assertThat(umsetzungsstand.getKostenDerMassnahme()).isEqualTo(30000L);
					assertThat(umsetzungsstand.getGrundFuerNichtUmsetzungDerMassnahme()).isEqualTo(
						GrundFuerNichtUmsetzungDerMassnahme.NOCH_IN_PLANUNG_UMSETZUNG);
					assertThat(umsetzungsstand.getBenutzerLetzteAenderung()).isEqualTo(testBenutzer);
					assertThat(umsetzungsstand.getLetzteAenderung()).isAfter(letzteAenderungInitial);
				}));
	}
}
