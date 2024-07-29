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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.GeometryTestdataProvider;

public class HaendigkeitTest {

	LineString vertikaleKante;
	LineString horizontaleKante;
	LineString schraegeKante;

	@BeforeEach
	void setUp() {
		vertikaleKante = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 10),
			new Coordinate(10, 15),
			new Coordinate(10, 20));

		horizontaleKante = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 10),
			new Coordinate(15, 10),
			new Coordinate(20, 10));

		schraegeKante = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 10),
			new Coordinate(15, 15),
			new Coordinate(20, 20));
	}

	@Test
	void testeOf_IstUnbestimmtFuerGleicheGeometrien() {

		assertThat(Haendigkeit.of(vertikaleKante, vertikaleKante).wahrscheinlichkeit)
			.isEqualTo(0.);
		assertThat(Haendigkeit.of(vertikaleKante, vertikaleKante).orientierung)
			.isEqualTo(
				Haendigkeit.Orientierung.UNBESTIMMT);
		assertThat(Haendigkeit.of(horizontaleKante, horizontaleKante).wahrscheinlichkeit)
			.isEqualTo(0.);
		assertThat(Haendigkeit.of(horizontaleKante, horizontaleKante).orientierung)
			.isEqualTo(
				Haendigkeit.Orientierung.UNBESTIMMT);
		assertThat(Haendigkeit.of(schraegeKante, schraegeKante).wahrscheinlichkeit)
			.isEqualTo(0.);
		assertThat(Haendigkeit.of(schraegeKante, schraegeKante).orientierung)
			.isEqualTo(
				Haendigkeit.Orientierung.UNBESTIMMT);
	}

	@Test
	void testeOf_LinksIstLinksUndRechtsIstRechts() {

		assertThat(Haendigkeit
			.of(vertikaleKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, 5, 0)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.LINKS);
		assertThat(Haendigkeit
			.of(vertikaleKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, -5, 0)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.RECHTS);

		assertThat(Haendigkeit
			.of(schraegeKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 5, 0)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.LINKS);
		assertThat(Haendigkeit
			.of(schraegeKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, -5, 0)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.RECHTS);

		assertThat(Haendigkeit
			.of(schraegeKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 0, -5)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.LINKS);
		assertThat(Haendigkeit
			.of(schraegeKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 0, 5)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.RECHTS);

		assertThat(Haendigkeit
			.of(horizontaleKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, -5)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.LINKS);
		assertThat(Haendigkeit
			.of(horizontaleKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, 5)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.RECHTS);
	}

	@Test
	void testeOf_UnabhaendigVonVonKanteStationierungsrichtung() {

		assertThat(Haendigkeit
			.of(vertikaleKante.reverse(),
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, 5, 0)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.LINKS);
		assertThat(Haendigkeit
			.of(vertikaleKante.reverse(),
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, -5, 0)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.RECHTS);

		assertThat(Haendigkeit
			.of(schraegeKante.reverse(),
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 5, 0)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.LINKS);
		assertThat(Haendigkeit
			.of(schraegeKante.reverse(),
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, -5, 0)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.RECHTS);

		assertThat(Haendigkeit
			.of(schraegeKante.reverse(),
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 0, -5)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.LINKS);
		assertThat(Haendigkeit
			.of(schraegeKante.reverse(),
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante, 0, 5)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.RECHTS);

		assertThat(Haendigkeit
			.of(horizontaleKante.reverse(),
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, -5)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.LINKS);
		assertThat(Haendigkeit
			.of(horizontaleKante.reverse(),
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, 5)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.RECHTS);
	}

	@Test
	void testeOf_OrientierungReversedWennZuKanteStationierungsrichtungReversed() {

		assertThat(Haendigkeit
			.of(vertikaleKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante.reverse(), 5,
					0)).orientierung)
						.isEqualTo(
							Haendigkeit.Orientierung.RECHTS);
		assertThat(Haendigkeit
			.of(vertikaleKante.reverse(),
				GeometryTestdataProvider
					.getLinestringVerschobenUmCoordinate(vertikaleKante.reverse(), -5, 0)).orientierung)
						.isEqualTo(
							Haendigkeit.Orientierung.LINKS);

		assertThat(Haendigkeit
			.of(schraegeKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante.reverse(), 5,
					0)).orientierung)
						.isEqualTo(
							Haendigkeit.Orientierung.RECHTS);
		assertThat(Haendigkeit
			.of(schraegeKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante.reverse(), -5,
					0)).orientierung)
						.isEqualTo(
							Haendigkeit.Orientierung.LINKS);

		assertThat(Haendigkeit
			.of(schraegeKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante.reverse(), 0,
					-5)).orientierung)
						.isEqualTo(
							Haendigkeit.Orientierung.RECHTS);
		assertThat(Haendigkeit
			.of(schraegeKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(schraegeKante.reverse(), 0,
					5)).orientierung)
						.isEqualTo(
							Haendigkeit.Orientierung.LINKS);

		assertThat(Haendigkeit
			.of(horizontaleKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, -5)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.LINKS);
		assertThat(Haendigkeit
			.of(horizontaleKante,
				GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(horizontaleKante, 0, 5)).orientierung)
					.isEqualTo(
						Haendigkeit.Orientierung.RECHTS);
	}

	@Test
	void testeOf_komplizierteresBeispiel() {
		LineString komplizierteKante = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 10),
			new Coordinate(12, 12),
			new Coordinate(8, 14),
			new Coordinate(11, 18),
			new Coordinate(12, 22));

		assertThat(Haendigkeit.of(komplizierteKante, vertikaleKante).orientierung)
			.isEqualTo(
				Haendigkeit.Orientierung.RECHTS);
		assertThat(Haendigkeit.of(komplizierteKante, vertikaleKante).wahrscheinlichkeit)
			.isLessThan(0.3);
		assertThat(Haendigkeit.of(komplizierteKante,
			GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, -5, 0)).wahrscheinlichkeit)
				.isGreaterThan(0.6);
	}

	@Test
	void testeOf_robustAufOutlier() {
		LineString komplizierteKante = GeometryTestdataProvider.createLineString(
			new Coordinate(10, 10),
			new Coordinate(12, 12),
			new Coordinate(2, 14),
			new Coordinate(11, 18),
			new Coordinate(12, 22));

		assertThat(Haendigkeit.of(komplizierteKante, vertikaleKante).orientierung)
			.isEqualTo(
				Haendigkeit.Orientierung.RECHTS);
		assertThat(Haendigkeit.of(komplizierteKante, vertikaleKante).wahrscheinlichkeit)
			.isLessThan(0.3);
		assertThat(Haendigkeit.of(komplizierteKante,
			GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(vertikaleKante, -6, 0)).wahrscheinlichkeit)
				.isGreaterThan(0.5);
	}

	@Test
	void testeOrderingLinksNachRechts() {
		ArrayList<Haendigkeit> haendigkeiten = new ArrayList<>();
		haendigkeiten.add(Haendigkeit.of(-0.8));
		haendigkeiten.add(Haendigkeit.of(0.4));
		haendigkeiten.add(Haendigkeit.of(-0.2));
		haendigkeiten.add(Haendigkeit.of(0.8));
		haendigkeiten.add(Haendigkeit.of(0.1));
		haendigkeiten.add(Haendigkeit.of(-0.05));
		haendigkeiten.add(Haendigkeit.of(-0.9));
		haendigkeiten.add(Haendigkeit.of(0.2));

		haendigkeiten.sort(Haendigkeit.vonLinksNachRechts);

		assertThat(haendigkeiten).extracting(Haendigkeit::getOrientierung)
			.containsExactly(Haendigkeit.Orientierung.LINKS, Haendigkeit.Orientierung.LINKS,
				Haendigkeit.Orientierung.LINKS, Haendigkeit.Orientierung.LINKS, Haendigkeit.Orientierung.RECHTS,
				Haendigkeit.Orientierung.RECHTS, Haendigkeit.Orientierung.RECHTS, Haendigkeit.Orientierung.RECHTS);

		assertThat(haendigkeiten).extracting(Haendigkeit::getWahrscheinlichkeit)
			.containsExactly(
				0.8, 0.4, 0.2, 0.1, // links
				0.05, 0.2, 0.8, 0.9 // rechts
			);
	}
}
