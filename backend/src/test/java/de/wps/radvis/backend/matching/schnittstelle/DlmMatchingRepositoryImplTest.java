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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopper;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchedGraphHopperFactory;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.DlmMatchingRepositoryImpl;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;

class DlmMatchingRepositoryImplTest {

	@Mock
	private GraphhopperOsmConfigurationProperties graphhopperOsmConfigurationProperties;

	@Mock
	private OsmPbfConfigurationProperties osmPbfConfigurationProperties;

	@Mock
	private CommonConfigurationProperties commonConfigurationProperties;

	@TempDir
	public File temp;

	DlmMatchingRepository dlmMatchingRepository;
	private MatchingConfiguration graphhopperServiceConfiguration;

	LineString testLineString;
	LineString vergleichsLineString;
	LineString lineStringAusserhalbGebiet;

	// @formatter:off
	// LINESTRING(47.99848 7.853072,47.999428 7.853397,47.999909 7.853615,48.000899 7.854052,48.00149 7.85428,48.002129 7.854502,48.00301 7.854731,48.003481 7.854856,48.004431 7.85501,48.004943 7.855117,48.005259 7.855182,48.006173 7.855289,48.006154 7.856491,48.006106 7.857335,48.006068 7.858293,48.006051 7.859434,48.006034 7.859924)
	// @formatter:on
	private String testLineStringInUTM32 = "LINESTRING (414468.21587206563 5316872.694639614, 414485.2725343314 5316925.912835875, 414519.5058246597 5317035.460292882, 414537.4892169263 5317100.893350286, 414555.1039603605 5317171.668008491, 414573.6400449512 5317269.331718679, 414583.7414200881 5317321.541844417, 414596.7964775269 5317426.95765979, 414605.6225423241 5317483.7446629815, 414610.9923038645 5317518.794050723, 414620.4816484315 5317620.260834424, 414710.1015687146 5317616.818420284, 414772.97230370087 5317610.550001946, 414844.36233679904 5317605.267763674, 414929.43611989124 5317602.118450434, 414965.95494705735 5317599.688345746)";

	// @formatter:off
	// LINESTRING(47.999782 7.858311,47.999811 7.859116,48.00011 7.860138,48.000861 7.860593,48.001766 7.861084,48.003309 7.862052,48.003283 7.859731,48.004797 7.86119)
	// @formatter:on
	private final String vergleichsLineStringUTM32 = "LINESTRING(414835.357305946 5316906.59438541,414895.453410237 5316908.92854599,414972.180111932 5316941.03273123,415007.354015394 5317023.99981228,415045.465129512 5317124.0435485,415120.201258751 5317294.47183916,414947.036959004 5317294.14037891,415058.348647777 5317460.8031104)";

	private String lineStringAusserhalbGebietUTM32 = "LINESTRING(514835.357305946 6316906.59438541,514895.453410237 6316908.92854599)";
	private DlmMatchedGraphHopperFactory graphhopperFactory;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);

		Mockito.when(commonConfigurationProperties.getExterneResourcenBasisPfad()).thenReturn("");

		File tiffTile = new File(temp, "/test-tiff-tiles");
		tiffTile.mkdir();

		GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties = new GraphhopperDlmConfigurationProperties(
			"src/test/resources/test_freiburg.osm.pbf",
			new File(temp, "test-routing-graph-cache").getAbsolutePath(),
			new File(temp, "test-mapping-graph-cache").getAbsolutePath(), 1d,
			new File(temp, "/test-elevation-cache").getAbsolutePath(),
			tiffTile.getPath());
		graphhopperServiceConfiguration = new MatchingConfiguration(
			new GeoConverterConfiguration(commonConfigurationProperties),
			commonConfigurationProperties,
			graphhopperOsmConfigurationProperties,
			osmPbfConfigurationProperties,
			graphhopperDlmConfigurationProperties);

		graphhopperFactory = spy(graphhopperServiceConfiguration.dlmMatchedGraphHopperFactory());
		dlmMatchingRepository = new DlmMatchingRepositoryImpl(graphhopperFactory,
			new CoordinateReferenceSystemConverter(commonConfigurationProperties.getBadenWuerttembergEnvelope()),
			graphhopperDlmConfigurationProperties.getMeasurementErrorSigma());

		testLineString = createLineString(testLineStringInUTM32, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		vergleichsLineString = createLineString(vergleichsLineStringUTM32,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		lineStringAusserhalbGebiet = createLineString(lineStringAusserhalbGebietUTM32,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
	}

	@Test
	void testMatching() throws KeinMatchGefundenException {
		// Act
		LineString match = dlmMatchingRepository.matchGeometry(testLineString, "bike").getGeometrie();

		// Assert
		assertThat(match.getCoordinates()).hasSize(48);
	}

	@Test
	void testMatching_concurrency() throws InterruptedException, ExecutionException {
		// Act & Assert
		Future<?> future1;
		Future<?> future2;
		ExecutorService es = null;
		try {
			es = Executors.newFixedThreadPool(2);
			future1 = es.submit(() -> {
				assertThatNoException().isThrownBy(
					() -> {
						for (int i = 0; i < 100; i++) {
							dlmMatchingRepository.matchGeometry(testLineString, "bike").getGeometrie();
						}
					});
				return null;
			});
			Thread.sleep(10);

			future2 = es.submit(() -> {
				assertThatNoException().isThrownBy(
					() -> {
						for (int i = 0; i < 100; i++) {
							dlmMatchingRepository.matchGeometry(vergleichsLineString, "bike").getGeometrie();
						}
					});
				return null;
			});
		} finally {
			if (es != null)
				es.shutdown();
		}

		future1.get();
		future2.get();
	}

	@Test
	void matcheGeometrie_keinMatch() {

		// Act + Assert
		assertThatThrownBy(() -> {
			dlmMatchingRepository.matchGeometry(lineStringAusserhalbGebiet, "bike");
		}).isInstanceOf(KeinMatchGefundenException.class)
			.hasMessageStartingWith(
				"Der LineString konnte nicht gematched werden. Dies liegt beispielsweise daran, dass die Geometrie oder"
					+ " Teile nicht Teil der importierten OsmBasiskarte sind oder dass die Geometrie keine passendes"
					+ " Pendant in den OSM-Daten hat");
	}

	@SuppressWarnings("unchecked")
	@Test
	void extrahiereLineString() throws KeinMatchGefundenException {

		// Act
		LineString match = dlmMatchingRepository.matchGeometry(testLineString, "bike").getGeometrie();

		boolean isContained = match.within(testLineString.buffer(10));

		// Assert
		assertThat(match).isNotNull();
		assertTrue(isContained);
	}

	@SuppressWarnings("unchecked")
	@Test
	void matchGeometry_abhaengigkeitVonMeasurementErrorSigma_match()
		throws ParseException, KeinMatchGefundenException {
		// Arrange

		String testLineStringReferenz = "LINESTRING (414467.456862417 5316872.920001071, 414482.8370156474 5316926.615656143)";
		LineString testLineStringReferenzGeometrie = createLineString(testLineStringReferenz,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		double measurementErrorSigma = 1.0;

		dlmMatchingRepository = new DlmMatchingRepositoryImpl(
			graphhopperServiceConfiguration.dlmMatchedGraphHopperFactory(),
			new CoordinateReferenceSystemConverter(commonConfigurationProperties.getBadenWuerttembergEnvelope()),
			measurementErrorSigma);

		// Act
		LineString result = dlmMatchingRepository.matchGeometry(testLineStringReferenzGeometrie, "bike").getGeometrie();

		// Assert
		assertThat(result).isNotNull();
	}

	@Test
	void matchGeometry_abhaengigkeitVonMeasurementErrorSigma_keinMatch() throws ParseException {
		// Arrange
		String testLineStringVerschoben = "LINESTRING (414468.456862417 5316873.920001071, 414483.8370156474 5316927.615656143)";
		LineString testLineStringVerschobenGeometrie = createLineString(testLineStringVerschoben,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		double measurementErrorSigma = 0.013;
		dlmMatchingRepository = new DlmMatchingRepositoryImpl(
			graphhopperServiceConfiguration.dlmMatchedGraphHopperFactory(),
			new CoordinateReferenceSystemConverter(commonConfigurationProperties.getBadenWuerttembergEnvelope()),
			measurementErrorSigma);

		// Act
		// Assert
		assertThrows(KeinMatchGefundenException.class,
			() -> dlmMatchingRepository.matchGeometry(testLineStringVerschobenGeometrie, "bike"));
	}

	@Test
	void matchGeometry_gleicheGeometrieLiefertGleicheGeometrie() throws KeinMatchGefundenException {
		// Act
		LineString match = dlmMatchingRepository.matchGeometry(testLineString, "bike").getGeometrie();
		LineString match2 = dlmMatchingRepository.matchGeometry(testLineString, "bike").getGeometrie();

		// Assert
		assertThat(match.equalsExact(match2)).isTrue();
	}

	@Test
	void testSwapGraphhopper_oldGraphhopperClosedAndCleaned_MatchingStillWorks() throws KeinMatchGefundenException {
		// Arrange
		DlmMatchedGraphHopper oldGraphHopperSpy = spy(
			(DlmMatchedGraphHopper) Objects.requireNonNull(ReflectionTestUtils.getField(graphhopperFactory,
				"currentGraphhopper")));
		ReflectionTestUtils.setField(dlmMatchingRepository, "graphHopper", oldGraphHopperSpy);
		ReflectionTestUtils.setField(graphhopperFactory, "currentGraphhopper", oldGraphHopperSpy);

		// Act
		graphhopperFactory.updateDlmGraphHopper();
		dlmMatchingRepository.swapGraphHopper();

		// Assert
		assertThat(oldGraphHopperSpy).isNotSameAs(ReflectionTestUtils.getField(dlmMatchingRepository,
			"graphHopper"));

		assertThat(dlmMatchingRepository.matchGeometry(testLineString, "bike")).isNotNull();
	}

	@Test
	void testMatch_inReihenfolgeUndMitDuplikaten() throws ParseException, KeinMatchGefundenException {
		LineString lineStringTestUTM32 = createLineString(
			"LINESTRING(414084.906554157845676 5327145.883031556382775,414241.671186204534024 5327352.496770462952554,414326.90781082957983 5327289.282404191792011,414392.229230600700248 5327235.895945344120264,414451.818164336902555 5327195.528266977518797,414560.45589438220486 5327325.274237154982984,414458.099167378502898 5327188.62797656096518,414508.459387594950385 5327140.911447181366384)",
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		OsmMatchResult matchedForBike = dlmMatchingRepository.matchGeometry(lineStringTestUTM32, "bike");
		assertThat(matchedForBike.getOsmWayIdsAsOrderedList()).containsExactly(
			OsmWayId.of(227973492),
			OsmWayId.of(27086341),
			OsmWayId.of(28578401),
			OsmWayId.of(28578401),
			OsmWayId.of(27086341)
		);
	}

	private static GeometryFactory createGeometryFactory(Integer srid) {
		return KoordinatenReferenzSystem.ofSrid(srid).getGeometryFactory();
	}

	private static LineString createLineString(final String lineWkt, Integer srid) throws ParseException {
		WKTReader reader = new WKTReader(createGeometryFactory(srid));
		return (LineString) reader.read(lineWkt);
	}

}
