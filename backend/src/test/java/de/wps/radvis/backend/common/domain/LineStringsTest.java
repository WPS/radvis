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

package de.wps.radvis.backend.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;

class LineStringsTest {
	@DisplayName("findeKehrtwenden: Parametrisierter Test")
	@ParameterizedTest(name = "{index} => lineString={0}, expectedKehrtwendenCoordinates={1}")
	@MethodSource("findeKehrtwendenTest_testParamProvider")
	void findeKehrtwenden_paramTest(
		LineString lineString,
		List<Coordinate> expectedKehrtwendenCoordinates) {
		// act
		MultiPoint kehrtwenden = LineStrings.findeKehrtwenden(lineString);

		// assert
		assertThat(kehrtwenden.getCoordinates()).containsExactlyElementsOf(expectedKehrtwendenCoordinates);
	}

	private static Stream<Arguments> findeKehrtwendenTest_testParamProvider() {
		return Stream.of(
			// keineKehrtwendeUndKeinFehler
			Arguments.of(
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(1, 0)),
				List.of()),
			// rechterWinkel -> keineKehrtwende
			Arguments.of(
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(1, 0),
					new Coordinate(1, 1)),
				List.of()),
			// spitzerWinkel -> keineKehrtwende
			Arguments.of(
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(1, 0),
					new Coordinate(0, 0.1)),
				List.of()),
			// hinZurueck -> einfacheKehrtwende
			Arguments.of(
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(1, 0),
					new Coordinate(0, 0)),
				List.of(new Coordinate(1, 0))),
			// hinZurueckHin -> zweiKehrtwenden
			Arguments.of(
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(1, 0),
					new Coordinate(0, 0),
					new Coordinate(1, 0)),
				List.of(new Coordinate(1, 0),
					new Coordinate(0, 0))));
	}

	@DisplayName("entferneArtifizielleKehrtwenden: Parametrisierter Test")
	@ParameterizedTest(name = "{index} => lineStringAusRoutingOderMatching={0}, originalLineString={1}, expectedCoordinates={2}")
	@MethodSource("entferneArtifizielleKehrtwendenTest_testParamProvider")
	void entferneArtifizielleKehrtwenden_paramTest(
		LineString lineStringAusRoutingOderMatching,
		LineString originalLineString,
		List<Coordinate> expectedCoordinates) {
		// act
		LineString bereinigt = LineStrings.entferneArtifizielleKehrtwenden(lineStringAusRoutingOderMatching,
			originalLineString);

		// assert
		assertThat(bereinigt.getCoordinates()).containsExactlyElementsOf(expectedCoordinates);
	}

	private static Stream<Arguments> entferneArtifizielleKehrtwendenTest_testParamProvider() {
		return Stream.of(
			// keine Kehrtwende: zurückgegebener LineString ist identisch
			Arguments.of(
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(10, 100)),
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(11, 101)),
				List.of(new Coordinate(0, 0),
					new Coordinate(10, 100))),
			// kehrtwende mit Gegenstück im originalLineString: wird nicht entfernt
			Arguments.of(
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(10, 100),
					new Coordinate(0, 0)),
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(11, 101),
					new Coordinate(0, 0)),
				List.of(new Coordinate(0, 0),
					new Coordinate(10, 100),
					new Coordinate(0, 0))),
			// kehrtwende ohne Gegenstück im originalLineString: wird entfernt
			Arguments.of(
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(10, 100),
					new Coordinate(0, 0),
					new Coordinate(100, 10)),
				GeometryTestdataProvider.createLineString(
					new Coordinate(1, 1),
					new Coordinate(101, 9)),
				List.of(new Coordinate(0, 0),
					new Coordinate(100, 10))));
	}

	@DisplayName("Abweichende Segmente: Parametrisierter Test")
	@ParameterizedTest(name = "{index} => referenz={0}, abweichung={1}, expectedCoordinates={2}")
	@MethodSource("abweichendeSegmenteTest_testParamProvider")
	void findeSegmenteZweierLinestringsMitAbstandGroesserAls_paramTest(
		LineString referenz,
		LineString abweichung,
		List<Coordinate> expectedCoordinates) {
		// act
		MultiLineString abweichendeSegmente = LineStrings.findeSegmenteZweierLinestringsMitAbstandGroesserAls(
			abweichung,
			referenz, 5);

		// assert
		assertThat(abweichendeSegmente.getCoordinates()).containsExactlyElementsOf(expectedCoordinates);
	}

	private static Stream<Arguments> abweichendeSegmenteTest_testParamProvider() {
		LineString referenzLineString2Coords = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(10, 0));
		LineString referenzLineString3Coords = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0));
		LineString referenzLineString5Coords = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(10, 0),
			new Coordinate(20, 0),
			new Coordinate(30, 0),
			new Coordinate(40, 0),
			new Coordinate(50, 0));
		return Stream.of(
			// ******************** LineString aus 2 Koordinaten **************************
			// LineStringNurZweiPunkte_keineAbw
			Arguments.of(
				referenzLineString2Coords,
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(10, 0)),
				List.of()),
			// LineStringNurZweiPunkte_abw
			Arguments.of(
				referenzLineString2Coords,
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(10, 10)),
				List.of(new Coordinate(0, 0),
					new Coordinate(10, 10))),
			// ersteVonZweiKoordinatenWeichtAb
			Arguments.of(
				referenzLineString2Coords,
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 10),
					new Coordinate(10, 0)),
				List.of(new Coordinate(0, 10),
					new Coordinate(10, 0))),
			// zweiteVonZweiKoordinatenWeichtAb
			Arguments.of(
				referenzLineString2Coords,
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(10, 10)),
				List.of(new Coordinate(0, 0),
					new Coordinate(10, 10))),
			// ******************** LineString aus 2 Koordinaten **************************
			// ersteKoordinateWeichtAb
			Arguments.of(
				referenzLineString3Coords,
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 10),
					new Coordinate(10, 0),
					new Coordinate(20, 0)),
				List.of(new Coordinate(0, 10),
					new Coordinate(10, 0))),
			// letzteKoordinateWeichtAb
			Arguments.of(
				referenzLineString3Coords,
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(10, 0),
					new Coordinate(20, 10)),
				List.of(new Coordinate(10, 0),
					new Coordinate(20, 10))),
			// mittlereKoordinateWeichtAb
			Arguments.of(
				referenzLineString3Coords,
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(10, 10),
					new Coordinate(20, 0)),
				List.of(new Coordinate(0, 0),
					new Coordinate(10, 10),
					new Coordinate(20, 0))),
			// ******************** LineString aus vielen Koordinaten **************************
			// mittlereKoordinateWeichtAb
			Arguments.of(
				referenzLineString5Coords,
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(10, 0),
					new Coordinate(20, 0),
					new Coordinate(30, 0),
					new Coordinate(40, 0),
					new Coordinate(50, 0)),
				List.of()),
			// mittlereKoordinateWeichtAb
			Arguments.of(
				referenzLineString5Coords,
				GeometryTestdataProvider.createLineString(
					new Coordinate(0, 0),
					new Coordinate(10, 0),
					new Coordinate(20, 10),
					new Coordinate(30, 10),
					new Coordinate(40, 0),
					new Coordinate(50, 0)),
				List.of(new Coordinate(10, 0),
					new Coordinate(20, 10),
					new Coordinate(30, 10),
					new Coordinate(40, 0))));
	}

	@Test
	void haveSameStationierungsrichtung_sameLineString_true() {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(100, 100));

		// act + assert
		assertThat(LineStrings.haveSameStationierungsrichtung(lineString, lineString)).isTrue();
	}

	@Test
	void haveSameStationierungsrichtung_reverseLineString_false() {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(100, 100));

		// act + assert
		assertThat(LineStrings.haveSameStationierungsrichtung(lineString, lineString.reverse())).isFalse();
	}

	@Test
	void haveSameStationierungsrichtung_sameLineStringUeberschneidungEnde_true() {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(100, 100));
		LineString lineStringUeberschneidung = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(50, 150));

		// act + assert
		assertThat(LineStrings.haveSameStationierungsrichtung(lineString, lineStringUeberschneidung)).isTrue();
	}

	@Test
	void haveSameStationierungsrichtung_reverseLineStringUeberschneidungEnde_false() {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(100, 100));
		LineString lineStringUeberschneidung = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(50, 150));

		// act + assert
		assertThat(LineStrings.haveSameStationierungsrichtung(lineString, lineStringUeberschneidung.reverse()))
			.isFalse();
	}

	@Test
	void haveSameStationierungsrichtung_sameLineStringUeberschneidungAnfang_true() {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(100, 100));
		LineString lineStringUeberschneidung = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(50, 150));

		// act + assert
		assertThat(LineStrings.haveSameStationierungsrichtung(lineString, lineStringUeberschneidung)).isTrue();
	}

	@Test
	void haveSameStationierungsrichtung_reverseLineStringUeberschneidungAnfang_false() {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(100, 100));
		LineString lineStringUeberschneidung = GeometryTestdataProvider.createLineString(new Coordinate(0, 0),
			new Coordinate(50, 150));

		// act + assert
		assertThat(LineStrings.haveSameStationierungsrichtung(lineString, lineStringUeberschneidung.reverse()))
			.isFalse();
	}

}