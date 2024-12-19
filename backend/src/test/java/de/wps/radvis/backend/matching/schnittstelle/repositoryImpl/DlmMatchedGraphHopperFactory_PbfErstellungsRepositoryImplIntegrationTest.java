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

package de.wps.radvis.backend.matching.schnittstelle.repositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;

import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.common.DlmMatchedGraphhopperTestdataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.domain.exception.KeineRouteGefundenException;
import de.wps.radvis.backend.matching.domain.repository.CustomRoutingProfileRepository;
import de.wps.radvis.backend.matching.domain.repository.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.valueObject.RoutingResult;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;

class DlmMatchedGraphHopperFactory_PbfErstellungsRepositoryImplIntegrationTest {

	PbfErstellungsRepositoryImpl pbfErstellungsRepository;

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
		new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95))
	);

	@Mock
	private BarriereRepository barriereRepository;

	@Mock
	private KantenRepository kantenRepository;

	@TempDir
	public File temp;

	String pbfFileName = "generatedOsmFile.osm.pbf";

	@BeforeEach
	void setUp() {
		openMocks(this);
		pbfErstellungsRepository = new PbfErstellungsRepositoryImpl(coordinateReferenceSystemConverter,
			barriereRepository, kantenRepository);
	}

	@Test
	void test_update_cleansCache() throws KeineRouteGefundenException, IOException {
		List<Kante> kanten = List.of(
			KanteTestDataProvider.withDefaultValues().geometry(
				GeometryTestdataProvider.createLineString(new Coordinate(417700, 5288700),
					new Coordinate(417720, 5288700)))
				.id(1L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
				GeometryTestdataProvider.createLineString(new Coordinate(417720, 5288700),
					new Coordinate(417720, 5288710),
					new Coordinate(417720, 5288720), new Coordinate(417720, 5288720), new Coordinate(417730, 5288700)))
				.id(2L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
				GeometryTestdataProvider.createLineString(new Coordinate(417730, 5288700),
					new Coordinate(417740, 5288700)))
				.id(3L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
				GeometryTestdataProvider.createLineString(new Coordinate(417720, 5288700),
					new Coordinate(417730, 5288700)))
				.id(4L)
				.build()
		);

		File pbfFile = new File(temp, pbfFileName);
		pbfErstellungsRepository.writePbf(PbfErstellungsTestDataProvider.getEnvelopeToKantenStreamMap(List.of(kanten)),
			pbfFile);

		DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory = DlmMatchedGraphhopperTestdataProvider
			.initializeFactoryForPBFFile(
				pbfFile.getAbsolutePath(),
				temp.getAbsolutePath());

		GraphhopperRoutingRepository graphhopperRoutingRepository = new GraphhopperRoutingRepositoryImpl(
			dlmMatchedGraphHopperFactory,
			coordinateReferenceSystemConverter,
			mock(CustomRoutingProfileRepository.class));

		// derzeitige pbf ueberpruefen
		List<Coordinate> pointsToRoute = List.of(
			new Coordinate(417701, 5288700),
			new Coordinate(417739, 5288700));
		RoutingResult route = graphhopperRoutingRepository.route(pointsToRoute,
			GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, true);
		assertThat(route.getKantenIDs()).containsExactly(1L, 4L, 3L);

		// pbf neu erstellen ohne die zweite Kante
		pbfErstellungsRepository.writePbf(
			PbfErstellungsTestDataProvider.getEnvelopeToKantenStreamMap(List.of(kanten.subList(0, 3))), pbfFile);

		// act
		dlmMatchedGraphHopperFactory.updateDlmGraphHopper();
		graphhopperRoutingRepository.updateGraphHopper();

		// assert
		RoutingResult newRoute = graphhopperRoutingRepository.route(pointsToRoute,
			GraphhopperRoutingRepository.DEFAULT_PROFILE_ID, true);
		assertThat(newRoute.getKantenIDs()).containsExactly(1L, 2L, 3L);
	}
}
