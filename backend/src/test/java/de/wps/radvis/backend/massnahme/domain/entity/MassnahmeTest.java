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

package de.wps.radvis.backend.massnahme.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.dokument.domain.entity.provider.DokumentTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.event.MassnahmeChangedEvent;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerAbweichungZumMassnahmenblatt;
import de.wps.radvis.backend.massnahme.domain.valueObject.GrundFuerNichtUmsetzungDerMassnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.PruefungQualitaetsstandardsErfolgt;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

class MassnahmeTest {
	private Verwaltungseinheit testOrganisation;

	MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@BeforeEach
	void setUp() {
		testOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L)
			.name("Coole Organisation").organisationsArt(
				OrganisationsArt.BUNDESLAND).build();
		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@Test
	void massnahmeErstellen_wirdNichtErstellt_bezeichnungIstNull() {
		// arrange + act + assert

		assertThatThrownBy(() -> MassnahmeTestDataProvider
			.withDefaultValues()
			.bezeichnung(null)
			.build())
			.isInstanceOf(RequireViolation.class);
	}

	@Test
	void massnahmeErstellen_wirdNichtErstellt_massnahmeKategorieIstNull() {
		// arrange + act + assert

		assertThatThrownBy(() -> MassnahmeTestDataProvider
			.withDefaultValues()
			.massnahmenkategorien(null)
			.build())
			.isInstanceOf(RequireViolation.class);
	}

	@Test
	void massnahmeErstellen_wirdNichtErstellt_netzbezugIstNull() {
		// arrange + act + assert

		assertThatThrownBy(() -> MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(null)
			.build())
			.isInstanceOf(RequireViolation.class);
	}

	@Test
	void massnahmeErstellen_wirdNichtErstellt_umsetzungsstatusIstNull() {
		// arrange + act + assert

		assertThatThrownBy(() -> MassnahmeTestDataProvider
			.withDefaultValues()
			.umsetzungsstatus(null)
			.build())
			.isInstanceOf(RequireViolation.class);
	}

	@Test
	void massnahmeErstellen_wirdNichtErstellt_veroeffentlichungsstatusIstNull() {
		// arrange + act + assert

		assertThatThrownBy(() -> MassnahmeTestDataProvider
			.withDefaultValues()
			.veroeffentlicht(null)
			.build())
			.isInstanceOf(RequireViolation.class);
	}

	@Test
	void massnahmeErstellen_wirdNichtErstellt_dokumentJahrNichtValide() {
		// arrange + act + assert

		assertThatThrownBy(() -> MassnahmeTestDataProvider
			.withDefaultValues()
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(1000))
			.build())
			.isInstanceOf(RequireViolation.class)
			.hasMessageContaining("Jahr ist nicht valide.");
	}

	@Test
	@Disabled
	void massnahmeErstellen_wirdNichtErstellt_netzbezugInvalide() {
		// arrange + act + assert

		assertThatThrownBy(() -> MassnahmeTestDataProvider
			.withDefaultValues()
			// .netzbezug(new Netzbezug(new ArrayList<Knoten>(), new ArrayList<KantenSeitenAbschnitt>()))
			.build())
			.isInstanceOf(RequireViolation.class)
			.hasMessageContaining("Knoten und KantenSeitenAbschnitte dürfen nicht gleichzeitig leer sein.");
	}

	@Test
	void massnahmeErstellen_wirdNichtErstellt_pflichtFeldDurchfuehrungszeitraumAbStatusIstNull() {
		// arrange + act + assert

		assertThatThrownBy(() -> MassnahmeTestDataProvider
			.withDefaultValues()
			.baulastZustaendiger(testOrganisation)
			.durchfuehrungszeitraum(null)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(BenutzerTestDataProvider.admin(testOrganisation).build())
			.build())
			.isInstanceOf(RequireViolation.class)
			.hasMessageContaining(
				"Durchführungszeitraum, Baulastträger und Handlungsverantwortlicher sind ab Status 'Planung' Pflichtfelder.");
	}

	@Test
	void massnahmeErstellen_wirdNichtErstellt_pflichtFeldBaulastZustaendigerAbStatusIstNull() {
		// arrange + act + assert

		assertThatThrownBy(() -> MassnahmeTestDataProvider
			.withDefaultValues()
			.baulastZustaendiger(null)
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2021))
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(BenutzerTestDataProvider.admin(testOrganisation).build())
			.build())
			.isInstanceOf(RequireViolation.class)
			.hasMessageContaining(
				"Durchführungszeitraum, Baulastträger und Handlungsverantwortlicher sind ab Status 'Planung' Pflichtfelder.");
	}

	@Test
	void massnahmeErstellen_wirdErstellt_pflichtFelderAbStatusValide() {
		// arrange + act + assert

		assertDoesNotThrow(() -> MassnahmeTestDataProvider
			.withDefaultValues()
			.baulastZustaendiger(testOrganisation)
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2021))
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(BenutzerTestDataProvider.admin(testOrganisation).build())
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.sonstigeKonzeptionsquelle("WAMBO")
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.build());
	}

	@Test
	void massnahmeErstellenUeberKonstruktor_wirdErstellt_felderSindGesetzt() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(1L).build();
		AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
			kante,
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS);

		// act
		Massnahme massnahme = new Massnahme(Bezeichnung.of("Bezeichnung"),
			Set.of(Massnahmenkategorie.MARKIERUNGSTECHNISCHE_MASSNAHME),
			new MassnahmeNetzBezug(Set.of(abschnittsweiserKantenSeitenBezug), Set.of(), Collections.emptySet()),
			Umsetzungsstatus.PLANUNG, true, true, Durchfuehrungszeitraum.of(2021),
			testOrganisation, testOrganisation, LocalDateTime.of(2021, 12, 17, 14, 20),
			BenutzerTestDataProvider.admin(testOrganisation).build(), SollStandard.BASISSTANDARD,
			Handlungsverantwortlicher.BAULASTTRAEGER, Konzeptionsquelle.KOMMUNALES_KONZEPT, null);

		// assert
		assertThat(massnahme.getBaulastZustaendiger()).contains(testOrganisation);
	}

	@Test
	void nurEineMassnahmenkategorieProOberkategorie_Streckenmassnahmenkategorien() {
		// arrange
		Set<Massnahmenkategorie> massnahmenkategorien = Set.of(Massnahmenkategorie.STRECKE_FUER_KFZVERKEHR_SPERREN,
			Massnahmenkategorie.UMWIDMUNG_GEMEINSAMER_RADGEHWEG);

		// act & assert
		assertThat(Massnahme.hatNurEineMassnahmenkategorieProOberkategorie(massnahmenkategorien)).isFalse();
	}

	@Test
	void nurEineMassnahmenkategorieProOberkategorie_Barrieremassnahmenkategorien() {
		// arrange
		Set<Massnahmenkategorie> massnahmenkategorien = Set.of(
			Massnahmenkategorie.BARRIERE_SICHERN_BZW_PRUEFUNG_AUF_VERZICHT,
			Massnahmenkategorie.SONSTIGE_MASSNAHME_AN_BARRIERE);

		// act & assert
		assertThat(Massnahme.hatNurEineMassnahmenkategorieProOberkategorie(massnahmenkategorien)).isFalse();
	}

	@Test
	void massnahmeErstellen_RadNETZMassnahmeOhneUmsetzungsstand_wirftRequireViolation() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(1L).build();
		AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
			kante,
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS);

		// act & assert
		assertThatThrownBy(() -> Massnahme.builder()
			.bezeichnung(Bezeichnung.of("Bezeichnung"))
			.massnahmenkategorien(Set.of(Massnahmenkategorie.MARKIERUNGSTECHNISCHE_MASSNAHME))
			.netzbezug(
				new MassnahmeNetzBezug(Set.of(abschnittsweiserKantenSeitenBezug), Set.of(), Collections.emptySet()))
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.veroeffentlicht(true)
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2021))
			.baulastZustaendiger(testOrganisation)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(BenutzerTestDataProvider.admin(testOrganisation).build())
			.netzklassen(null)
			.sollStandard(SollStandard.BASISSTANDARD)
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstand(null)
			.build()).isInstanceOf(RequireViolation.class);

	}

	@Test
	void massnahmeErstellen_RadNETZMassnahmeMitUmsetzungsstand_wirdErstellt() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(1L).build();
		AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
			kante,
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS);

		// act & assert
		assertDoesNotThrow(() -> Massnahme.builder()
			.bezeichnung(Bezeichnung.of("Bezeichnung"))
			.massnahmenkategorien(Set.of(Massnahmenkategorie.MARKIERUNGSTECHNISCHE_MASSNAHME))
			.netzbezug(
				new MassnahmeNetzBezug(Set.of(abschnittsweiserKantenSeitenBezug), Set.of(), Collections.emptySet()))
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.veroeffentlicht(true)
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2021))
			.baulastZustaendiger(testOrganisation)
			.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
			.benutzerLetzteAenderung(BenutzerTestDataProvider.admin(testOrganisation).build())
			.netzklassen(null)
			.sollStandard(SollStandard.BASISSTANDARD)
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstand(new Umsetzungsstand()));

	}

	@TestFactory
	Stream<DynamicTest> massnahmeErstellen_KeineRadNETZMassnahmeOhneUmsetzungsstand_wirdErstellt() {

		// act & assert
		return Arrays.stream(Konzeptionsquelle.values())
			.filter(konzeptionsquelle -> konzeptionsquelle != Konzeptionsquelle.RADNETZ_MASSNAHME)
			.map(konzeptionsquelle -> DynamicTest.dynamicTest(
				"Massnahme erstellen mit Konzeptionsquelle '" + konzeptionsquelle
					+ "' ohne Umsetzungsstand: wird erstellt",
				() -> {
					// act & assert
					assertDoesNotThrow(() -> MassnahmeTestDataProvider.withDefaultValues()
						.konzeptionsquelle(konzeptionsquelle)
						.sonstigeKonzeptionsquelle(konzeptionsquelle == Konzeptionsquelle.SONSTIGE ? "sonstige" : null)
						.umsetzungsstand(null)
						.build());
				}));
	}

	@TestFactory
	Stream<DynamicTest> massnahmeErstellen_KeineRadNETZMassnahmeAberUmsetzungsstand_wirftRequireViolation() {

		// act & assert
		return Arrays.stream(Konzeptionsquelle.values())
			.filter(konzeptionsquelle -> konzeptionsquelle != Konzeptionsquelle.RADNETZ_MASSNAHME)
			.map(konzeptionsquelle -> DynamicTest.dynamicTest(
				"Massnahme erstellen mit Konzeptionsquelle '" + konzeptionsquelle
					+ "' ohne Umsetzungsstand wirft RequireViolation",
				() -> {
					// arrange
					Kante kante = KanteTestDataProvider.withDefaultValues().id(1L).build();
					AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
						kante,
						LinearReferenzierterAbschnitt.of(0, 1),
						Seitenbezug.LINKS);

					// act & assert
					assertThatThrownBy(() -> Massnahme.builder()
						.bezeichnung(Bezeichnung.of("Bezeichnung"))
						.massnahmenkategorien(Set.of(Massnahmenkategorie.MARKIERUNGSTECHNISCHE_MASSNAHME))
						.netzbezug(
							new MassnahmeNetzBezug(Set.of(abschnittsweiserKantenSeitenBezug), Set.of(),
								Collections.emptySet()))
						.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
						.veroeffentlicht(true)
						.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2021))
						.baulastZustaendiger(testOrganisation)
						.letzteAenderung(LocalDateTime.of(2021, 12, 17, 14, 20))
						.benutzerLetzteAenderung(BenutzerTestDataProvider.admin(testOrganisation).build())
						.netzklassen(null)
						.sollStandard(SollStandard.BASISSTANDARD)
						.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
						.konzeptionsquelle(konzeptionsquelle)
						.sonstigeKonzeptionsquelle(konzeptionsquelle == Konzeptionsquelle.SONSTIGE ? "sonstige" : null)
						.umsetzungsstand(new Umsetzungsstand())
						.build()).isInstanceOf(RequireViolation.class);
				}));
	}

	@Test
	void update_gleicherUmsetzungsstatus_aktualisiertUmsetzungsstandNicht() {
		// arrange
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.STORNIERT)
			.umsetzungsstand(umsetzungsstand)
			.id(42L)
			.build();
		umsetzungsstand.update(true,
			LocalDateTime.of(2020, 4, 12, 23, 45),
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GrundFuerAbweichungZumMassnahmenblatt.SONSTIGER_GRUND,
			PruefungQualitaetsstandardsErfolgt.JA_STANDARDS_EINGEHALTEN,
			"Beschreibung abweichender Massnahme",
			20000L,
			GrundFuerNichtUmsetzungDerMassnahme.LAUT_VERKEHRSSCHAU_NICHT_ERFORDERLICH,
			"Anmerkung", massnahme.getUmsetzungsstatus());

		// act
		massnahme.update(massnahme.getBezeichnung(),
			massnahme.getMassnahmenkategorien(),
			massnahme.getNetzbezug(),
			massnahme.getDurchfuehrungszeitraum().get(),
			massnahme.getUmsetzungsstatus(),
			massnahme.getVeroeffentlicht(),
			massnahme.getPlanungErforderlich(),
			massnahme.getMaViSID().get(),
			massnahme.getVerbaID().get(),
			massnahme.getLGVFGID().get(),
			massnahme.getPrioritaet().get(),
			massnahme.getKostenannahme().get(),
			massnahme.getNetzklassen(),
			massnahme.getBenutzerLetzteAenderung(),
			massnahme.getLetzteAenderung(),
			massnahme.getBaulastZustaendiger().orElse(null),
			massnahme.getunterhaltsZustaendiger().orElse(null),
			massnahme.getZustaendiger().orElse(null),
			massnahme.getMassnahmeKonzeptID().get(),
			massnahme.getSollStandard(),
			massnahme.getHandlungsverantwortlicher().get(),
			massnahme.getKonzeptionsquelle(),
			null,
			massnahme.getRealisierungshilfe().get());

		// assert
		assertThat(massnahme.getUmsetzungsstand()).isPresent();
		assertThat(massnahme.getUmsetzungsstand().get().getUmsetzungsstandStatus()).isEqualTo(
			UmsetzungsstandStatus.AKTUALISIERT);
		ArgumentCaptor<MassnahmeChangedEvent> captor = ArgumentCaptor.forClass(MassnahmeChangedEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
		assertThat(captor.getValue().getMassnahmeId()).isEqualTo(42L);
	}

	@Test
	void update_geaenderterUmsetzungsstatus_aktualisiertUmsetzungsstand() {
		// arrange
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L)
			.build();
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.umsetzungsstand(umsetzungsstand)
			.baulastZustaendiger(organisation)
			.id(42L)
			.build();
		umsetzungsstand.update(true,
			LocalDateTime.of(2020, 4, 12, 23, 45),
			BenutzerTestDataProvider.defaultBenutzer().build(),
			GrundFuerAbweichungZumMassnahmenblatt.SONSTIGER_GRUND,
			PruefungQualitaetsstandardsErfolgt.JA_STANDARDS_EINGEHALTEN,
			"Beschreibung abweichender Massnahme",
			20000L,
			GrundFuerNichtUmsetzungDerMassnahme.LAUT_VERKEHRSSCHAU_NICHT_ERFORDERLICH,
			"Anmerkung", massnahme.getUmsetzungsstatus());

		// act
		massnahme.update(massnahme.getBezeichnung(),
			massnahme.getMassnahmenkategorien(),
			massnahme.getNetzbezug(),
			massnahme.getDurchfuehrungszeitraum().get(),
			Umsetzungsstatus.UMGESETZT,
			massnahme.getVeroeffentlicht(),
			massnahme.getPlanungErforderlich(),
			massnahme.getMaViSID().get(),
			massnahme.getVerbaID().get(),
			massnahme.getLGVFGID().get(),
			massnahme.getPrioritaet().get(),
			massnahme.getKostenannahme().get(),
			massnahme.getNetzklassen(),
			massnahme.getBenutzerLetzteAenderung(),
			massnahme.getLetzteAenderung(),
			massnahme.getBaulastZustaendiger().get(),
			massnahme.getunterhaltsZustaendiger().orElse(null),
			massnahme.getZustaendiger().orElse(null),
			massnahme.getMassnahmeKonzeptID().get(),
			massnahme.getSollStandard(),
			massnahme.getHandlungsverantwortlicher().get(),
			massnahme.getKonzeptionsquelle(),
			null,
			massnahme.getRealisierungshilfe().get());

		// assert
		assertThat(massnahme.getUmsetzungsstand()).isPresent();
		assertThat(massnahme.getUmsetzungsstand().get().getUmsetzungsstandStatus()).isEqualTo(
			UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT);
		ArgumentCaptor<MassnahmeChangedEvent> captor = ArgumentCaptor.forClass(MassnahmeChangedEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
		assertThat(captor.getValue().getMassnahmeId()).isEqualTo(42L);
	}

	@Test
	void update_KonzeptionsquelleVorherRadNETZNachherNicht_wirftRequireViolation() {
		// arrange
		Umsetzungsstand umsetzungsstand = new Umsetzungsstand();

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstand(umsetzungsstand)
			.build();

		// act & assert
		assertThatThrownBy(
			() -> massnahme.update(massnahme.getBezeichnung(),
				massnahme.getMassnahmenkategorien(),
				massnahme.getNetzbezug(),
				massnahme.getDurchfuehrungszeitraum().get(),
				massnahme.getUmsetzungsstatus(),
				massnahme.getVeroeffentlicht(),
				massnahme.getPlanungErforderlich(),
				massnahme.getMaViSID().get(),
				massnahme.getVerbaID().get(),
				massnahme.getLGVFGID().get(),
				massnahme.getPrioritaet().get(),
				massnahme.getKostenannahme().get(),
				massnahme.getNetzklassen(),
				massnahme.getBenutzerLetzteAenderung(),
				massnahme.getLetzteAenderung(),
				massnahme.getBaulastZustaendiger().orElse(null),
				massnahme.getunterhaltsZustaendiger().orElse(null),
				massnahme.getZustaendiger().orElse(null),
				massnahme.getMassnahmeKonzeptID().get(),
				massnahme.getSollStandard(),
				massnahme.getHandlungsverantwortlicher().get(),
				Konzeptionsquelle.KREISKONZEPT,
				null,
				massnahme.getRealisierungshilfe().get()))
			.isInstanceOf(RequireViolation.class)
			.hasMessageContaining("Eine RadNETZ-Maßnahme darf nicht zu einer Non-RadNETZ-Maßnahme werden!");
		domainPublisherMock.verifyNoInteractions();
	}

	@Test
	void update_KonzeptionsquelleNachherRadNETZVorherNicht_umsetzungsstandWirdErstellt() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.KREISKONZEPT)
			.id(42L)
			.build();

		// act
		massnahme.update(massnahme.getBezeichnung(),
			massnahme.getMassnahmenkategorien(),
			massnahme.getNetzbezug(),
			massnahme.getDurchfuehrungszeitraum().get(),
			massnahme.getUmsetzungsstatus(),
			massnahme.getVeroeffentlicht(),
			massnahme.getPlanungErforderlich(),
			massnahme.getMaViSID().get(),
			massnahme.getVerbaID().get(),
			massnahme.getLGVFGID().get(),
			massnahme.getPrioritaet().get(),
			massnahme.getKostenannahme().get(),
			massnahme.getNetzklassen(),
			massnahme.getBenutzerLetzteAenderung(),
			massnahme.getLetzteAenderung(),
			massnahme.getBaulastZustaendiger().orElse(null),
			massnahme.getunterhaltsZustaendiger().orElse(null),
			massnahme.getZustaendiger().orElse(null),
			massnahme.getMassnahmeKonzeptID().get(),
			massnahme.getSollStandard(),
			massnahme.getHandlungsverantwortlicher().get(),
			Konzeptionsquelle.RADNETZ_MASSNAHME,
			null,
			massnahme.getRealisierungshilfe().get());

		// assert
		assertThat(massnahme.getUmsetzungsstand()).isPresent();

		ArgumentCaptor<MassnahmeChangedEvent> captor = ArgumentCaptor.forClass(MassnahmeChangedEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
		assertThat(captor.getValue().getMassnahmeId()).isEqualTo(42L);
	}

	@Test
	void update_KonzeptionsquelleNachherRadNETZVorherAuch_umsetzungsstandBleibtErhalten() {
		// arrange
		Umsetzungsstand umsetzungsstandVorher = new Umsetzungsstand();

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstand(umsetzungsstandVorher)
			.id(42L)
			.build();

		// act
		massnahme.update(massnahme.getBezeichnung(),
			massnahme.getMassnahmenkategorien(),
			massnahme.getNetzbezug(),
			massnahme.getDurchfuehrungszeitraum().get(),
			massnahme.getUmsetzungsstatus(),
			massnahme.getVeroeffentlicht(),
			massnahme.getPlanungErforderlich(),
			massnahme.getMaViSID().get(),
			massnahme.getVerbaID().get(),
			massnahme.getLGVFGID().get(),
			massnahme.getPrioritaet().get(),
			massnahme.getKostenannahme().get(),
			massnahme.getNetzklassen(),
			massnahme.getBenutzerLetzteAenderung(),
			massnahme.getLetzteAenderung(),
			massnahme.getBaulastZustaendiger().orElse(null),
			massnahme.getunterhaltsZustaendiger().orElse(null),
			massnahme.getZustaendiger().orElse(null),
			massnahme.getMassnahmeKonzeptID().get(),
			massnahme.getSollStandard(),
			massnahme.getHandlungsverantwortlicher().get(),
			Konzeptionsquelle.RADNETZ_MASSNAHME,
			null,
			massnahme.getRealisierungshilfe().get());

		// assert
		assertThat(massnahme.getUmsetzungsstand()).isPresent();
		assertThat(massnahme.getUmsetzungsstand().get()).isEqualTo(umsetzungsstandVorher);

		ArgumentCaptor<MassnahmeChangedEvent> captor = ArgumentCaptor.forClass(MassnahmeChangedEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
		assertThat(captor.getValue().getMassnahmeId()).isEqualTo(42L);
	}

	@Test
	void add_Dokment() {
		// Arrange
		Dokument dokument = DokumentTestDataProvider.withDefaultValues().build();
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(42L).build();

		// Act
		massnahme.addDokument(dokument);

		// Assert
		assertThat(massnahme.getDokumentListe().getDokumente()).containsExactly(dokument);

		ArgumentCaptor<MassnahmeChangedEvent> captor = ArgumentCaptor.forClass(MassnahmeChangedEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
		assertThat(captor.getValue().getMassnahmeId()).isEqualTo(42L);

	}

	@Test
	void berechneMittelpunkt_emptyNetzbezugAfterDlmReimport_noException_mittelpunktEmpty() {
		MassnahmeNetzBezug netzbezugMock = mock(MassnahmeNetzBezug.class);
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().netzbezug(netzbezugMock).build();

		when(netzbezugMock.getImmutableKantenAbschnittBezug()).thenReturn(new HashSet<>());
		when(netzbezugMock.getImmutableKantenPunktBezug()).thenReturn(new HashSet<>());
		when(netzbezugMock.getImmutableKnotenBezug()).thenReturn(new HashSet<>());

		assertThatNoException().isThrownBy(() -> massnahme.berechneMittelpunkt());
		assertThat(massnahme.berechneMittelpunkt()).isEmpty();
	}
}
