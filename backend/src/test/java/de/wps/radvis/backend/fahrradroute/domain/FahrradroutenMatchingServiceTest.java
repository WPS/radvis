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

package de.wps.radvis.backend.fahrradroute.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisherSensitiveTest;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteMatchingStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteNetzbezugResult;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation.FahrradroutenMatchingAndRoutingInformationBuilder;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Hoehenunterschied;
import de.wps.radvis.backend.matching.domain.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.exception.KeineRouteGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.matching.domain.valueObject.RoutingResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import lombok.Getter;
import lombok.Setter;

class FahrradroutenMatchingServiceTest implements RadVisDomainEventPublisherSensitiveTest {

	private FahrradroutenMatchingService fahrradroutenMatchingService;
	private FahrradrouteMatchingStatistik fahrradrouteMatchingStatistik;
	private FahrradroutenMatchingAndRoutingInformationBuilder fahrradroutenMatchingAndRoutingInformationBuilder;

	@Mock
	KantenRepository kantenRepository;
	@Mock
	GraphhopperRoutingRepository graphhopperRoutingRepository;
	@Mock
	private DlmMatchingRepository dlmMatchingRepository;
	@Mock
	VerwaltungseinheitService verwaltungseinheitService;

	@Getter
	@Setter
	MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		fahrradroutenMatchingService = new FahrradroutenMatchingService(
			kantenRepository,
			org.springframework.data.util.Lazy.of(() -> dlmMatchingRepository),
			org.springframework.data.util.Lazy.of(() -> graphhopperRoutingRepository),
			verwaltungseinheitService);
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100)));
		fahrradrouteMatchingStatistik = new FahrradrouteMatchingStatistik();
		fahrradroutenMatchingAndRoutingInformationBuilder = FahrradroutenMatchingAndRoutingInformation.builder()
			.abbildungDurchRouting(false);
	}

	@Test
	void getFahrradrouteNetzbezugResult_originalGeometrieValid_matching_NetzbezugErstellt()
		throws KeinMatchGefundenException {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(20, 20));

		// gematchte geometrie entspricht exakt der originalgeometrie
		when(dlmMatchingRepository.matchGeometry(lineString, "bike"))
			.thenReturn(new OsmMatchResult(lineString, List.of(OsmWayId.of(1L))));

		when(dlmMatchingRepository.matchGeometryUndDetails(lineString, "bike"))
			.thenReturn(new ProfilMatchResult(lineString, List.of(OsmWayId.of(1L)), Collections.emptyList()));

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 20, 20, QuellSystem.DLM)
			.id(1L).build();
		when(kantenRepository.findById(1L)).thenReturn(Optional.of(kante));

		// act
		Optional<FahrradrouteNetzbezugResult> fahrradrouteNetzbezugResult = fahrradroutenMatchingService
			.getFahrradrouteNetzbezugResult(
				lineString, fahrradrouteMatchingStatistik, fahrradroutenMatchingAndRoutingInformationBuilder, true);

		// assert
		Set<AbschnittsweiserKantenBezug> expectedKantenBezug = Set
			.of(new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)));
		assertThat(fahrradrouteNetzbezugResult).isPresent();
		assertThat(fahrradrouteNetzbezugResult.get().getAbschnittsweiserKantenBezug())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactlyInAnyOrderElementsOf(expectedKantenBezug);

		FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation = fahrradroutenMatchingAndRoutingInformationBuilder
			.build();
		assertThat(fahrradroutenMatchingAndRoutingInformation.getAbbildungDurchRouting()).contains(false);
	}

	@Test
	void getFahrradrouteNetzbezugResult_originalGeometrieValid_routing_NetzbezugErstellt()
		throws KeinMatchGefundenException, KeineRouteGefundenException {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(20, 20));

		// matching schlaegt fehl
		when(dlmMatchingRepository.matchGeometry(lineString, "bike"))
			.thenThrow(new KeinMatchGefundenException("oh no", new Throwable()));
		// routing klappt -> geroutete geometrie entspricht exakt der originalgeometrie
		when(graphhopperRoutingRepository.route(any(), eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID),
			eq(true))).thenReturn(
				new RoutingResult(List.of(1L),
					lineString,
					Hoehenunterschied.of(12.3),
					Hoehenunterschied.of(23.4)));
		when(dlmMatchingRepository.matchGeometryUndDetails(any(LineString.class), eq("bike")))
			.thenReturn(new ProfilMatchResult(
				lineString,
				List.of(OsmWayId.of(1L)),
				Collections.emptyList()));

		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 20, 20, QuellSystem.DLM)
			.id(1L).build();
		when(kantenRepository.findById(1L)).thenReturn(Optional.of(kante));

		// act
		Optional<FahrradrouteNetzbezugResult> fahrradrouteNetzbezugResult = fahrradroutenMatchingService
			.getFahrradrouteNetzbezugResult(
				lineString, fahrradrouteMatchingStatistik, fahrradroutenMatchingAndRoutingInformationBuilder, true);

		// assert
		verify(dlmMatchingRepository, times(1)).matchGeometryUndDetails(any(), eq("bike"));
		Set<AbschnittsweiserKantenBezug> expectedKantenBezug = Set
			.of(new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)));
		assertThat(fahrradrouteNetzbezugResult).isPresent();
		assertThat(fahrradrouteNetzbezugResult.get().getAbschnittsweiserKantenBezug())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactlyInAnyOrderElementsOf(expectedKantenBezug);

		FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation = fahrradroutenMatchingAndRoutingInformationBuilder
			.build();
		assertThat(fahrradroutenMatchingAndRoutingInformation.getAbbildungDurchRouting()).contains(true);
		assertThat(fahrradroutenMatchingAndRoutingInformation.getAbweichendeSegmente()).isEmpty();
	}

	@Test
	void getFahrradrouteNetzbezugResult_originalGeometrieValid_fail_leererNetzbezug()
		throws KeinMatchGefundenException, KeineRouteGefundenException {
		// arrange
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(20, 20));

		when(dlmMatchingRepository.matchGeometry(lineString, "bike"))
			.thenThrow(new KeinMatchGefundenException("oh no", new Throwable()));
		when(graphhopperRoutingRepository.route(any(), eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID),
			eq(true))).thenThrow(
				new KeineRouteGefundenException("oh no"));
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 25, 25)));

		// act
		Optional<FahrradrouteNetzbezugResult> fahrradrouteNetzbezugResult = fahrradroutenMatchingService
			.getFahrradrouteNetzbezugResult(
				lineString, fahrradrouteMatchingStatistik, fahrradroutenMatchingAndRoutingInformationBuilder, true);

		// assert
		assertThat(fahrradrouteNetzbezugResult).isEmpty();

		FahrradroutenMatchingAndRoutingInformation fahrradroutenMatchingAndRoutingInformation = fahrradroutenMatchingAndRoutingInformationBuilder
			.build();
		assertThat(fahrradroutenMatchingAndRoutingInformation.getAbbildungDurchRouting()).contains(false);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getFahrradrouteNetzbezugResult_geometrieEndeAusserhalbBundesland_geometrieWirdGeschnitten()
		throws KeinMatchGefundenException, KeineRouteGefundenException {
		// arrange
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 10, 10)));

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(7, 8),
			new Coordinate(20, 20));

		// matching schlaegt fehl
		ArgumentCaptor<LineString> lineStringArgumentCaptor = ArgumentCaptor.forClass(LineString.class);
		when(dlmMatchingRepository.matchGeometry(lineStringArgumentCaptor.capture(), eq("bike")))
			.thenThrow(new KeinMatchGefundenException("oh no", new Throwable()));
		// routing klappt -> geroutete geometrie entspricht exakt der originalgeometrie
		ArgumentCaptor<List<Coordinate>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
		when(graphhopperRoutingRepository.route(listArgumentCaptor.capture(),
			eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID), eq(true)))
				.thenThrow(new KeineRouteGefundenException("oh no"));

		// act
		fahrradroutenMatchingService.getFahrradrouteNetzbezugResult(lineString, fahrradrouteMatchingStatistik,
			fahrradroutenMatchingAndRoutingInformationBuilder, true);

		// assert
		verify(dlmMatchingRepository, times(2)).matchGeometry(any(), eq("bike"));
		assertThat(lineStringArgumentCaptor.getAllValues()).containsExactly(
			lineString,
			GeometryTestdataProvider.createLineString(new Coordinate(1, 1), new Coordinate(7, 8)));
		verify(graphhopperRoutingRepository, times(2)).route(any(), eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID),
			eq(true));
		assertThat(listArgumentCaptor.getAllValues()).containsExactly(
			List.of(new Coordinate(1, 1), new Coordinate(20, 20)),
			List.of(new Coordinate(1, 1), new Coordinate(7, 8)));
	}

	@Test
	@SuppressWarnings("unchecked")
	void getFahrradrouteNetzbezugResult_geometrieAnfangUndEndeAusserhalbBundesland_geometrieWirdGeschnitten()
		throws KeinMatchGefundenException, KeineRouteGefundenException {
		// arrange
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(1, 1, 10, 10)));

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(4, 6),
			new Coordinate(7, 8), new Coordinate(20, 20));

		// matching schlaegt fehl
		ArgumentCaptor<LineString> lineStringArgumentCaptor = ArgumentCaptor.forClass(LineString.class);
		when(dlmMatchingRepository.matchGeometry(lineStringArgumentCaptor.capture(), eq("bike")))
			.thenThrow(new KeinMatchGefundenException("oh no", new Throwable()));
		// routing klappt -> geroutete geometrie entspricht exakt der originalgeometrie
		ArgumentCaptor<List<Coordinate>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
		when(graphhopperRoutingRepository.route(listArgumentCaptor.capture(),
			eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID), eq(true)))
				.thenThrow(new KeineRouteGefundenException("oh no"));

		// act
		fahrradroutenMatchingService.getFahrradrouteNetzbezugResult(lineString, fahrradrouteMatchingStatistik,
			fahrradroutenMatchingAndRoutingInformationBuilder, true);

		// assert
		verify(dlmMatchingRepository, times(2)).matchGeometry(any(), eq("bike"));
		assertThat(lineStringArgumentCaptor.getAllValues()).containsExactly(
			lineString,
			GeometryTestdataProvider.createLineString(new Coordinate(4, 6), new Coordinate(7, 8)));
		verify(graphhopperRoutingRepository, times(2)).route(any(), eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID),
			eq(true));
		assertThat(listArgumentCaptor.getAllValues()).containsExactly(
			List.of(new Coordinate(0, 0), new Coordinate(20, 20)),
			List.of(new Coordinate(4, 6),
				new Coordinate(7, 8)));
	}

	@Test
	@SuppressWarnings("unchecked")
	void getFahrradrouteNetzbezugResult_geometrieAnfangAusserhalbBundesland_geometrieWirdGeschnitten()
		throws KeinMatchGefundenException, KeineRouteGefundenException {
		// arrange
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(1, 1, 30, 30)));

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(4, 6),
			new Coordinate(7, 8),
			new Coordinate(20, 20));

		// matching schlaegt fehl
		ArgumentCaptor<LineString> lineStringArgumentCaptor = ArgumentCaptor.forClass(LineString.class);
		when(dlmMatchingRepository.matchGeometry(lineStringArgumentCaptor.capture(), eq("bike")))
			.thenThrow(new KeinMatchGefundenException("oh no", new Throwable()));
		// routing klappt -> geroutete geometrie entspricht exakt der originalgeometrie
		ArgumentCaptor<List<Coordinate>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
		when(graphhopperRoutingRepository.route(listArgumentCaptor.capture(),
			eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID), eq(true)))
				.thenThrow(new KeineRouteGefundenException("oh no"));

		// act
		fahrradroutenMatchingService.getFahrradrouteNetzbezugResult(lineString, fahrradrouteMatchingStatistik,
			fahrradroutenMatchingAndRoutingInformationBuilder, true);

		// assert
		verify(dlmMatchingRepository, times(2)).matchGeometry(any(), eq("bike"));
		assertThat(lineStringArgumentCaptor.getAllValues()).containsExactly(
			lineString,
			GeometryTestdataProvider.createLineString(new Coordinate(4, 6),
				new Coordinate(7, 8),
				new Coordinate(20, 20)));
		verify(graphhopperRoutingRepository, times(2)).route(any(), eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID),
			eq(true));
		assertThat(listArgumentCaptor.getAllValues()).containsExactly(
			List.of(new Coordinate(0, 0), new Coordinate(20, 20)),
			List.of(new Coordinate(4, 6), new Coordinate(20, 20)));
	}

	@Test
	@SuppressWarnings("unchecked")
	void getFahrradrouteNetzbezugResult_geometrieMehrfachGrenzueberschneidend_keinMatching()
		throws KeinMatchGefundenException, KeineRouteGefundenException {
		// arrange
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 10, 10)));

		LineString lineString = GeometryTestdataProvider.createLineString(
			new Coordinate(1, 1),
			new Coordinate(3, 4),
			new Coordinate(5, 20000),
			new Coordinate(5, 5),
			new Coordinate(7, 8));

		// matching schlaegt fehl
		ArgumentCaptor<LineString> lineStringArgumentCaptor = ArgumentCaptor.forClass(LineString.class);
		when(dlmMatchingRepository.matchGeometry(lineStringArgumentCaptor.capture(), eq("bike")))
			.thenThrow(new KeinMatchGefundenException("oh no", new Throwable()));
		// routing klappt -> geroutete geometrie entspricht exakt der originalgeometrie
		ArgumentCaptor<List<Coordinate>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
		when(graphhopperRoutingRepository.route(listArgumentCaptor.capture(),
			eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID), eq(true)))
				.thenThrow(new KeineRouteGefundenException("oh no"));

		// act
		fahrradroutenMatchingService.getFahrradrouteNetzbezugResult(lineString, fahrradrouteMatchingStatistik,
			fahrradroutenMatchingAndRoutingInformationBuilder, true);

		// assert
		verify(dlmMatchingRepository, times(1)).matchGeometry(any(), eq("bike"));
		assertThat(lineStringArgumentCaptor.getValue()).isEqualTo(lineString);
		verify(graphhopperRoutingRepository, times(1)).route(any(), eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID),
			eq(true));
		assertThat(listArgumentCaptor.getValue()).isEqualTo(List.of(lineString.getCoordinates()));
	}

	@Test
	@SuppressWarnings("unchecked")
	void getFahrradrouteNetzbezugResult_geometrieTeilweiseAusserhalbBundesland_mitKehrtwenden_geometrieWirdGeschnitten()
		throws KeinMatchGefundenException, KeineRouteGefundenException {
		// arrange
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 15, 15)));

		LineString lineString = GeometryTestdataProvider.createLineString(
			new Coordinate(1, 1),
			new Coordinate(10, 1),
			new Coordinate(10, 10),
			new Coordinate(10, 1),
			new Coordinate(20, 1));

		// matching schlaegt fehl
		ArgumentCaptor<LineString> lineStringArgumentCaptor = ArgumentCaptor.forClass(LineString.class);
		when(dlmMatchingRepository.matchGeometry(lineStringArgumentCaptor.capture(), eq("bike")))
			.thenThrow(new KeinMatchGefundenException("oh no", new Throwable()));
		// routing klappt -> geroutete geometrie entspricht exakt der originalgeometrie
		ArgumentCaptor<List<Coordinate>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
		when(graphhopperRoutingRepository.route(listArgumentCaptor.capture(),
			eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID), eq(true)))
				.thenThrow(new KeineRouteGefundenException("oh no"));

		// act
		fahrradroutenMatchingService.getFahrradrouteNetzbezugResult(lineString, fahrradrouteMatchingStatistik,
			fahrradroutenMatchingAndRoutingInformationBuilder, true);

		// assert
		verify(dlmMatchingRepository, times(2)).matchGeometry(any(), eq("bike"));
		assertThat(lineStringArgumentCaptor.getAllValues()).containsExactly(lineString,
			GeometryTestdataProvider.createLineString(new Coordinate(1, 1),
				new Coordinate(10, 1),
				new Coordinate(10, 10),
				new Coordinate(10, 1)));
		verify(graphhopperRoutingRepository, times(2)).route(any(), eq(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID),
			eq(true));
		// beim Routing wird die Geometrie vorher vereinfacht
		assertThat(listArgumentCaptor.getAllValues()).containsExactly(
			List.of(new Coordinate(1, 1),
				new Coordinate(20, 1)),
			List.of(new Coordinate(1, 1),
				new Coordinate(10, 1)));
	}

	@Test
	@SuppressWarnings("unchecked")
	void schneideAnfangUndEndeAusserhalbBWsAb_nurMitteAusserhalb_nichtsTun() {
		// arrange
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 10, 10)));

		LineString lineString = GeometryTestdataProvider.createLineString(
			new Coordinate(1, 1),
			new Coordinate(3, 4),
			new Coordinate(5, 20000),
			new Coordinate(5, 5),
			new Coordinate(7, 8));

		// act
		Optional<LineString> zugeschnitten = fahrradroutenMatchingService.schneideAnfangUndEndeAusserhalbBWsAb(
			lineString);

		// assert
		assertThat(zugeschnitten).isPresent();
		assertThat(zugeschnitten.get().getCoordinates()).containsExactly(
			new Coordinate(1, 1),
			new Coordinate(3, 4),
			new Coordinate(5, 20000),
			new Coordinate(5, 5),
			new Coordinate(7, 8)
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	void schneideAnfangUndEndeAusserhalbBWsAb_startEndeAusserhalb_zuschneiden() {
		// arrange
		when(verwaltungseinheitService.getBundeslandBereichPrepared()).thenReturn(
			PreparedGeometryFactory.prepare(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 10, 10)));

		LineString lineString = GeometryTestdataProvider.createLineString(
			new Coordinate(-1, -1),
			new Coordinate(1, 1),
			new Coordinate(3, 4),
			new Coordinate(5, 20000),
			new Coordinate(5, 5),
			new Coordinate(7, 8),
			new Coordinate(14, 14));

		// act
		Optional<LineString> zugeschnitten = fahrradroutenMatchingService.schneideAnfangUndEndeAusserhalbBWsAb(
			lineString);

		// assert
		assertThat(zugeschnitten).isPresent();
		assertThat(zugeschnitten.get().getCoordinates()).containsExactly(
			new Coordinate(1, 1),
			new Coordinate(3, 4),
			new Coordinate(5, 20000),
			new Coordinate(5, 5),
			new Coordinate(7, 8)
		);
	}
}
