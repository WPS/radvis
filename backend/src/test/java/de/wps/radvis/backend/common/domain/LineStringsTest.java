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
import org.locationtech.jts.geom.util.AffineTransformation;

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

	@Test
	void sindExaktParallel_betrachtetLineStringRichtungKorrekt() {
		// Arrange
		// Beide LineStrings bilden einen Bogen, der A-Bogen ist etwas genauer (mehr Koordinaten) als der B-Bogen. Sie
		// verlaufen aber trotzdem einigermaßen parallel.
		LineString lineStringA = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 0),
			new Coordinate(15, 10),
			new Coordinate(20, 15),
			new Coordinate(25, 17),
			new Coordinate(30, 20),
			new Coordinate(40, 22),
			new Coordinate(50, 20),
			new Coordinate(55, 17),
			new Coordinate(60, 15),
			new Coordinate(65, 10),
			new Coordinate(70, 0)
		);

		LineString lineStringB = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(10, 20),
			new Coordinate(40, 30),
			new Coordinate(60, 20),
			new Coordinate(70, 0)
		);

		// Act & Assert
		double tolerance = 6d;
		assertThat(LineStrings.sindExaktParallel(lineStringA, lineStringB, 10, tolerance)).isTrue();
		assertThat(LineStrings.sindExaktParallel(lineStringA.reverse(), lineStringB, 10, tolerance)).isFalse();
		assertThat(LineStrings.sindExaktParallel(lineStringA, lineStringB.reverse(), 10, tolerance)).isFalse();
		assertThat(LineStrings.sindExaktParallel(lineStringA.reverse(), lineStringB.reverse(), 10, tolerance)).isTrue();

		assertThat(LineStrings.sindExaktParallel(lineStringB, lineStringA, 10, tolerance)).isTrue();
		assertThat(LineStrings.sindExaktParallel(lineStringB.reverse(), lineStringA, 10, tolerance)).isFalse();
		assertThat(LineStrings.sindExaktParallel(lineStringB, lineStringA.reverse(), 10, tolerance)).isFalse();
		assertThat(LineStrings.sindExaktParallel(lineStringB.reverse(), lineStringA.reverse(), 10, tolerance)).isTrue();
	}

	@Test
	void sindParallel_lineStringRichtungIstIrrelevant() {
		// Arrange
		// Beide LineStrings bilden einen Bogen, der A-Bogen ist etwas genauer (mehr Koordinaten) als der B-Bogen. Sie
		// verlaufen aber trotzdem einigermaßen parallel.
		LineString lineStringA = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 0),
			new Coordinate(15, 10),
			new Coordinate(20, 15),
			new Coordinate(25, 17),
			new Coordinate(30, 20),
			new Coordinate(40, 22),
			new Coordinate(50, 20),
			new Coordinate(55, 17),
			new Coordinate(60, 15),
			new Coordinate(65, 10),
			new Coordinate(70, 0)
		);

		LineString lineStringB = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(10, 20),
			new Coordinate(40, 30),
			new Coordinate(60, 20),
			new Coordinate(70, 0)
		);

		LineString lineStringC = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(10, 100)
		);

		// Act & Assert
		double tolerance = 6d;

		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, tolerance)).isTrue();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB.reverse(), 10, tolerance)).isTrue();
		assertThat(LineStrings.sindParallel(lineStringA.reverse(), lineStringB, 10, tolerance)).isTrue();
		assertThat(LineStrings.sindParallel(lineStringA.reverse(), lineStringB.reverse(), 10, tolerance)).isTrue();

		assertThat(LineStrings.sindParallel(lineStringA, lineStringC, 10, tolerance)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringC.reverse(), 10, tolerance)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA.reverse(), lineStringC, 10, tolerance)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA.reverse(), lineStringC.reverse(), 10, tolerance)).isFalse();
	}

	@Test
	void sindExaktParallel_toleranzWirdEingehalten() {
		// Arrange
		LineString lineStringA = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(0, 100)
		);

		LineString lineStringB = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 0),
			new Coordinate(10, 100)
		);

		// Act & Assert
		double tolerance = 45.1; // .1 um float-Ungenauigkeiten zu ignorieren

		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, tolerance)).isTrue();

		// Schrittweise rotieren, bis Grenzwert überschritten wurde
		AffineTransformation transformation = AffineTransformation.rotationInstance(Math.toRadians(1), lineStringB
			.getCentroid().getX(), lineStringB.getCentroid().getY());

		LineString rotatedLineStringB = lineStringB;

		// 0° - 45°
		for (int i = 0; i < 46; i++) {
			assertThat(LineStrings.sindExaktParallel(lineStringA, rotatedLineStringB, 10, tolerance)).isTrue();
			rotatedLineStringB = (LineString) transformation.transform(rotatedLineStringB);
		}
		// 46° - 314° (314° = -46°)
		for (int i = 46; i < 315; i++) {
			assertThat(LineStrings.sindExaktParallel(lineStringA, rotatedLineStringB, 10, tolerance)).isFalse();
			rotatedLineStringB = (LineString) transformation.transform(rotatedLineStringB);
		}
		// 315° - 360°
		for (int i = 315; i < 361; i++) {
			assertThat(LineStrings.sindExaktParallel(lineStringA, rotatedLineStringB, 10, tolerance)).isTrue();
			rotatedLineStringB = (LineString) transformation.transform(rotatedLineStringB);
		}
	}

	@Test
	void sindParallel_toleranzWirdEingehaltenUndRichtungIstIrrelevant() {
		// Arrange
		LineString lineStringA = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(0, 100)
		);

		LineString lineStringB = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 0),
			new Coordinate(10, 100)
		);

		// Act & Assert
		double tolerance = 45.1; // .1 um float-Ungenauigkeiten zu ignorieren

		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, tolerance)).isTrue();

		// Schrittweise rotieren, bis Grenzwert überschritten wurde
		AffineTransformation transformation = AffineTransformation.rotationInstance(Math.toRadians(1), lineStringB
			.getCentroid().getX(), lineStringB.getCentroid().getY());

		LineString rotatedLineStringB = lineStringB;

		// 0° - 45°
		for (int i = 0; i < 46; i++) {
			assertThat(LineStrings.sindParallel(lineStringA, rotatedLineStringB, 10, tolerance)).isTrue();
			assertThat(LineStrings.sindParallel(lineStringA, rotatedLineStringB.reverse(), 10, tolerance)).isTrue();
			assertThat(LineStrings.sindParallel(lineStringA.reverse(), rotatedLineStringB, 10, tolerance)).isTrue();
			assertThat(LineStrings.sindParallel(lineStringA.reverse(), rotatedLineStringB.reverse(), 10, tolerance))
				.isTrue();
			rotatedLineStringB = (LineString) transformation.transform(rotatedLineStringB);
		}
		// 46° - 134°
		for (int i = 46; i < 135; i++) {
			assertThat(LineStrings.sindParallel(lineStringA, rotatedLineStringB, 10, tolerance)).isFalse();
			assertThat(LineStrings.sindParallel(lineStringA, rotatedLineStringB.reverse(), 10, tolerance)).isFalse();
			assertThat(LineStrings.sindParallel(lineStringA.reverse(), rotatedLineStringB, 10, tolerance)).isFalse();
			assertThat(LineStrings.sindParallel(lineStringA.reverse(), rotatedLineStringB.reverse(), 10, tolerance))
				.isFalse();
			rotatedLineStringB = (LineString) transformation.transform(rotatedLineStringB);
		}
		// 135° - 225° (rotierter LineStrings ist jetzt anti-parallel)
		for (int i = 135; i < 226; i++) {
			assertThat(LineStrings.sindParallel(lineStringA, rotatedLineStringB, 10, tolerance)).isTrue();
			assertThat(LineStrings.sindParallel(lineStringA, rotatedLineStringB.reverse(), 10, tolerance)).isTrue();
			assertThat(LineStrings.sindParallel(lineStringA.reverse(), rotatedLineStringB, 10, tolerance)).isTrue();
			assertThat(LineStrings.sindParallel(lineStringA.reverse(), rotatedLineStringB.reverse(), 10, tolerance))
				.isTrue();
			rotatedLineStringB = (LineString) transformation.transform(rotatedLineStringB);
		}
		// 226° - 314°
		for (int i = 226; i < 315; i++) {
			assertThat(LineStrings.sindParallel(lineStringA, rotatedLineStringB, 10, tolerance)).isFalse();
			assertThat(LineStrings.sindParallel(lineStringA, rotatedLineStringB.reverse(), 10, tolerance)).isFalse();
			assertThat(LineStrings.sindParallel(lineStringA.reverse(), rotatedLineStringB, 10, tolerance)).isFalse();
			assertThat(LineStrings.sindParallel(lineStringA.reverse(), rotatedLineStringB.reverse(), 10, tolerance))
				.isFalse();
			rotatedLineStringB = (LineString) transformation.transform(rotatedLineStringB);
		}
		// 315° - 360°
		for (int i = 315; i < 361; i++) {
			assertThat(LineStrings.sindParallel(lineStringA, rotatedLineStringB, 10, tolerance)).isTrue();
			assertThat(LineStrings.sindParallel(lineStringA, rotatedLineStringB.reverse(), 10, tolerance)).isTrue();
			assertThat(LineStrings.sindParallel(lineStringA.reverse(), rotatedLineStringB, 10, tolerance)).isTrue();
			assertThat(LineStrings.sindParallel(lineStringA.reverse(), rotatedLineStringB.reverse(), 10, tolerance))
				.isTrue();
			rotatedLineStringB = (LineString) transformation.transform(rotatedLineStringB);
		}
	}

	@Test
	void sindParallel_kurvigeLineStringsSindNichtParallel() {
		// Arrange

		// "S"-förmiger LineString
		LineString lineStringA = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 0),
			new Coordinate(20, 10),
			new Coordinate(10, 20),
			new Coordinate(0, 30),
			new Coordinate(10, 40)
		);

		// Quasi "lineStringA" aber an der y-Achse gespiegelt
		LineString lineStringB = GeometryTestdataProvider.createLineString(
			new Coordinate(110, 0),
			new Coordinate(100, 10),
			new Coordinate(110, 20),
			new Coordinate(120, 30),
			new Coordinate(110, 40)
		);

		// "C"-förmiger LineString
		LineString lineStringC = GeometryTestdataProvider.createLineString(
			new Coordinate(200, 0),
			new Coordinate(210, 10),
			new Coordinate(210, 10),
			new Coordinate(200, 20)
		);

		// Act & Assert (nur Start- und Endpunkt vergleichen indem nur mit einem Segment gesampled wird)
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 1, 0)).isTrue();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringC, 1, 0)).isTrue();
		assertThat(LineStrings.sindParallel(lineStringC, lineStringB, 1, 0)).isTrue();

		// Act & Assert (lineStringA vs. lineStringB)
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 0)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 10)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 20)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 30)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 40)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 50)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 60)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 70)).isFalse();

		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 80)).isTrue();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 90)).isTrue();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringB, 10, 100)).isTrue();

		// Act & Assert (lineStringA vs. lineStringC)
		assertThat(LineStrings.sindParallel(lineStringA, lineStringC, 10, 0)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringC, 10, 10)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringC, 10, 20)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringC, 10, 30)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringC, 10, 40)).isFalse();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringC, 10, 50)).isTrue();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringC, 10, 60)).isTrue();
		assertThat(LineStrings.sindParallel(lineStringA, lineStringC, 10, 70)).isTrue();
	}
}