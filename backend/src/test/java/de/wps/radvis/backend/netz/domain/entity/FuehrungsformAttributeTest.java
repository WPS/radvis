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

package de.wps.radvis.backend.netz.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute.FuehrungsformAttributeBuilder;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;

class FuehrungsformAttributeTest {

	@Test
	void testeUnion_GleicheAttribute_MitIntersection() {
		FuehrungsformAttribute FA1 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.VORHANDEN)
			.build();
		FuehrungsformAttribute FA2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.VORHANDEN)
			.build();

		Optional<FuehrungsformAttribute> union1 = FA1.union(FA2);
		Optional<FuehrungsformAttribute> union2 = FA2.union(FA1);

		assertThat(union1).isPresent();
		assertThat(union2).isPresent();
		assertThat(union1.get().sindAttributeGleich(union2.get())).isTrue();
		assertThat(
			LinearReferenzierterAbschnitt.fractionEqual(union1.get().getLinearReferenzierterAbschnitt(),
				union2.get().getLinearReferenzierterAbschnitt()))
					.isTrue();
		assertThat(union1.get().getBordstein()).isEqualTo(Bordstein.KEINE_ABSENKUNG);
		assertThat(union1.get().getBelagArt()).isEqualTo(BelagArt.BETON);
		assertThat(union1.get().getOberflaechenbeschaffenheit()).isEqualTo(Oberflaechenbeschaffenheit.NEUWERTIG);
		assertThat(union1.get().getRadverkehrsfuehrung()).isEqualTo(Radverkehrsfuehrung.BEGEGNUNBSZONE);
		assertThat(union1.get().getParkenTyp()).isEqualTo(KfzParkenTyp.LAENGS_PARKEN);
		assertThat(union1.get().getParkenForm()).isEqualTo(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT);
		assertThat(union1.get().getBreite()).contains(Laenge.of(3.4));

		assertThat(LinearReferenzierterAbschnitt.fractionEqual(union1.get().getLinearReferenzierterAbschnitt(),
			LinearReferenzierterAbschnitt.of(0, 1))).isTrue();
	}

	@Test
	void testeUnion_GleicheAttribute_OhneIntersection() {
		FuehrungsformAttribute FA1 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
			.build();
		FuehrungsformAttribute FA2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
			.build();

		Optional<FuehrungsformAttribute> union1 = FA1.union(FA2);
		Optional<FuehrungsformAttribute> union2 = FA2.union(FA1);

		assertThat(union1).isEmpty();
		assertThat(union2).isEmpty();
	}

	@Test
	void testeUnion_UngleicheAttribute() {
		FuehrungsformAttribute FA1 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
			.build();
		FuehrungsformAttribute FA2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER) // anders
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(null) // anders
			.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
			.build();

		Optional<FuehrungsformAttribute> union1 = FA1.union(FA2);
		Optional<FuehrungsformAttribute> union2 = FA2.union(FA1);

		assertThat(union1).isEmpty();
		assertThat(union2).isEmpty();
	}

	@Test
	void testeSindAttributeGleich_GleicheAttribute() {
		FuehrungsformAttribute FA1 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(null)
			.benutzungspflicht(Benutzungspflicht.VORHANDEN)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteLinks(Laenge.of(1))
			.build();
		FuehrungsformAttribute FA2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(null)
			.benutzungspflicht(Benutzungspflicht.VORHANDEN)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteLinks(Laenge.of(1))
			.build();

		assertThat(FA1.sindAttributeGleich(FA1)).isTrue();
		assertThat(FA2.sindAttributeGleich(FA2)).isTrue();
		assertThat(FA1.sindAttributeGleich(FA2)).isTrue();
		assertThat(FA2.sindAttributeGleich(FA1)).isTrue();
	}

	@Test
	void testeSindAttributeGleich_NichtGleicheAttribute() {

		FuehrungsformAttribute FA0 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttribute FA1 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();
		FuehrungsformAttribute FA2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.ASPHALT)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.SEHR_GUTER_BIS_GUTER_ZUSTAND)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();
		FuehrungsformAttribute FA3 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();
		FuehrungsformAttribute FA4 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.ASPHALT)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.SEHR_GUTER_BIS_GUTER_ZUSTAND)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.GEHWEGPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttribute FA5 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.SCHRAEG_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttribute FA6 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.9))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttribute FA7 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(null)
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttribute FA8 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(null)
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteLinks(Laenge.of(1))
			.build();

		FuehrungsformAttribute FA9 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(null)
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteRechts(Laenge.of(1))
			.build();

		FuehrungsformAttribute FA10 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(null)
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteRechts(Laenge.of(1))
			.build();

		// viele asserts, aber im grunde einfach:
		// keine der obigen Attribute sollte miteinander gleich sein
		// weil sie sich immer in mindestens einem feld unterscheiden

		assertThat(FA1.sindAttributeGleich(FA0)).isFalse();
		assertThat(FA0.sindAttributeGleich(FA1)).isFalse();
		assertThat(FA1.sindAttributeGleich(FA2)).isFalse();
		assertThat(FA2.sindAttributeGleich(FA1)).isFalse();
		assertThat(FA1.sindAttributeGleich(FA3)).isFalse();
		assertThat(FA3.sindAttributeGleich(FA1)).isFalse();
		assertThat(FA1.sindAttributeGleich(FA4)).isFalse();
		assertThat(FA4.sindAttributeGleich(FA1)).isFalse();
		assertThat(FA1.sindAttributeGleich(FA5)).isFalse();
		assertThat(FA5.sindAttributeGleich(FA1)).isFalse();
		assertThat(FA1.sindAttributeGleich(FA6)).isFalse();
		assertThat(FA6.sindAttributeGleich(FA1)).isFalse();
		assertThat(FA1.sindAttributeGleich(FA7)).isFalse();
		assertThat(FA7.sindAttributeGleich(FA1)).isFalse();

		assertThat(FA2.sindAttributeGleich(FA0)).isFalse();
		assertThat(FA0.sindAttributeGleich(FA2)).isFalse();
		assertThat(FA2.sindAttributeGleich(FA3)).isFalse();
		assertThat(FA3.sindAttributeGleich(FA2)).isFalse();
		assertThat(FA2.sindAttributeGleich(FA4)).isFalse();
		assertThat(FA4.sindAttributeGleich(FA2)).isFalse();
		assertThat(FA2.sindAttributeGleich(FA5)).isFalse();
		assertThat(FA5.sindAttributeGleich(FA2)).isFalse();
		assertThat(FA2.sindAttributeGleich(FA6)).isFalse();
		assertThat(FA6.sindAttributeGleich(FA2)).isFalse();
		assertThat(FA2.sindAttributeGleich(FA7)).isFalse();
		assertThat(FA7.sindAttributeGleich(FA2)).isFalse();

		assertThat(FA3.sindAttributeGleich(FA0)).isFalse();
		assertThat(FA0.sindAttributeGleich(FA3)).isFalse();
		assertThat(FA3.sindAttributeGleich(FA4)).isFalse();
		assertThat(FA4.sindAttributeGleich(FA3)).isFalse();
		assertThat(FA3.sindAttributeGleich(FA5)).isFalse();
		assertThat(FA5.sindAttributeGleich(FA3)).isFalse();
		assertThat(FA3.sindAttributeGleich(FA6)).isFalse();
		assertThat(FA6.sindAttributeGleich(FA3)).isFalse();
		assertThat(FA3.sindAttributeGleich(FA7)).isFalse();
		assertThat(FA7.sindAttributeGleich(FA3)).isFalse();

		assertThat(FA4.sindAttributeGleich(FA0)).isFalse();
		assertThat(FA0.sindAttributeGleich(FA4)).isFalse();
		assertThat(FA4.sindAttributeGleich(FA5)).isFalse();
		assertThat(FA5.sindAttributeGleich(FA4)).isFalse();
		assertThat(FA4.sindAttributeGleich(FA6)).isFalse();
		assertThat(FA6.sindAttributeGleich(FA4)).isFalse();
		assertThat(FA4.sindAttributeGleich(FA7)).isFalse();
		assertThat(FA7.sindAttributeGleich(FA4)).isFalse();

		assertThat(FA5.sindAttributeGleich(FA0)).isFalse();
		assertThat(FA0.sindAttributeGleich(FA5)).isFalse();
		assertThat(FA5.sindAttributeGleich(FA6)).isFalse();
		assertThat(FA6.sindAttributeGleich(FA5)).isFalse();
		assertThat(FA5.sindAttributeGleich(FA7)).isFalse();
		assertThat(FA7.sindAttributeGleich(FA5)).isFalse();

		assertThat(FA6.sindAttributeGleich(FA0)).isFalse();
		assertThat(FA0.sindAttributeGleich(FA6)).isFalse();
		assertThat(FA6.sindAttributeGleich(FA7)).isFalse();
		assertThat(FA7.sindAttributeGleich(FA6)).isFalse();

		assertThat(FA7.sindAttributeGleich(FA0)).isFalse();
		assertThat(FA0.sindAttributeGleich(FA7)).isFalse();

		assertThat(FA7.sindAttributeGleich(FA8)).isFalse();
		assertThat(FA8.sindAttributeGleich(FA7)).isFalse();

		assertThat(FA8.sindAttributeGleich(FA9)).isFalse();
		assertThat(FA9.sindAttributeGleich(FA8)).isFalse();

		assertThat(FA9.sindAttributeGleich(FA10)).isFalse();
		assertThat(FA10.sindAttributeGleich(FA9)).isFalse();
	}

	@Test
	void widersprechenSichAttribute_unterschiedlicheAttribute_widersprechenSich() {
		FuehrungsformAttribute FA0 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttribute FA1 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();
		FuehrungsformAttribute FA2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.ASPHALT)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.SEHR_GUTER_BIS_GUTER_ZUSTAND)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();
		FuehrungsformAttribute FA3 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();
		FuehrungsformAttribute FA4 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.ASPHALT)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.SEHR_GUTER_BIS_GUTER_ZUSTAND)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.GEHWEGPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttribute FA5 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.SCHRAEG_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttribute FA6 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.9))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttribute FA7 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(4.2))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		// viele asserts, aber im grunde einfach:
		// keine der obigen Attribute sollte miteinander gleich sein
		// weil sie sich immer in mindestens einem feld unterscheiden

		assertThat(FA1.widersprechenSichAttribute(FA0)).isTrue();
		assertThat(FA0.widersprechenSichAttribute(FA1)).isTrue();
		assertThat(FA1.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isTrue();
		assertThat(FA1.widersprechenSichAttribute(FA3)).isTrue();
		assertThat(FA3.widersprechenSichAttribute(FA1)).isTrue();
		assertThat(FA1.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA1)).isTrue();
		assertThat(FA1.widersprechenSichAttribute(FA5)).isTrue();
		assertThat(FA5.widersprechenSichAttribute(FA1)).isTrue();
		assertThat(FA1.widersprechenSichAttribute(FA6)).isTrue();
		assertThat(FA6.widersprechenSichAttribute(FA1)).isTrue();
		assertThat(FA1.widersprechenSichAttribute(FA7)).isTrue();
		assertThat(FA7.widersprechenSichAttribute(FA1)).isTrue();

		assertThat(FA2.widersprechenSichAttribute(FA0)).isTrue();
		assertThat(FA0.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA3)).isTrue();
		assertThat(FA3.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA5)).isTrue();
		assertThat(FA5.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA6)).isTrue();
		assertThat(FA6.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA7)).isTrue();
		assertThat(FA7.widersprechenSichAttribute(FA2)).isTrue();

		assertThat(FA3.widersprechenSichAttribute(FA0)).isTrue();
		assertThat(FA0.widersprechenSichAttribute(FA3)).isTrue();
		assertThat(FA3.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isTrue();
		assertThat(FA3.widersprechenSichAttribute(FA5)).isTrue();
		assertThat(FA5.widersprechenSichAttribute(FA3)).isTrue();
		assertThat(FA3.widersprechenSichAttribute(FA6)).isTrue();
		assertThat(FA6.widersprechenSichAttribute(FA3)).isTrue();
		assertThat(FA3.widersprechenSichAttribute(FA7)).isTrue();
		assertThat(FA7.widersprechenSichAttribute(FA3)).isTrue();

		assertThat(FA4.widersprechenSichAttribute(FA0)).isTrue();
		assertThat(FA0.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA5)).isTrue();
		assertThat(FA5.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA6)).isTrue();
		assertThat(FA6.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA7)).isTrue();
		assertThat(FA7.widersprechenSichAttribute(FA4)).isTrue();

		assertThat(FA5.widersprechenSichAttribute(FA0)).isTrue();
		assertThat(FA0.widersprechenSichAttribute(FA5)).isTrue();
		assertThat(FA5.widersprechenSichAttribute(FA6)).isTrue();
		assertThat(FA6.widersprechenSichAttribute(FA5)).isTrue();
		assertThat(FA5.widersprechenSichAttribute(FA7)).isTrue();
		assertThat(FA7.widersprechenSichAttribute(FA5)).isTrue();

		assertThat(FA6.widersprechenSichAttribute(FA0)).isTrue();
		assertThat(FA0.widersprechenSichAttribute(FA6)).isTrue();
		assertThat(FA6.widersprechenSichAttribute(FA7)).isTrue();
		assertThat(FA7.widersprechenSichAttribute(FA6)).isTrue();

		assertThat(FA7.widersprechenSichAttribute(FA0)).isTrue();
		assertThat(FA0.widersprechenSichAttribute(FA7)).isTrue();
	}

	@Test
	void widersprechenSichAttribute_GleicheAttribute() {
		FuehrungsformAttribute FA1 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.45))
			.benutzungspflicht(Benutzungspflicht.VORHANDEN)
			.build();
		FuehrungsformAttribute FA2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.45))
			.benutzungspflicht(Benutzungspflicht.VORHANDEN)
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA1)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA1.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isFalse();
	}

	@Test
	void widersprechenSichAttribute_UnbekannteAttribute_isFalse() {
		FuehrungsformAttribute FA1 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.45))
			.benutzungspflicht(Benutzungspflicht.VORHANDEN)
			.build();
		FuehrungsformAttribute FA2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
			.bordstein(Bordstein.UNBEKANNT)
			.belagArt(BelagArt.UNBEKANNT)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.UNBEKANNT)
			.radverkehrsfuehrung(Radverkehrsfuehrung.UNBEKANNT)
			.parkenForm(KfzParkenForm.UNBEKANNT)
			.parkenTyp(KfzParkenTyp.UNBEKANNT)
			.breite(null)
			.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA1)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA1.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isFalse();
	}

	@Test
	void widersprechenSichAttribute_equal_ohne_TrennungZu() {
		// Links
		FuehrungsformAttribute FA1 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenBreiteLinks(Laenge.of(4))
			.build();

		FuehrungsformAttribute FA2 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenBreiteLinks(Laenge.of(4))
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA1)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA1.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isFalse();

		// Rechts
		FuehrungsformAttribute FA3 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenBreiteRechts(Laenge.of(4))
			.build();

		FuehrungsformAttribute FA4 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenBreiteRechts(Laenge.of(4))
			.build();

		assertThat(FA3.widersprechenSichAttribute(FA3)).isFalse();
		assertThat(FA4.widersprechenSichAttribute(FA4)).isFalse();
		assertThat(FA3.widersprechenSichAttribute(FA4)).isFalse();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isFalse();
	}

	@Test
	void widersprechenSichAttribute_TrennstreifenForm_UnEqual() {
		// Links
		FuehrungsformAttribute FA1 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenBreiteLinks(Laenge.of(1))
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.build();

		FuehrungsformAttribute FA2 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteLinks(Laenge.of(1))
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isTrue();

		// Rechts
		FuehrungsformAttribute FA3 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenBreiteRechts(Laenge.of(1))
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.build();

		FuehrungsformAttribute FA4 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteRechts(Laenge.of(1))
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.build();

		assertThat(FA3.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isTrue();
	}

	@Test
	void widersprechenSichAttribute_TrennstreifenForm_UnEqual_null() {
		// Links
		FuehrungsformAttribute FA1 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenBreiteLinks(Laenge.of(1))
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.build();

		FuehrungsformAttribute FA2 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(null)
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isTrue();

		// Rechts
		FuehrungsformAttribute FA3 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(null)
			.build();

		FuehrungsformAttribute FA4 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteRechts(Laenge.of(1))
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.build();

		assertThat(FA3.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isTrue();
	}

	@Test
	void widersprechenSichAttribute_equal() {
		// Links
		FuehrungsformAttribute FA1 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteLinks(Laenge.of(5))
			.build();

		FuehrungsformAttribute FA2 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteLinks(Laenge.of(5))
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA1)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA1.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isFalse();

		// Rechts
		FuehrungsformAttribute FA3 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteRechts(Laenge.of(5))
			.build();

		FuehrungsformAttribute FA4 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteRechts(Laenge.of(5))
			.build();

		assertThat(FA3.widersprechenSichAttribute(FA3)).isFalse();
		assertThat(FA4.widersprechenSichAttribute(FA4)).isFalse();
		assertThat(FA3.widersprechenSichAttribute(FA4)).isFalse();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isFalse();
	}

	@Test
	void widersprechenSichAttribute_equal_null() {
		// Links
		FuehrungsformAttribute FA1 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.build();

		FuehrungsformAttribute FA2 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA1)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA1.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isFalse();

		// Rechts
		FuehrungsformAttribute FA3 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.build();

		FuehrungsformAttribute FA4 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.build();

		assertThat(FA3.widersprechenSichAttribute(FA3)).isFalse();
		assertThat(FA4.widersprechenSichAttribute(FA4)).isFalse();
		assertThat(FA3.widersprechenSichAttribute(FA4)).isFalse();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isFalse();
	}

	@Test
	void widersprechenSichAttribute_TrennungZu_UnEqual() {
		// Links
		FuehrungsformAttribute FA1 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.trennstreifenBreiteLinks(Laenge.of(3))
			.build();

		FuehrungsformAttribute FA2 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteLinks(Laenge.of(3))
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isTrue();

		// Rechts
		FuehrungsformAttribute FA3 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteRechts(Laenge.of(3))
			.build();

		FuehrungsformAttribute FA4 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
			.trennstreifenBreiteRechts(Laenge.of(3))
			.build();

		assertThat(FA3.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isTrue();
	}

	@Test
	void widersprechenSichAttribute_TrennungZu_UnEqual_null() {
		// Links
		FuehrungsformAttribute FA1 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteLinks(Laenge.of(2))
			.build();

		FuehrungsformAttribute FA2 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenTrennungZuLinks(null)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteLinks(Laenge.of(2))
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isTrue();

		// Rechts
		FuehrungsformAttribute FA3 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenTrennungZuRechts(null)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteRechts(Laenge.of(2))
			.build();

		FuehrungsformAttribute FA4 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteRechts(Laenge.of(2))
			.build();

		assertThat(FA3.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isTrue();
	}

	@Test
	void widersprechenSichAttribute_TrennstreifenBreite_equal() {
		// Links
		FuehrungsformAttribute FA1 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteLinks(Laenge.of(2))
			.build();

		FuehrungsformAttribute FA2 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteLinks(Laenge.of(2))
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA1)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA1.widersprechenSichAttribute(FA2)).isFalse();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isFalse();

		// Rechts
		FuehrungsformAttribute FA3 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteRechts(Laenge.of(0.2))
			.build();

		FuehrungsformAttribute FA4 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteRechts(Laenge.of(0.2))
			.build();

		assertThat(FA3.widersprechenSichAttribute(FA3)).isFalse();
		assertThat(FA4.widersprechenSichAttribute(FA4)).isFalse();
		assertThat(FA3.widersprechenSichAttribute(FA4)).isFalse();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isFalse();
	}

	@Test
	void widersprechenSichAttribute_TrennstreifenBreite_UnEqual() {
		// Links
		FuehrungsformAttribute FA1 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteLinks(Laenge.of(2))
			.build();

		FuehrungsformAttribute FA2 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteLinks(Laenge.of(2.5))
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isTrue();

		// Rechts
		FuehrungsformAttribute FA3 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteRechts(Laenge.of(0.1))
			.build();

		FuehrungsformAttribute FA4 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteRechts(Laenge.of(2))
			.build();

		assertThat(FA3.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isTrue();
	}

	@Test
	void widersprechenSichAttribute_TrennstreifenBreite_UnEqual_null() {
		// Links
		FuehrungsformAttribute FA1 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteLinks(Laenge.of(2))
			.build();

		FuehrungsformAttribute FA2 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenBreiteLinks(null)
			.build();

		assertThat(FA1.widersprechenSichAttribute(FA2)).isTrue();
		assertThat(FA2.widersprechenSichAttribute(FA1)).isTrue();

		// Rechts
		FuehrungsformAttribute FA3 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenBreiteRechts(null)
			.build();

		FuehrungsformAttribute FA4 = FuehrungsformAttributeTestDataProvider.createWithValuesButWithoutTrennstreifen()
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenBreiteRechts(Laenge.of(2))
			.build();

		assertThat(FA3.widersprechenSichAttribute(FA4)).isTrue();
		assertThat(FA4.widersprechenSichAttribute(FA3)).isTrue();
	}

	@Test
	void Trennstreifen_radverkehrsfuehrungHatKeinTrennstreifen() {
		// Arrange
		FuehrungsformAttribute.FuehrungsformAttributeBuilder builderValid = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG);

		FuehrungsformAttribute.FuehrungsformAttributeBuilder builderInvalid = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.trennstreifenBreiteLinks(Laenge.of(2))
			.trennstreifenBreiteRechts(Laenge.of(2));

		// Act & Assert - Keine Trennstreifen = Keine Exception
		builderValid.build();

		// Act & Assert - Trennstreifen obwohl nicht erlaubt = Exception
		assertThrows(RequireViolation.class, builderInvalid::build);
	}

	@Test
	void Trennstreifen_radverkehrsfuehrungNurParken() {
		// Arrange
		FuehrungsformAttribute.FuehrungsformAttributeBuilder builderValid = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
			.trennstreifenBreiteLinks(Laenge.of(2))
			.trennstreifenBreiteRechts(Laenge.of(2));

		FuehrungsformAttribute.FuehrungsformAttributeBuilder builderInvalid = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN);

		// Act & Assert - Valide Trennstreifen
		builderValid.build();

		// Act & Assert - Invalide Trennstreifen
		assertThrows(RequireViolation.class, builderInvalid::build);
	}

	@Test
	void Trennstreifen_trennungFormOhneAttribute() {
		// Arrange
		FuehrungsformAttribute.FuehrungsformAttributeBuilder builderInvalid = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
			.trennstreifenFormLinks(TrennstreifenForm.UNBEKANNT)
			.trennstreifenFormRechts(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.trennstreifenBreiteLinks(Laenge.of(2))
			.trennstreifenBreiteRechts(Laenge.of(2));

		FuehrungsformAttribute.FuehrungsformAttributeBuilder builderValid = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
			.trennstreifenFormLinks(TrennstreifenForm.UNBEKANNT)
			.trennstreifenFormRechts(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
			.trennstreifenTrennungZuLinks(null)
			.trennstreifenTrennungZuRechts(null)
			.trennstreifenBreiteLinks(null)
			.trennstreifenBreiteRechts(null);

		// Act & Assert - Valide Trennstreifen
		builderValid.build();

		// Act & Assert - Invalide Trennstreifen
		assertThrows(RequireViolation.class, builderInvalid::build);
	}

	@Test
	void Trennstreifen_initialNullValuesAreAllowed() {
		// Arrange
		FuehrungsformAttribute.FuehrungsformAttributeBuilder builderValid = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
			.trennstreifenFormLinks(null)
			.trennstreifenFormRechts(null)
			.trennstreifenTrennungZuLinks(null)
			.trennstreifenTrennungZuRechts(null)
			.trennstreifenBreiteLinks(null)
			.trennstreifenBreiteRechts(null);

		// Act & Assert - Null-Trennstreifen = aber valide, da vorher schon null.
		builderValid.build();
	}

	@Test
	void Trennstreifen_partialNullValuesAreNotAllowed() {
		// Arrange
		FuehrungsformAttribute.FuehrungsformAttributeBuilder builderInvalid = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte()
			.radverkehrsfuehrung(Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND)
			.trennstreifenFormLinks(null)
			.trennstreifenFormRechts(null)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.trennstreifenTrennungZuRechts(null)
			.trennstreifenBreiteLinks(null)
			.trennstreifenBreiteRechts(Laenge.of(2));

		// Act & Assert
		assertThrows(RequireViolation.class, builderInvalid::build);
	}

	@Test
	void withUmgekehrterStationierungsrichtung() {
		// arrange
		FuehrungsformAttributeBuilder builder = FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
			.belagArt(BelagArt.ASPHALT)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
			.trennstreifenBreiteRechts(Laenge.of(0.1))
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN)
			.trennstreifenTrennungZuLinks(null)
			.trennstreifenBreiteLinks(Laenge.of(0.2));
		FuehrungsformAttribute fuehrungsformAttribute = builder.build();

		// act
		FuehrungsformAttribute withUmgekehrterStationierungsrichtung = fuehrungsformAttribute
			.withUmgekehrterStationierungsrichtung();

		// assert
		assertThat(fuehrungsformAttribute).isEqualTo(builder.build());
		assertThat(withUmgekehrterStationierungsrichtung).isEqualTo(builder
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
			.trennstreifenBreiteLinks(Laenge.of(0.1))
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN)
			.trennstreifenTrennungZuRechts(null)
			.trennstreifenBreiteRechts(Laenge.of(0.2)).build());
	}

}
