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

package de.wps.radvis.backend.matching.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.graphhopper.config.Profile;

import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.domain.OsmMatchingRepository;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.LinearReferenziertesOsmMatchResult;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.OsmMatchedGraphHopper;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.OsmMatchingCacheRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.OsmMatchingRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import jakarta.persistence.EntityManager;

public class OsmMatchingRepositoryImplWithCustomPbfTest {

	@Mock
	private EntityManager entityManager;

	@Mock
	private BarriereRepository barriereRepository;

	@Mock
	private KantenRepository kantenRepository;

	OsmMatchingRepository osmMatchingRepository;

	private List<Kante> kanten;

	private CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
		new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95))
	);

	@TempDir
	public File temp;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		PbfErstellungsRepositoryImpl pbfErstellungsRepository = new PbfErstellungsRepositoryImpl(
			coordinateReferenceSystemConverter, entityManager, barriereRepository, kantenRepository);

		kanten = List.of(
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(10, 0),
						new Coordinate(10, 10),
						new Coordinate(10, 20))) // Länge 20m
				.id(1L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(10, 20),
						new Coordinate(20, 20),
						new Coordinate(30, 20),
						new Coordinate(40, 20))) // Länge 30m
				.id(2L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(40, 20),
						new Coordinate(50, 20),
						new Coordinate(50, 70))) // Länge 60m
				.id(3L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(40, 20),
						new Coordinate(40, 80))) // Länge 60m
				.id(4L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(50, 70),
						new Coordinate(60, 70),
						new Coordinate(60, 90))) // Länge 30m
				.id(5L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(-500, -500),
						new Coordinate(500, -500),
						new Coordinate(-500, -500),
						new Coordinate(-500, 500)))
				.id(6L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(-500, -500),
						new Coordinate(0, 50),
						new Coordinate(500, 500)))
				.id(7L)
				.build()
		);

		File pbfFile = new File(temp, "testForTopology.osm.pbf");

		pbfErstellungsRepository.writePbf(PbfErstellungsTestDataProvider.getEnvelopeToKantenStreamMap(List.of(kanten)),
			pbfFile);

		File mappingCache = new File(temp, "test-mapping-cache");

		OsmMatchedGraphHopper osmMatchedGraphHopper = new OsmMatchedGraphHopper(
			new OsmMatchingCacheRepositoryImpl(mappingCache.getAbsolutePath()));
		osmMatchedGraphHopper.setOSMFile(pbfFile.getAbsolutePath());
		osmMatchedGraphHopper.setGraphHopperLocation(new File(temp, "test-routing-graph-cache").getAbsolutePath());

		if (!mappingCache.exists()) {
			mappingCache.mkdirs();
		} else {
			mappingCache.delete();
			mappingCache.mkdirs();
		}

		Profile profile = new Profile("bike").setVehicle("bike").setWeighting("fastest").setTurnCosts(false);
		Profile profileCar = new Profile("car").setVehicle("car").setWeighting("fastest").setTurnCosts(false);
		Profile profileFoot = new Profile("foot").setVehicle("foot").setWeighting("fastest").setTurnCosts(false);
		osmMatchedGraphHopper.setProfiles(profile, profileCar, profileFoot);
		osmMatchedGraphHopper.importOrLoad();

		osmMatchingRepository = new OsmMatchingRepositoryImpl(osmMatchedGraphHopper,
			coordinateReferenceSystemConverter, 0.6);
	}

	@Test
	void teste_matchGeometryLinearReferenziert() throws KeinMatchGefundenException {

		LineString lineString = GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
			new Coordinate(10, 10),
			new Coordinate(10, 20),
			new Coordinate(40, 20),
			new Coordinate(50, 20),
			new Coordinate(50, 30)
			//			new Coordinate(41, 20)
			//			new Coordinate(50, 20)
		);

		LinearReferenziertesOsmMatchResult linearReferenziertesOsmMatchResult = osmMatchingRepository.matchGeometryLinearReferenziert(
			lineString, "foot");

		assertThat(linearReferenziertesOsmMatchResult.getLinearReferenzierteOsmWayIds()).extracting(
			LinearReferenzierteOsmWayId::getValue).containsExactly(1L, 2L, 3L);

		assertThat(linearReferenziertesOsmMatchResult.getLinearReferenzierteOsmWayIds())
			.usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
				.withComparatorForType(LineareReferenzTestProvider.comparatorWithTolerance(0.0005),
					LinearReferenzierterAbschnitt.class).build())
			.containsExactlyInAnyOrder(
				LinearReferenzierteOsmWayId.of(1L,
					LinearReferenzierterAbschnitt.of(0.5, 1)
				),
				LinearReferenzierteOsmWayId.of(2L,
					LinearReferenzierterAbschnitt.of(0, 1)
				),
				LinearReferenzierteOsmWayId.of(3L,
					LinearReferenzierterAbschnitt.of(0, 1. / 3.)
				)
			);
	}
}
