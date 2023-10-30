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

package de.wps.radvis.backend.integration.grundnetzReimport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.KnotenTupel;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.TopologischesUpdate;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.StartUndEndpunktGleichException;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;

public class FindKnotenFromIndexServiceTest {

	private FindKnotenFromIndexService findKnotenFromIndexService;
	GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		findKnotenFromIndexService = new FindKnotenFromIndexService();
	}

	@Test
	void testeFindOrCreateKnotenTupel_PunkteGleich_throwsException() {
		// arrange
		Point a = geometryFactory.createPoint(new Coordinate(0, 0));
		Point b = geometryFactory.createPoint(new Coordinate(0, 0));

		// act & assert
		assertThatThrownBy(() -> this.findKnotenFromIndexService.findOrCreateKnotenTupel(a, b, new KnotenIndex()))
			.isInstanceOf(StartUndEndpunktGleichException.class);
	}

	@Test
	void testeFindOrCreateKnotenTupel_knotenNichtImIndex_legtNeueKnotenAnUndPacktInIndex() throws Exception {
		// arrange
		Point a = geometryFactory.createPoint(new Coordinate(10, 10));
		Point b = geometryFactory.createPoint(new Coordinate(20, 10));

		KnotenIndex knotenIndex = new KnotenIndex();
		// act
		KnotenTupel result = this.findKnotenFromIndexService.findOrCreateKnotenTupel(a, b, knotenIndex);

		assertThat(result.vonKnoten.getPoint().equals(a)).isTrue();
		assertThat(result.nachKnoten.getPoint().equals(b)).isTrue();
		assertThat(knotenIndex.finde(a)).contains(result.vonKnoten);
		assertThat(knotenIndex.finde(b)).contains(result.nachKnoten);
		assertThat(knotenIndex.size()).isEqualTo(2);
	}

	@Test
	void testeFindOrCreateKnotenTupel_VonUndNachKnotenImIndex_legtKeineNeueKnotenAn() throws Exception {
		// arrange
		Knoten a = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build();
		Knoten b = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();

		KnotenIndex knotenIndex = new KnotenIndex();
		knotenIndex.fuegeEin(a);
		knotenIndex.fuegeEin(b);

		Point nA = geometryFactory.createPoint(new Coordinate(10.9, 10));
		Point nB = geometryFactory.createPoint(new Coordinate(19.1, 10));

		// act
		KnotenTupel result = this.findKnotenFromIndexService.findOrCreateKnotenTupel(nA, nB, knotenIndex);

		assertThat(result.vonKnoten.getPoint().equals(a.getPoint())).isTrue();
		assertThat(result.nachKnoten.getPoint().equals(b.getPoint())).isTrue();
		assertThat(knotenIndex.finde(nA)).contains(result.vonKnoten);
		assertThat(knotenIndex.finde(nB)).contains(result.nachKnoten);
		assertThat(knotenIndex.size()).isEqualTo(2);
	}

	@Test
	void testeFindOrCreateKnotenTupel_SehrNaheKnotenUndVonNaeherAnKnotenAusIndex_legtEinenNeuenNachKnotenAn()
		throws Exception {
		// arrange
		Knoten a = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build();

		KnotenIndex knotenIndex = new KnotenIndex();
		knotenIndex.fuegeEin(a);

		Point nA = geometryFactory.createPoint(new Coordinate(9, 10));
		Point nB = geometryFactory.createPoint(new Coordinate(11.9, 10));

		// act
		KnotenTupel result = this.findKnotenFromIndexService.findOrCreateKnotenTupel(nA, nB, knotenIndex);

		assertThat(result.vonKnoten.getPoint().equals(a.getPoint())).isTrue();
		assertThat(result.nachKnoten.getPoint().equals(nB)).isTrue();
		assertThat(knotenIndex.finde(nA)).contains(result.vonKnoten);
		assertThat(knotenIndex.finde(nB)).contains(result.nachKnoten);
		assertThat(knotenIndex.size()).isEqualTo(2);
	}

	@Test
	void testeFindOrCreateKnotenTupel_SehrNaheKnotenUndNachNaeherAnKnotenAusIndex_legtEinenNeuenVonKnotenAn()
		throws Exception {
		// arrange
		Knoten a = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build();

		KnotenIndex knotenIndex = new KnotenIndex();
		knotenIndex.fuegeEin(a);

		Point nA = geometryFactory.createPoint(new Coordinate(8.1, 10));
		Point nB = geometryFactory.createPoint(new Coordinate(11, 10));

		// act
		KnotenTupel result = this.findKnotenFromIndexService.findOrCreateKnotenTupel(nA, nB, knotenIndex);

		assertThat(result.vonKnoten.getPoint().equals(nA)).isTrue();
		assertThat(result.nachKnoten.getPoint().equals(a.getPoint())).isTrue();
		assertThat(knotenIndex.finde(nA)).contains(result.vonKnoten);
		assertThat(knotenIndex.finde(nB)).contains(result.nachKnoten);
		assertThat(knotenIndex.size()).isEqualTo(2);
	}

	@Test
	void testeFindOrCreateKnotenTupelMitKnotenGefixt_DerSelbeKnotenFuerBeideGefunden_legtTrotzdemEinenNeuenKnotenAn()
		throws StartUndEndpunktGleichException {
		// arrange
		Knoten a = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build();

		KnotenIndex knotenIndex = new KnotenIndex();
		knotenIndex.fuegeEin(a);

		Point nB = geometryFactory.createPoint(new Coordinate(11, 10));

		// act
		KnotenTupel result = this.findKnotenFromIndexService
			.findOrCreateKnotenTupelMitKnotenGefixt(nB, a, knotenIndex, false);

		assertThat(result.vonKnoten).isEqualTo(a);
		assertThat(result.nachKnoten.getPoint().equals(nB)).isTrue();
		assertThat(knotenIndex.finde(a.getPoint())).contains(result.vonKnoten);
		assertThat(knotenIndex.finde(nB)).contains(result.nachKnoten);
		assertThat(knotenIndex.size()).isEqualTo(2);
	}

	@Test
	void testeFindOrCreateKnotenTupelMitKnotenGefixt_KeinKnotenGefunden_LegtNeuenKnotenAn()
		throws StartUndEndpunktGleichException {
		// arrange
		Knoten a = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build();

		KnotenIndex knotenIndex = new KnotenIndex();
		knotenIndex.fuegeEin(a);

		Point nB = geometryFactory.createPoint(new Coordinate(20, 10));

		// act
		KnotenTupel result = this.findKnotenFromIndexService
			.findOrCreateKnotenTupelMitKnotenGefixt(nB, a, knotenIndex, false);

		assertThat(result.vonKnoten).isEqualTo(a);
		assertThat(result.nachKnoten.getPoint().equals(nB)).isTrue();
		assertThat(knotenIndex.finde(a.getPoint())).contains(result.vonKnoten);
		assertThat(knotenIndex.finde(nB)).contains(result.nachKnoten);
		assertThat(knotenIndex.size()).isEqualTo(2);
	}

	@Test
	void testeFindOrCreateKnotenTupelMitKnotenGefixt_KnotenGefunden_VerwendetGefundenenKnoten()
		throws StartUndEndpunktGleichException {
		// arrange
		Knoten a = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build();
		Knoten b = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();

		KnotenIndex knotenIndex = new KnotenIndex();
		knotenIndex.fuegeEin(a);
		knotenIndex.fuegeEin(b);

		Point nB = geometryFactory.createPoint(new Coordinate(20, 10));

		// act
		KnotenTupel result = this.findKnotenFromIndexService
			.findOrCreateKnotenTupelMitKnotenGefixt(nB, a, knotenIndex, false);

		assertThat(result.vonKnoten).isEqualTo(a);
		assertThat(result.nachKnoten).isEqualTo(b);
		assertThat(knotenIndex.finde(a.getPoint())).contains(result.vonKnoten);
		assertThat(knotenIndex.finde(nB)).contains(result.nachKnoten);
		assertThat(knotenIndex.finde(b.getPoint())).contains(result.nachKnoten);
		assertThat(knotenIndex.size()).isEqualTo(2);
	}

	@Test
	void testeFindOrCreateKnotenTupelMitKnotenGefixt_istNeuerKnotenVonKnotenTrueUndKnotenGefunden_VerwendetGefundenenKnoten()
		throws StartUndEndpunktGleichException {
		// arrange
		Knoten a = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 10), QuellSystem.DLM).build();
		Knoten b = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 10), QuellSystem.DLM).build();

		KnotenIndex knotenIndex = new KnotenIndex();
		knotenIndex.fuegeEin(a);
		knotenIndex.fuegeEin(b);

		Point nB = geometryFactory.createPoint(new Coordinate(20, 10));

		// act
		KnotenTupel result = this.findKnotenFromIndexService
			.findOrCreateKnotenTupelMitKnotenGefixt(nB, a, knotenIndex, true);

		assertThat(result.nachKnoten).isEqualTo(a);
		assertThat(result.vonKnoten).isEqualTo(b);
		assertThat(knotenIndex.finde(a.getPoint())).contains(result.nachKnoten);
		assertThat(knotenIndex.finde(nB)).contains(result.vonKnoten);
		assertThat(knotenIndex.finde(b.getPoint())).contains(result.vonKnoten);
		assertThat(knotenIndex.size()).isEqualTo(2);
	}

	@Test
	void getNeuenKnotenTupelFuerUpdate_BeideKnotenNeu_legtZweiNeueKnotenAnUndImIndexAb()
		throws Exception {
		// arrange
		KnotenIndex knotenIndex = new KnotenIndex();

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 30, 10, QuellSystem.DLM)
			.build();

		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(10, 12.1), new Coordinate(20, 12.1), new Coordinate(30, 12.1) });

		// act
		KnotenTupel knotenTupel = this.findKnotenFromIndexService
			.getNeuenKnotenTupelFuerUpdate(new TopologischesUpdate(kante, lineStringFeature.getStartPoint(),
				lineStringFeature.getEndPoint(), lineStringFeature), knotenIndex);

		// assert
		assertThat(knotenTupel.vonKnoten.getId()).isNull();
		assertThat(knotenTupel.vonKnoten.getPoint().equals(lineStringFeature.getStartPoint())).isTrue();
		assertThat(knotenTupel.nachKnoten.getId()).isNull();
		assertThat(knotenTupel.nachKnoten.getPoint().equals(lineStringFeature.getEndPoint())).isTrue();

		assertThat(knotenIndex.size()).isEqualTo(2);
	}

	@Test
	void getNeuenKnotenTupelFuerUpdate_VonKnotenNeu_legtNeuenKnotenAnUndImIndexAb()
		throws Exception {
		// arrange
		KnotenIndex knotenIndex = new KnotenIndex();

		Knoten altVon = KnotenTestDataProvider.withCoordinateAndQuelle(
			new Coordinate(10, 10), QuellSystem.DLM).id(10L).build();
		Knoten altNach = KnotenTestDataProvider.withCoordinateAndQuelle(
			new Coordinate(30, 10), QuellSystem.DLM).id(20L).build();
		Kante kante = KanteTestDataProvider.fromKnoten(altVon, altNach)
			.quelle(QuellSystem.DLM)
			.id(10L)
			.build();

		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(10, 12.1), new Coordinate(20, 12.1), new Coordinate(30, 10.9) });

		// act
		KnotenTupel knotenTupel = this.findKnotenFromIndexService
			.getNeuenKnotenTupelFuerUpdate(new TopologischesUpdate(kante, lineStringFeature.getStartPoint(),
				lineStringFeature.getEndPoint(), lineStringFeature), knotenIndex);

		// assert
		assertThat(knotenTupel.vonKnoten.getId()).isNull();
		assertThat(knotenTupel.vonKnoten.getPoint().equals(lineStringFeature.getStartPoint())).isTrue();
		assertThat(knotenTupel.nachKnoten).isEqualTo(altNach);
		assertThat(knotenTupel.nachKnoten.getPoint().equals(altNach.getPoint())).isTrue();

		assertThat(knotenIndex.size()).isEqualTo(1);
	}

	@Test
	void updateLineareReferenzenAndTopologieOnKante_NachKnotenNeu_legtNeuenKnotenAnUndImIndexAb()
		throws Exception {
		// arrange
		KnotenIndex knotenIndex = new KnotenIndex();

		Knoten altVon = KnotenTestDataProvider.withCoordinateAndQuelle(
			new Coordinate(10, 10), QuellSystem.DLM).id(10L).build();
		Knoten altNach = KnotenTestDataProvider.withCoordinateAndQuelle(
			new Coordinate(30, 10), QuellSystem.DLM).id(20L).build();
		Kante kante = KanteTestDataProvider.fromKnoten(altVon, altNach)
			.quelle(QuellSystem.DLM)
			.id(10L)
			.build();

		LineString lineStringFeature = this.geometryFactory.createLineString(
			new Coordinate[] { new Coordinate(10, 9), new Coordinate(20, 12.1), new Coordinate(30, 7.9) });

		// act
		KnotenTupel knotenTupel = this.findKnotenFromIndexService
			.getNeuenKnotenTupelFuerUpdate(new TopologischesUpdate(kante, lineStringFeature.getStartPoint(),
				lineStringFeature.getEndPoint(), lineStringFeature), knotenIndex);

		// assert
		assertThat(knotenTupel.vonKnoten).isEqualTo(altVon);
		assertThat(knotenTupel.vonKnoten.getPoint().equals(altVon.getPoint())).isTrue();
		assertThat(knotenTupel.nachKnoten.getId()).isNull();
		assertThat(knotenTupel.nachKnoten.getPoint().equals(lineStringFeature.getEndPoint())).isTrue();

		assertThat(knotenIndex.size()).isEqualTo(1);
	}
}
