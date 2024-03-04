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

package de.wps.radvis.backend.integration.attributAbbildung.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

import de.wps.radvis.backend.common.domain.exception.KeineUeberschneidungException;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KanteDubletteTest {

	private static GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	/**
	 * Definiert einen LineString aus 10 Koordinaten mit der Länge ~663 Meter
	 * <p>
	 * Hinter den Coordinaten ist der Index, sowie die Distanz zur nachfolgenden Coordinate angegeben
	 */
	private static Coordinate[] C = new Coordinate[] {
		new Coordinate(414468.2158720656, 5316872.694639614), // 0; 55
		new Coordinate(414485.2725343314, 5316925.912835875), // 1; 114
		new Coordinate(414519.5058246597, 5317035.460292882), // 2; 67
		new Coordinate(414537.4892169263, 5317100.893350286), // 3; 72
		new Coordinate(414555.1039603605, 5317171.668008491), // 4; 99
		new Coordinate(414573.6400449512, 5317269.331718679), // 5; 52
		new Coordinate(414583.7414200881, 5317321.541844417), // 6; 106
		new Coordinate(414596.7964775269, 5317426.957659791), // 7; 57
		new Coordinate(414605.6225423241, 5317483.744662981), // 8; 35
		new Coordinate(414610.9923038645, 5317518.794050723) // 9; -
	};
	private static LineString LS = GEO_FACTORY.createLineString(C);
	private static LocationIndexedLine LIL = new LocationIndexedLine(LS);

	// Activate Test to see information about sample Linestring
	// @Test
	public void showLinestringInfo() {

		var lil = new LocationIndexedLine(LS);

		double sum = 0;
		for (int i = 1; i < 10; i++) {
			var ll1 = lil.project(C[i - 1]);
			var ll2 = lil.project(C[i]);
			var segment = lil.extractLine(ll1, ll2);
			var length = segment.getLength();
			sum += length;
			// log.info(ll1 + " > " + ll2 + " : " + length);
		}
		log.info("Sum: " + sum);
	}

	/**
	 * Test der Variante2. Bei dieser enthält LineString A den LineString B komplett.
	 */
	@Test
	public void testV2() throws KeineUeberschneidungException {

		// Arrange
		var A = LS;
		var B = (LineString) LIL.extractLine(new LinearLocation(4, 0.2),
			new LinearLocation(5, 0.7)); // (zwischen (C4 und C5) -> (zwischen (C5 und C6)

		// Act
		LineString beschreibung = (new KanteDublette(createKante(A), createKante(B)))
			.getZielnetzUeberschneidung();

		// Assert

		// B in A? -> erwartet im oben gewählten Ausschnitt
		LocationIndexedLine indexedLineA = new LocationIndexedLine(A);
		assertThat(
			beschreibung.buffer(0.001)
				.contains((indexedLineA.extractLine(new LinearLocation(4, 0.2), new LinearLocation(5, 0.7))))).isTrue();
	}

	/**
	 * Test der Variante3. Bei dieser überlappen LineString A den LineString B am Anfang bzw. Ende.
	 */
	@Test
	public void testV3() throws KeineUeberschneidungException {

		// Arrange
		var A = (LineString) LIL
			.extractLine(LIL.getStartIndex(), new LinearLocation(5, 0.7)); // (C0) -> (zwischen (C5 und C6)
		var B = (LineString) LIL
			.extractLine(new LinearLocation(4, 0.2), LIL.getEndIndex()); // (zwischen (C4 und C5) -> (C9)

		// Act
		LineString beschreibung = (new KanteDublette(createKante(A), createKante(B)))
			.getZielnetzUeberschneidung();

		// Assert
		LocationIndexedLine indexedLineA = new LocationIndexedLine(A);

		assertThat(beschreibung.buffer(0.001)
			.contains(indexedLineA.extractLine(new LinearLocation(4, 0.2), indexedLineA.getEndIndex()))).isTrue();
	}

	/**
	 * Test der Variante3b. Bei dieser ueberlappen die Strings ebenfalls. Der Linestring ist jedoch sehr viel einfacher
	 * als bei Variante 3
	 */
	@Test
	public void testV3b() throws KeineUeberschneidungException {

		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(414000, 5316000),
			new Coordinate(414200, 5316000),
			new Coordinate(414400, 5316000)
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);

		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(414105, 5316000),
			new Coordinate(414305, 5316000),
			new Coordinate(414505, 5316000)
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		Coordinate[] coordinatesUeberschneidung = new Coordinate[] {
			new Coordinate(414105, 5316000),
			new Coordinate(414305, 5316000),
			new Coordinate(414400, 5316000)
		};
		LineString ueberschneidung = GEO_FACTORY.createLineString(coordinatesUeberschneidung);

		// Act
		LineString beschreibung = (new KanteDublette(createKante(A), createKante(B)))
			.getZielnetzUeberschneidung();

		// Assert
		assertThat(beschreibung.equals(ueberschneidung)).isTrue();
	}

	@Test
	public void testV5VonBranch() throws KeineUeberschneidungException {

		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(916287.5989122939, 6235697.245613056),
			new Coordinate(916406.7747932096, 6235576.850948493),
			new Coordinate(916415.5249089473, 6235566.343690654),
			new Coordinate(916408.9934244511, 6235559.171079013)
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);

		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(916231.0753935167, 6235754.347782079),
			new Coordinate(916406.7747932096, 6235576.850948493),
			new Coordinate(916415.5249089473, 6235566.343690654),
			new Coordinate(916415.7030376607, 6235566.150177472)
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		Coordinate[] coordinatesUeberschneidung = new Coordinate[] {
			new Coordinate(916287.5989122939, 6235697.245613056),
			new Coordinate(916406.7747932096, 6235576.850948493),
			new Coordinate(916415.5249089473, 6235566.343690654)
		};
		LineString ueberschneidung = GEO_FACTORY.createLineString(coordinatesUeberschneidung);

		// Act
		LineString beschreibung = (new KanteDublette(createKante(A), createKante(B))).getZielnetzUeberschneidung();

		// Assert
		assertThat(beschreibung.buffer(0.0001).contains(ueberschneidung)).isTrue();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKanteDublette_V1kompletteUeberschneidung_korrekteUeberschneidung()
		throws KeineUeberschneidungException {
		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(10.0, 20.0),
			new Coordinate(20.0, 10.0),
			new Coordinate(30.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(50.0, 20.0)
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);

		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(10.0, 20.0),
			new Coordinate(20.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(50.0, 20.0)
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		// Act
		KanteDublette kanteDublette = new KanteDublette(createKante(A), createKante(B));
		LineString ueberschneidungLineString = kanteDublette.getZielnetzUeberschneidung();

		// Richtiger LineString?
		LineString expectedLineString = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(10.0, 20.0),
			new Coordinate(20.0, 10.0),
			new Coordinate(30.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(50.0, 20.0)
		});
		assertThat(ueberschneidungLineString).isEqualTo(expectedLineString);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKanteDublette_V2Beinhaltung_korrekteUeberschneidung() throws KeineUeberschneidungException {
		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(10.0, 20.0),
			new Coordinate(20.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(50.0, 20.0)
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);

		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(20.0, 10.0),
			new Coordinate(30.0, 10.0)
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		// Act
		KanteDublette kanteDublette = new KanteDublette(createKante(A), createKante(B));
		LineString ueberschneidungLineString = kanteDublette.getZielnetzUeberschneidung();

		// Richtiger LineString?
		LineString expectedLineString = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(20.0, 10.0),
			new Coordinate(30.0, 10.0) });
		assertThat(ueberschneidungLineString).isEqualTo(expectedLineString);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKanteDublette_V3Reinlaufen_korrekteUeberschneidung() throws KeineUeberschneidungException {
		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(20.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(60.0, 10.0)
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);

		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(30.0, 10.0),
			new Coordinate(50.0, 10.0),
			new Coordinate(70.0, 10.0),
			new Coordinate(90.0, 10.0)
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		// Act
		KanteDublette kanteDublette = new KanteDublette(createKante(A), createKante(B));
		LineString ueberschneidungLineString = kanteDublette.getZielnetzUeberschneidung();

		LineString expectedLineString = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(30.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(60.0, 10.0) });
		assertThat(ueberschneidungLineString).isEqualTo(expectedLineString);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKanteDublette_V4GabelungGleicherStart_korrekteUeberschneidung()
		throws KeineUeberschneidungException {
		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(20.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(60.0, 20.0)
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);

		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(20.0, 10.0),
			new Coordinate(60.0, 10.0)
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		// Act
		KanteDublette kanteDublette = new KanteDublette(createKante(A), createKante(B));
		LineString ueberschneidungLineString = kanteDublette.getZielnetzUeberschneidung();

		LineString expectedLineString = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(20.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(40.53665631459995, 10.268328157299976)
		});
		assertThat(ueberschneidungLineString).isEqualTo(expectedLineString);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKanteDublette_V5GabelungUnterschiedlicherStart_korrekteUeberschneidung()
		throws KeineUeberschneidungException {
		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(20.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(60.0, 20.0)
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);

		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(0.0, 10.0),
			new Coordinate(80.0, 10.0)
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		// Act
		KanteDublette kanteDublette = new KanteDublette(createKante(A), createKante(B));
		LineString ueberschneidungLineString = kanteDublette.getZielnetzUeberschneidung();

		// Richtiger LineString?
		LineString expectedLineString = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(20.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(40.53665631459995, 10.268328157299976)
		});
		assertThat(ueberschneidungLineString).isEqualTo(expectedLineString);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKanteDublette_V6xForm_korrekteUeberschneidung() throws KeineUeberschneidungException {

		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(10.0, 20.0),
			new Coordinate(20.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(50.0, 20.0)
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);

		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(10.0, 0.0),
			new Coordinate(20.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(50.0, 0.0)
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		// Act
		KanteDublette kanteDublette = new KanteDublette(createKante(A), createKante(B));
		LineString ueberschneidungLineString = kanteDublette.getZielnetzUeberschneidung();

		// Richtiger LineString?
		LineString expectedLineString = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(20.0, 10.0),
			new Coordinate(40.0, 10.0),
			new Coordinate(40.0, 10.0)
		});
		assertThat(ueberschneidungLineString).isEqualTo(expectedLineString);
	}

	@Test
	public void testKanteDublette_V8KeineUeberschneidung_wirftKeineUeberschneidungsException() {
		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(10.0, 20.0),
			new Coordinate(20.0, 20.0),
			new Coordinate(30.0, 0.31),
			new Coordinate(40.0, 0.31)
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);
		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(0.0, 0.0),
			new Coordinate(20.0, 0.0),
			new Coordinate(30.0, 0.0),
			new Coordinate(50.0, 0.0)
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		// Act + Assert
		assertThatThrownBy(() -> {
			new KanteDublette(createKante(A), createKante(B));
		}).isInstanceOf(KeineUeberschneidungException.class);
	}

	@Test
	public void testKanteDublette_V9UeberschneidungInZweiPunkten_wirftKeineUeberschneidungsException() {
		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(0.0, 10.0),
			new Coordinate(20.0, 10.0),
			new Coordinate(50.0, 10.0)
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);
		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(10.0, 30.0),
			new Coordinate(10.0, 0.0),
			new Coordinate(30.0, 0.0),
			new Coordinate(30.0, 30.0)
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		// Act + Assert
		assertThatThrownBy(() -> {
			new KanteDublette(createKante(A), createKante(B));
		}).isInstanceOf(KeineUeberschneidungException.class);
	}

	@Test
	public void testKanteDublette_PunktUeberschneidung_wirftKeineUeberschneidungsException() {
		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(10.0, 0.0),
			new Coordinate(20.0, 0.0),
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);
		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(20.0, 0.0),
			new Coordinate(30.0, 0.0),
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		// Act + Assert
		assertThatThrownBy(() -> {
			new KanteDublette(createKante(A), createKante(B));
		}).isInstanceOf(KeineUeberschneidungException.class);
	}

	@Test
	public void testKanteDublette_UeberschneidungDurchUngenauigkeitBeimOSMMatching_wirftKeineUeberschneidungsException() {
		// Arrange
		Coordinate[] coordinatesA = new Coordinate[] {
			new Coordinate(10.0, 0.0),
			new Coordinate(20.0, 0.0),
		};
		LineString A = GEO_FACTORY.createLineString(coordinatesA);
		Coordinate[] coordinatesB = new Coordinate[] {
			new Coordinate(19.6, 0.0),
			new Coordinate(20.0, 0.0),
			new Coordinate(30.0, 0.0),
		};
		LineString B = GEO_FACTORY.createLineString(coordinatesB);

		// Act + Assert
		assertThatThrownBy(() -> {
			new KanteDublette(createKante(A), createKante(B));
		}).isInstanceOf(KeineUeberschneidungException.class);
	}

	@Test
	public void testKanteDublette_UeberschneidungDerGeometrienNurAnZweiPunkten_wirftKeineUeberschneidungsException() {
		LineString baseGeometry = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(10., 10.),
			new Coordinate(14., 10.),
			new Coordinate(20., 10.)
		});
		LineString nurAnZweiPunktenIntersection = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(10., 10.),
			new Coordinate(14., 20),
			new Coordinate(20., 10.)
		});

		assertThatThrownBy(() -> {
			new KanteDublette(createKante(baseGeometry), createKante(nurAnZweiPunktenIntersection));
		}).isInstanceOf(KeineUeberschneidungException.class)
			.hasMessage("Keine Überschneidung detektierbar");
	}

	private Kante createKante(LineString lineString) {
		Kante kante = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).geometry(lineString)
			.build();
		kante.setAufDlmAbgebildeteGeometry(lineString);
		return kante;
	}

}
