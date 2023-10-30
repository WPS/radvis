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

package de.wps.radvis.backend.quellimport.ttsib.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.quellimport.ttsib.domain.valueObject.TtSibEinordnung;
import de.wps.radvis.backend.quellimport.ttsib.domain.valueObject.TtSibQuerschnittArt;

class TtSibAbschnittOderAstTest {

	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
	private static final double TOLERANCE = 0.01;

	@Test
	void test_daten_werden_vollstaendig_festgehalten() {
		// arrange
		TtSibStreifen ttSibStreifenA = new TtSibStreifen(100, 150, TtSibEinordnung.MITTE, 0,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);
		TtSibStreifen ttSibStreifenB = new TtSibStreifen(110, 160, TtSibEinordnung.LINKS, 1,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);

		TtSibQuerschnitt ttSibQuerschnitt = new TtSibQuerschnitt();
		ttSibQuerschnitt.addStreifen(ttSibStreifenA);
		ttSibQuerschnitt.addStreifen(ttSibStreifenB);

		TtSibTeilabschnitt ttSibTeilabschnittA = new TtSibTeilabschnitt(12, 34, ttSibQuerschnitt);
		TtSibTeilabschnitt ttSibTeilabschnittB = new TtSibTeilabschnitt(56, 78, ttSibQuerschnitt);

		// act
		TtSibAbschnittOderAst ttSibAoA = new TtSibAbschnittOderAst();
		ttSibAoA.addTeilabschnitt(ttSibTeilabschnittA);
		ttSibAoA.addTeilabschnitt(ttSibTeilabschnittB);

		// assert
		assertThat(ttSibAoA.getTeilabschnitte()).containsExactlyInAnyOrder(ttSibTeilabschnittA, ttSibTeilabschnittB);
	}

	@Test
	void test_ueberschneidungen_von_stationen_ist_nicht_erlaubt() {
		// arrange
		TtSibStreifen ttSibStreifenA = new TtSibStreifen(100, 150, TtSibEinordnung.MITTE, 0,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);
		TtSibStreifen ttSibStreifenB = new TtSibStreifen(110, 160, TtSibEinordnung.LINKS, 1,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);

		TtSibQuerschnitt ttSibQuerschnitt = new TtSibQuerschnitt();
		ttSibQuerschnitt.addStreifen(ttSibStreifenA);
		ttSibQuerschnitt.addStreifen(ttSibStreifenB);

		TtSibTeilabschnitt ttSibTeilabschnittA = new TtSibTeilabschnitt(12, 34, ttSibQuerschnitt);
		TtSibTeilabschnitt ttSibTeilabschnittB = new TtSibTeilabschnitt(33, 78, ttSibQuerschnitt);

		// act
		TtSibAbschnittOderAst ttSibAoA = new TtSibAbschnittOderAst();
		ttSibAoA.addTeilabschnitt(ttSibTeilabschnittA);

		// assert
		try {
			// act
			ttSibAoA.addTeilabschnitt(ttSibTeilabschnittB);
			// assert fails
			fail();
		} catch (RequireViolation e) {
			// assert message is correct
			String expectedMessage = "Teilabschnitte überschneiden sich";
			String actualMessage = e.getMessage();

			assertTrue(actualMessage.contains(expectedMessage));
		}
	}

	@Test
	void ermittleRadwegVerlaeufe_soll_radwege_geometrie_zureuckgeben() {
		// arrange
		TtSibStreifen ttSibStreifenA = new TtSibStreifen(200, 200, TtSibEinordnung.MITTE, 0,
			TtSibQuerschnittArt.RADWEG);
		TtSibStreifen ttSibStreifenB = new TtSibStreifen(400, 400, TtSibEinordnung.LINKS, 1,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);
		TtSibStreifen ttSibStreifenC = new TtSibStreifen(600, 600, TtSibEinordnung.LINKS, 2,
			TtSibQuerschnittArt.SCHLITZRINNE);
		LineString mittelstreifengeometrie = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(10, 0), new Coordinate(10, 20) });

		TtSibQuerschnitt ttSibQuerschnitt = new TtSibQuerschnitt();
		ttSibQuerschnitt.addStreifen(ttSibStreifenA);
		ttSibQuerschnitt.addStreifen(ttSibStreifenB);
		ttSibQuerschnitt.addStreifen(ttSibStreifenC);

		TtSibTeilabschnitt ttSibTeilabschnitt = new TtSibTeilabschnitt(0, 20, ttSibQuerschnitt);

		TtSibAbschnittOderAst ttSibAoA = new TtSibAbschnittOderAst();
		ttSibAoA.addTeilabschnitt(ttSibTeilabschnitt);
		ttSibAoA.setGeometry(mittelstreifengeometrie);

		// act
		Set<LineString> radwegVerlaeufe = ttSibAoA.ermittleRadwegverlaeufe();

		// assert
		assertThat(radwegVerlaeufe).hasSize(2);
		LineString expectedRadwegVerlaufLinks = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(7, 0), new Coordinate(7, 20) });
		LineString expectedRadwegVerlaufMitte = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(10, 0), new Coordinate(10, 20) });
		assertThat(radwegVerlaeufe)
			.contains(expectedRadwegVerlaufLinks)
			.contains(expectedRadwegVerlaufMitte);
	}

	@Test
	void ermittleRadwegVerlaeufe_soll_aussenverlaufende_trapezfoermige_streifen_beruecksichtigen() {
		// arrange
		TtSibStreifen ttSibStreifenA = new TtSibStreifen(0, 0, TtSibEinordnung.MITTE, 0,
			TtSibQuerschnittArt.SCHLITZRINNE);
		TtSibStreifen ttSibStreifenB = new TtSibStreifen(200, 600, TtSibEinordnung.LINKS, 1,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);
		LineString mittelstreifengeometrie = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(5, 0), new Coordinate(5, 4), new Coordinate(9, 4) });

		TtSibQuerschnitt ttSibQuerschnitt = new TtSibQuerschnitt();
		ttSibQuerschnitt.addStreifen(ttSibStreifenA);
		ttSibQuerschnitt.addStreifen(ttSibStreifenB);

		TtSibTeilabschnitt ttSibTeilabschnitt = new TtSibTeilabschnitt(0, 8, ttSibQuerschnitt);

		TtSibAbschnittOderAst ttSibAoA = new TtSibAbschnittOderAst();
		ttSibAoA.addTeilabschnitt(ttSibTeilabschnitt);
		ttSibAoA.setGeometry(mittelstreifengeometrie);

		// act
		Set<LineString> radwegVerlaeufe = ttSibAoA.ermittleRadwegverlaeufe();

		// assert
		assertThat(radwegVerlaeufe.size()).isEqualTo(1);
		Optional<LineString> radwegeVerlauf = radwegVerlaeufe.stream().findFirst();
		assertThat(radwegeVerlauf).isPresent();

		LineString expectedRadwegVerlauf = GEO_FACTORY
			.createLineString(
				new Coordinate[] { new Coordinate(4, 0), new Coordinate(2.64, 5.41),
					new Coordinate(9, 7) });

		assertThat(radwegeVerlauf.get().equalsExact(expectedRadwegVerlauf, TOLERANCE)).isTrue();
	}

	@Test
	void ermittleRadwegVerlaeufe_soll_innenverlaufende_trapezfoermige_streifen_beruecksichtigen() {
		// arrange
		TtSibStreifen ttSibStreifenA = new TtSibStreifen(0, 0, TtSibEinordnung.MITTE, 0,
			TtSibQuerschnittArt.SCHLITZRINNE);
		TtSibStreifen ttSibStreifenB = new TtSibStreifen(200, 400, TtSibEinordnung.RECHTS, 1,
			TtSibQuerschnittArt.RAD_UND_GEHWEG_Z_240);
		LineString mittelstreifengeometrie = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(5, 0), new Coordinate(5, 4), new Coordinate(9, 4) });

		TtSibQuerschnitt ttSibQuerschnitt = new TtSibQuerschnitt();
		ttSibQuerschnitt.addStreifen(ttSibStreifenA);
		ttSibQuerschnitt.addStreifen(ttSibStreifenB);

		TtSibTeilabschnitt ttSibTeilabschnitt = new TtSibTeilabschnitt(0, 8, ttSibQuerschnitt);

		TtSibAbschnittOderAst ttSibAoA = new TtSibAbschnittOderAst();
		ttSibAoA.addTeilabschnitt(ttSibTeilabschnitt);
		ttSibAoA.setGeometry(mittelstreifengeometrie);

		// act
		Set<LineString> radwegVerlaeufe = ttSibAoA.ermittleRadwegverlaeufe();

		// assert
		assertThat(radwegVerlaeufe.size()).isEqualTo(1);
		Optional<LineString> radwegeVerlauf = radwegVerlaeufe.stream().findFirst();
		assertThat(radwegeVerlauf).isPresent();

		LineString expectedRadwegVerlauf = GEO_FACTORY
			.createLineString(
				new Coordinate[] { new Coordinate(6, 0), new Coordinate(6.29, 2.33), new Coordinate(9, 2) });

		assertThat(radwegeVerlauf.get().equalsExact(expectedRadwegVerlauf, TOLERANCE)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	void ermittleRadwegVerlaeufe_soll_AoA_geometrie_korrekt_zerschneiden() throws Exception {
		// arrange
		ArgumentCaptor<LineString> lineStringArgumentCaptor = ArgumentCaptor.forClass(LineString.class);
		TtSibTeilabschnitt ttSibTeilabschnittMockA = Mockito.mock(TtSibTeilabschnitt.class);
		when(ttSibTeilabschnittMockA.getVonStation()).thenReturn(0);
		when(ttSibTeilabschnittMockA.getBisStation()).thenReturn(2);
		TtSibTeilabschnitt ttSibTeilabschnittMockB = Mockito.mock(TtSibTeilabschnitt.class);
		when(ttSibTeilabschnittMockB.getVonStation()).thenReturn(2);
		when(ttSibTeilabschnittMockB.getBisStation()).thenReturn(8);
		TtSibAbschnittOderAst ttSibAoA = new TtSibAbschnittOderAst();
		ttSibAoA.addTeilabschnitt(ttSibTeilabschnittMockA);
		ttSibAoA.addTeilabschnitt(ttSibTeilabschnittMockB);
		LineString mittelstreifengeometrie = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(5, 0), new Coordinate(5, 4), new Coordinate(9, 4) });
		ttSibAoA.setGeometry(mittelstreifengeometrie);

		// act
		ttSibAoA.ermittleRadwegverlaeufe();

		// assert
		Mockito.verify(ttSibTeilabschnittMockA).ermittleRadwegverlaeufe(lineStringArgumentCaptor.capture());
		LineString resultGeometryA = lineStringArgumentCaptor.getValue();
		Mockito.verify(ttSibTeilabschnittMockB).ermittleRadwegverlaeufe(lineStringArgumentCaptor.capture());
		LineString resultGeometryB = lineStringArgumentCaptor.getValue();
		LineString expectedGeometryA = GEO_FACTORY.createLineString(
			new Coordinate[] { new Coordinate(5, 0), new Coordinate(5, 2) });
		LineString expectedGeometryB = GEO_FACTORY.createLineString(
			new Coordinate[] { new Coordinate(5, 2), new Coordinate(5, 4), new Coordinate(9, 4) });

		assertThat(resultGeometryA).isEqualTo(expectedGeometryA);
		assertThat(resultGeometryB).isEqualTo(expectedGeometryB);
	}

}
