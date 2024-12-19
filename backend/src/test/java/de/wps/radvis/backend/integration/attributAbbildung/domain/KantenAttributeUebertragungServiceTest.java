/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FahrtrichtungAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class KantenAttributeUebertragungServiceTest {

	RecursiveComparisonConfiguration attributeComparisonConfiguration;
	KantenAttributeUebertragungService kantenAttributeUebertragungService;

	@BeforeEach
	public void setUp() {
		attributeComparisonConfiguration = RecursiveComparisonConfiguration
			.builder()
			.withIgnoreAllOverriddenEquals(true)
			.build();

		kantenAttributeUebertragungService = new KantenAttributeUebertragungService(Laenge.of(3));
	}

	@Test
	public void testUebertrageAllgemeinAttribute() {
		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 0).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 0).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten1)
			.nachKnoten(knoten2)
			.kantenAttributGruppe(
				KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADVORRANGROUTEN))
					.kantenAttribute(
						KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
							.beleuchtung(Beleuchtung.VORHANDEN)
							.dtvPkw(VerkehrStaerke.of(1))
							.build()
					)
					.build()
			)
			.build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten2)
			.nachKnoten(knoten3)
			.kantenAttributGruppe(
				KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADNETZ_ALLTAG))
					.kantenAttribute(
						KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
							.beleuchtung(Beleuchtung.VORHANDEN)
							.dtvPkw(VerkehrStaerke.of(2))
							.build()
					)
					.build()
			)
			.build();

		Kante aufKante = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten2)
			.nachKnoten(knoten4)
			.build();

		// Act
		kantenAttributeUebertragungService.uebertrageGleicheAttribute(List.of(kante1, kante2), knoten2, aufKante,
			false);

		// Assert
		assertThat(aufKante.getKantenAttributGruppe().getNetzklassen()).containsExactlyInAnyOrder(
			Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.RADVORRANGROUTEN);
		assertThat(aufKante.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung()).isEqualTo(
			Beleuchtung.VORHANDEN);
		assertThat(aufKante.getKantenAttributGruppe().getKantenAttribute().getDtvPkw()).isNotPresent();
	}

	@Test
	public void testUebertrageGleicheAttribute_ohneZweiseitigkeit() {
		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 0).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 0).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		/*
			Kanten:
		
				   4
				   ↑
				   |
			1 ---> 2 ---> 3
		 */

		// Eine Kante führt zu Knoten 2, die andere beginnt dort. Damit sind die Seiten bei beiden Kanten ungleich
		// orientiert und werden _nicht_ übertragen, da nicht eindeutig ist welche rechte Seite von Kante 1 bzw. 2 auf
		// der "aufKante" die rechte Seite sein soll (links analog), da die eine rechte Seite rechts und die andere
		// links auf der "aufKante" liegt.
		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten1)
			.nachKnoten(knoten2)
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG))
					.build()
			)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()
					))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()
					))
					.build()
			)
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.geschwindigkeitAttribute(List.of(
						GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH)
							.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
								Hoechstgeschwindigkeit.MAX_30_KMH)
							.ortslage(KantenOrtslage.INNERORTS)
							.build()
					))
					.build()
			)
			.build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten2)
			.nachKnoten(knoten3)
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT))
					.build()
			)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER)
							.build()
					))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER)
							.build()
					))
					.build()
			)
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.geschwindigkeitAttribute(List.of(
						GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
							.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
								Hoechstgeschwindigkeit.MAX_50_KMH)
							.ortslage(KantenOrtslage.INNERORTS)
							.build()
					))
					.build()
			)
			.build();

		Kante aufKante = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten2)
			.nachKnoten(knoten4)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();

		// Act
		kantenAttributeUebertragungService.uebertrageGleicheAttribute(List.of(kante1, kante2), knoten2, aufKante,
			false);

		// Assert
		assertThat(aufKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0)
			.getBelagArt()).isEqualTo(BelagArt.ASPHALT);
		assertThat(aufKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0)
			.getBordstein()).isEqualTo(Bordstein.UNBEKANNT);
		assertThat(aufKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0)
			.sindAttributeGleich(aufKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()
				.get(0))).isTrue();
		assertThat(aufKante.getKantenAttributGruppe().getNetzklassen()).containsExactlyInAnyOrder(
			Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT);

		assertThat(aufKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute()).hasSize(1);
		assertThat(aufKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0))
			.usingRecursiveComparison()
			.isEqualTo(kante1.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0));
	}

	@Test
	public void testUebertrageGleicheAttribute_trennstreifenNurAufEinerSeite() {
		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 0).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 0).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 0).id(3L).build();

		/*
			Kanten:
		
			1 <--- 2 ---> 3
		
			Simuliert wird hier, dass wir eine Hilfskante 2->1 haben auf die Attribute übernommen werden müssen. Zum
			Konzept Hilfskanten siehe AttributlueckenService.
		 */

		Kante aufKante = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten2)
			.nachKnoten(knoten1)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true).build())
			.build();
		Kante kante23 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten2)
			.nachKnoten(knoten3)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
							.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
							.trennstreifenBreiteLinks(Laenge.of(0.5))
							.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
							.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)
							.trennstreifenBreiteRechts(Laenge.of(1.23))
							.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
							.build()
					))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.build()
					))
					.build()
			)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true).build())
			.build();

		// Act
		kantenAttributeUebertragungService.uebertrageGleicheAttribute(List.of(kante23), knoten2, aufKante, false);

		// Assert
		FuehrungsformAttribute actualLinks = aufKante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks().get(0);
		FuehrungsformAttribute actualRechts = aufKante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts().get(0);
		FuehrungsformAttribute expectedLinks = kante23.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks().get(0);
		FuehrungsformAttribute expectedRechts = kante23.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts().get(0);

		// Trennstreifen A auf kante23 wurde auf D der aufKante übertragen
		assertThat(actualRechts.getTrennstreifenFormRechts()).isEqualTo(expectedLinks.getTrennstreifenFormLinks());
		assertThat(actualRechts.getTrennstreifenBreiteRechts()).isEqualTo(expectedLinks.getTrennstreifenBreiteLinks());
		assertThat(actualRechts.getTrennstreifenTrennungZuRechts()).isEqualTo(expectedLinks
			.getTrennstreifenTrennungZuLinks());

		// Trennstreifen B auf kante23 wurde auf C der aufKante übertragen
		assertThat(actualRechts.getTrennstreifenFormLinks()).isEqualTo(expectedLinks.getTrennstreifenFormRechts());
		assertThat(actualRechts.getTrennstreifenBreiteLinks()).isEqualTo(expectedLinks.getTrennstreifenBreiteRechts());
		assertThat(actualRechts.getTrennstreifenTrennungZuLinks()).isEqualTo(expectedLinks
			.getTrennstreifenTrennungZuRechts());

		// Trennstreifen C auf kante23 wurde auf B der aufKante übertragen
		assertThat(actualLinks.getTrennstreifenFormRechts()).isEqualTo(expectedRechts.getTrennstreifenFormLinks());
		assertThat(actualLinks.getTrennstreifenBreiteRechts()).isEqualTo(expectedRechts.getTrennstreifenBreiteLinks());
		assertThat(actualLinks.getTrennstreifenTrennungZuRechts()).isEqualTo(expectedRechts
			.getTrennstreifenTrennungZuLinks());

		// Trennstreifen D auf kante23 wurde auf A der aufKante übertragen
		assertThat(actualLinks.getTrennstreifenFormLinks()).isEqualTo(expectedRechts.getTrennstreifenFormRechts());
		assertThat(actualLinks.getTrennstreifenBreiteLinks()).isEqualTo(expectedRechts.getTrennstreifenBreiteRechts());
		assertThat(actualLinks.getTrennstreifenTrennungZuLinks()).isEqualTo(expectedRechts
			.getTrennstreifenTrennungZuRechts());
	}

	@Test
	public void testUebertrageGleicheAttribute_mitZweiseitigkeit_kantenInGleicheRichtungen_ungleicheAttribute() {
		/*
		       4
		       ↑
		       |
		       2
		      ↗ \
		     |   ↓
		     1   3
		
		Auf die Kante 2-4 soll von 1-2 und 2-3 übertragen werden. Damit wird die RECHTE Seite der Kante 1-2 und die
		LINKE Seite der Kante 2-3 auf die Lücke 2-4 übertragen (und umgekehrt). Das liegt daran, weil aus Sicht von der
		Kante 2-4 sind die Kanten 1-2 und 2-3 entgegengesetzt. Die rechte Seite von 1-2 entspricht der rechten Seite
		von 2-4. Aber die rechte Seite von 2-3 entspricht der LINKEN Seite von 2-4.
		 */

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 0).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 0).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		// Rechts mit dummy Werten und links mit komplett anderen dummy Werten. Zwei Überschneidungen um zu schauen, dass
		// gleiche STS Attribute aber in Verbindung mit den anderen nicht übernommenen Attributen (bspw. der
		// Radverkehrsführung) zu ungültigen Zuständen führen und damit nicht übernommen werden.
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformAttributeRechtsBuilder = FuehrungsformAttributeTestDataProvider
			.createWithValues()
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART);
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformAttributeLinksBuilder = FuehrungsformAttribute
			.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 1.))
			.bordstein(Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER)
			.belagArt(BelagArt.ASPHALT)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE)
			.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND)
			.parkenForm(KfzParkenForm.PARKBUCHTEN)
			.parkenTyp(KfzParkenTyp.PARKEN_VERBOTEN)
			.breite(Laenge.of(4.56))
			.benutzungspflicht(Benutzungspflicht.NICHT_VORHANDEN)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART) // Gleich rechter Seite rechter STS
			.trennstreifenBreiteLinks(Laenge.of(2.3))
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG)
			.trennstreifenBreiteRechts(Laenge.of(5.67))
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN); // Gleich rechter Seite linker STS

		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten1)
			.nachKnoten(knoten2)
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true).build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeLinksBuilder.build()))
					.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeRechtsBuilder.build()))
					.build()
			)
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.geschwindigkeitAttribute(List.of(
						GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH)
							.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
								Hoechstgeschwindigkeit.MAX_50_KMH)
							.ortslage(KantenOrtslage.INNERORTS)
							.build()
					))
					.build()
			)
			.build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten2)
			.nachKnoten(knoten3)
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true).build())
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeLinksBuilder.build()))
					.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeRechtsBuilder.build()))
					.build()
			)
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.geschwindigkeitAttribute(List.of(
						GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH)
							.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
								Hoechstgeschwindigkeit.MAX_30_KMH)
							.ortslage(KantenOrtslage.INNERORTS)
							.build()
					))
					.build()
			)
			.build();

		Kante aufKante = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten2)
			.nachKnoten(knoten4)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();

		// Act
		kantenAttributeUebertragungService.uebertrageGleicheAttribute(List.of(kante1, kante2), knoten2, aufKante,
			false);

		// Assert
		FuehrungsformAttribute expectedFuehrungsform = FuehrungsformAttributeTestDataProvider
			.withGrundnetzDefaultwerte().build();
		assertThat(aufKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0))
			.usingRecursiveComparison()
			.isEqualTo(expectedFuehrungsform);

		assertThat(aufKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute()).hasSize(1);
		assertThat(aufKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0))
			.usingRecursiveComparison()
			.isEqualTo(kante1.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0));
	}

	@Test
	public void testUebertrageGleicheAttribute_mitZweiseitigkeit_kantenInUnterschiedlicheRichtungen() {
		/*
		       4
		       ↑
		       |
		1 ---> 2 <--- 3
		
		Damit wird die rechte Seite der Kante 1-2 und auch die rechte Seite der Kante 3-2 auf die Lücke 2-4 übertragen.
		(links analog)
		 */

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 0).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 0).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Foostadt")
			.build();

		Kante kante1 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten1)
			.nachKnoten(knoten2)
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true)
				.fahrtrichtungRechts(Richtung.IN_RICHTUNG)
				.fahrtrichtungLinks(Richtung.BEIDE_RICHTUNGEN)
				.build()
			)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.belagArt(BelagArt.ASPHALT)
							.build()
					))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.belagArt(BelagArt.BETON)
							.build()
					))
					.build()
			)
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppe.builder()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
							.vereinbarungsKennung(VereinbarungsKennung.of("Vereinbaru"))
							.erhaltsZustaendiger(gebietskoerperschaft)
							.unterhaltsZustaendiger(gebietskoerperschaft)
							.baulastTraeger(gebietskoerperschaft)
							.build()
					)
					)
					.build()
			)
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder()
					.geschwindigkeitAttribute(List.of(
						GeschwindigkeitAttribute.builder()
							.ortslage(KantenOrtslage.INNERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH)
							.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
								Hoechstgeschwindigkeit.MAX_9_KMH)
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
							.build()
					)
					)
					.build()
			)
			.build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten3)
			.nachKnoten(knoten2)
			.isZweiseitig(true)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true)
				.fahrtrichtungRechts(Richtung.GEGEN_RICHTUNG) // <- anders als bei kante1
				.fahrtrichtungLinks(Richtung.BEIDE_RICHTUNGEN)
				.build()
			)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.belagArt(BelagArt.ASPHALT)
							.build()
					))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.belagArt(BelagArt.BETON)
							.build()
					))
					.build()
			)
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppe.builder()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
							.vereinbarungsKennung(VereinbarungsKennung.of("Vereinbaru"))
							.erhaltsZustaendiger(gebietskoerperschaft)
							.unterhaltsZustaendiger(gebietskoerperschaft)
							.baulastTraeger(gebietskoerperschaft)
							.build()
					)
					)
					.build()
			)
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder()
					.geschwindigkeitAttribute(List.of(
						GeschwindigkeitAttribute.builder()
							.ortslage(KantenOrtslage.INNERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_30_KMH) // <- anders als bei kante1
							.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
								Hoechstgeschwindigkeit.MAX_9_KMH)
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
							.build()
					)
					)
					.build()
			)
			.build();

		Kante aufKante = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten2)
			.nachKnoten(knoten4)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();

		// Act
		kantenAttributeUebertragungService.uebertrageGleicheAttribute(List.of(kante1, kante2), knoten2, aufKante,
			false);

		// Assert
		assertThat(aufKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0)
			.getBelagArt()).isEqualTo(BelagArt.ASPHALT);
		assertThat(aufKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0)
			.getBelagArt()).isEqualTo(BelagArt.BETON);

		assertThat(aufKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(Richtung.UNBEKANNT);
		assertThat(aufKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
			Richtung.BEIDE_RICHTUNGEN);

		ZustaendigkeitAttribute expectedZustaendigkeitAttribute = kante1.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().get(0);
		assertThat(aufKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute().get(0))
			.usingRecursiveComparison()
			.isEqualTo(expectedZustaendigkeitAttribute);

		GeschwindigkeitAttribute expectedGeschwindigkeitAttribute = GeschwindigkeitAttribute.builder()
			.ortslage(KantenOrtslage.INNERORTS)
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UNBEKANNT)
			.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_9_KMH)
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();
		assertThat(aufKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0))
			.usingRecursiveComparison()
			.isEqualTo(expectedGeschwindigkeitAttribute);
	}

	@Test
	public void testUebertrageGleicheAttribute_netzklassenUndIstStandards() {
		/*
		4 <--- 5 ---> 6
		       ↑
		       |
		       ↓
		1 ---> 2 <--- 3
		
		Die anliegenden Kanten 1-2, 3-2, 5-4 und 5-6 haben unterschiedliche Kombinationen an Netzklassen. Es sollen die
		übertragen werden, die an den unteren Kanten (1-2 und 3-2) und an den oberen Kanten (5-4 und 5-6) attribuiert sind.
		 */

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 0).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 0).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 0).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(10, 10).id(4L).build();
		Knoten knoten5 = KnotenTestDataProvider.withPosition(20, 10).id(5L).build();
		Knoten knoten6 = KnotenTestDataProvider.withPosition(30, 10).id(6L).build();

		Kante kante12 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten1)
			.nachKnoten(knoten2)
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					// Kreisnetz existiert nur hier und soll daher nicht übernommen werden.
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_ALLTAG))
					.istStandards(Set.of(IstStandard.RADSCHNELLVERBINDUNG))
					.build()
			)
			.build();
		Kante kante32 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten3)
			.nachKnoten(knoten2)
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					.netzklassen(Set.of(Netzklasse.RADNETZ_FREIZEIT, Netzklasse.KREISNETZ_ALLTAG))
					.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.RADSCHNELLVERBINDUNG))
					.build()
			)
			.build();
		Kante kante54 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten5)
			.nachKnoten(knoten4)
			.build();
		Kante kante56 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten5)
			.nachKnoten(knoten6)
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					.netzklassen(Set.of(Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ALLTAG))
					.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
					.build()
			)
			.build();

		Kante kante25 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten2)
			.nachKnoten(knoten5)
			.build();
		Kante kante52 = KanteTestDataProvider.withDefaultValues()
			.vonKnoten(knoten5)
			.nachKnoten(knoten2)
			.build();

		// Act & Assert mit einem Attribute-Merge
		kantenAttributeUebertragungService.uebertrageGleicheAttribute(List.of(kante12, kante32), knoten2, kante25,
			false);
		assertThat(kante25.getKantenAttributGruppe().getNetzklassen()).containsExactlyInAnyOrder(
			Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.KREISNETZ_ALLTAG);
		assertThat(kante25.getKantenAttributGruppe().getIstStandards()).containsExactlyInAnyOrder(
			IstStandard.STARTSTANDARD_RADNETZ, IstStandard.RADSCHNELLVERBINDUNG);

		kantenAttributeUebertragungService.uebertrageGleicheAttribute(List.of(kante54, kante56), knoten5, kante52,
			false);
		assertThat(kante52.getKantenAttributGruppe().getNetzklassen()).containsExactlyInAnyOrder(
			Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT);
		assertThat(kante52.getKantenAttributGruppe().getIstStandards()).containsExactlyInAnyOrder(
			IstStandard.STARTSTANDARD_RADNETZ);

		// Act & Assert mit Attribute-Verschneidung (intersection)
		kante25.getKantenAttributGruppe().update(new HashSet<>(), new HashSet<>(), kante25.getKantenAttributGruppe()
			.getKantenAttribute());
		kante52.getKantenAttributGruppe().update(new HashSet<>(), new HashSet<>(), kante52.getKantenAttributGruppe()
			.getKantenAttribute());

		kantenAttributeUebertragungService.uebertrageGleicheAttribute(List.of(kante12, kante32), knoten2, kante25,
			true);
		assertThat(kante25.getKantenAttributGruppe().getNetzklassen()).containsExactly(Netzklasse.KREISNETZ_ALLTAG);
		assertThat(kante25.getKantenAttributGruppe().getIstStandards()).containsExactly(
			IstStandard.RADSCHNELLVERBINDUNG);

		kantenAttributeUebertragungService.uebertrageGleicheAttribute(List.of(kante54, kante56), knoten5, kante52,
			true);
		assertThat(kante52.getKantenAttributGruppe().getNetzklassen()).isEmpty();
		assertThat(kante52.getKantenAttributGruppe().getIstStandards()).isEmpty();
	}
}