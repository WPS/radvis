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

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class ZustaendigkeitAttributeTest {

	@Test
	void testeSindAttributeGleich_GleicheAttributeSindGleich() {
		ZustaendigkeitAttribute ZA1 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("Testus Maximus"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();
		ZustaendigkeitAttribute ZA2 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("Testus Maximus"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.7))
			.build();

		assertThat(ZA1.sindAttributeGleich(ZA1)).isTrue();
		assertThat(ZA2.sindAttributeGleich(ZA2)).isTrue();
		assertThat(ZA1.sindAttributeGleich(ZA2)).isTrue();
		assertThat(ZA2.sindAttributeGleich(ZA1)).isTrue();
	}

	@Test
	void testeSindAttributeGleich_AttributeUnterscheidenSichInEinemFeld() {
		ZustaendigkeitAttribute ZA1 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("Testus Maximus"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();
		ZustaendigkeitAttribute ZA2 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("Testus Maximus"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.7))
			.build();
		ZustaendigkeitAttribute ZA3 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("Testus Maximus"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.7))
			.build();
		ZustaendigkeitAttribute ZA4 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("Testus Maximus"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.7))
			.build();
		ZustaendigkeitAttribute ZA5 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("未観測だった花が咲く"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();

		assertThat(ZA1.sindAttributeGleich(ZA2)).isFalse();
		assertThat(ZA2.sindAttributeGleich(ZA1)).isFalse();
		assertThat(ZA1.sindAttributeGleich(ZA3)).isFalse();
		assertThat(ZA3.sindAttributeGleich(ZA1)).isFalse();
		assertThat(ZA1.sindAttributeGleich(ZA4)).isFalse();
		assertThat(ZA4.sindAttributeGleich(ZA1)).isFalse();
		assertThat(ZA1.sindAttributeGleich(ZA5)).isFalse();
		assertThat(ZA5.sindAttributeGleich(ZA1)).isFalse();

		assertThat(ZA2.sindAttributeGleich(ZA3)).isFalse();
		assertThat(ZA3.sindAttributeGleich(ZA2)).isFalse();
		assertThat(ZA2.sindAttributeGleich(ZA4)).isFalse();
		assertThat(ZA4.sindAttributeGleich(ZA2)).isFalse();
		assertThat(ZA2.sindAttributeGleich(ZA5)).isFalse();
		assertThat(ZA5.sindAttributeGleich(ZA2)).isFalse();

		assertThat(ZA3.sindAttributeGleich(ZA4)).isFalse();
		assertThat(ZA4.sindAttributeGleich(ZA3)).isFalse();
		assertThat(ZA3.sindAttributeGleich(ZA5)).isFalse();
		assertThat(ZA5.sindAttributeGleich(ZA3)).isFalse();

		assertThat(ZA4.sindAttributeGleich(ZA5)).isFalse();
		assertThat(ZA5.sindAttributeGleich(ZA4)).isFalse();
	}

	@Test
	void testeWidersprechenSichAttribute_GleicheAttribute_Nein() {
		ZustaendigkeitAttribute ZA1 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("Testus Maximus"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();
		ZustaendigkeitAttribute ZA2 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("Testus Maximus"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.7))
			.build();

		assertThat(ZA1.widersprechenSichAttribute(ZA1)).isFalse();
		assertThat(ZA2.widersprechenSichAttribute(ZA2)).isFalse();
		assertThat(ZA1.widersprechenSichAttribute(ZA2)).isFalse();
		assertThat(ZA2.widersprechenSichAttribute(ZA1)).isFalse();
	}

	@Test
	void testeWidersprechenSichAttribute_AnderesAttributLeer_Nein() {
		ZustaendigkeitAttribute ZA1 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("Testus Maximus"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();
		ZustaendigkeitAttribute ZA2 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(null)
			.baulastTraeger(null)
			.unterhaltsZustaendiger(null)
			.vereinbarungsKennung(null)
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.7))
			.build();

		assertThat(ZA1.widersprechenSichAttribute(ZA1)).isFalse();
		assertThat(ZA2.widersprechenSichAttribute(ZA2)).isFalse();
		assertThat(ZA1.widersprechenSichAttribute(ZA2)).isFalse();
		assertThat(ZA2.widersprechenSichAttribute(ZA1)).isFalse();
	}

	@Test
	void testeWidersprechenSichAttribute_UnterschiedlicheAttribute_Ja() {
		ZustaendigkeitAttribute ZA1 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("その先の歴史を駆け抜ける奴らに歌いたい"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();
		ZustaendigkeitAttribute ZA2 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("その先の歴史を駆け抜ける奴らに歌いたい"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.7))
			.build();
		ZustaendigkeitAttribute ZA3 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("その先の歴史を駆け抜ける奴らに歌いたい"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.7))
			.build();
		ZustaendigkeitAttribute ZA4 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(100L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("その先の歴史を駆け抜ける奴らに歌いたい"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.7))
			.build();
		ZustaendigkeitAttribute ZA5 = ZustaendigkeitAttribute.builder()
			.erhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build())
			.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
			.unterhaltsZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(3L).build())
			.vereinbarungsKennung(VereinbarungsKennung.of("未観測だった花が咲く"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();

		assertThat(ZA1.widersprechenSichAttribute(ZA2)).isTrue();
		assertThat(ZA2.widersprechenSichAttribute(ZA1)).isTrue();
		assertThat(ZA1.widersprechenSichAttribute(ZA3)).isTrue();
		assertThat(ZA3.widersprechenSichAttribute(ZA1)).isTrue();
		assertThat(ZA1.widersprechenSichAttribute(ZA4)).isTrue();
		assertThat(ZA4.widersprechenSichAttribute(ZA1)).isTrue();
		assertThat(ZA1.widersprechenSichAttribute(ZA5)).isTrue();
		assertThat(ZA5.widersprechenSichAttribute(ZA1)).isTrue();

		assertThat(ZA2.widersprechenSichAttribute(ZA3)).isTrue();
		assertThat(ZA3.widersprechenSichAttribute(ZA2)).isTrue();
		assertThat(ZA2.widersprechenSichAttribute(ZA4)).isTrue();
		assertThat(ZA4.widersprechenSichAttribute(ZA2)).isTrue();
		assertThat(ZA2.widersprechenSichAttribute(ZA5)).isTrue();
		assertThat(ZA5.widersprechenSichAttribute(ZA2)).isTrue();

		assertThat(ZA3.widersprechenSichAttribute(ZA4)).isTrue();
		assertThat(ZA4.widersprechenSichAttribute(ZA3)).isTrue();
		assertThat(ZA3.widersprechenSichAttribute(ZA5)).isTrue();
		assertThat(ZA5.widersprechenSichAttribute(ZA3)).isTrue();

		assertThat(ZA4.widersprechenSichAttribute(ZA5)).isTrue();
		assertThat(ZA5.widersprechenSichAttribute(ZA4)).isTrue();
	}
}
