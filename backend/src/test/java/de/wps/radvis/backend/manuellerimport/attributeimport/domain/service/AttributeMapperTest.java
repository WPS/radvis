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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.exception.AttributUebernahmeException;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributes;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributesProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FahrtrichtungAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;

class AttributeMapperTest {

	private AttributeMapper attributeMapper;

	@BeforeEach
	void setup() {
		attributeMapper = new AttributeMapper() {
			@Override
			public void applyEinfach(String attribut, String attributwert, Kante kante) {

			}

			@Override
			public void applyBeideSeiten(String attribut, String attributwertLinks, String attributwertRechts,
				Kante kante) {

			}

			@Override
			public void applyLinearReferenzierterAbschnitt(String attribut,
				MappedAttributesProperties attributesProperties,
				LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante) {

			}

			@Override
			public void applyLinearReferenzierterAbschnittSeitenbezogen(String attribut,
				MappedAttributesProperties attributesProperties,
				LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
				Seitenbezug seitenbezug, Kante kante) {

			}

			@Override
			public boolean isAttributSeitenbezogen(String attribut) {
				return false;
			}

			@Override
			public boolean isLinearReferenziert(String attribut) {
				return false;
			}

			@Override
			protected String getUmgekehrteRichtung(String wertFuerRichtung) {
				switch (wertFuerRichtung) {
				case "links":
					return "rechts";
				case "rechts":
					return "links";
				default:
					return wertFuerRichtung;
				}

			}

			@Override
			public boolean isRichtung(String attribut) {
				return "richtung".equals(attribut);
			}

			@Override
			public boolean isAttributNameValid(String attributName) {
				// Auto-generated method stub
				return false;
			}

			@Override
			public boolean isAttributWertValid(String attributName, String attributWert) {
				// Auto-generated method stub
				return false;
			}

			@Override
			public String getRadVisAttributName(String importedAttributName) {
				// Auto-generated method stub
				return null;
			}

		};
	}

	@Test
	void testApply_Vereinbarungskennung_LineareReferenzSchneidetVerschiedeneSegmente() {
		// arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttribute.builder()
							.vereinbarungsKennung(VereinbarungsKennung.of("1"))
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.4))
							.build(),
						ZustaendigkeitAttribute.builder()
							.vereinbarungsKennung(VereinbarungsKennung.of("2"))
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 0.6))
							.build(),
						ZustaendigkeitAttribute.builder()
							.vereinbarungsKennung(VereinbarungsKennung.of("3"))
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, 1))
							.build()))
					.build())
			.build();

		// act
		attributeMapper.applyVereinbarungskennung(kante, VereinbarungsKennung.of("x"),
			LinearReferenzierterAbschnitt.of(0.2, 0.8));

		// assert
		assertThat(kante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("1"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.2))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("x"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.4))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("x"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 0.6))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("x"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, 0.8))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("3"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.8, 1))
					.build());
	}

	@Test
	void testApply_Vereinbarungskennung_LineareReferenzSchneidetEinzelnesSegment() {
		// arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttribute.builder()
							.vereinbarungsKennung(VereinbarungsKennung.of("1"))
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.2))
							.build(),
						ZustaendigkeitAttribute.builder()
							.vereinbarungsKennung(VereinbarungsKennung.of("2"))
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.8))
							.build(),
						ZustaendigkeitAttribute.builder()
							.vereinbarungsKennung(VereinbarungsKennung.of("3"))
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.8, 1))
							.build()))
					.build())
			.build();

		// act
		attributeMapper.applyVereinbarungskennung(kante, VereinbarungsKennung.of("x"),
			LinearReferenzierterAbschnitt.of(0.4, 0.6));

		// assert
		assertThat(kante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("1"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.2))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("2"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.4))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("x"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 0.6))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("2"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, 0.8))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("3"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.8, 1))
					.build());
	}

	@Test
	void testeBeleuchtung() {
		Kante original = KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG))
				.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
					.beleuchtung(Beleuchtung.UNBEKANNT).build())
				.build())
			.id(1L)
			.build();
		Kante copyZumVeraendern = KanteTestDataProvider.withDefaultValues()
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG))
				.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
					.beleuchtung(Beleuchtung.UNBEKANNT).build())
				.build())
			.id(1L)
			.build();

		attributeMapper.applyBeleuchtung(copyZumVeraendern, Beleuchtung.VORHANDEN);

		assertThat(copyZumVeraendern.isZweiseitig()).isEqualTo(original.isZweiseitig());
		assertThat(copyZumVeraendern.getKantenAttributGruppe().getNetzklassen()).isEqualTo(
			original.getKantenAttributGruppe().getNetzklassen());
		assertThat(copyZumVeraendern.getKantenAttributGruppe().getIstStandards()).isEqualTo(
			original.getKantenAttributGruppe().getIstStandards());
		assertThat(copyZumVeraendern.getKantenAttributGruppe().getKantenAttribute()).isEqualTo(
			original.getKantenAttributGruppe().getKantenAttribute().getBuilderMitGleichenAttributen()
				.beleuchtung(Beleuchtung.VORHANDEN).build());
	}

	@Test
	void testeApplyRichtung_setztZweiseitigkeit() {
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fahrtrichtungLinks(Richtung.UNBEKANNT)
					.fahrtrichtungRechts(Richtung.UNBEKANNT)
					.isZweiseitig(false)
					.build())
			.id(1L)
			.build();

		attributeMapper.applyFahrtrichtung(kante, Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG);

		assertThat(kante.getFahrtrichtungAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.IN_RICHTUNG);
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(Richtung.GEGEN_RICHTUNG);
	}

	@Test
	void testeApplyRichtung_ueberschreibtWerteRichtig() {
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fahrtrichtungLinks(Richtung.GEGEN_RICHTUNG)
					.fahrtrichtungRechts(Richtung.BEIDE_RICHTUNGEN)
					.isZweiseitig(true)
					.build())
			.id(1L)
			.build();

		attributeMapper.applyFahrtrichtung(kante, Richtung.IN_RICHTUNG, Richtung.GEGEN_RICHTUNG);

		assertThat(kante.getFahrtrichtungAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.IN_RICHTUNG);
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(Richtung.GEGEN_RICHTUNG);
	}

	@Test
	void testeApplyBelagart_wederKanteNochUeberschreibungBeidseitig_vollstaendigeUeberlappung() {
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.build())
			.id(2L).build();

		attributeMapper.applyBelagArt(kante, BelagArt.WASSERGEBUNDENE_DECKE, LinearReferenzierterAbschnitt.of(0, 1));

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).isEqualTo(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1.)
				.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.build()));
	}

	@Test
	void testeApplyBelagart_KanteBeidseitigAberUeberschreibungGiltFuerBeideSeiten() {
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .4)
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(.4, 1)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.build())
			.id(2L).build();

		attributeMapper.applyBelagArt(kante, BelagArt.WASSERGEBUNDENE_DECKE, LinearReferenzierterAbschnitt.of(.3, .6));

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();

		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .3)
					.belagArt(BelagArt.BETON)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(.3, .4)
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(.4, .6)
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(.6, 1.)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build());

		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .3)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(.3, .6)
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(.6, 1.)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build());

	}

	@Test
	void testeApplyBelagart_KanteBeidseitigUndUeberschreibungLinks() {
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .4)
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(.4, 1)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.build())
			.id(2L).build();

		attributeMapper.applyBelagArt(kante, BelagArt.WASSERGEBUNDENE_DECKE, Seitenbezug.LINKS,
			LinearReferenzierterAbschnitt.of(.3, .6));

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .3)
					.belagArt(BelagArt.BETON)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(.3, .4)
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(.4, .6)
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(.6, 1.)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build());
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build());
	}

	@Test
	void testeApplyBelagart_KanteBeidseitigUndUeberschreibungRechts() {
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .4)
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(.4, 1)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.build())
			.id(2L).build();

		attributeMapper.applyBelagArt(kante, BelagArt.WASSERGEBUNDENE_DECKE, Seitenbezug.RECHTS,
			LinearReferenzierterAbschnitt.of(.3, .6));

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .4)
					.belagArt(BelagArt.BETON)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(.4, 1)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build());
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .3)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, .6)
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1.)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build());
	}

	@Test
	void testeApplyBelagart_KanteEinseitigUndUeberschreibungLinks() {
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .4)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1.)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KOMPLETT_ABGESENKT)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .4)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1.)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KOMPLETT_ABGESENKT)
							.build()))
					.build())
			.isZweiseitig(false)
			.id(2L).build();

		attributeMapper.applyBelagArt(kante, BelagArt.WASSERGEBUNDENE_DECKE, Seitenbezug.LINKS,
			LinearReferenzierterAbschnitt.of(.3, .6));

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .3)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, .4)
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, .6)
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1.)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build());
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .4)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1.)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build());
	}

	@Test
	void testeApplyBelagart_KanteEinseitigUndUeberschreibungRechts() {
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .4)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1.)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KOMPLETT_ABGESENKT)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .4)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1.)
							.belagArt(BelagArt.ASPHALT)
							.bordstein(Bordstein.KOMPLETT_ABGESENKT)
							.build()))
					.build())
			.isZweiseitig(false)
			.id(2L).build();

		attributeMapper.applyBelagArt(kante, BelagArt.WASSERGEBUNDENE_DECKE, Seitenbezug.RECHTS,
			LinearReferenzierterAbschnitt.of(.3, .6));

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .4)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1.)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build());
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.containsExactlyInAnyOrder(
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .3)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.3, .4)
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.bordstein(Bordstein.KEINE_ABSENKUNG)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, .6)
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build(),
				FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1.)
					.belagArt(BelagArt.ASPHALT)
					.bordstein(Bordstein.KOMPLETT_ABGESENKT)
					.build());
	}

	// Ab hier nur noch exemplarische Tests, nicht mehr alle Einzelfälle, da alle Methoden für die Führungsformattribute
	// die selbe Implementation unter der Haube verwenden

	@Test
	void testeApplyBreite_ohneSeitenbezug_schreibtAttributeAnRelevantesSegment() {
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.breite(Laenge.of(5))
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.breite(Laenge.of(5))
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.build())
			.id(2L).build();

		attributeMapper.applyBreite(kante, Laenge.of(3), LinearReferenzierterAbschnitt.of(0, 1));

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).isEqualTo(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1.)
				.breite(Laenge.of(3))
				.belagArt(BelagArt.BETON)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.build()));
	}

	@Test
	void testeApplyBreite_MitSeitenbezug_schreibtAttributeAnRelevantesSegment() {
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.breite(Laenge.of(5))
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.breite(Laenge.of(5))
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.build())
			.id(2L).build();

		attributeMapper.applyBreite(kante, Laenge.of(3), Seitenbezug.LINKS, LinearReferenzierterAbschnitt.of(0, 1));

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).isEqualTo(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1.)
				.breite(Laenge.of(3))
				.belagArt(BelagArt.BETON)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.build()));
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts()).isEqualTo(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1.)
				.breite(Laenge.of(5))
				.belagArt(BelagArt.BETON)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.build()));
	}

	@Test
	void testeApplyRadverkehrsfuehrung_ohneSeitenbezug_schreibtAttributeAnRelevantesSegment()
		throws AttributUebernahmeException {
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.build())
			.id(2L).build();

		attributeMapper.applyRadverkehrsfuehrung(kante, Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT,
			LinearReferenzierterAbschnitt.of(0, 1));

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).isEqualTo(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1.)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT)
				.belagArt(BelagArt.BETON)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.build()));
	}

	@Test
	void testeApplyRadverkehrsfuehrung_MitSeitenbezug_schreibtAttributeAnRelevantesSegment() {
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
							.belagArt(BelagArt.BETON)
							.bordstein(Bordstein.KEINE_ABSENKUNG)
							.build()))
					.build())
			.id(2L).build();

		attributeMapper.applyRadverkehrsfuehrung(kante, Radverkehrsfuehrung.BEGEGNUNBSZONE, Seitenbezug.LINKS,
			LinearReferenzierterAbschnitt.of(0, 1));

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).isEqualTo(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1.)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.belagArt(BelagArt.BETON)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.build()));
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts()).isEqualTo(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1.)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG)
				.belagArt(BelagArt.BETON)
				.bordstein(Bordstein.KEINE_ABSENKUNG)
				.build()));
	}

	@Test
	void testeApplyRadverkehrsfuehrung_inkompatibleTrennstreifenFuehrtZuException() {
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
							.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
							.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
							.trennstreifenBreiteLinks(Laenge.of(0.5))
							.trennstreifenFormRechts(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
							.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
							.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
							.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
							.trennstreifenBreiteLinks(Laenge.of(0.5))
							.trennstreifenFormRechts(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
							.build()))
					.build())
			.id(1L).build();
		LinearReferenzierterAbschnitt abschnitt = LinearReferenzierterAbschnitt.of(0, 1);
		Radverkehrsfuehrung newFuehrungsform = Radverkehrsfuehrung.UNBEKANNT;

		AttributUebernahmeException exception = catchThrowableOfType(
			() -> attributeMapper.applyRadverkehrsfuehrung(kante, newFuehrungsform, abschnitt),
			AttributUebernahmeException.class);

		assertThat(exception.getFehler()).hasSize(2);

		assertThat(exception.getFehler().get(0).getMessage()).contains(
			"führt zu inkompatiblen Attributen des linken Sicherheitstrennstreifens");
		assertThat(exception.getFehler().get(0).getSeitenbezug()).isEqualTo(Seitenbezug.BEIDSEITIG);
		assertThat(exception.getFehler().get(0).getNichtUerbenommeneWerte()).containsExactly(newFuehrungsform
			.toString());
		assertThat(exception.getFehler().get(0).getLinearReferenzierterAbschnitt()).isEqualTo(abschnitt);

		assertThat(exception.getFehler().get(1).getMessage()).contains(
			"führt zu inkompatiblen Attributen des rechten Sicherheitstrennstreifens");
		assertThat(exception.getFehler().get(1).getSeitenbezug()).isEqualTo(Seitenbezug.BEIDSEITIG);
		assertThat(exception.getFehler().get(1).getNichtUerbenommeneWerte()).containsExactly(newFuehrungsform
			.toString());
		assertThat(exception.getFehler().get(1).getLinearReferenzierterAbschnitt()).isEqualTo(abschnitt);

		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).isEqualTo(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
				.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
				.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
				.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
				.trennstreifenBreiteLinks(Laenge.of(0.5))
				.trennstreifenFormRechts(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
				.build()));
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts()).isEqualTo(List.of(
			FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 1)
				.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
				.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
				.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
				.trennstreifenBreiteLinks(Laenge.of(0.5))
				.trennstreifenFormRechts(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
				.build()));
	}

	@Test
	void testeDreheRichtungUm_drehtUmWennOrientierungUMgekehrt() {
		MappedAttributes mappedAttributes1MuessenUmgedrehtWerden = MappedAttributes.of(
			Map.of("生きる意味", "を探す僕らは", "richtung", "links"),
			LinearReferenzierterAbschnitt.of(0, 1.),
			Seitenbezug.BEIDSEITIG, true);

		MappedAttributes umgedreht = attributeMapper.dreheRichtungUm(mappedAttributes1MuessenUmgedrehtWerden,
			"richtung");

		assertThat(umgedreht.getProperty("richtung")).isEqualTo("rechts");
		assertThat(umgedreht.getProperty("生きる意味")).isEqualTo("を探す僕らは");
	}

	@Test
	void testeDreheRichtungUm_drehtNichtUmWennOrientierungRichtigRum() {
		MappedAttributes mappedAttributes2 = MappedAttributes.of(
			Map.of("生きる意味", "を探したんだ", "richtung", "rechts"),
			LinearReferenzierterAbschnitt.of(0, 1.),
			Seitenbezug.BEIDSEITIG, false);

		MappedAttributes nichtUmgedreht = attributeMapper.dreheRichtungUm(mappedAttributes2,
			"richtung");

		assertThat(nichtUmgedreht.getProperty("richtung")).isEqualTo("rechts");
		assertThat(nichtUmgedreht.getProperty("生きる意味")).isEqualTo("を探したんだ");
	}
}