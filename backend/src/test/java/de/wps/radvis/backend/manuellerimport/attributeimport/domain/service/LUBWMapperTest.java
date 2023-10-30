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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.exception.AttributUebernahmeException;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributesProperties;
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
				.trennstreifenFormRechts(null)
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
