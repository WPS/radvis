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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.dokument.domain.entity.provider.DokumentTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.event.MassnahmeChangedEvent;
import de.wps.radvis.backend.massnahme.domain.event.MassnahmeStornierungAngefragtEvent;
import de.wps.radvis.backend.massnahme.domain.valueObject.BegruendungStornierungsanfrage;
import de.wps.radvis.backend.massnahme.domain.valueObject.BegruendungZurueckstellung;
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
import de.wps.radvis.backend.massnahme.domain.valueObject.ZurueckstellungsGrund;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class MassnahmeTest {
	private Verwaltungseinheit testOrganisation;

	MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@BeforeEach
	void setUp() {
		testOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L)
			.name("Coole Organisation").organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();
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
			Handlungsverantwortlicher.BAULASTTRAEGER, Konzeptionsquelle.KOMMUNALES_KONZEPT, null, null, null, null);

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

	@TestFactory
	Stream<DynamicTest> update_radnetzQuelleChangeForbidden() {
		return Arrays.stream(Konzeptionsquelle.values())
			.filter(konzeptionsquelle -> Konzeptionsquelle.isRadNetzKonzeptionsquelle(konzeptionsquelle))
			.map(konzeptionsquelle -> DynamicTest.dynamicTest(
				"update Massnahme, Konzeptionsquelle " + konzeptionsquelle
					+ " zu Kreisnetz not allowed",
				() -> {
					// arrange
					Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
						.konzeptionsquelle(konzeptionsquelle)
						.id(42L)
						.build();

					// act + assert
					assertThat(massnahme.canUpdateKonzeptionsquelle(Konzeptionsquelle.KREISKONZEPT)).isFalse();
				}));
	}

	@Nested
	class KonzeptionsquelleUndUmsetzungsstand {

		private MassnahmeNetzBezug netzbezug;
		private Verwaltungseinheit zustaendiger;
		private Benutzer benutzer;

		@BeforeEach
		void setup() {
			Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Coole Organisation")
				.organisationsArt(OrganisationsArt.BUNDESLAND)
				.build();

			zustaendiger = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Mega coole zuständige Organisation")
				.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK)
				.build();

			benutzer = BenutzerTestDataProvider.admin(organisation).build();
			Kante kante = KanteTestDataProvider.withDefaultValues().build();
			netzbezug = new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(
					kante, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS)),
				Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
				Collections.emptySet());
		}

		@Test
		void create_NotRadNetzMassnahme_umsetzungsstandIsNull() {
			// act
			Massnahme massnahme = new Massnahme(Bezeichnung.of("Test"),
				Set.of(Massnahmenkategorie.AENDERUNG_DER_VERKEHRSRECHTLICHEN_ANORDNUNG),
				netzbezug,
				Umsetzungsstatus.IDEE,
				false,
				true,
				null,
				zustaendiger,
				zustaendiger,
				LocalDateTime.now(),
				benutzer,
				SollStandard.BASISSTANDARD,
				Handlungsverantwortlicher.BAULASTTRAEGER_UND_VERKEHRSBEHORDE_TECHNIK,
				Konzeptionsquelle.KOMMUNALES_KONZEPT,
				null, null, null, null);

			// assert
			assertThat(massnahme.getUmsetzungsstand()).isEmpty();
		}

		@Test
		void create_RadNetzMassnahme2024_erstelltUmsetzungsstand() {
			// act
			Massnahme massnahme = new Massnahme(Bezeichnung.of("Test"),
				Set.of(Massnahmenkategorie.SONSTIGE_MASSNAHME_AN_BARRIERE),
				netzbezug,
				Umsetzungsstatus.IDEE,
				false,
				true,
				null,
				zustaendiger,
				zustaendiger,
				LocalDateTime.now(),
				benutzer,
				SollStandard.BASISSTANDARD,
				Handlungsverantwortlicher.BAULASTTRAEGER_UND_VERKEHRSBEHORDE_TECHNIK,
				Konzeptionsquelle.RADNETZ_MASSNAHME_2024,
				null, null, null, null);

			// assert
			assertThat(massnahme.getUmsetzungsstand()).isPresent();
		}

		@Test
		void update_KonzeptionsquelleNachherKeinRadNETZVorherAuchNicht_umsetzungsstandWirdErstellt() {
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
				Konzeptionsquelle.KOMMUNALES_KONZEPT,
				null,
				massnahme.getRealisierungshilfe().get(), null, null, null);

			// assert
			assertThat(massnahme.getUmsetzungsstand()).isEmpty();
		}

		@TestFactory
		Stream<DynamicTest> update_zuRadnetzMassnahme_erstelltUmsetzungsstand() {
			return Arrays.stream(Konzeptionsquelle.values())
				.filter(konzeptionsquelle -> Konzeptionsquelle.isRadNetzKonzeptionsquelle(konzeptionsquelle))
				.map(konzeptionsquelle -> DynamicTest.dynamicTest(
					"update Massnahme, Konzeptionsquelle von Kreisnetz zu " + konzeptionsquelle
						+ " erstellt Umsetzungsstand",
					() -> {
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
							konzeptionsquelle,
							null,
							massnahme.getRealisierungshilfe().get(), null, null, null);

						// assert
						assertThat(massnahme.getUmsetzungsstand()).isPresent();
					}));
		}

		@TestFactory
		Stream<DynamicTest> update_bleibtRadnetzMassnahme_umsetzungsstandUntouched() {
			return Arrays.stream(Konzeptionsquelle.values())
				.filter(konzeptionsquelle -> Konzeptionsquelle.isRadNetzKonzeptionsquelle(konzeptionsquelle))
				.map(konzeptionsquelle -> DynamicTest.dynamicTest(
					"update Massnahme, Konzeptionsquelle bleibt " + konzeptionsquelle
						+ ", Umsetzungsstand bleibt erhalten",
					() -> {
						// arrange
						Umsetzungsstand umsetzungsstandVorher = new Umsetzungsstand();

						Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
							.konzeptionsquelle(konzeptionsquelle)
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
							konzeptionsquelle,
							null,
							massnahme.getRealisierungshilfe().get(), null, null, null);

						// assert
						assertThat(massnahme.getUmsetzungsstand()).isPresent();
						assertThat(massnahme.getUmsetzungsstand().get()).isEqualTo(umsetzungsstandVorher);
					}));
		}
	}

	@Test
	void update_gleicherUmsetzungsstatus_aktualisiertUmsetzungsstandNicht() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.id(42L)
			.build();
		massnahme.getUmsetzungsstand().orElseThrow().update(true,
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
			massnahme.getRealisierungshilfe().get(), null, null, null);

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
			massnahme.getRealisierungshilfe().get(), null, null, null);

		// assert
		assertThat(massnahme.getUmsetzungsstand()).isPresent();
		assertThat(massnahme.getUmsetzungsstand().get().getUmsetzungsstandStatus()).isEqualTo(
			UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT);
		ArgumentCaptor<MassnahmeChangedEvent> captor = ArgumentCaptor.forClass(MassnahmeChangedEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
		assertThat(captor.getValue().getMassnahmeId()).isEqualTo(42L);
	}

	@Test
	void canUpdateKonzeptionsquelle_RadNETZ() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)
			.build();

		// act & assert
		assertThat(massnahme.canUpdateKonzeptionsquelle(Konzeptionsquelle.KOMMUNALES_KONZEPT)).isFalse();
		assertThat(massnahme.canUpdateKonzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME)).isTrue();
	}

	@Test
	void canUpdateKonzeptionsquelle_RadNETZ_2024() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024)
			.build();

		// act & assert
		assertThat(massnahme.canUpdateKonzeptionsquelle(Konzeptionsquelle.KOMMUNALES_KONZEPT)).isFalse();
		assertThat(massnahme.canUpdateKonzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024)).isTrue();
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

	@SuppressWarnings("unchecked")
	@Test
	void archivieren() {
		// arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).build();

		AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
			kante, LinearReferenzierterAbschnitt.of(0, 0.3), Seitenbezug.LINKS);
		AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug2 = new AbschnittsweiserKantenSeitenBezug(
			kante, LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.LINKS);
		PunktuellerKantenSeitenBezug punktuellerKantenSeitenBezug = new PunktuellerKantenSeitenBezug(kante,
			LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG);
		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(abschnittsweiserKantenSeitenBezug, abschnittsweiserKantenSeitenBezug2),
			Set.of(punktuellerKantenSeitenBezug),
			Set.of(kante.getVonKnoten()));

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(netzbezug)
			.build();

		// act
		massnahme.archivieren();

		// assert
		assertThat(massnahme.isArchiviert()).isTrue();
		assertThat(massnahme.getNetzbezugSnapshot()).isPresent();
		assertThat(massnahme.getNetzbezugSnapshot().get().getNumGeometries()).isEqualTo(4);
		for (int i = 0; i < massnahme.getNetzbezugSnapshot().get().getNumGeometries(); i++) {
			assertThat(massnahme.getNetzbezugSnapshot().get().getGeometryN(i)).matches(g -> {
				return g.equals(abschnittsweiserKantenSeitenBezug.getGeometrie())
					|| g.equals(punktuellerKantenSeitenBezug.getPointGeometry())
					|| g.equals(kante.getVonKnoten().getPoint())
					|| g.equals(abschnittsweiserKantenSeitenBezug2.getGeometrie());
			});
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	void archivieren_nurPoints() {
		// arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).build();

		PunktuellerKantenSeitenBezug punktuellerKantenSeitenBezug = new PunktuellerKantenSeitenBezug(kante,
			LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG);
		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(),
			Set.of(punktuellerKantenSeitenBezug),
			Set.of(kante.getVonKnoten()));

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(netzbezug)
			.build();

		// act
		massnahme.archivieren();

		// assert
		assertThat(massnahme.isArchiviert()).isTrue();
		assertThat(massnahme.getNetzbezugSnapshot()).isPresent();
		assertThat(massnahme.getNetzbezugSnapshotLines()).isNull();
		assertThat(massnahme.getNetzbezugSnapshot().get().getNumGeometries()).isEqualTo(2);
		for (int i = 0; i < massnahme.getNetzbezugSnapshot().get().getNumGeometries(); i++) {
			assertThat(massnahme.getNetzbezugSnapshot().get().getGeometryN(i)).matches(g -> {
				return g.equals(punktuellerKantenSeitenBezug.getPointGeometry())
					|| g.equals(kante.getVonKnoten().getPoint());
			});
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	void archivieren_nurLines() {
		// arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).build();

		AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
			kante, LinearReferenzierterAbschnitt.of(0, 0.3), Seitenbezug.LINKS);
		AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug2 = new AbschnittsweiserKantenSeitenBezug(
			kante, LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.LINKS);
		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(abschnittsweiserKantenSeitenBezug, abschnittsweiserKantenSeitenBezug2),
			Set.of(),
			Set.of());

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(netzbezug)
			.build();

		// act
		massnahme.archivieren();

		// assert
		assertThat(massnahme.isArchiviert()).isTrue();
		assertThat(massnahme.getNetzbezugSnapshotPoints()).isNull();
		assertThat(massnahme.getNetzbezugSnapshot()).isPresent();
		assertThat(massnahme.getNetzbezugSnapshot().get().getNumGeometries()).isEqualTo(2);
		for (int i = 0; i < massnahme.getNetzbezugSnapshot().get().getNumGeometries(); i++) {
			assertThat(massnahme.getNetzbezugSnapshot().get().getGeometryN(i)).matches(g -> {
				return g.equals(abschnittsweiserKantenSeitenBezug.getGeometrie())
					|| g.equals(abschnittsweiserKantenSeitenBezug2.getGeometrie());
			});
		}
	}

	@Test
	void unarchivieren() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().build();
		massnahme.archivieren();

		// act
		massnahme.archivierungAufheben();

		// assert
		assertThat(massnahme.isArchiviert()).isFalse();
		assertThat(massnahme.getNetzbezugSnapshot()).isEmpty();
	}

	@Test
	void ersetzeKanteInNetzbezug() {
		// arrange
		Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
			.id(1l)
			.build();
		Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM).id(2l)
			.build();
		Kante ersatzKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(50, 0, 100, 0, QuellSystem.DLM).id(3l)
			.build();

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().netzbezug(new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante, LinearReferenzierterAbschnitt.of(0.3, 0.7),
				Seitenbezug.BEIDSEITIG)),
			Set.of(new PunktuellerKantenSeitenBezug(zuErsetzendeKante, LineareReferenz.of(0.25),
				Seitenbezug.BEIDSEITIG)),
			Collections.emptySet())).build();

		// act
		massnahme.ersetzeKanteInNetzbezug(zuErsetzendeKante, Set.of(ersatzKante1, ersatzKante2), 1.0);

		// assert
		assertThat(massnahme.getNetzbezug().getImmutableKantenPunktBezug()).hasSize(1);
		assertThat(massnahme.getNetzbezug().getImmutableKantenPunktBezug())
			.contains(new PunktuellerKantenSeitenBezug(ersatzKante1, LineareReferenz.of(0.5),
				Seitenbezug.BEIDSEITIG));

		assertThat(massnahme.getNetzbezug().getImmutableKantenAbschnittBezug()).hasSize(2);
		assertThat(massnahme.getNetzbezug().getImmutableKantenAbschnittBezug())
			.contains(
				new AbschnittsweiserKantenSeitenBezug(ersatzKante1, LinearReferenzierterAbschnitt.of(0.6, 1),
					Seitenbezug.BEIDSEITIG),
				new AbschnittsweiserKantenSeitenBezug(ersatzKante2, LinearReferenzierterAbschnitt.of(0, 0.4),
					Seitenbezug.BEIDSEITIG));
	}

	@Test
	void removeKanteFromNetzbezug() {
		// arrange
		Kante zuEntfernendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
			.id(1l)
			.build();

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().netzbezug(new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(zuEntfernendeKante, LinearReferenzierterAbschnitt.of(0.3, 0.7),
				Seitenbezug.BEIDSEITIG)),
			Set.of(new PunktuellerKantenSeitenBezug(zuEntfernendeKante, LineareReferenz.of(0.25),
				Seitenbezug.BEIDSEITIG)),
			Collections.emptySet())).build();

		// act
		massnahme.removeKanteFromNetzbezug(Set.of(zuEntfernendeKante.getId()));

		// assert
		assertThat(massnahme.getNetzbezug().getImmutableKantenPunktBezug()).isEmpty();
		assertThat(massnahme.getNetzbezug().getImmutableKantenAbschnittBezug()).isEmpty();
	}

	@Test
	void removeKnotenFromNetzbezug() {
		// arrange
		Knoten knoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().netzbezug(new MassnahmeNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(knoten))).build();

		// act
		massnahme.removeKnotenFromNetzbezug(Set.of(knoten.getId()));

		// assert
		assertThat(massnahme.getNetzbezug().getImmutableKnotenBezug()).isEmpty();
	}

	@Test
	void ersetzeKnotenInNetzbezug() {
		// arrange
		Knoten zuErsetzenderKnoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten knoten2 = KnotenTestDataProvider.withDefaultValues().id(2l).build();
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().netzbezug(new MassnahmeNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuErsetzenderKnoten, knoten2))).build();

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		massnahme.ersetzeKnotenInNetzbezug(ersatzKnotenZuordnung);

		// assert
		assertThat(massnahme.getNetzbezug().getImmutableKnotenBezug()).contains(knoten2, ersatzKnoten);
		assertThat(massnahme.getNetzbezug().getImmutableKnotenBezug()).doesNotContain(zuErsetzenderKnoten);
	}

	@Test
	void areKategorienValidForKonzeptionsquelle() {
		assertThat(Massnahme.areKategorienValidForKonzeptionsquelle(null, Set.of())).isTrue();
		assertThat(Massnahme.areKategorienValidForKonzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME, Set.of()))
			.isTrue();
		assertThat(Massnahme.areKategorienValidForKonzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME,
			Set.of(Massnahmenkategorie.NEUBAU_WEG_NACH_RADNETZ_QUALITAETSSTANDARD))).isTrue();
		assertThat(Massnahme.areKategorienValidForKonzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024,
			Set.of(Massnahmenkategorie.SONSTIGE_MASSNAHME_AN_BARRIERE))).isTrue();
		assertThat(Massnahme.areKategorienValidForKonzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024,
			Set.of(Massnahmenkategorie.SONSTIGE_MASSNAHME_AN_BARRIERE,
				Massnahmenkategorie.NEUBAU_WEG_NACH_RADNETZ_QUALITAETSSTANDARD))).isFalse();
	}

	@Test
	void isUmsetzungsstatusAenderungValid_alreadyStornierteMassnahme_WeiterhinSpeicherbar() {
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().umsetzungsstatus(Umsetzungsstatus.STORNIERT)
			.build();

		assertThat(massnahme.isUmsetzungsstatusAenderungValid(Umsetzungsstatus.STORNIERT)).isTrue();
	}

	@Test
	void isUmsetzungsstatusAenderungValid() {
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().umsetzungsstatus(Umsetzungsstatus.IDEE)
			.build();

		assertThat(massnahme.isUmsetzungsstatusAenderungValid(Umsetzungsstatus.STORNIERT)).isFalse();
	}

	@Test
	void isZurueckstellungsGrundValidForUmsetzungsstatus() {
		assertThat(Massnahme.isZurueckstellungsGrundValidForUmsetzungsstatus(Umsetzungsstatus.ZURUECKGESTELLT, null))
			.isFalse();
		assertThat(Massnahme.isZurueckstellungsGrundValidForUmsetzungsstatus(Umsetzungsstatus.PLANUNG, null)).isTrue();
		assertThat(Massnahme.isZurueckstellungsGrundValidForUmsetzungsstatus(Umsetzungsstatus.ZURUECKGESTELLT,
			ZurueckstellungsGrund.FINANZIELLE_RESSOURCEN)).isTrue();
	}

	@Test
	void create_stornierungAngefragt_publishesEvent() {
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
			Umsetzungsstatus.STORNIERUNG_ANGEFRAGT, true, true, Durchfuehrungszeitraum.of(2021),
			testOrganisation, testOrganisation, LocalDateTime.of(2021, 12, 17, 14, 20),
			BenutzerTestDataProvider.admin(testOrganisation).build(), SollStandard.BASISSTANDARD,
			Handlungsverantwortlicher.BAULASTTRAEGER, Konzeptionsquelle.RADNETZ_MASSNAHME_2024, null, null,
			BegruendungStornierungsanfrage.of("Test"), null);

		// assert
		ArgumentCaptor<MassnahmeStornierungAngefragtEvent> captor = ArgumentCaptor
			.forClass(MassnahmeStornierungAngefragtEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
		assertThat(captor.getValue().getMassnahme()).usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
			.withComparedFields("zustaendiger", "benutzerLetzteAenderung", "begruendungStornierungsanfrage")
			.build())
			.isEqualTo(massnahme);
	}

	@Test
	void create_keineStornierungAngefragt_doesNotPublish() {
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
			Umsetzungsstatus.IDEE, true, true, Durchfuehrungszeitraum.of(2021),
			testOrganisation, testOrganisation, LocalDateTime.of(2021, 12, 17, 14, 20),
			BenutzerTestDataProvider.admin(testOrganisation).build(), SollStandard.BASISSTANDARD,
			Handlungsverantwortlicher.BAULASTTRAEGER, Konzeptionsquelle.RADNETZ_MASSNAHME_2024, null, null, null, null);

		// assert
		ArgumentCaptor<MassnahmeStornierungAngefragtEvent> captor = ArgumentCaptor
			.forClass(MassnahmeStornierungAngefragtEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()), never());
	}

	@Test
	void update_stornierungAngefragt_publishesEvent() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues()
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024).id(42l)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.build();

		// act
		massnahme.update(massnahme.getBezeichnung(),
			massnahme.getMassnahmenkategorien(),
			massnahme.getNetzbezug(),
			massnahme.getDurchfuehrungszeitraum().get(),
			Umsetzungsstatus.STORNIERUNG_ANGEFRAGT,
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
			"Test",
			massnahme.getRealisierungshilfe().get(), null, BegruendungStornierungsanfrage.of("Test"), null);

		// assert
		ArgumentCaptor<MassnahmeStornierungAngefragtEvent> captor = ArgumentCaptor
			.forClass(MassnahmeStornierungAngefragtEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()));
		assertThat(captor.getValue().getMassnahme())
			.usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
				.withComparedFields("zustaendiger", "benutzerLetzteAenderung", "begruendungStornierungsanfrage")
				.build())
			.isEqualTo(massnahme);
	}

	@Test
	void update_stornierungAngefragt_alreadyAngefragt_doesNotPublishEvent() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(42l)
			.umsetzungsstatus(Umsetzungsstatus.STORNIERUNG_ANGEFRAGT)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024)
			.begruendungStornierungsanfrage(BegruendungStornierungsanfrage.of("Test"))
			.build();

		// act
		massnahme.update(massnahme.getBezeichnung(),
			massnahme.getMassnahmenkategorien(),
			massnahme.getNetzbezug(),
			massnahme.getDurchfuehrungszeitraum().get(),
			Umsetzungsstatus.STORNIERUNG_ANGEFRAGT,
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
			"Test",
			massnahme.getRealisierungshilfe().get(), null, BegruendungStornierungsanfrage.of("Test"), null);

		// assert
		ArgumentCaptor<MassnahmeStornierungAngefragtEvent> captor = ArgumentCaptor
			.forClass(MassnahmeStornierungAngefragtEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()), never());
	}

	@Test
	void update_keineStornierungAngefragt_doesNotPublishEvent() {
		// arrange
		Massnahme massnahme = MassnahmeTestDataProvider.withDefaultValues().id(42l)
			.umsetzungsstatus(Umsetzungsstatus.STORNIERUNG_ANGEFRAGT)
			.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024)
			.begruendungStornierungsanfrage(BegruendungStornierungsanfrage.of("Test"))
			.build();

		// act
		massnahme.update(massnahme.getBezeichnung(),
			massnahme.getMassnahmenkategorien(),
			massnahme.getNetzbezug(),
			massnahme.getDurchfuehrungszeitraum().get(),
			Umsetzungsstatus.IDEE,
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
			"Test",
			massnahme.getRealisierungshilfe().get(), null, null, null);

		// assert
		ArgumentCaptor<MassnahmeStornierungAngefragtEvent> captor = ArgumentCaptor
			.forClass(MassnahmeStornierungAngefragtEvent.class);
		domainPublisherMock.verify(() -> RadVisDomainEventPublisher.publish(captor.capture()), never());
	}

	@Test
	void isBegruendungStornierungsanfrageValidForUmsetzungsstatus() {
		// act + assert
		assertThat(Massnahme.isBegruendungStornierungsanfrageValidForUmsetzungsstatus(Umsetzungsstatus.IDEE, null))
			.isTrue();
		assertThat(Massnahme.isBegruendungStornierungsanfrageValidForUmsetzungsstatus(Umsetzungsstatus.IDEE,
			BegruendungStornierungsanfrage.of("Test")))
				.isFalse();
		assertThat(Massnahme
			.isBegruendungStornierungsanfrageValidForUmsetzungsstatus(Umsetzungsstatus.STORNIERUNG_ANGEFRAGT,
				BegruendungStornierungsanfrage.of("Test")))
					.isTrue();
		assertThat(Massnahme
			.isBegruendungStornierungsanfrageValidForUmsetzungsstatus(Umsetzungsstatus.STORNIERUNG_ANGEFRAGT, null))
				.isFalse();
	}

	@Test
	void isUmsetzungsstatusValidForKonzeptionsquelle() {
		// act + assert
		assertThat(Massnahme.isUmsetzungsstatusValidForKonzeptionsquelle(Konzeptionsquelle.KOMMUNALES_KONZEPT,
			Umsetzungsstatus.IDEE)).isTrue();
		assertThat(Massnahme.isUmsetzungsstatusValidForKonzeptionsquelle(Konzeptionsquelle.KOMMUNALES_KONZEPT,
			Umsetzungsstatus.STORNIERUNG_ANGEFRAGT)).isFalse();
		assertThat(Massnahme.isUmsetzungsstatusValidForKonzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME,
			Umsetzungsstatus.STORNIERUNG_ANGEFRAGT)).isTrue();
		assertThat(Massnahme.isUmsetzungsstatusValidForKonzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024,
			Umsetzungsstatus.STORNIERUNG_ANGEFRAGT)).isTrue();
	}

	@Test
	void isBegruendungZurueckstellungValidForZurueckstellungsgrund() {
		// act + assert
		assertThat(Massnahme.isBegruendungZurueckstellungValidForZurueckstellungsgrund(null, null)).isTrue();
		assertThat(Massnahme.isBegruendungZurueckstellungValidForZurueckstellungsgrund(null,
			BegruendungZurueckstellung.of("Test"))).isFalse();
		assertThat(
			Massnahme.isBegruendungZurueckstellungValidForZurueckstellungsgrund(ZurueckstellungsGrund.WEITERE_GRUENDE,
				BegruendungZurueckstellung.of("Test"))).isTrue();
		assertThat(Massnahme.isBegruendungZurueckstellungValidForZurueckstellungsgrund(
			ZurueckstellungsGrund.FINANZIELLE_RESSOURCEN,
			BegruendungZurueckstellung.of("Test"))).isFalse();

	}
}
