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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;

class LinearReferenzierterAbschnittTest {

	private static final Comparator<LinearReferenzierterAbschnitt> lineareReferenzComparatorMitToleranz = (LR1,
		LR2) -> {
		if (Math.abs(LR1.getVonValue() - LR2.getVonValue()) + Math.abs(LR1.getBisValue() - LR2.getBisValue()) < 0.002) {
			return 0;
		} else {
			return LinearReferenzierterAbschnitt.vonZuerst.compare(LR1, LR2);
		}
	};

	@Test
	void testSegmentsCoverFullLine() {
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt1 = LinearReferenzierterAbschnitt.of(0.0, 0.5);
		ZustaendigkeitAttribute zustaendigkeitAttribute1 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt1,
			null, null, null, VereinbarungsKennung.of("1"));
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt2 = LinearReferenzierterAbschnitt.of(0.5, 1);
		ZustaendigkeitAttribute zustaendigkeitAttribute2 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt2,
			null, null, null, VereinbarungsKennung.of("1"));

		List<ZustaendigkeitAttribute> listWithAttributeSegments = Arrays
			.asList(zustaendigkeitAttribute1, zustaendigkeitAttribute2);

		boolean covers = LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			listWithAttributeSegments.stream()
				.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));

		assertThat(covers).isTrue();
	}

	@Test
	void testSegmentsCoverFullLine_round() {
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt1 = LinearReferenzierterAbschnitt.of(0.00000475,
			0.5);
		ZustaendigkeitAttribute zustaendigkeitAttribute1 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt1,
			null, null, null, VereinbarungsKennung.of("1"));
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt2 = LinearReferenzierterAbschnitt.of(0.5, 1.0000123);
		ZustaendigkeitAttribute zustaendigkeitAttribute2 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt2,
			null, null, null, VereinbarungsKennung.of("1"));

		List<ZustaendigkeitAttribute> listWithAttributeSegments = Arrays
			.asList(zustaendigkeitAttribute1, zustaendigkeitAttribute2);

		boolean covers = LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			listWithAttributeSegments.stream()
				.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));

		assertThat(covers).isTrue();
	}

	@Test
	void testSegmentsCoverFullLine_unsorted() {
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt1 = LinearReferenzierterAbschnitt.of(0.0, 0.5);
		ZustaendigkeitAttribute geschwindigkeitAttribute1 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt1,
			null, null, null, VereinbarungsKennung.of("1"));
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt2 = LinearReferenzierterAbschnitt.of(0.5, 1);
		ZustaendigkeitAttribute geschwindigkeitAttribute2 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt2,
			null, null, null, VereinbarungsKennung.of("1"));

		List<ZustaendigkeitAttribute> listWithAttributeSegments = Arrays
			.asList(geschwindigkeitAttribute2, geschwindigkeitAttribute1);

		boolean covers = LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			listWithAttributeSegments.stream()
				.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));

		assertThat(covers).isTrue();
	}

	@Test
	void testSegmentsCoverFullLine_overlap() {
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt1 = LinearReferenzierterAbschnitt.of(0.0, 0.5);
		ZustaendigkeitAttribute geschwindigkeitAttribute1 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt1,
			null, null, null, VereinbarungsKennung.of("1"));
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt2 = LinearReferenzierterAbschnitt.of(0.4, 1);
		ZustaendigkeitAttribute geschwindigkeitAttribute2 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt2,
			null, null, null, VereinbarungsKennung.of("1"));

		List<ZustaendigkeitAttribute> listWithAttributeSegments = Arrays
			.asList(geschwindigkeitAttribute2, geschwindigkeitAttribute1);

		boolean covers = LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			listWithAttributeSegments.stream()
				.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));

		assertThat(covers).isFalse();
	}

	@Test
	void testSegmentsCoverFullLine_hasLuecke() {
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt1 = LinearReferenzierterAbschnitt.of(0.0, 0.5);
		ZustaendigkeitAttribute geschwindigkeitAttribute1 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt1,
			null, null, null, VereinbarungsKennung.of("1"));
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt2 = LinearReferenzierterAbschnitt.of(0.7, 1);
		ZustaendigkeitAttribute geschwindigkeitAttribute2 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt2,
			null, null, null, VereinbarungsKennung.of("1"));

		List<ZustaendigkeitAttribute> listWithAttributeSegments = Arrays
			.asList(geschwindigkeitAttribute1, geschwindigkeitAttribute2);

		boolean covers = LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			listWithAttributeSegments.stream()
				.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));

		assertThat(covers).isFalse();
	}

	@Test
	void testSegmentsCoverFullLine_missingStart() {
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt1 = LinearReferenzierterAbschnitt.of(0.1, 0.5);
		ZustaendigkeitAttribute geschwindigkeitAttribute1 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt1,
			null, null, null, VereinbarungsKennung.of("1"));
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt2 = LinearReferenzierterAbschnitt.of(0.5, 1);
		ZustaendigkeitAttribute geschwindigkeitAttribute2 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt2,
			null, null, null, VereinbarungsKennung.of("1"));

		List<ZustaendigkeitAttribute> listWithAttributeSegments = Arrays
			.asList(geschwindigkeitAttribute1, geschwindigkeitAttribute2);

		boolean covers = LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			listWithAttributeSegments.stream()
				.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));

		assertThat(covers).isFalse();
	}

	@Test
	void testSegmentsCoverFullLine_missingEnd() {
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt1 = LinearReferenzierterAbschnitt.of(0.0, 0.5);
		ZustaendigkeitAttribute geschwindigkeitAttribute1 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt1,
			null, null, null, VereinbarungsKennung.of("1"));
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt2 = LinearReferenzierterAbschnitt.of(0.5, 0.9);
		ZustaendigkeitAttribute geschwindigkeitAttribute2 = new ZustaendigkeitAttribute(linearReferenzierterAbschnitt2,
			null, null, null, VereinbarungsKennung.of("1"));

		List<ZustaendigkeitAttribute> listWithAttributeSegments = Arrays
			.asList(geschwindigkeitAttribute1, geschwindigkeitAttribute2);

		boolean covers = LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			listWithAttributeSegments.stream()
				.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));

		assertThat(covers).isFalse();
	}

	@Test
	void testSegmentsCoverFullLine_noSegment() {
		List<ZustaendigkeitAttribute> listWithAttributeSegments = Collections.emptyList();

		boolean covers = LinearReferenzierterAbschnitt.segmentsCoverFullLine(
			listWithAttributeSegments.stream()
				.map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));

		assertThat(covers).isFalse();
	}

	@Test
	void intersects_TeilweiseUeberschneidung() {
		LinearReferenzierterAbschnitt LR1 = LinearReferenzierterAbschnitt.of(.1, 0.4);
		LinearReferenzierterAbschnitt LR2 = LinearReferenzierterAbschnitt.of(0.3, 0.8);

		assertThat(LR1.intersects(LR2)).isTrue();
		assertThat(LR2.intersects(LR1)).isTrue();
	}

	@Test
	void intersects_Beinhaltung() {
		LinearReferenzierterAbschnitt LR1 = LinearReferenzierterAbschnitt.of(.1, 0.8);
		LinearReferenzierterAbschnitt LR2 = LinearReferenzierterAbschnitt.of(0.3, 0.6);

		assertThat(LR1.intersects(LR2)).isTrue();
		assertThat(LR2.intersects(LR1)).isTrue();
	}

	@Test
	void intersects_UeberschneidetSichSelbst() {
		LinearReferenzierterAbschnitt LR1 = LinearReferenzierterAbschnitt.of(.1, 0.8);
		assertThat(LR1.intersects(LR1)).isTrue();

	}

	@Test
	void intersects_KeineUeberschneidung() {
		LinearReferenzierterAbschnitt LR1 = LinearReferenzierterAbschnitt.of(.1, 0.4);
		LinearReferenzierterAbschnitt LR2 = LinearReferenzierterAbschnitt.of(0.4001, 0.8);

		assertThat(LR1.intersects(LR2)).isFalse();
		assertThat(LR2.intersects(LR1)).isFalse();
	}

	@Test
	void getIntersection_TeilweiseUeberschneidung() {
		LinearReferenzierterAbschnitt LR1 = LinearReferenzierterAbschnitt.of(.1, 0.4);
		LinearReferenzierterAbschnitt LR2 = LinearReferenzierterAbschnitt.of(0.3, 0.8);

		assertThat(LR1.intersection(LR2)).isPresent();
		assertThat(LR1.intersection(LR2).get()).usingComparator(lineareReferenzComparatorMitToleranz)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.3, 0.4));
		assertThat(LR2.intersection(LR1)).isPresent();
		assertThat(LR2.intersection(LR1).get()).usingComparator(lineareReferenzComparatorMitToleranz)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.3, 0.4));
	}

	@Test
	void getIntersection_Beinhaltung() {
		LinearReferenzierterAbschnitt LR1 = LinearReferenzierterAbschnitt.of(.1, 0.8);
		LinearReferenzierterAbschnitt LR2 = LinearReferenzierterAbschnitt.of(0.3, 0.6);

		assertThat(LR1.intersection(LR2)).isPresent();
		assertThat(LR1.intersection(LR2).get()).usingComparator(lineareReferenzComparatorMitToleranz)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.3, 0.6));
		assertThat(LR2.intersection(LR1)).isPresent();
		assertThat(LR2.intersection(LR1).get()).usingComparator(lineareReferenzComparatorMitToleranz)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.3, 0.6));
	}

	@Test
	void getIntersection_UeberschneidetSichSelbst() {
		LinearReferenzierterAbschnitt LR1 = LinearReferenzierterAbschnitt.of(.1, 0.8);
		assertThat(LR1.intersection(LR1)).isPresent();
		assertThat(LR1.intersection(LR1).get()).usingComparator(lineareReferenzComparatorMitToleranz)
			.isEqualTo(LR1);
	}

	@Test
	void getIntersection_KeineUeberschneidung() {
		LinearReferenzierterAbschnitt LR1 = LinearReferenzierterAbschnitt.of(.1, 0.4);
		LinearReferenzierterAbschnitt LR2 = LinearReferenzierterAbschnitt.of(0.4001, 0.8);

		assertThat(LR1.intersection(LR2)).isEmpty();
		assertThat(LR2.intersection(LR1)).isEmpty();
	}

	@Test
	void testeFuerUmgedrehteStrecke() {
		LinearReferenzierterAbschnitt LR1 = LinearReferenzierterAbschnitt.of(0, 0.4);
		LinearReferenzierterAbschnitt LR2 = LinearReferenzierterAbschnitt.of(0.2, 0.7);
		LinearReferenzierterAbschnitt LR3 = LinearReferenzierterAbschnitt.of(0, 1);

		assertThat(LR1.fuerUmgedrehteStrecke()).isEqualTo(LinearReferenzierterAbschnitt.of(0.6, 1));
		assertThat(LR2.fuerUmgedrehteStrecke()).usingComparator(LineareReferenzTestProvider.lenientComparator)
			.isEqualTo(LinearReferenzierterAbschnitt.of(0.3, 0.8));
		assertThat(LR3.fuerUmgedrehteStrecke()).isEqualTo(LR3);
	}

	@Test
	void testeOfFuerLinestrings_liegenUebereinander_punktueberschneidung() {
		LineString base = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(200, 100));

		LineString orthogonaleSchneidend = GeometryTestdataProvider.createLineString(new Coordinate(100, 50),
			new Coordinate(100, 150));

		assertThatThrownBy(() -> LinearReferenzierterAbschnitt.of(base, orthogonaleSchneidend)).isInstanceOf(
			RequireViolation.class)
			.hasMessage("Lineare Referenz darf nicht Punktförmig sein");
	}

	@Test
	void testeOfFuerLinesrings__liegenUebereinander_teilweiseUeberlappung() {
		LineString base = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(200, 100));

		LineString teilweiseUeberlappung = GeometryTestdataProvider.createLineString(new Coordinate(150, 100),
			new Coordinate(250, 100));

		assertThat(LinearReferenzierterAbschnitt.of(base, teilweiseUeberlappung)).usingComparator(
			LineareReferenzTestProvider.lenientComparator).isEqualTo(LinearReferenzierterAbschnitt.of(0.5, 1.));
	}

	@Test
	void testeOfFuerLinesrings_liegenUebereinander_vollstaendigeUeberlappung() {
		LineString base = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(200, 100));

		LineString vollstaendigeUeberlappung = GeometryTestdataProvider.createLineString(new Coordinate(80, 100),
			new Coordinate(220, 100));

		assertThat(LinearReferenzierterAbschnitt.of(base, vollstaendigeUeberlappung)).usingComparator(
			LineareReferenzTestProvider.lenientComparator).isEqualTo(LinearReferenzierterAbschnitt.of(0, 1.));
	}

	@Test
	void testeOfFuerLinesrings_liegenUebereinander_enthalten() {
		LineString base = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(200, 100));

		LineString enthalten = GeometryTestdataProvider.createLineString(new Coordinate(120, 100),
			new Coordinate(180, 100));

		assertThat(LinearReferenzierterAbschnitt.of(base, enthalten)).usingComparator(
			LineareReferenzTestProvider.lenientComparator).isEqualTo(LinearReferenzierterAbschnitt.of(0.2, 0.8));
	}

	@Test
	void testeOfFuerLinesrings_liegenEntfernt_VoelligVerschobenGibtException() {
		LineString base = GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
			new Coordinate(200, 100));

		LineString ganzWoanders = GeometryTestdataProvider.createLineString(new Coordinate(220, 150),
			new Coordinate(270, 150), new Coordinate(230, 170));

		assertThatThrownBy(() -> LinearReferenzierterAbschnitt.of(base, ganzWoanders)).isInstanceOf(
			RequireViolation.class)
			.hasMessage("Lineare Referenz darf nicht Punktförmig sein");
	}

	@Test
	void testeOfFuerLinesrings_liegenEntfernt_ProjiziertKompliziertesBeispielRichtig() {
		LineString base = GeometryTestdataProvider.createLineString(
			new Coordinate(100, 100), new Coordinate(120, 105), new Coordinate(140, 103),
			new Coordinate(160, 95), new Coordinate(180, 90), new Coordinate(200, 100));

		LineString entferntAberRealistisch = GeometryTestdataProvider.createLineString(new Coordinate(119, 113),
			new Coordinate(130, 119), new Coordinate(170, 107));

		assertThat(LinearReferenzierterAbschnitt.of(base, entferntAberRealistisch)).usingComparator(
			LineareReferenzTestProvider.lenientComparator).isEqualTo(LinearReferenzierterAbschnitt.of(0.2, 0.7));
	}

	@Test
	void snappeAufEndpunkte_beideSnapped() {
		// Arrange & Act
		LinearReferenzierterAbschnitt beidePunkteSnapped = LinearReferenzierterAbschnitt
			.snappeAufEndpunkte(LinearReferenzierterAbschnitt.of(0.09, 0.91), 10.0, 1.0);

		// Assert
		assertThat(beidePunkteSnapped).isEqualTo(LinearReferenzierterAbschnitt.of(0, 1));
	}

	@Test
	void snappeAufEndpunkte_vonSnapped() {
		// Arrange & Act
		LinearReferenzierterAbschnitt beidePunkteSnapped = LinearReferenzierterAbschnitt
			.snappeAufEndpunkte(LinearReferenzierterAbschnitt.of(0.09, 0.9), 10.0, 1.0);

		// Assert
		assertThat(beidePunkteSnapped).isEqualTo(LinearReferenzierterAbschnitt.of(0, 0.9));
	}

	@Test
	void snappeAufEndpunkte_bisSnapped() {
		// Arrange & Act
		LinearReferenzierterAbschnitt beidePunkteSnapped = LinearReferenzierterAbschnitt
			.snappeAufEndpunkte(LinearReferenzierterAbschnitt.of(0.1, 0.91), 10.0, 1.0);

		// Assert
		assertThat(beidePunkteSnapped).isEqualTo(LinearReferenzierterAbschnitt.of(0.1, 1));
	}

	@Test
	void snappeAufEndpunkte_keinerSnapped() {
		// Arrange & Act
		LinearReferenzierterAbschnitt beidePunkteSnapped = LinearReferenzierterAbschnitt
			.snappeAufEndpunkte(LinearReferenzierterAbschnitt.of(0.09, 0.91), 10.0, 0.85);

		// Assert
		assertThat(beidePunkteSnapped).isEqualTo(LinearReferenzierterAbschnitt.of(0.09, 0.91));
	}
}
