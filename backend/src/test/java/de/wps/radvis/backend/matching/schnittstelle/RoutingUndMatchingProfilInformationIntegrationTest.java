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
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.common.DlmMatchedGraphhopperTestdataProvider;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.matching.domain.CustomRoutingProfileRepository;
import de.wps.radvis.backend.matching.domain.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.exception.KeineRouteGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilRoutingResult;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchingRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.GraphhopperRoutingRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsRepositoryImpl;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.PbfErstellungsTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import jakarta.persistence.EntityManager;

public class RoutingUndMatchingProfilInformationIntegrationTest {

	@Mock
	private EntityManager entityManager;

	@Mock
	private BarriereRepository barriereRepository;

	@Mock
	private KantenRepository kantenRepository;

	GraphhopperRoutingRepository graphhopperRoutingRepository;

	DlmMatchingRepository dlmMatchingRepository;

	private List<Kante> kanten;

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
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
			KanteTestDataProvider.withDefaultValuesAndZweiseitig().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(10, 0),
						new Coordinate(10, 10),
						new Coordinate(10, 20))) // Länge 20m
				.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(
						List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte().belagArt(
							BelagArt.WASSERGEBUNDENE_DECKE).build()))
					.fuehrungsformAttributeRechts(
						List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte().belagArt(
							BelagArt.UNGEBUNDENE_DECKE).build()))
					.build())
				.id(1L)
				.build(),
			KanteTestDataProvider.withDefaultValuesAndZweiseitig().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(10, 20),
						new Coordinate(20, 20),
						new Coordinate(30, 20),
						new Coordinate(40, 20))) // Länge 30m
				.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(
						List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.6).belagArt(
								BelagArt.NATURSTEINPFLASTER).build(),
							FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.6, 1).belagArt(BelagArt.BETON)
								.build()
						))
					.fuehrungsformAttributeRechts(
						List.of(FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, 0.4).belagArt(
								BelagArt.WASSERGEBUNDENE_DECKE).build(),
							FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.4, 1).belagArt(
								BelagArt.ASPHALT).build()
						))
					.build())
				.id(2L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(40, 20),
						new Coordinate(50, 20),
						new Coordinate(50, 50))) // Länge 40m
				.id(3L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(50, 50),
						new Coordinate(60, 50),
						new Coordinate(60, 70))) // Länge 30m
				.id(4L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(-500, -500),
						new Coordinate(500, -500),
						new Coordinate(-500, -500),
						new Coordinate(-500, 500)))
				.id(5L)
				.build(),
			KanteTestDataProvider.withDefaultValues().geometry(
					GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
						new Coordinate(-500, -500),
						new Coordinate(0, 50),
						new Coordinate(500, 500)))
				.id(6L)
				.build()
		);

		File pbfFile = new File(temp, "testForTopology.osm.pbf");

		pbfErstellungsRepository.writePbf(PbfErstellungsTestDataProvider.getEnvelopeToKantenStreamMap(List.of(kanten)),
			pbfFile);

		DlmMatchedGraphHopperFactory dlmMatchedGraphHopperFactory = DlmMatchedGraphhopperTestdataProvider.
			initializeFactoryForPBFFile(pbfFile.getAbsolutePath(), temp.getAbsolutePath());

		graphhopperRoutingRepository = new GraphhopperRoutingRepositoryImpl(dlmMatchedGraphHopperFactory,
			coordinateReferenceSystemConverter, mock(CustomRoutingProfileRepository.class));

		dlmMatchingRepository = new DlmMatchingRepositoryImpl(dlmMatchedGraphHopperFactory,
			coordinateReferenceSystemConverter, 0.6);
	}

	@Test
	void teste_routeMitProfileigenschaften_inorder_liestRechtenWertMitGroesstemAnteilProKante()
		throws KeineRouteGefundenException {
		List<Coordinate> pointsToRoute = new ArrayList<>(List.of(
			GeometryTestdataProvider.moveAllToValidBounds(
				new Coordinate(10, 0),
				new Coordinate(50, 50))));

		ProfilRoutingResult profilRoutingResult = graphhopperRoutingRepository.routeMitProfileigenschaften(
			pointsToRoute, GraphhopperRoutingRepository.DEFAULT_PROFILE_ID);

		assertThat(profilRoutingResult.getKantenIDs()).containsExactly(1L, 2L, 3L);
		assertThat(Arrays.asList(profilRoutingResult.getRoutenGeometrie().getCoordinates()))
			.usingComparatorForType(GeometryTestdataProvider.LENIENT_COORDINATE_COMPARATOR, Coordinate.class)
			.containsExactly(
				GeometryTestdataProvider.moveAllToValidBounds(
					new Coordinate(10, 0),
					new Coordinate(10, 20),
					new Coordinate(40, 20),
					new Coordinate(50, 20),
					new Coordinate(50, 50))
			);
		double kantenUebergangsLR = 20. / 90.;
		double kantenUebergangsLR2 = (20. + 30.) / 90.;

		assertThat(profilRoutingResult.getLinearReferenzierteProfilEigenschaften())
			.usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
				.withComparatorForType(LineareReferenzTestProvider.comparatorWithTolerance(0.0005),
					LinearReferenzierterAbschnitt.class).build())
			.containsExactly(
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.UNGEBUNDENE_DECKE,
						Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(0, kantenUebergangsLR)
				),
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.ASPHALT, Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(kantenUebergangsLR, kantenUebergangsLR2)
				),
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.UNBEKANNT, Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(kantenUebergangsLR2, 1)
				)
			);
	}

	@Test
	void teste_routeMitProfileigenschaften_reverseorder_liestLinkenWertMitGroesstemAnteilProKante()
		throws KeineRouteGefundenException {

		List<Coordinate> pointsToRoute = new ArrayList<>(List.of(
			GeometryTestdataProvider.moveAllToValidBounds(
				new Coordinate(50, 50),
				new Coordinate(10, 0))));

		ProfilRoutingResult profilRoutingResult = graphhopperRoutingRepository.routeMitProfileigenschaften(
			pointsToRoute, GraphhopperRoutingRepository.DEFAULT_PROFILE_ID);

		assertThat(profilRoutingResult.getKantenIDs()).containsExactly(3L, 2L, 1L);

		assertThat(Arrays.asList(profilRoutingResult.getRoutenGeometrie().getCoordinates()))
			.usingComparatorForType(GeometryTestdataProvider.LENIENT_COORDINATE_COMPARATOR, Coordinate.class)
			.containsExactly(
				GeometryTestdataProvider.moveAllToValidBounds(
					new Coordinate(50, 50),
					new Coordinate(50, 20),
					new Coordinate(40, 20),
					new Coordinate(10, 20),
					new Coordinate(10, 0))
			);

		double kantenUebergangsLR = 40. / 90.;
		double kantenUebergangsLR2 = (40. + 30.) / 90.;

		assertThat(profilRoutingResult.getLinearReferenzierteProfilEigenschaften())
			.usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
				.withComparatorForType(LineareReferenzTestProvider.comparatorWithTolerance(0.00005),
					LinearReferenzierterAbschnitt.class).build())
			.containsExactly(
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.UNBEKANNT,
						Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(0, kantenUebergangsLR)
				),
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.NATURSTEINPFLASTER, Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(kantenUebergangsLR, kantenUebergangsLR2)
				),
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.WASSERGEBUNDENE_DECKE, Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(kantenUebergangsLR2, 1)
				)
			);
	}

	@Test
	void teste_matcheMitProfileigenschaften_inorder_liestRechtenWertMitGroesstemAnteilProKante()
		throws KeinMatchGefundenException {

		LineString lineString = GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
			new Coordinate(10, 0),
			new Coordinate(10, 10),
			new Coordinate(10, 20),
			new Coordinate(40, 20),
			new Coordinate(50, 20),
			new Coordinate(50, 50));

		ProfilMatchResult profileMatchingResult = dlmMatchingRepository.matchGeometryUndDetails(
			lineString, "bike");

		assertThat(profileMatchingResult.getOsmWayIdsUnwrapped()).containsExactly(1L, 2L, 3L);
		double kantenUebergangsLR = 20. / 90.;
		double kantenUebergangsLR2 = (20. + 30.) / 90.;

		assertThat(profileMatchingResult.getProfilEigenschaften())
			.usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
				.withComparatorForType(LineareReferenzTestProvider.comparatorWithTolerance(0.00005),
					LinearReferenzierterAbschnitt.class).build())
			.containsExactly(
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.UNGEBUNDENE_DECKE,
						Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(0, kantenUebergangsLR)
				),
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.ASPHALT, Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(kantenUebergangsLR, kantenUebergangsLR2)
				),
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.UNBEKANNT, Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(kantenUebergangsLR2, 1)
				)
			);
	}

	@Test
	void teste_matcheMitProfileigenschaften_inorder_abschnitteMitGleichenEigenschaftenWerdenZusammengefasst()
		throws KeinMatchGefundenException {

		LineString lineString = GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
			new Coordinate(10, 0),
			new Coordinate(10, 10), // 10
			new Coordinate(10, 20), // 10
			new Coordinate(40, 20), // 30
			new Coordinate(50, 20), // 10
			new Coordinate(50, 50), // 30
			new Coordinate(60, 50) // 10
		);

		ProfilMatchResult profileMatchingResult = dlmMatchingRepository.matchGeometryUndDetails(
			lineString, "bike");

		assertThat(profileMatchingResult.getOsmWayIdsUnwrapped()).containsExactly(1L, 2L, 3L, 4L);
		double kantenUebergangsLR = 20. / 100.;
		double kantenUebergangsLR2 = (20. + 30.) / 100.;

		assertThat(profileMatchingResult.getProfilEigenschaften())
			.usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
				.withComparatorForType(LineareReferenzTestProvider.comparatorWithTolerance(0.005),
					LinearReferenzierterAbschnitt.class).build())
			.containsExactly(
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.UNGEBUNDENE_DECKE,
						Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(0, kantenUebergangsLR)
				),
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.ASPHALT, Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(kantenUebergangsLR, kantenUebergangsLR2)
				),
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.UNBEKANNT, Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(kantenUebergangsLR2, 1)
				)
			);
	}

	@Test
	void teste_matcheMitProfileigenschaften_reverseorder_liestLinkenWertMitGroesstemAnteilProKante()
		throws KeinMatchGefundenException {

		LineString lineString = GeometryTestdataProvider.createLineStringWithCoordinatesMovedToValidBounds(
			new Coordinate(10, 0),
			new Coordinate(10, 10),
			new Coordinate(10, 20),
			new Coordinate(40, 20),
			new Coordinate(50, 20),
			new Coordinate(50, 50));

		ProfilMatchResult profileMatchingResult = dlmMatchingRepository.matchGeometryUndDetails(
			lineString.reverse(), "bike");

		assertThat(profileMatchingResult.getOsmWayIdsUnwrapped()).containsExactly(3L, 2L, 1L);

		double kantenUebergangsLR = 40. / 90.;
		double kantenUebergangsLR2 = (40. + 30.) / 90.;

		assertThat(profileMatchingResult.getProfilEigenschaften())
			.usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
				.withComparatorForType(LineareReferenzTestProvider.comparatorWithTolerance(0.05),
					LinearReferenzierterAbschnitt.class).build())
			.containsExactly(
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.UNBEKANNT,
						Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(0, kantenUebergangsLR)
				),
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.NATURSTEINPFLASTER, Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(kantenUebergangsLR, kantenUebergangsLR2)
				),
				new LinearReferenzierteProfilEigenschaften(
					FahrradrouteProfilEigenschaften.of(BelagArt.WASSERGEBUNDENE_DECKE, Radverkehrsfuehrung.UNBEKANNT),
					LinearReferenzierterAbschnitt.of(kantenUebergangsLR2, 1)
				)
			);
	}
}
