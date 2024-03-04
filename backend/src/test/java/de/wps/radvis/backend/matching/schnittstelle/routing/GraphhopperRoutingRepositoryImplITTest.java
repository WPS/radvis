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

package de.wps.radvis.backend.matching.schnittstelle.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.common.DlmMatchedGraphhopperTestdataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.domain.CustomRoutingProfileRepository;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.exception.KeineRouteGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.RoutingResult;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.GraphhopperRoutingRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import jakarta.persistence.EntityManager;

class GraphhopperRoutingRepositoryImplITTest {
	@Mock
	private GraphhopperOsmConfigurationProperties graphhopperOsmConfigurationProperties;

	@Mock
	private CommonConfigurationProperties commonConfigurationProperties;

	GraphhopperRoutingRepository graphhopperRoutingRepository;

	GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
		new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95))
	);
	private List<Kante> kanten;

	@Mock
	EntityManager entityManager;

	@Mock
	private BarriereRepository barriereRepository;

	@Mock
	private KantenRepository kantenRepository;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Nested
	public class BasicRoutingTest {
		@TempDir
		public File temp;

		@BeforeEach
		void setUp() {

			PbfErstellungsRepositoryImpl pbfErstellungsRepository = new PbfErstellungsRepositoryImpl(
				coordinateReferenceSystemConverter, entityManager, barriereRepository, kantenRepository);

			List<Kante> kanten = List.of(
				KanteTestDataProvider.withDefaultValues().geometry(
						GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(100, 100),
							new Coordinate(1000, 1000)))
					.id(1L)
					.build(),
				KanteTestDataProvider.withDefaultValues().geometry(
						GeometryTestdataProvider.createLineString(new Coordinate(10, 10), new Coordinate(10, 100),
							new Coordinate(10, 1000)))
					.id(2L)
					.build(),
				KanteTestDataProvider.withDefaultValues().geometry(
						GeometryTestdataProvider.createLineString(new Coordinate(10, 1000), new Coordinate(1000, 1000)))
					.id(3L)
					.build(),
				KanteTestDataProvider.withDefaultValues().geometry(
						GeometryTestdataProvider.createLineString(new Coordinate(1000, 1000), new Coordinate(10000, 1000),
							new Coordinate(10000, 10000), new Coordinate(1000, 10000), new Coordinate(1000, 1000))).id(4L)
					.build(),
				KanteTestDataProvider.withDefaultValues().geometry(
						GeometryTestdataProvider.createLineString(new Coordinate(1000, 1000), new Coordinate(5000, 5000),
							new Coordinate(10000, 10000))).id(5L)
					.build()
			);

			File pbfFile = new File(temp, "generatedOsmFile.osm.pbf");

			pbfErstellungsRepository.writePbf(
				PbfErstellungsTestDataProvider.getEnvelopeToKantenStreamMap(List.of(kanten)), pbfFile);

			DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory = DlmMatchedGraphhopperTestdataProvider
				.initializeFactoryForPBFFile(pbfFile.getAbsolutePath(), temp.getAbsolutePath());

			graphhopperRoutingRepository = new GraphhopperRoutingRepositoryImpl(dlmMatchedGraphHopperFactory,
				coordinateReferenceSystemConverter, mock(CustomRoutingProfileRepository.class));
		}

		@Test
		void testeRouting_IDsInCorrectOrder() throws KeineRouteGefundenException {
			List<Coordinate> pointsToRoute = List.of(new Coordinate(30, 30), new Coordinate(10, 10),
				new Coordinate(10, 20),
				new Coordinate(10, 120));
			RoutingResult route = graphhopperRoutingRepository.route(pointsToRoute,
				GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, true);
			ArrayList<Coordinate> reversedRoute = new ArrayList<>(pointsToRoute);
			Collections.reverse(reversedRoute);
			RoutingResult routeReversed = graphhopperRoutingRepository.route(reversedRoute,
				GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, true);

			assertThat(route.getKantenIDs()).containsExactly(1L, 2L);
			assertThat(routeReversed.getKantenIDs()).containsExactly(2L, 1L);
			assertThat(route.getRoutenGeometrie().reverse().getCoordinates()).containsExactlyElementsOf(
				Arrays.asList(routeReversed.getRoutenGeometrie().getCoordinates()));
		}

		@Test
		void testeRouting_RoutedGeometryHasCorrectLength() throws KeineRouteGefundenException {
			LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(30, 30),
				new Coordinate(10, 10), new Coordinate(10, 20),
				new Coordinate(10, 120));

			lineString = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(lineString, 0, 2);

			List<Coordinate> pointsToRoute = Arrays.asList(lineString.getCoordinates());

			RoutingResult route = graphhopperRoutingRepository.route(pointsToRoute,
				GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, true);
			List<Coordinate> reversedRoute = Arrays.asList(lineString.reverse().getCoordinates());
			RoutingResult routeReversed = graphhopperRoutingRepository.route(reversedRoute,
				GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, true);

			assertThat(route.getRoutenGeometrie().reverse().getCoordinates()).containsExactlyElementsOf(
				Arrays.asList(routeReversed.getRoutenGeometrie().getCoordinates()));
			assertThat(route.getRoutenGeometrie().getLength()).isCloseTo(lineString.getLength(), Offset.offset(5.));

			for (Coordinate coordinate : lineString.getCoordinates()) {
				assertThat(route.getRoutenGeometrie().distance(GEO_FACTORY.createPoint(coordinate))).isCloseTo(0.,
					Offset.offset(2.));
			}
		}

		@Test
		void testeRouting_KeineKanteIrgendwoInDerNaehe_WirftException() throws KeineRouteGefundenException {
			assertThatThrownBy(() ->
				graphhopperRoutingRepository.route(
					List.of(new Coordinate(8650, 5000), new Coordinate(5000, 5000),
						new Coordinate(5050, 5050)), GraphhopperRoutingRepository.DEFAULT_PROFILE_ID,
					true)).isInstanceOf(
					KeineRouteGefundenException.class)
				.hasMessageContaining("com.graphhopper.util.exceptions.PointNotFoundException");
		}

		@Test
		void testeRouting_OutOfBounds_WirftException() throws KeineRouteGefundenException {
			assertThatThrownBy(() ->
				graphhopperRoutingRepository.route(
					List.of(new Coordinate(10050, 10050), new Coordinate(5000, 5000),
						new Coordinate(5050, 5050)), GraphhopperRoutingRepository.DEFAULT_PROFILE_ID,
					true)).isInstanceOf(
					KeineRouteGefundenException.class)
				.hasMessageContaining("com.graphhopper.util.exceptions.PointOutOfBoundsException");
		}
	}

	@Nested
	public class TesteTopologieDesRoutingNetzes {

		@TempDir
		public File temp;

		@BeforeEach
		void setUp() {
			PbfErstellungsRepositoryImpl pbfErstellungsRepository = new PbfErstellungsRepositoryImpl(
				new CoordinateReferenceSystemConverter(commonConfigurationProperties.getBadenWuerttembergEnvelope()),
				entityManager, barriereRepository, kantenRepository);

			kanten = List.of(
				KanteTestDataProvider.withDefaultValues().geometry(
						GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
							new Coordinate(10, 0),
							new Coordinate(10, 10),
							new Coordinate(10, 20)))
					.id(1L)
					.build(),
				KanteTestDataProvider.withDefaultValues().geometry(
						GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
							new Coordinate(0, 10),
							new Coordinate(10, 10),
							new Coordinate(20, 10)))
					.id(2L)
					.build(),
				KanteTestDataProvider.withDefaultValues().geometry(
						GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
							new Coordinate(70, 70),
							new Coordinate(60, 60),
							new Coordinate(50, 50)))
					.id(3L)
					.build()
			);

			File pbfFile = new File(temp, "testForTopology.osm.pbf");

			pbfErstellungsRepository.writePbf(
				PbfErstellungsTestDataProvider.getEnvelopeToKantenStreamMap(List.of(kanten)), pbfFile);

			DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory = DlmMatchedGraphhopperTestdataProvider
				.initializeFactoryForPBFFile(pbfFile.getAbsolutePath(), temp.getAbsolutePath());

			graphhopperRoutingRepository = new GraphhopperRoutingRepositoryImpl(dlmMatchedGraphHopperFactory,
				coordinateReferenceSystemConverter, mock(CustomRoutingProfileRepository.class));
		}

		@Test
		void teste_entlangEeberlappendeKantenOhneTopologischeVerbindung_wirdNichtGerouted() {
			List<Coordinate> pointsToRoute = new ArrayList<>(List.of(
				GeometryTestdataProvider.moveAllToValidBounds(
					new Coordinate(00, 10),
					new Coordinate(10, 10),
					new Coordinate(10, 20))));

			assertThatThrownBy(
				() -> graphhopperRoutingRepository.route(pointsToRoute, GraphhopperRoutingRepository.DEFAULT_PROFILE_ID,
					true)).isInstanceOf(KeineRouteGefundenException.class)
				.hasMessageContaining("ConnectionNotFoundException");

			Collections.reverse(pointsToRoute);
			assertThatThrownBy(
				() -> graphhopperRoutingRepository.route(pointsToRoute, GraphhopperRoutingRepository.DEFAULT_PROFILE_ID,
					true)).isInstanceOf(KeineRouteGefundenException.class)
				.hasMessageContaining("ConnectionNotFoundException");
		}
	}
}
