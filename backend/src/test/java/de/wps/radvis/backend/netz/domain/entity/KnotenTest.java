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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Bauwerksmangel;
import de.wps.radvis.backend.netz.domain.valueObject.BauwerksmangelArt;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.QuerungshilfeDetails;

class KnotenTest {

	@Test
	public void getKoordinate() {
		// arrange
		Coordinate koordinate = new Coordinate(10, 10, 0);
		Knoten knoten = KnotenTestDataProvider.withCoordinateAndQuelle(koordinate, QuellSystem.LGL).build();

		// act
		Coordinate result = knoten.getKoordinate();

		// assert
		assertEquals(koordinate, result);
	}

	@Test
	public void getQuelle() {
		// arrange
		QuellSystem quelle = QuellSystem.LGL;
		Coordinate koordinate = new Coordinate(10, 10, 0);
		Knoten knoten = KnotenTestDataProvider.withCoordinateAndQuelle(koordinate, quelle).build();

		// act
		QuellSystem result = knoten.getQuelle();

		// assert
		assertEquals(quelle, result);
	}

	@Test
	public void isQuerungshilfeDetailsValid_knotenformNull() {
		assertThat(Knoten.isQuerungshilfeDetailsValid(null, null)).isTrue();
		assertThat(
			Knoten.isQuerungshilfeDetailsValid(QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL, null))
				.isFalse();
	}

	@Test
	public void isQuerungshilfeDetailsValid_knotenOhneQuerungshilfe() {
		assertThat(Knoten.isQuerungshilfeDetailsValid(null, KnotenForm.UEBERFUEHRUNG)).isTrue();
		assertThat(Knoten.isQuerungshilfeDetailsValid(QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL,
			KnotenForm.UEBERFUEHRUNG)).isFalse();
	}

	@Test
	public void isQuerungshilfeDetailsValid_knotenformMitQuerungshilfe() {
		assertThat(Knoten.isQuerungshilfeDetailsValid(QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL,
			KnotenForm.MITTELINSEL_EINFACH)).isTrue();
		// querungshilfe ist required
		assertThat(Knoten.isQuerungshilfeDetailsValid(null, KnotenForm.MITTELINSEL_EINFACH)).isFalse();
		// querungshilfe muss zu knotenform passen
		assertThat(Knoten.isQuerungshilfeDetailsValid(QuerungshilfeDetails.VORHANDEN_MIT_FURT,
			KnotenForm.MITTELINSEL_EINFACH))
				.isFalse();
	}

	@Test
	public void isBauwerksmangelValid_knotenformNull() {
		assertThat(Knoten.isBauwerksmangelValid(null, null, null)).isTrue();
		assertThat(Knoten.isBauwerksmangelValid(Bauwerksmangel.NICHT_VORHANDEN, null, null)).isFalse();
		assertThat(
			Knoten.isBauwerksmangelValid(Bauwerksmangel.VORHANDEN, Set.of(BauwerksmangelArt.ANDERER_MANGEL), null))
				.isFalse();
		assertThat(Knoten.isBauwerksmangelValid(null, Set.of(BauwerksmangelArt.ANDERER_MANGEL), null))
			.isFalse();
	}

	@Test
	public void isBauwerksmangelValid_keineKnotenformMitBauwerksmangel_bauwerksmangelNotAllowed() {
		assertThat(Knoten.isBauwerksmangelValid(null, null, KnotenForm.MITTELINSEL_EINFACH)).isTrue();

		assertThat(Knoten.isBauwerksmangelValid(Bauwerksmangel.NICHT_VORHANDEN, null, KnotenForm.MITTELINSEL_EINFACH))
			.isFalse();
		assertThat(Knoten.isBauwerksmangelValid(Bauwerksmangel.VORHANDEN, Set.of(BauwerksmangelArt.ANDERER_MANGEL),
			KnotenForm.MITTELINSEL_EINFACH))
				.isFalse();
		assertThat(Knoten.isBauwerksmangelValid(null, Set.of(BauwerksmangelArt.ANDERER_MANGEL),
			KnotenForm.MITTELINSEL_EINFACH))
				.isFalse();
	}

	@Test
	public void isBauwerksmangelValid_knotenformMitBauwerksmangel_bauwerksmangelRequired() {
		assertThat(Knoten.isBauwerksmangelValid(null, null, KnotenForm.UEBERFUEHRUNG)).isFalse();
		assertThat(Knoten.isBauwerksmangelValid(Bauwerksmangel.NICHT_VORHANDEN, null, KnotenForm.UEBERFUEHRUNG))
			.isTrue();

	}

	@Test
	public void isBauwerksmangelValid_bauwerksmangelVorhanden_bauwerksmangelArtValid() {
		// BauwerksmangelArt ist required
		assertThat(Knoten.isBauwerksmangelValid(Bauwerksmangel.VORHANDEN, null, KnotenForm.UEBERFUEHRUNG)).isFalse();
		assertThat(
			Knoten.isBauwerksmangelValid(Bauwerksmangel.VORHANDEN, Collections.emptySet(), KnotenForm.UEBERFUEHRUNG))
				.isFalse();
		assertThat(Knoten.isBauwerksmangelValid(Bauwerksmangel.VORHANDEN, Set.of(BauwerksmangelArt.ANDERER_MANGEL),
			KnotenForm.UEBERFUEHRUNG)).isTrue();

		// BauwerksmangelArt muss zu Knotenform passen
		assertThat(Knoten.isBauwerksmangelValid(Bauwerksmangel.VORHANDEN,
			Set.of(BauwerksmangelArt.ANDERER_MANGEL, BauwerksmangelArt.ZU_NIEDRIG),
			KnotenForm.UEBERFUEHRUNG)).isFalse();
	}
}