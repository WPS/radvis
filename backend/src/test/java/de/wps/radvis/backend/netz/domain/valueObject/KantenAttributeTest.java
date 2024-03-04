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

package de.wps.radvis.backend.netz.domain.valueObject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;

class KantenAttributeTest {

	@Test
	void getterUndConstructor_WerteVorhanden() {
		// arrange
		KantenAttribute attribute = KantenAttribute.builder()
			.laengeManuellErfasst(Laenge.of(120d))
			.dtvFussverkehr(VerkehrStaerke.of(1))
			.dtvRadverkehr(VerkehrStaerke.of(2))
			.dtvPkw(VerkehrStaerke.of(3))
			.sv(VerkehrStaerke.of(4))
			.kommentar(Kommentar.of("Anmerkungen"))
			.strassenName(StrassenName.of("Sierichstraße"))
			.strassenNummer(StrassenNummer.of("42d"))
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.umfeld(Umfeld.GEWERBEGEBIET)
			.beleuchtung(Beleuchtung.VORHANDEN)
			.status(Status.defaultWert())
			.build();

		// assert
		assertThat(attribute.getStrassenquerschnittRASt06()).isEqualTo(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE);
		assertThat(attribute.getUmfeld()).isEqualTo(Umfeld.GEWERBEGEBIET);
		assertThat(attribute.getBeleuchtung()).isEqualTo(Beleuchtung.VORHANDEN);
		assertThat(attribute.getLaengeManuellErfasst()).contains(Laenge.of(120d));
		assertThat(attribute.getDtvFussverkehr()).contains(VerkehrStaerke.of(1));
		assertThat(attribute.getDtvRadverkehr()).contains(VerkehrStaerke.of(2));
		assertThat(attribute.getDtvPkw()).contains(VerkehrStaerke.of(3));
		assertThat(attribute.getSv()).contains(VerkehrStaerke.of(4));
		assertThat(attribute.getKommentar()).contains(Kommentar.of("Anmerkungen"));
		assertThat(attribute.getStrassenName()).contains(StrassenName.of("Sierichstraße"));
		assertThat(attribute.getStrassenNummer()).contains(StrassenNummer.of("42d"));
		assertThat(attribute.getStatus()).isEqualTo(Status.defaultWert());
	}

	@Test
	void getterUndConstructor_OhneWerte() {
		// arrange
		KantenAttribute attribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute().build();

		// act + assert
		assertThat(attribute.getWegeNiveau()).isEmpty();
		assertThat(attribute.getStrassenkategorieRIN()).isEmpty();
	}

	@Test
	void hasSameValues_True() {
		// arrange
		KantenAttribute attribute = KantenAttribute.builder()
			.wegeNiveau(WegeNiveau.FAHRBAHN)
			.laengeManuellErfasst(Laenge.of(15))
			.dtvFussverkehr(VerkehrStaerke.of(155))
			.dtvRadverkehr(VerkehrStaerke.of(190))
			.dtvPkw(VerkehrStaerke.of(210))
			.sv(VerkehrStaerke.of(199))
			.kommentar(Kommentar.of("Das ist ein Kommentar"))
			.strassenName(StrassenName.of("Eppendorfer Weg"))
			.strassenNummer(StrassenNummer.of("B273"))
			.beleuchtung(Beleuchtung.VORHANDEN)
			.umfeld(Umfeld.GEWERBEGEBIET)
			.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.IN_BAU)
			.build();

		KantenAttribute otherAttribute = KantenAttribute.builder()
			.wegeNiveau(WegeNiveau.FAHRBAHN)
			.laengeManuellErfasst(Laenge.of(15))
			.dtvFussverkehr(VerkehrStaerke.of(155))
			.dtvRadverkehr(VerkehrStaerke.of(190))
			.dtvPkw(VerkehrStaerke.of(210))
			.sv(VerkehrStaerke.of(199))
			.kommentar(Kommentar.of("Das ist ein Kommentar"))
			.strassenName(StrassenName.of("Eppendorfer Weg"))
			.strassenNummer(StrassenNummer.of("B273"))
			.beleuchtung(Beleuchtung.VORHANDEN)
			.umfeld(Umfeld.GEWERBEGEBIET)
			.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
			.status(Status.IN_BAU)
			.build();
		// act
		boolean result = attribute.equals(otherAttribute);

		// assert
		assertThat(result).isTrue();
	}

	@Test
	void hasSameValues_True_EmptyAttribute() {
		// arrange
		KantenAttribute attribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute().build();
		KantenAttribute otherAttribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute().build();

		// act
		assertThat(attribute).isEqualTo(otherAttribute);
	}

	@Test
	void hasSameValue_mancheGleich_mancheVerschieden() {
		// arrange
		KantenAttribute attribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
			.wegeNiveau(WegeNiveau.FAHRBAHN) // <- unterschiedlich
			.build();

		KantenAttribute otherAttribute = KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
			.wegeNiveau(WegeNiveau.GEHWEG) // <- unterschiedlich
			.build();

		// act
		assertThat(attribute).isNotEqualTo(otherAttribute);
	}

}
