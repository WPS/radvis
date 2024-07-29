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

package de.wps.radvis.backend.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.matching.domain.entity.MatchingJobStatistik;
import de.wps.radvis.backend.matching.domain.exception.GeometryLaengeMismatchException;
import de.wps.radvis.backend.matching.domain.exception.GeometryZuWeitEntferntException;
import de.wps.radvis.backend.matching.domain.service.MatchingKorrekturService;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;

class MatchingKorrekturServiceTest {

	private MatchingKorrekturService service;
	@Mock
	private MatchingJobStatistik reporter;

	static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(),
		KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

	@BeforeEach
	void setUp() {
		service = new MatchingKorrekturService();
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void testCheckOsmGeometrieAufFehlerUndKorrigiere_OsmGeometrieZuLangRatioWirftException() {

		Coordinate[] originalKoordinaten = new Coordinate[] {
			new Coordinate(5, 5),
			new Coordinate(10, 5),
			new Coordinate(15, 10)
		};

		LineString originalGeometrie = geometryFactory.createLineString(originalKoordinaten);
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(originalGeometrie).id(1L).build();

		Coordinate[] osmKoordinaten = new Coordinate[] {
			new Coordinate(0, 5),
			new Coordinate(10, 5),
			new Coordinate(30, 10)
		};

		LineString osmGeometrie = geometryFactory.createLineString(osmKoordinaten);

		assertThatThrownBy(
			() -> service.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(), kante.getGeometry(),
				osmGeometrie, reporter))
					.isInstanceOf(GeometryLaengeMismatchException.class);
	}

	@Test
	public void testCheckOsmGeometrieAufFehlerUndKorrigiere_OsmGeometrieZuKurzRatioWirftException() {

		Coordinate[] originalKoordinaten = new Coordinate[] {
			new Coordinate(0, 5),
			new Coordinate(10, 5),
			new Coordinate(30, 10)
		};

		LineString originalGeometrie = geometryFactory.createLineString(originalKoordinaten);
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(originalGeometrie).id(1L).build();

		Coordinate[] osmKoordinaten = new Coordinate[] {
			new Coordinate(5, 5),
			new Coordinate(10, 5),
			new Coordinate(15, 10)
		};

		LineString osmGeometrie = geometryFactory.createLineString(osmKoordinaten);

		assertThatThrownBy(
			() -> service.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(), kante.getGeometry(),
				osmGeometrie, reporter))
					.isInstanceOf(GeometryLaengeMismatchException.class);
	}

	@Test
	public void testCheckOsmGeometrieAufFehlerUndKorrigiere_OsmGeometrieZuKurzFlatWirftException() {

		Coordinate[] originalKoordinaten = new Coordinate[] {
			new Coordinate(5, 5),
			new Coordinate(30, 5),
			new Coordinate(74, 10)
		};

		LineString originalGeometrie = geometryFactory.createLineString(originalKoordinaten);
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(originalGeometrie).id(1L).build();

		Coordinate[] osmKoordinaten = new Coordinate[] {
			new Coordinate(30, 5),
			new Coordinate(45, 5),
			new Coordinate(65, 10)
		};

		LineString osmGeometrie = geometryFactory.createLineString(osmKoordinaten);

		assertThatThrownBy(
			() -> service.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(), kante.getGeometry(),
				osmGeometrie, reporter))
					.isInstanceOf(GeometryLaengeMismatchException.class);
	}

	@Test
	public void testCheckOsmGeometrieAufFehlerUndKorrigiere_OsmGeometrieZuLangFlatWirftException() {

		Coordinate[] originalKoordinaten = new Coordinate[] {
			new Coordinate(30, 5),
			new Coordinate(45, 5),
			new Coordinate(65, 10)
		};

		LineString originalGeometrie = geometryFactory.createLineString(originalKoordinaten);
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(originalGeometrie).id(1L).build();

		Coordinate[] osmKoordinaten = new Coordinate[] {
			new Coordinate(5, 5),
			new Coordinate(30, 5),
			new Coordinate(74, 10)
		};

		LineString osmGeometrie = geometryFactory.createLineString(osmKoordinaten);

		assertThatThrownBy(
			() -> service.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(), kante.getGeometry(),
				osmGeometrie, reporter))
					.isInstanceOf(GeometryLaengeMismatchException.class);
	}

	@Test
	public void testCheckOsmGeometrieAufFehlerUndKorrigiere_OsmGeometrieZuWeitEntferntWirftException() {

		Coordinate[] originalKoordinaten = new Coordinate[] {
			new Coordinate(5, 5),
			new Coordinate(10, 5),
			new Coordinate(50, 8)
		};

		LineString originalGeometrie = geometryFactory.createLineString(originalKoordinaten);
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(originalGeometrie).id(1L).build();

		Coordinate[] osmKoordinaten = new Coordinate[] {
			new Coordinate(5, 30),
			new Coordinate(7.5, 40),
			new Coordinate(10, 25),
			new Coordinate(50, 28)
		};

		LineString osmGeometrie = geometryFactory.createLineString(osmKoordinaten);

		assertThatThrownBy(
			() -> service.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(), kante.getGeometry(),
				osmGeometrie, reporter))
					.isInstanceOf(GeometryZuWeitEntferntException.class);
	}

	@Test
	public void testCheckOsmGeometrieAufFehlerUndKorrigiere_OsmGeometrieLaenge0() {
		Coordinate[] originalKoordinaten = new Coordinate[] {
			new Coordinate(5, 5),
			new Coordinate(10, 5),
			new Coordinate(50, 8)
		};

		LineString originalGeometrie = geometryFactory.createLineString(originalKoordinaten);
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(originalGeometrie).id(1L).build();

		Coordinate[] osmKoordinaten = new Coordinate[] {
			new Coordinate(10, 5),
			new Coordinate(10, 5)
		};

		LineString osmGeometrie = geometryFactory.createLineString(osmKoordinaten);

		assertThatThrownBy(
			() -> service.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(), kante.getGeometry(),
				osmGeometrie, reporter))
					.isInstanceOf(GeometryLaengeMismatchException.class);
	}

	@Test
	public void testCheckOsmGeometrieAufFehlerUndKorrigiere_OsmGeometrieLaengeZuLangDurchLoop()
		throws GeometryZuWeitEntferntException, GeometryLaengeMismatchException {
		Coordinate[] originalKoordinaten = new Coordinate[] {
			new Coordinate(5, 5),
			new Coordinate(10, 5),
			new Coordinate(50, 8)
		};

		LineString originalGeometrie = geometryFactory.createLineString(originalKoordinaten);
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(originalGeometrie).id(1L).build();

		Coordinate[] osmKoordinaten = new Coordinate[] {
			new Coordinate(3, 5),
			new Coordinate(10, 6),
			new Coordinate(9, 13),
			new Coordinate(7, 23),
			new Coordinate(5, 12),
			new Coordinate(7, 23),
			new Coordinate(9, 13),
			new Coordinate(10, 6),
			new Coordinate(55, 8)
		};

		LineString osmGeometrie = geometryFactory.createLineString(osmKoordinaten);

		LineString korrigierteOsmGeometrie = service
			.checkMatchingGeometrieAufFehlerUndKorrigiere(kante.getId(), kante.getGeometry(), osmGeometrie, reporter);

		// Assert
		Coordinate[] expectedKoordinaten = new Coordinate[] {
			new Coordinate(3, 5),
			new Coordinate(10, 6),
			new Coordinate(55, 8)
		};
		assertThat(korrigierteOsmGeometrie.getCoordinates()).containsExactly(expectedKoordinaten);
	}
}
