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

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;

public class FuehrungsformAttributGruppeTest {

	@Test
	public void changeSeitenbezug_toTrue() {
		// Arrange
		FuehrungsformAttribute fuehrungsFormAttribute1 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 0.5))
			.bordstein(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();
		FuehrungsformAttribute fuehrungsFormAttribute2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1.0))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.ASPHALT)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.SEHR_GUTER_BIS_GUTER_ZUSTAND)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(
			Arrays.asList(new FuehrungsformAttribute[] { fuehrungsFormAttribute1, fuehrungsFormAttribute2 }), false);

		// Act
		fuehrungsformAttributGruppe.changeSeitenbezug(true);

		// Assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks()).hasSize(2);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks()).contains(
			fuehrungsFormAttribute1);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks()).contains(
			fuehrungsFormAttribute2);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts()).contains(
			fuehrungsFormAttribute1);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts()).contains(
			fuehrungsFormAttribute2);
		assertThat(fuehrungsformAttributGruppe.isZweiseitig()).isTrue();
	}

	@Test
	public void changeSeitenbezug_toFalse() {
		// Arrange
		FuehrungsformAttribute links1 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 0.6))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();
		FuehrungsformAttribute links2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, 1.))
			.bordstein(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttribute rechts1 = FuehrungsformAttribute.builder()
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
		FuehrungsformAttribute rechts2 = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 1.))
			.bordstein(Bordstein.KEINE_ABSENKUNG)
			.belagArt(BelagArt.BETON)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.NEUWERTIG)
			.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
			.parkenForm(KfzParkenForm.FAHRBAHNPARKEN_MARKIERT)
			.parkenTyp(KfzParkenTyp.LAENGS_PARKEN)
			.breite(Laenge.of(3.4))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.build();

		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe.FuehrungsformAttributGruppeBuilder()
			.isZweiseitig(true)
			.fuehrungsformAttributeLinks(new ArrayList<FuehrungsformAttribute>(Arrays.asList(links1, links2)))
			.fuehrungsformAttributeRechts(new ArrayList<FuehrungsformAttribute>(Arrays.asList(rechts1, rechts2)))
			.build();

		// Act
		fuehrungsformAttributGruppe.changeSeitenbezug(false);

		// Assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks()).hasSize(2);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks()).contains(links1);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks()).contains(links2);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts()).hasSize(2);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts()).contains(links1);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts()).contains(links2);
		assertThat(fuehrungsformAttributGruppe.isZweiseitig()).isFalse();
	}
}
