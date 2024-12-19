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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
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
	public void insert_einseitig_splitExistingLR() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
				.belagArt(BelagArt.ASPHALT).build()),
			false);

		// act
		fuehrungsformAttributGruppe
			.insert(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.NATURSTEINPFLASTER).build());

		// assert
		List<FuehrungsformAttribute> expectedValues = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.3)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.NATURSTEINPFLASTER).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1)
				.belagArt(BelagArt.ASPHALT).build());

		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrderElementsOf(expectedValues);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrderElementsOf(expectedValues);
	}

	@Test
	public void insert_einseitig_defragmentieren() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
				.belagArt(BelagArt.ASPHALT).build()),
			false);

		// act
		fuehrungsformAttributGruppe
			.insert(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.BETON).build());

		// assert
		List<FuehrungsformAttribute> expectedValues = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.6)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1)
				.belagArt(BelagArt.ASPHALT).build());

		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrderElementsOf(expectedValues);
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrderElementsOf(expectedValues);
	}

	@Test
	public void insert_zweiseitig_throws() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
				.belagArt(BelagArt.ASPHALT).build()),
			true);

		// act + assert
		assertThrows(RequireViolation.class, () -> fuehrungsformAttributGruppe
			.insert(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.NATURSTEINPFLASTER).build()));
	}

	@Test
	public void insertLinks_einseitig_throws() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
				.belagArt(BelagArt.ASPHALT).build()),
			false);

		// act + assert
		assertThrows(RequireViolation.class, () -> fuehrungsformAttributGruppe
			.insertLinks(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.NATURSTEINPFLASTER).build()));
	}

	@Test
	public void insertLinks_zweiseitig_splitExistingLR() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1)
				.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build()),
			List.of(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5)
					.belagArt(BelagArt.BETON).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
					.belagArt(BelagArt.ASPHALT).build()),
			true);

		// act
		fuehrungsformAttributGruppe
			.insertLinks(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.NATURSTEINPFLASTER).build());

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.3)
				.belagArt(BelagArt.BETON).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
					.belagArt(BelagArt.NATURSTEINPFLASTER).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1)
					.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build());
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrderElementsOf(
				fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts());
	}

	@Test
	public void insertLinks_zweiseitig_defragmentieren() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1)
				.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build()),
			List.of(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5)
					.belagArt(BelagArt.BETON).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
					.belagArt(BelagArt.ASPHALT).build()),
			true);

		// act
		fuehrungsformAttributGruppe
			.insertLinks(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.BETON).build());

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.6)
				.belagArt(BelagArt.BETON).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1)
					.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build());
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrderElementsOf(
				fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts());
	}

	@Test
	public void insertRechts_einseitig_throws() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
				.belagArt(BelagArt.ASPHALT).build()),
			false);

		// act + assert
		assertThrows(RequireViolation.class, () -> fuehrungsformAttributGruppe
			.insertRechts(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.NATURSTEINPFLASTER).build()));
	}

	@Test
	public void insertRechts_zweiseitig_splitExistingLR() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1)
				.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build()),
			List.of(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5)
					.belagArt(BelagArt.BETON).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
					.belagArt(BelagArt.ASPHALT).build()),
			true);

		// act
		fuehrungsformAttributGruppe
			.insertRechts(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.NATURSTEINPFLASTER).build());

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrder(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.3)
				.belagArt(BelagArt.BETON).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
					.belagArt(BelagArt.NATURSTEINPFLASTER).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1)
					.belagArt(BelagArt.ASPHALT).build());
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrderElementsOf(
				fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks());
	}

	@Test
	public void insertRechts_zweiseitig_defragmentieren() {
		// arrange
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = new FuehrungsformAttributGruppe(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1)
				.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build()),
			List.of(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5)
					.belagArt(BelagArt.BETON).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
					.belagArt(BelagArt.ASPHALT).build()),
			true);

		// act
		fuehrungsformAttributGruppe
			.insertRechts(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.BETON).build());

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrder(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.6)
				.belagArt(BelagArt.BETON).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1)
					.belagArt(BelagArt.ASPHALT).build());
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrderElementsOf(
				fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks());
	}

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

	@Test
	void mergeSegmentsKleinerAls_noMatchingSegment_doesNothing() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4).belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1).belagArt(BelagArt.BETON).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).fuehrungsformAttributeRechts(fuehrungsformAttribute)
			.build();
		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.39));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrderElementsOf(fuehrungsformAttribute);
	}

	@Test
	void mergeSegmentsKleinerAls_segmentAtBeginning_mergeRight() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttributeLinks = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 0.6).belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1).belagArt(BelagArt.BETON).build());
		List<FuehrungsformAttribute> fuehrungsformAttributeRechts = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 0.5).belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1).belagArt(BelagArt.BETON).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttributeLinks).isZweiseitig(true)
			.fuehrungsformAttributeRechts(fuehrungsformAttributeRechts).build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.6).belagArt(BelagArt.ASPHALT).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1).belagArt(BelagArt.BETON).build());
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5).belagArt(BelagArt.ASPHALT).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1).belagArt(BelagArt.BETON).build());
	}

	@Test
	void mergeSegmentsKleinerAls_mergeAll() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttributeLinks = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.2).belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 0.6).belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1)
				.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build());
		List<FuehrungsformAttribute> fuehrungsformAttributeRechts = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.2).belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 0.5).belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1)
				.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttributeLinks).isZweiseitig(true)
			.fuehrungsformAttributeRechts(fuehrungsformAttributeRechts).build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(1.0));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
					.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build());
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
					.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build());
	}

	@Test
	void mergeSegmentsKleinerAls_segmentAtEnd_mergeLeft() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4).belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 0.8).belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.8, 1).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).fuehrungsformAttributeRechts(fuehrungsformAttribute)
			.build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4).belagArt(BelagArt.ASPHALT).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1).belagArt(BelagArt.BETON).build());
	}

	@Test
	void mergeSegmentsKleinerAls_segmentInTheMiddle_mergeRight() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4).belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 0.6).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1).belagArt(BelagArt.BETON).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).fuehrungsformAttributeRechts(fuehrungsformAttribute)
			.build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4).belagArt(BelagArt.ASPHALT).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1).belagArt(BelagArt.BETON).build());
	}

	@Test
	void mergeSegmentsKleinerAls_segmentInTheMiddle_mergeUndFasseZusammen() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4).belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 0.6).belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1).belagArt(BelagArt.ASPHALT).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).fuehrungsformAttributeRechts(fuehrungsformAttribute)
			.build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactly(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).belagArt(BelagArt.ASPHALT).build());
	}

	@Test
	void mergeSegmentsKleinerAls_konsekutivSegments_mergeBoth() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.1).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.1, 0.2).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 0.55).belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.55, 1).belagArt(BelagArt.BETON).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).fuehrungsformAttributeRechts(fuehrungsformAttribute)
			.build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.55).belagArt(BelagArt.ASPHALT).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.55, 1).belagArt(BelagArt.BETON).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoSegments_mergeLeftAndRight() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 0.8).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.8, 1).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).fuehrungsformAttributeRechts(fuehrungsformAttribute)
			.build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoSegments_mergeRight() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.1).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.1, 0.5).belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 0.6).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1).belagArt(BelagArt.BETON).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).fuehrungsformAttributeRechts(fuehrungsformAttribute)
			.build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.5).belagArt(BelagArt.ASPHALT).build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.5, 1).belagArt(BelagArt.BETON).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoMiniSegmentsAtEnd_mergeTowardsBigSegment() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 0.6)
				.belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 0.8)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.8, 1)
				.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).fuehrungsformAttributeRechts(fuehrungsformAttribute)
			.build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactly(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
				.belagArt(BelagArt.ASPHALT).build());
	}

	@Test
	void mergeSegmentsKleinerAls_onlyMiniSegments_mergeAll() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.3).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, 0.6)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1)
				.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).fuehrungsformAttributeRechts(fuehrungsformAttribute)
			.build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.5));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactly(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
				.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build());
	}

	@Test
	void mergeSegmentsKleinerAls_twoMiniSegmentsAtStart_mergeTowardsBigSegment() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.2).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.2, 0.4)
				.belagArt(BelagArt.ASPHALT).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 0.8)
				.belagArt(BelagArt.BETON).build(),
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.8, 1)
				.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).fuehrungsformAttributeRechts(fuehrungsformAttribute)
			.build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactly(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
				.belagArt(BelagArt.BETON).build());
	}

	@Test
	void mergeSegmentsKleinerAls_onlyOneSegment_doesNothing() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttribute = List
			.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).build());
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(fuehrungsformAttribute).build();

		// act
		fuehrungsformAttributGruppe.mergeSegmentsKleinerAls(LineareReferenz.of(0.3));

		// assert
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).build());
		assertThat(fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrder(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1).build());

	}
}
