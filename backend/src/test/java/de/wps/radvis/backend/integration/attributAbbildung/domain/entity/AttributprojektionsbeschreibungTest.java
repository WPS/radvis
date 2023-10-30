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

package de.wps.radvis.backend.integration.attributAbbildung.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.linearref.LinearLocation;
import org.springframework.test.util.ReflectionTestUtils;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FahrtrichtungAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;

class AttributprojektionsbeschreibungTest {

	// Hier müssen ALLE (nicht linear referenzierte) Attributgruppen getestet werden
	@SuppressWarnings("unchecked")
	@Test
	void testAnteilGleicherNichtLinearReferenzierterAttributgruppenWirdZusammengefasst() {
		// arrange

		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe1 = FahrtrichtungAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();
		fahrtrichtungAttributGruppe1.changeSeitenbezug(true);
		fahrtrichtungAttributGruppe1.setRichtung(Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG);
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe2 = FahrtrichtungAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();
		fahrtrichtungAttributGruppe2.changeSeitenbezug(true);
		fahrtrichtungAttributGruppe2.setRichtung(Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG);

		KantenAttribute kantenAttribute1 = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
			.beleuchtung(Beleuchtung.VORHANDEN).build();
		KantenAttribute kantenAttribute2 = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
			.beleuchtung(Beleuchtung.VORHANDEN).build();

		Set<IstStandard> istStandards1 = new HashSet<>(
			Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.ZIELSTANDARD_RADNETZ));
		Set<IstStandard> istStandards2 = new HashSet<>(
			Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.ZIELSTANDARD_RADNETZ));

		Set<Netzklasse> netzklassen1 = new HashSet<>(
			Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT));
		Set<Netzklasse> netzklassen2 = new HashSet<>(
			Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT));

		Kante kante1 = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppe1)
			.kantenAttributGruppe(
				KantenAttributGruppe.builder().kantenAttribute(kantenAttribute1)
					.netzklassen(netzklassen1).istStandards(istStandards1).build())
			.build();
		Kante kante2 = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppe2)
			.kantenAttributGruppe(
				KantenAttributGruppe.builder().kantenAttribute(kantenAttribute2)
					.netzklassen(netzklassen2).istStandards(istStandards2).build())
			.build();

		KantenSegment kantenSegment = new KantenSegment(
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 0.5), new LinearLocation(0, 0),
				new LinearLocation(0, 0), false),
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0.5, 1), new LinearLocation(0, 0),
				new LinearLocation(0, 0), false),
			kante1, KanteTestDataProvider.withDefaultValues().build()
		);

		KantenSegment kantenSegment2 = new KantenSegment(
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0.5, 1.), new LinearLocation(0, 0),
				new LinearLocation(0, 0), false),
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 0.5), new LinearLocation(0, 0),
				new LinearLocation(0, 0), false),
			kante2, KanteTestDataProvider.withDefaultValues().build()
		);

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(
			KanteTestDataProvider.withDefaultValues().build());

		// act
		attributprojektionsbeschreibung.addSegment(kantenSegment);
		attributprojektionsbeschreibung.addSegment(kantenSegment2);

		// seitenbezogene attribute werden nach richtung getrennt gespeichert
		assertThat(attributprojektionsbeschreibung.getSeitenbezogeneProjizierteAttribute().stream()
			.map(SeitenbezogeneProjizierteAttribute::getRichtung))
			.containsExactly(fahrtrichtungAttributGruppe1.getFahrtrichtungLinks(),
				fahrtrichtungAttributGruppe2.getFahrtrichtungLinks());
		assertThat(attributprojektionsbeschreibung.getSeitenbezogeneProjizierteAttribute().stream()
			.map(SeitenbezogeneProjizierteAttribute::getRichtung))
			.containsExactly(fahrtrichtungAttributGruppe1.getFahrtrichtungLinks(),
				fahrtrichtungAttributGruppe2.getFahrtrichtungLinks());
		assertThat(attributprojektionsbeschreibung.getSeitenbezogeneProjizierteAttribute().stream()
			.map(SeitenbezogeneProjizierteAttribute::getLinearReferenzierterAbschnittAufZielnetzkante))
			.containsExactly(LinearReferenzierterAbschnitt.of(0., 0.5), LinearReferenzierterAbschnitt.of(0.5, 1.));

		Map<KantenAttribute, Double> potentiellInkonsistenteProjizierteKantenattributeZuAnteil =
			(Map<KantenAttribute, Double>) ReflectionTestUtils
				.getField(attributprojektionsbeschreibung,
					"potentiellInkonsistenteProjizierteKantenattributeZuAnteil");
		assertThat(potentiellInkonsistenteProjizierteKantenattributeZuAnteil)
			.containsOnlyKeys(kantenAttribute1);
		assertThat(potentiellInkonsistenteProjizierteKantenattributeZuAnteil)
			.containsEntry(kantenAttribute1, 1.);

		Map<Set<IstStandard>, Double> potentiellInkonsistenteProjizierteIstStandards =
			(Map<Set<IstStandard>, Double>) ReflectionTestUtils
				.getField(attributprojektionsbeschreibung,
					"potentiellInkonsistenteProjizierteIstStandards");
		assertThat(potentiellInkonsistenteProjizierteIstStandards)
			.containsOnlyKeys(istStandards1);
		assertThat(potentiellInkonsistenteProjizierteIstStandards)
			.containsEntry(istStandards1, 1.);

		Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>> potentiellInkonsistenteProjizierteNetzklassen =
			(Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>>) ReflectionTestUtils
				.getField(attributprojektionsbeschreibung,
					"potentiellInkonsistenteProjizierteNetzklassen");
		assertThat(potentiellInkonsistenteProjizierteNetzklassen)
			.containsOnlyKeys(netzklassen1);
		assertThat(potentiellInkonsistenteProjizierteNetzklassen)
			.containsEntry(netzklassen1, List.of(
				LinearReferenzierterAbschnitt.of(0, 0.5), LinearReferenzierterAbschnitt.of(0.5, 1.)));
	}

	// Hier müssen ALLE (nicht linear referenzierte) Attributgruppen getestet werden
	@SuppressWarnings("unchecked")
	@Test
	void testAnteilUnGleicherNichtLinearReferenzierterAttributgruppenWirdSeparatGespeichert() {
		// arrange

		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe1 = FahrtrichtungAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();
		fahrtrichtungAttributGruppe1.changeSeitenbezug(true);
		fahrtrichtungAttributGruppe1.setRichtung(Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG);
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe2 = FahrtrichtungAttributGruppeTestDataProvider
			.withGrundnetzDefaultwerte().build();
		fahrtrichtungAttributGruppe2.changeSeitenbezug(true);
		fahrtrichtungAttributGruppe2.setRichtung(Richtung.GEGEN_RICHTUNG, Richtung.IN_RICHTUNG);

		KantenAttribute kantenAttribute1 = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
			.beleuchtung(Beleuchtung.NICHT_VORHANDEN).build();
		KantenAttribute kantenAttribute2 = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
			.beleuchtung(Beleuchtung.UNBEKANNT).build();

		Set<IstStandard> istStandards1 = new HashSet<>(
			Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.RADVORRANGROUTEN));
		Set<IstStandard> istStandards2 = new HashSet<>(
			Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.ZIELSTANDARD_RADNETZ));

		Set<Netzklasse> netzklassen1 = new HashSet<>(
			Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT));
		Set<Netzklasse> netzklassen2 = new HashSet<>(
			Set.of(Netzklasse.RADNETZ_ALLTAG));

		Kante kante1 = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppe1)
			.kantenAttributGruppe(
				KantenAttributGruppe.builder().kantenAttribute(kantenAttribute1)
					.istStandards(istStandards1)
					.netzklassen(netzklassen1)
					.build())
			.build();
		Kante kante2 = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppe2)
			.kantenAttributGruppe(
				KantenAttributGruppe.builder().kantenAttribute(kantenAttribute2)
					.istStandards(istStandards2)
					.netzklassen(netzklassen2)
					.build())
			.build();

		KantenSegment kantenSegment = new KantenSegment(
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 0.5), new LinearLocation(0, 0),
				new LinearLocation(0, 0), false),
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0.5, 1), new LinearLocation(0, 0),
				new LinearLocation(0, 0), false),
			kante1, KanteTestDataProvider.withDefaultValues().build()
		);

		KantenSegment kantenSegment2 = new KantenSegment(
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0.5, 1.), new LinearLocation(0, 0),
				new LinearLocation(0, 0), false),
			new LineareReferenzProjektionsergebnis(LinearReferenzierterAbschnitt.of(0, 0.5), new LinearLocation(0, 0),
				new LinearLocation(0, 0), false),
			kante2, KanteTestDataProvider.withDefaultValues().build()
		);

		Attributprojektionsbeschreibung attributprojektionsbeschreibung = new Attributprojektionsbeschreibung(
			KanteTestDataProvider.withDefaultValues().build());

		// act
		attributprojektionsbeschreibung.addSegment(kantenSegment);
		attributprojektionsbeschreibung.addSegment(kantenSegment2);

		assertThat(attributprojektionsbeschreibung.getSeitenbezogeneProjizierteAttribute().stream()
			.map(SeitenbezogeneProjizierteAttribute::getRichtung))
			.containsExactly(fahrtrichtungAttributGruppe1.getFahrtrichtungLinks(),
				fahrtrichtungAttributGruppe2.getFahrtrichtungLinks());
		assertThat(attributprojektionsbeschreibung.getSeitenbezogeneProjizierteAttribute().stream()
			.map(SeitenbezogeneProjizierteAttribute::getRichtung))
			.containsExactly(fahrtrichtungAttributGruppe1.getFahrtrichtungLinks(),
				fahrtrichtungAttributGruppe2.getFahrtrichtungLinks());
		assertThat(attributprojektionsbeschreibung.getSeitenbezogeneProjizierteAttribute().stream()
			.map(SeitenbezogeneProjizierteAttribute::getLinearReferenzierterAbschnittAufZielnetzkante))
			.containsExactly(LinearReferenzierterAbschnitt.of(0., 0.5), LinearReferenzierterAbschnitt.of(0.5, 1.));

		Map<KantenAttribute, Double> potentiellInkonsistenteProjizierteKantenattributeZuAnteil =
			(Map<KantenAttribute, Double>) ReflectionTestUtils.getField(attributprojektionsbeschreibung,
				"potentiellInkonsistenteProjizierteKantenattributeZuAnteil");
		assertThat(potentiellInkonsistenteProjizierteKantenattributeZuAnteil)
			.containsOnlyKeys(kantenAttribute1, kantenAttribute2);
		assertThat(potentiellInkonsistenteProjizierteKantenattributeZuAnteil)
			.containsEntry(kantenAttribute1, .5);
		assertThat(potentiellInkonsistenteProjizierteKantenattributeZuAnteil)
			.containsEntry(kantenAttribute2, .5);

		Map<Set<IstStandard>, Double> potentiellInkonsistenteProjizierteIstStandards =
			(Map<Set<IstStandard>, Double>) ReflectionTestUtils.getField(attributprojektionsbeschreibung,
				"potentiellInkonsistenteProjizierteIstStandards");
		assertThat(potentiellInkonsistenteProjizierteIstStandards)
			.containsOnlyKeys(istStandards1, istStandards2);
		assertThat(potentiellInkonsistenteProjizierteIstStandards)
			.containsEntry(istStandards1, .5);
		assertThat(potentiellInkonsistenteProjizierteIstStandards)
			.containsEntry(istStandards2, .5);

		Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>> potentiellInkonsistenteProjizierteNetzklassen =
			(Map<Set<Netzklasse>, List<LinearReferenzierterAbschnitt>>) ReflectionTestUtils.getField(attributprojektionsbeschreibung,
				"potentiellInkonsistenteProjizierteNetzklassen");
		assertThat(potentiellInkonsistenteProjizierteNetzklassen)
			.containsOnlyKeys(netzklassen1, netzklassen2);
		assertThat(potentiellInkonsistenteProjizierteNetzklassen)
			.containsEntry(netzklassen1, List.of(LinearReferenzierterAbschnitt.of(0, 0.5)));
		assertThat(potentiellInkonsistenteProjizierteNetzklassen)
			.containsEntry(netzklassen2, List.of(LinearReferenzierterAbschnitt.of(0.5, 1.)));
	}
}
