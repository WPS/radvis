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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.exception.AttributUebernahmeException;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributesProperties;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;

// mr: Das ist bewusst nur ein Smoke-Test, wir wollen dieses String-Mapping nicht auch noch im Test pflegen müssen.
public class LUBWMapperTest {
	LUBWMapper mapper;

	@BeforeEach
	public void setup() {
		mapper = new LUBWMapper();
	}

	@Test
	void isAttributNameValid() {
		assertThat(mapper.isAttributNameValid("belag")).isTrue();
		assertThat(mapper.isAttributNameValid("Belag")).isTrue();
		assertThat(mapper.isAttributNameValid("irgendwas")).isFalse();
	}

	@Test
	void testApply_Simple() {
		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		mapper.applyEinfach("Beleuchtun", "10", kante);
		mapper.applyEinfach("Richtung", "1", kante);

		assertThat(kante.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung()).isEqualTo(
			Beleuchtung.UNBEKANNT);
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.BEIDE_RICHTUNGEN);
	}

	@Test
	void testApply_Seitenbezogen() {
		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		mapper.applyBeideSeiten("Richtung", "1", "2", kante);

		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.BEIDE_RICHTUNGEN);
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(Richtung.IN_RICHTUNG);
	}

	@Test
	void testApply_linearReferenziert() throws AttributUebernahmeException {
		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		mapper.applyLinearReferenzierterAbschnitt("VEREINBARU",
			MappedAttributesProperties.of(Map.of("VEREINBARU", "Kennung")), LinearReferenzierterAbschnitt.of(0, 1),
			kante);
		mapper.applyLinearReferenzierterAbschnitt("BELAG", MappedAttributesProperties.of(Map.of("BELAG", "10")),
			LinearReferenzierterAbschnitt.of(0, 1), kante);
		mapper.applyLinearReferenzierterAbschnitt("BREITE", MappedAttributesProperties.of(Map.of("BREITE", "2")),
			LinearReferenzierterAbschnitt.of(0, 1), kante);
		mapper.applyLinearReferenzierterAbschnitt("WEGART", MappedAttributesProperties.of(Map.of("WEGART", "110")),
			LinearReferenzierterAbschnitt.of(0, 1), kante);

		assertThat(kante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).containsExactly(
			ZustaendigkeitAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.vereinbarungsKennung(VereinbarungsKennung.of("Kennung"))
				.build()
		);

		FuehrungsformAttribute fuehrungsformAttribute = kante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts().get(0);

		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).containsExactly(
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.belagArt(BelagArt.ASPHALT)
				.breite(Laenge.of(1.5))
				.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG)
				.trennstreifenBreiteRechts(fuehrungsformAttribute.getTrennstreifenBreiteRechts().orElse(null))
				.trennstreifenBreiteLinks(fuehrungsformAttribute.getTrennstreifenBreiteLinks().orElse(null))
				.trennstreifenTrennungZuRechts(fuehrungsformAttribute.getTrennstreifenTrennungZuRechts().orElse(null))
				.trennstreifenTrennungZuLinks(fuehrungsformAttribute.getTrennstreifenTrennungZuLinks().orElse(null))
				.trennstreifenFormRechts(fuehrungsformAttribute.getTrennstreifenFormRechts().orElse(null))
				.trennstreifenFormLinks(fuehrungsformAttribute.getTrennstreifenFormLinks().orElse(null))
				.build());
	}

	@Test
	void testApply_linearReferenziertUndSeitenbezogen() throws AttributUebernahmeException {
		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		kante.changeSeitenbezug(true);
		mapper.applyLinearReferenzierterAbschnittSeitenbezogen("BELAG",
			MappedAttributesProperties.of(Map.of("BELAG", "10")), LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.RECHTS, kante);
		mapper.applyLinearReferenzierterAbschnittSeitenbezogen("BREITE",
			MappedAttributesProperties.of(Map.of("BREITE", "2")), LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.RECHTS, kante);
		mapper.applyLinearReferenzierterAbschnittSeitenbezogen("WEGART",
			MappedAttributesProperties.of(Map.of("WEGART", "110")), LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.RECHTS, kante);

		FuehrungsformAttribute fuehrungsformAttribute = kante.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts().get(0);

		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts()).containsExactly(
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.belagArt(BelagArt.ASPHALT)
				.breite(Laenge.of(1.5))
				.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG)
				.trennstreifenBreiteRechts(fuehrungsformAttribute.getTrennstreifenBreiteRechts().orElse(null))
				.trennstreifenBreiteLinks(fuehrungsformAttribute.getTrennstreifenBreiteLinks().orElse(null))
				.trennstreifenTrennungZuRechts(fuehrungsformAttribute.getTrennstreifenTrennungZuRechts().orElse(null))
				.trennstreifenTrennungZuLinks(fuehrungsformAttribute.getTrennstreifenTrennungZuLinks().orElse(null))
				.trennstreifenFormRechts(fuehrungsformAttribute.getTrennstreifenFormRechts().orElse(null))
				.trennstreifenFormLinks(fuehrungsformAttribute.getTrennstreifenFormLinks().orElse(null))
				.build());
	}

	@Test
	void testApply_linearReferenziertUndSeitenbezogen_Sicherheitstrennstreifen_happyPathWithInitialNullValues_Strassenbegleitend()
		throws AttributUebernahmeException {
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig().fuehrungsformAttributGruppe(
			FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true)
				.fuehrungsformAttributeRechts(
					List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND)
							.build()))
				.build())
			.build();
		kante.changeSeitenbezug(true);
		mapper.applyLinearReferenzierterAbschnittSeitenbezogen("ST",
			MappedAttributesProperties.of(
				Map.of("ST", "Sicherheitstrennstreifen innerorts ohne Parken", "BREITST", "", "BREITST2", "45",
					"ORTSLAGE", "Innerorts")),
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.RECHTS, kante);

		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts()).containsExactly(
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND)
				.trennstreifenBreiteLinks(Laenge.of(0.45))
				.trennstreifenBreiteRechts(null)
				.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
				.trennstreifenTrennungZuRechts(null)
				.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG)
				.trennstreifenFormRechts(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
				.build());
	}

	@Test
	void testApply_linearReferenziert_Sicherheitstrennstreifen_einseitig_withInitialNullValues_Strassenbegleitend_trennstreifenSeiteNichtErmittelbar_defaultLinks() {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttributeList = List.of(
			FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND)
				.build());
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					// Bei einseitigen Kanten muessen Attributgruppen links und rechts gleich sein
					.fuehrungsformAttributeLinks(fuehrungsformAttributeList)
					.fuehrungsformAttributeRechts(fuehrungsformAttributeList)
					.build())
			.build();

		kante.changeSeitenbezug(false);

		// act
		AttributUebernahmeException attributUebernahmeException = null;
		try {
			mapper.applyLinearReferenzierterAbschnitt("ST",
				MappedAttributesProperties.of(
					Map.of("ST", "Sicherheitstrennstreifen innerorts ohne Parken", "BREITST", "", "BREITST2", "45",
						"ORTSLAGE", "Innerorts")),
				LinearReferenzierterAbschnitt.of(0, 1),
				kante);
		} catch (AttributUebernahmeException e) {
			attributUebernahmeException = e;
		}

		// assert
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).containsExactly(
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND)
				.trennstreifenBreiteLinks(Laenge.of(0.45))
				.trennstreifenBreiteRechts(null)
				.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
				.trennstreifenTrennungZuRechts(null)
				.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG)
				.trennstreifenFormRechts(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
				.build());

		assertThat(kante.isZweiseitig()).isFalse();
		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(FuehrungsformAttributGruppe.isSeitenBezugValid(
			kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks(),
			kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts(),
			false)
		).isTrue();

		assertThat(attributUebernahmeException).isNotNull();
		assertThat(attributUebernahmeException.getFehler()).hasSize(1);
		assertThat(attributUebernahmeException.getFehler().get(0).getSeitenbezug()).isEqualTo(Seitenbezug.BEIDSEITIG);
	}

	@Test
	void testApply_linearReferenziert_einseitig_Sicherheitstrennstreifen_happyPathWithInitialNullValues_KeinSTSVorhandenAufBeideSeitenSchreiben()
		throws AttributUebernahmeException {
		// arrange
		List<FuehrungsformAttribute> fuehrungsformAttributeList = List.of(
			FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
				.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
				.build());
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(false)
					// Bei einseitigen Kanten muessen Attributgruppen links und rechts gleich sein
					.fuehrungsformAttributeLinks(fuehrungsformAttributeList)
					.fuehrungsformAttributeRechts(fuehrungsformAttributeList)
					.build())
			.build();

		kante.changeSeitenbezug(false);

		// act
		mapper.applyLinearReferenzierterAbschnitt("ST",
			MappedAttributesProperties.of(
				Map.of("ST", "Kein Sicherheitstrennstreifen vorhanden", "BREITST", "", "BREITST2", "",
					"ORTSLAGE", "Innerorts")),
			LinearReferenzierterAbschnitt.of(0, 1),
			kante);

		// assert
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).containsExactly(
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
				.trennstreifenBreiteLinks(null)
				.trennstreifenBreiteRechts(null)
				.trennstreifenTrennungZuLinks(null)
				.trennstreifenTrennungZuRechts(null)
				.trennstreifenFormLinks(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
				.trennstreifenFormRechts(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
				.build());
	}

	@Test
	void testApply_linearReferenziert_zweiseitig_Sicherheitstrennstreifen_happyPathWithInitialNullValues_KeinSTSVorhandenAufBeideSeitenSchreiben()
		throws AttributUebernahmeException {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig()
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
							.build()))
					.build())
			.build();

		kante.changeSeitenbezug(true);

		// act
		mapper.applyLinearReferenzierterAbschnittSeitenbezogen("ST",
			MappedAttributesProperties.of(
				Map.of("ST", "Kein Sicherheitstrennstreifen vorhanden", "BREITST", "", "BREITST2", "",
					"ORTSLAGE", "Innerorts")),
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS, kante);

		// assert
		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks()).containsExactly(
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
				.trennstreifenBreiteLinks(null)
				.trennstreifenBreiteRechts(null)
				.trennstreifenTrennungZuLinks(null)
				.trennstreifenTrennungZuRechts(null)
				.trennstreifenFormLinks(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
				.trennstreifenFormRechts(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
				.build());
	}

	@Test
	void testApply_linearReferenziertUndSeitenbezogen_Sicherheitstrennstreifen_happyPathWithInitialNullValuesSchutzstreifenAndBreiteParsed()
		throws AttributUebernahmeException {
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig().fuehrungsformAttributGruppe(
			FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true)
				.fuehrungsformAttributeRechts(
					List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
							.build()))
				.build())
			.build();
		kante.changeSeitenbezug(true);
		mapper.applyLinearReferenzierterAbschnittSeitenbezogen("ST",
			MappedAttributesProperties.of(
				Map.of("ST", "Sicherheitstrennstreifen innerorts mit Längsparken", "BREITST", "> 0,45 m", "BREITST2",
					"",
					"ORTSLAGE", "Innerorts")),
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.RECHTS, kante);

		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts()).containsExactly(
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.radverkehrsfuehrung(Radverkehrsfuehrung.SCHUTZSTREIFEN)
				.trennstreifenBreiteRechts(Laenge.of(0.45))
				.trennstreifenBreiteLinks(null)
				.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN)
				.trennstreifenTrennungZuLinks(null)
				.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG)
				.trennstreifenFormLinks(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
				.build());
	}

	@Test
	void testApply_linearReferenziertUndSeitenbezogen_Sicherheitstrennstreifen_Konflikt()
		throws AttributUebernahmeException {
		// Arrange
		Radverkehrsfuehrung fuehrungsformAmAbschnitt = Radverkehrsfuehrung.PIKTOGRAMMKETTE;
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig().fuehrungsformAttributGruppe(
			FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true)
				.fuehrungsformAttributeRechts(
					List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.radverkehrsfuehrung(fuehrungsformAmAbschnitt)
							.build()))
				.build())
			.build();
		kante.changeSeitenbezug(true);

		MappedAttributesProperties lubwAttribute = MappedAttributesProperties.of(
			Map.of(
				"ST", "Sicherheitstrennstreifen innerorts mit Längsparken",
				"BREITST", "> 0,45 m",
				"BREITST2", "",
				"ORTSLAGE", "Innerorts"
			));
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt = LinearReferenzierterAbschnitt.of(0, 0.4);
		Seitenbezug seitenbezug = Seitenbezug.RECHTS;

		// Act
		AttributUebernahmeException thrownException = catchThrowableOfType(
			() -> mapper.applyLinearReferenzierterAbschnittSeitenbezogen("ST", lubwAttribute,
				linearReferenzierterAbschnitt, seitenbezug, kante),
			AttributUebernahmeException.class);

		// Assert
		assertThat(thrownException.getFehler()).hasSize(1);
		assertThat(thrownException.getFehler().get(0).getLinearReferenzierterAbschnitt()).isEqualTo(
			linearReferenzierterAbschnitt);
		assertThat(thrownException.getFehler().get(0).getSeitenbezug()).isEqualTo(seitenbezug);
		assertThat(thrownException.getFehler().get(0).getMessage()).isEqualTo(String.format("""
			Es konnten keine TrennstreifenInformationen geschrieben werden.
			Radverkehrsführung: %s
			KanteId: %s""",
			fuehrungsformAmAbschnitt,
			kante.getId()
		));
		assertThat(thrownException.getFehler().get(0).getNichtUerbenommeneWerte()).isEqualTo(
			Set.of(
				"TrennstreifenForm: " + TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG.toString(),
				"TrennstreifenBreite: 0,45 m",
				"TrennungZu: " + TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN.toString())
		);
	}

	@Test
	void testApply_Sicherheitstrennstreifen_RadverkehrsfuehrungUnbekannt_STSwriteNull()
		throws AttributUebernahmeException {
		Kante kante = KanteTestDataProvider.withDefaultValuesAndZweiseitig().fuehrungsformAttributGruppe(
			FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.isZweiseitig(true)
				.fuehrungsformAttributeRechts(
					List.of(
						FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
							.radverkehrsfuehrung(Radverkehrsfuehrung.UNBEKANNT)
							.build()))
				.build())
			.build();
		kante.changeSeitenbezug(true);
		mapper.applyLinearReferenzierterAbschnittSeitenbezogen("ST",
			MappedAttributesProperties.of(
				Map.of("ST", "Kein Sicherheitstrennstreifen vorhanden", "BREITST", "", "BREITST2",
					"",
					"ORTSLAGE", "Innerorts")),
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.RECHTS, kante);

		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts()).containsExactly(
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.radverkehrsfuehrung(Radverkehrsfuehrung.UNBEKANNT)
				.trennstreifenBreiteRechts(null)
				.trennstreifenBreiteLinks(null)
				.trennstreifenTrennungZuRechts(null)
				.trennstreifenTrennungZuLinks(null)
				.trennstreifenFormRechts(null)
				.trennstreifenFormLinks(null)
				.build());
	}

	@Test
	void isAttributWertValid() {
		assertThat(mapper.isAttributWertValid("belag", "10")).isTrue();
		assertThat(mapper.isAttributWertValid("Belag", "10")).isTrue();
		assertThat(mapper.isAttributWertValid("belag", "66")).isTrue();
		assertThat(mapper.isAttributWertValid("Belag", "66")).isTrue();
		assertThat(mapper.isAttributWertValid("belag", "77")).isFalse();
		assertThat(mapper.isAttributWertValid("Belag", "77")).isFalse();
		assertThat(mapper.isAttributWertValid("richtung", "5")).isFalse();
	}

	@Test
	void validBelagartAttributeHabenEinMapping() {
		assertThatNoException().isThrownBy(() -> {
			mapper.belagArten.forEach(mapper::mapBelagArt);
		});
	}

	@Test
	void validRadverkehrsfuehrungenHabenEinMapping() {
		assertThatNoException().isThrownBy(() -> {
			mapper.radverkehrsfuehrungen.forEach(mapper::mapRadverkehrsfuehrung);
		});
	}

	@Test
	void validBeleuchtungenHabenEinMapping() {
		assertThatNoException().isThrownBy(() -> {
			mapper.beleuchtungen.forEach(mapper::mapBeleuchtung);
		});
	}

	@Test
	void validRichtungenHabenEinMapping() {
		assertThatNoException().isThrownBy(() -> {
			mapper.richtungen.forEach(mapper::mapRichtung);
		});
	}

	@Test
	void validBreitenHabenEinMapping() {
		assertThatNoException().isThrownBy(() -> {
			mapper.breiten.forEach(mapper::mapBreite);
		});
	}
}
