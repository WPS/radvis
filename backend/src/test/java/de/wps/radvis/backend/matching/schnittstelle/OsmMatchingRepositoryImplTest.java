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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.mockito.Mockito;

import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.Path;
import com.graphhopper.util.PointList;

import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.OsmMatchingCacheRepository;
import de.wps.radvis.backend.matching.domain.OsmMatchingRepository;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.LinearReferenziertesOsmMatchResult;
import de.wps.radvis.backend.matching.schnittstelle.repositoryImpl.OsmMatchingRepositoryImpl;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class OsmMatchingRepositoryImplTest {

	private String osmBasisDaten = "src/test/resources/test_freiburg.osm.pbf";
	private String cacheVerzeichnis = "target/test-routing-graph-cache";
	private String externeResourcenBasisPfad = "src/test/resources";
	private String mappingCacheVerzeichnis = "target/test-mapping-cache";

	OsmMatchingRepository osmMatchingRepository;
	private OsmMatchingCacheRepository osmMatchingCacheRepository;

	// @formatter:off
	// LINESTRING(47.99848 7.853072,47.999428 7.853397,47.999909 7.853615,48.000899 7.854052,48.00149 7.85428,48.002129 7.854502,48.00301 7.854731,48.003481 7.854856,48.004431 7.85501,48.004943 7.855117,48.005259 7.855182,48.006173 7.855289,48.006154 7.856491,48.006106 7.857335,48.006068 7.858293,48.006051 7.859434,48.006034 7.859924)
 	// @formatter:on
	private String testLineStringWkt = "LINESTRING (414468.21587206563 5316872.694639614, 414485.2725343314 5316925.912835875, 414519.5058246597 5317035.460292882, 414537.4892169263 5317100.893350286, 414555.1039603605 5317171.668008491, 414573.6400449512 5317269.331718679, 414583.7414200881 5317321.541844417, 414596.7964775269 5317426.95765979, 414605.6225423241 5317483.7446629815, 414610.9923038645 5317518.794050723, 414620.4816484315 5317620.260834424, 414710.1015687146 5317616.818420284, 414772.97230370087 5317610.550001946, 414844.36233679904 5317605.267763674, 414929.43611989124 5317602.118450434, 414965.95494705735 5317599.688345746)";
	LineString testLineString;

	// @formatter:off
	// LINESTRING(47.999782 7.858311,47.999811 7.859116,48.00011 7.860138,48.000861 7.860593,48.001766 7.861084,48.003309 7.862052,48.003283 7.859731,48.004797 7.86119)
 	// @formatter:on
	private String vergleichsLineStringWkt = "LINESTRING(414835.357305946 5316906.59438541,414895.453410237 5316908.92854599,414972.180111932 5316941.03273123,415007.354015394 5317023.99981228,415045.465129512 5317124.0435485,415120.201258751 5317294.47183916,414947.036959004 5317294.14037891,415058.348647777 5317460.8031104)";
	LineString vergleichsLineString;

	private String lineStringAusserhalbGebietWkt = "LINESTRING(514835.357305946 6316906.59438541,514895.453410237 6316908.92854599)";
	LineString lineStringAusserhalbGebiet;

	private MatchingConfiguration graphhopperServiceConfiguration;

	private final CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
		new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95)));

	@BeforeEach
	void setUp() throws Exception {
		deleteRecursively(cacheVerzeichnis);

		ExtentProperty extent = new ExtentProperty(492846.960, 500021.252, 5400410.543, 5418644.476);
		CommonConfigurationProperties commonConfigurationProperties = new CommonConfigurationProperties(
			externeResourcenBasisPfad,
			60,
			extent,
			null,
			"test",
			"https://radvis-dev.landbw.de/",
			"DLM", "Baden-Württemberg", OrganisationsArt.BUNDESLAND, "resources");
		GraphhopperOsmConfigurationProperties graphhopperOsmConfigurationProperties = new GraphhopperOsmConfigurationProperties(
			cacheVerzeichnis, mappingCacheVerzeichnis, 0.6d);
		GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties = Mockito.mock(
			GraphhopperDlmConfigurationProperties.class);
		graphhopperServiceConfiguration = new MatchingConfiguration(
			new GeoConverterConfiguration(commonConfigurationProperties),
			commonConfigurationProperties,
			graphhopperOsmConfigurationProperties,
			new OsmPbfConfigurationProperties("test_angereichert.osm.pbf", "test_osm-basisnetz.osm.pbf", osmBasisDaten,
				"https://someurl.com",
				0.0),
			graphhopperDlmConfigurationProperties);
		osmMatchingCacheRepository = graphhopperServiceConfiguration.osmMatchingCacheRepository();

		osmMatchingRepository = graphhopperServiceConfiguration
			.osmMatchingRepository(graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository));

		testLineString = createLineString(testLineStringWkt, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		vergleichsLineString = createLineString(vergleichsLineStringWkt,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		lineStringAusserhalbGebiet = createLineString(lineStringAusserhalbGebietWkt,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
	}

	private void deleteRecursively(String verzeichnisPfad) throws IOException {
		File graphhopperCacheVerzeichnis = new File(verzeichnisPfad);
		if (graphhopperCacheVerzeichnis.exists()) {
			Arrays.stream(graphhopperCacheVerzeichnis.listFiles()).forEach(File::delete);
			graphhopperCacheVerzeichnis.delete();
		}
	}

	@Test
	void extrahiereLineString_returnsCorrectCoordinates() throws ParseException {
		// Arrange
		LineString quellLineString = (LineString) CoordinateReferenceSystemConverterUtility.transformGeometry(
			createLineString(testLineStringWkt, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()),
			KoordinatenReferenzSystem.WGS84);

		PointList pointList = new PointList();
		Arrays.stream(quellLineString.getCoordinates()).forEach(coordinate -> {
			pointList.add(coordinate.x, coordinate.y);
		});

		Path path = Mockito.mock(Path.class);
		when(path.calcPoints()).thenReturn(pointList);

		MatchResult matchResult = Mockito.mock(MatchResult.class);
		when(matchResult.getMergedPath()).thenReturn(path);

		LineString expectedLineString = testLineString;

		// Act
		LineString actualLineString = osmMatchingRepository.extrahiereLineString(matchResult);

		// Assert
		assertThat(actualLineString.equalsExact(expectedLineString, OsmMatchingRepository.LINE_STRING_EQUAL_TOLERANCE));
	}

	@Test
	void extrahiereLineareReferenzierung_containsCorrectIds() throws KeinMatchGefundenException {
		// Arrange
		MatchResult matchResult = osmMatchingRepository.matchGeometry(testLineString);

		// Act
		LinearReferenziertesOsmMatchResult match = osmMatchingRepository.extrahiereLineareReferenzierung(matchResult);

		// Assert
		List<LinearReferenzierteOsmWayId> result = match.getLinearReferenzierteOsmWayIds();
		assertThat(result).isNotEmpty()
			.extracting(LinearReferenzierteOsmWayId::getValue)
			.containsExactlyInAnyOrder(529186113L, 529186112L,
				478639051L, 194244724L, 243995698L, 243995697L,
				243995700L, 245910628L, 245910630L, 245910640L,
				785592161L, 4229285L);
	}

	@Test
	void matchGeometry_matchHasCorrectSize() throws KeinMatchGefundenException {
		// Act
		MatchResult match = osmMatchingRepository.matchGeometry(testLineString);

		// Assert
		LineString matchGeometry = osmMatchingRepository.extrahiereLineString(match);
		assertThat(matchGeometry.getCoordinates()).hasSize(48);
	}

	@Test
	void matchGeometry_keinMatchGefundenThrowsException() {
		// Act + Assert
		assertThatThrownBy(() -> {
			osmMatchingRepository.matchGeometry(lineStringAusserhalbGebiet);
		}).isInstanceOf(KeinMatchGefundenException.class)
			.hasMessageStartingWith(
				"Der LineString konnte nicht gematched werden. Dies liegt beispielsweise daran, dass die Geometrie oder"
					+ " Teile nicht Teil der importierten OsmBasiskarte sind oder dass die Geometrie keine passendes"
					+ " Pendant in den OSM-Daten hat");
	}

	@SuppressWarnings("unchecked")
	@Test
	void matchGeometry_resultGeometryHasCorrectLocation() throws KeinMatchGefundenException {
		// Act
		MatchResult match = osmMatchingRepository.matchGeometry(testLineString);

		// Assert
		assertThat(match).isNotNull();

		LineString matchGeometry = osmMatchingRepository.extrahiereLineString(match);
		boolean isContained = matchGeometry.within(testLineString.buffer(10));
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
		osmMatchingRepository = new OsmMatchingRepositoryImpl(
			graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository),
			coordinateReferenceSystemConverter,
			measurementErrorSigma);

		// Act
		MatchResult result = osmMatchingRepository.matchGeometry(testLineStringReferenzGeometrie);

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
		osmMatchingRepository = new OsmMatchingRepositoryImpl(
			graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository),
			coordinateReferenceSystemConverter,
			measurementErrorSigma);

		// Act
		// Assert
		assertThrows(KeinMatchGefundenException.class,
			() -> osmMatchingRepository.matchGeometry(testLineStringVerschobenGeometrie));
	}

	@Test
	void matchGeometry_gleicheGeometrieLiefertGleicheGeometrie() throws KeinMatchGefundenException {
		// Act
		MatchResult match1 = osmMatchingRepository.matchGeometry(testLineString);
		MatchResult match2 = osmMatchingRepository.matchGeometry(testLineString);

		// Assert
		LineString matchGeometry1 = osmMatchingRepository.extrahiereLineString(match1);
		LineString matchGeometry2 = osmMatchingRepository.extrahiereLineString(match2);
		assertThat(matchGeometry1.equalsExact(matchGeometry2)).isTrue();
	}

	@Test
	void matchGeometryLinearReferenziert_strasseMitZweiEinseitigBefahrbarenKanten_gegenrichtungUnterschiedlich()
		throws KeinMatchGefundenException, ParseException {
		// Arrange
		String testLineString = "LINESTRING (414491.43793057336006314 5316950.58865212462842464, 414519.48919378232676536 5317042.31751907803118229)";

		LineString lineString = createLineString(testLineString, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		LineString lineStringReversed = lineString.reverse();

		// Act
		MatchResult matchResult = osmMatchingRepository.matchGeometry(lineString);
		MatchResult matchResultReversed = osmMatchingRepository.matchGeometry(lineStringReversed);

		// Assert
		LinearReferenziertesOsmMatchResult match = osmMatchingRepository.extrahiereLineareReferenzierung(matchResult);
		LinearReferenziertesOsmMatchResult matchReversed = osmMatchingRepository.extrahiereLineareReferenzierung(
			matchResultReversed);

		List<LinearReferenzierteOsmWayId> result = match.getLinearReferenzierteOsmWayIds();
		List<LinearReferenzierteOsmWayId> resultReversed = matchReversed.getLinearReferenzierteOsmWayIds();
		List<LinearReferenzierteOsmWayId> expectedResult = List.of(
			LinearReferenzierteOsmWayId.of(529186112L, LinearReferenzierterAbschnitt.of(0.963, 1)),
			LinearReferenzierteOsmWayId.of(478639051, LinearReferenzierterAbschnitt.of(0, 0.417)));
		List<LinearReferenzierteOsmWayId> expectedResultReversed = List.of(
			LinearReferenzierteOsmWayId.of(245910641L, LinearReferenzierterAbschnitt.of(0.582, 1)),
			LinearReferenzierteOsmWayId.of(478657380L, LinearReferenzierterAbschnitt.of(0, 0.1)),
			LinearReferenzierteOsmWayId.of(43846689L, LinearReferenzierterAbschnitt.of(0, 1)),
			LinearReferenzierteOsmWayId.of(529186112L, LinearReferenzierterAbschnitt.of(0, 0.963)));

		assertThat(result).usingComparatorForType(
			Comparator.comparing(LinearReferenzierteOsmWayId::getValue)
				.thenComparing(LinearReferenzierteOsmWayId::getLinearReferenzierterAbschnitt,
					LineareReferenzTestProvider.comparatorWithTolerance(0.0005)),
			LinearReferenzierteOsmWayId.class).containsExactlyInAnyOrderElementsOf(expectedResult);
		assertThat(resultReversed).usingComparatorForType(
			Comparator.comparing(LinearReferenzierteOsmWayId::getValue)
				.thenComparing(LinearReferenzierteOsmWayId::getLinearReferenzierterAbschnitt,
					LineareReferenzTestProvider.comparatorWithTolerance(0.0005)),
			LinearReferenzierteOsmWayId.class).containsExactlyInAnyOrderElementsOf(expectedResultReversed);
	}

	/**
	 * Wir definieren Dummy-DLM-Kanten im PBF-FIle (siehe Setup). Diese MÜSSEN mit dem großen Hauptnetz verbunden sein,
	 * denn kleine Subnetze werden vom GraphHopper ignoriert. Das hier getestete Verhalten wird definiert im
	 * CustomBikeFlagEncoder.
	 */
	@Test
	void matchGeometry_nichtBefahrbareKanten_cyclewayTags_keinMatching() throws ParseException {
		// Arrange
		String lineStringCyclewaySeparate = "LINESTRING (409244.114 5322933.882, 410241.826 5322915.986)";
		String lineStringCyclewayBothSeparate = "LINESTRING (409266.187 5323217.359, 410263.850 5323199.467)";
		String lineStringCyclewayLeftSeparateRightSeparate = "LINESTRING (409245.407 5322611.201, 410243.175 5322593.307)";
		String lineStringCyclewayLaneBicycleNo = "LINESTRING (409246.544 5321530.785, 410244.499 5321512.898)";
		String lineStringCyclewayLaneBicycleNo_reverse = "LINESTRING (410244.499 5321512.898, 409246.544 5321530.785)";
		String lineStringCyclewayLeftSeparateBicycleOnewayNo_reverse = "LINESTRING (410233.915 5320543.317, 409235.792 5320561.204)";
		String lineStringCyclewayLaneOnewayYes_reverse = "LINESTRING (410234.886 5321189.818, 409236.875 5321207.706)";

		List<String> dummyDlmKantenStrings = List.of(
			lineStringCyclewaySeparate,
			lineStringCyclewayBothSeparate,
			lineStringCyclewayLeftSeparateRightSeparate,
			lineStringCyclewayLaneBicycleNo,
			lineStringCyclewayLaneBicycleNo_reverse,
			lineStringCyclewayLeftSeparateBicycleOnewayNo_reverse,
			lineStringCyclewayLaneOnewayYes_reverse);

		List<LineString> testLineStrings = new ArrayList<>();
		for (String dummyDlmKantenString : dummyDlmKantenStrings) {
			testLineStrings.add(createLineString(
				dummyDlmKantenString,
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
		}

		// Act + Assert
		for (int i = 0; i < testLineStrings.size(); i++) {
			log.info("Match linestring " + i);
			LineString lineString = testLineStrings.get(i);
			assertThrows(
				KeinMatchGefundenException.class,
				() -> osmMatchingRepository.matchGeometry(lineString));
		}
	}

	@Test
	void matchGeometry_befahrbareKanten_cyclewayTags_korrektesMatching()
		throws ParseException, KeinMatchGefundenException {
		// Arrange

		// Diese Quellgeometrien wurden absichtlich um ein kleines bisschen verschoben, sodass sie nicht exakt auf die
		// Kanten in der PBF-Datei passen. Es sollen aber natürlich trotzdem alle matches korrekt gefunden werden.
		String lineStringCyclewayLane = "LINESTRING (410246.141 5321862.317, 409248.246 5321880.210)";
		String lineStringCyclewayLeftSeparateRightLane = "LINESTRING (409275.144 5324390.635, 410272.603 5324372.743)";
		String lineStringCyclewayLeftNoTagRightSeparate_reverse = "LINESTRING (410267.219 5324028.480, 409269.700 5324046.373)";
		String lineStringCyclewayLeftSeparateRightHasNoTag = "LINESTRING (409280.596 5323724.153, 410278.171 5323706.263)";
		String lineStringCyclewaySidepath = "LINESTRING (409294.664 5325018.390, 410292.014 5325000.500)";
		String lineStringCyclewayLaneOnewayYes = "LINESTRING (409236.875 5321207.706, 410234.886 5321189.818)";
		String lineStringCyclewayLeftSeparateBicycleOnewayNo = "LINESTRING (409235.792 5320561.204, 410233.915 5320543.317)";
		String lineStringCyclewayLaneBicycleOnewayNo = "LINESTRING (409240.827 5320879.842, 410238.896 5320861.956)";
		String lineStringCyclewayLaneBicycleOnewayNo_reverse = "LINESTRING (410238.896 5320861.956, 409240.827 5320879.842)";

		List<String> dummyDlmKantenStrings = List.of(
			lineStringCyclewayLane,
			lineStringCyclewayLeftSeparateRightLane,
			lineStringCyclewayLeftNoTagRightSeparate_reverse,
			lineStringCyclewayLeftSeparateRightHasNoTag,
			lineStringCyclewaySidepath,
			lineStringCyclewayLaneOnewayYes,
			lineStringCyclewayLeftSeparateBicycleOnewayNo,
			lineStringCyclewayLaneBicycleOnewayNo,
			lineStringCyclewayLaneBicycleOnewayNo_reverse);

		List<LineString> testLineStrings = new ArrayList<>();
		for (int i = 0; i < dummyDlmKantenStrings.size(); i++) {
			log.info("Convert WKT to linestring " + i);
			String dummyDlmKantenString = dummyDlmKantenStrings.get(i);
			testLineStrings.add(createLineString(
				dummyDlmKantenString,
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
		}

		// Act
		List<LineString> matches = new ArrayList<>();
		for (int i = 0; i < testLineStrings.size(); i++) {
			log.info("Match linestring " + i);
			LineString lineString = testLineStrings.get(i);
			matches.add(osmMatchingRepository.extrahiereLineString(osmMatchingRepository.matchGeometry(lineString)));
		}

		// Assert
		for (int i = 0; i < testLineStrings.size(); i++) {
			log.info("Check linestring " + i + " --> " + matches.get(i).toString());
			// "similar"-Vergleich, da Quell-Geometrien absichtlich verschoben wurden (s.o.).
			assertThatGeometriesAreSimilar(matches.get(i).getCoordinates(), testLineStrings.get(i).getCoordinates(),
				0.01);
		}
	}

	@Test
	void matchGeometry_nichtBefahrbareKanten_accessTags_keinMatching() throws ParseException {
		// Arrange
		String lineStringBicycleNoCyclewayLane = "LINESTRING (412100.370 5323229.097, 412994.881 5323214.477)";
		String lineStringBicyclePrivateCyclewayLane = "LINESTRING (412091.140 5322924.309, 412985.699 5322909.688)";
		String lineStringAccessNoCyclewayLane = "LINESTRING (412095.390 5322605.640, 412989.999 5322591.032)";
		String lineStringAccessPrivateCyclewayLane = "LINESTRING (412095.492 5322314.339, 412990.145 5322299.720)";

		List<String> dummyDlmKantenStrings = List.of(
			lineStringBicycleNoCyclewayLane,
			lineStringBicyclePrivateCyclewayLane,
			lineStringAccessNoCyclewayLane,
			lineStringAccessPrivateCyclewayLane);

		List<LineString> testLineStrings = new ArrayList<>();
		for (String dummyDlmKantenString : dummyDlmKantenStrings) {
			testLineStrings.add(createLineString(
				dummyDlmKantenString,
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
		}

		// Act + Assert
		for (int i = 0; i < testLineStrings.size(); i++) {
			log.info("Match linestring " + i);
			LineString lineString = testLineStrings.get(i);
			assertThrows(
				KeinMatchGefundenException.class,
				() -> osmMatchingRepository.matchGeometry(lineString));
		}
	}

	@Test
	void matchGeometry_shouldMatchParallelCycleway() throws ParseException, KeinMatchGefundenException {
		// Arrange

		// Diese Quellgeometrien wurden absichtlich um ein kleines bisschen verschoben, sodass sie nicht exakt auf die
		// Kanten in der PBF-Datei passen. Es sollen aber natürlich trotzdem alle matches korrekt gefunden werden.
		String roadLineStringWkt = "LINESTRING (412074.631 5321697.863, 412969.381 5321683.253)";
		String cyclewayLineStringWkt = "LINESTRING (412074.800 5321692.324, 412524.509 5321684.066, 412968.077 5321674.935)";

		LineString roadLineString = createLineString(roadLineStringWkt,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		LineString cyclewayLineString = createLineString(cyclewayLineStringWkt,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		// Act
		MatchResult match = osmMatchingRepository.matchGeometry(roadLineString);

		// Assert
		LineString matchGeometry = osmMatchingRepository.extrahiereLineString(match);
		// "similar"-Vergleich, da Quell-Geometrien absichtlich verschoben wurden (s.o.).
		assertThatGeometriesAreSimilar(matchGeometry.getCoordinates(), cyclewayLineString.getCoordinates(), 0.01);
	}

	private static GeometryFactory createGeometryFactory(Integer srid) {
		return KoordinatenReferenzSystem.ofSrid(srid).getGeometryFactory();
	}

	private static LineString createLineString(final String lineWkt, Integer srid) throws ParseException {
		WKTReader reader = new WKTReader(createGeometryFactory(srid));
		return (LineString) reader.read(lineWkt);
	}

	/**
	 * Vergleicht Koordinaten von Geometrien mit einer erlaubten durchschnittlichen Abweichung, da durch projektionen
	 * die Koordinaten (z.B. zwischen PBF- und Testdaten) leicht abweichen können, obwohl der gleiche LineString o.Ä.
	 * gemeint ist.
	 */
	private void assertThatGeometriesAreEqual(Coordinate[] coordinatesA, Coordinate[] coordinatesB) {
		assertThatGeometriesAreSimilar(coordinatesA, coordinatesB, OsmMatchingRepository.LINE_STRING_EQUAL_TOLERANCE);
	}

	private static void assertThatGeometriesAreSimilar(Coordinate[] coordinatesA, Coordinate[] coordinatesB,
		double similarityThreshold) {
		assertThat(coordinatesA.length).isEqualTo(coordinatesB.length);

		double cummulatedError = 0d;

		for (int i = 0; i < coordinatesA.length; i++) {
			Coordinate coordA = coordinatesA[i];
			Coordinate coordB = coordinatesB[i];
			cummulatedError += coordA.distance(coordB);
		}

		double similarity = cummulatedError / coordinatesA.length;
		if (similarity < similarityThreshold) {
			log.info("Similarity: {}", similarity);
		}
		assertThat(similarity).isLessThan(similarityThreshold);
	}
}
