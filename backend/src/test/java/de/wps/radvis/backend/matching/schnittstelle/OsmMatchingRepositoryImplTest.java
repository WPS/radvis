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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.mockito.Mockito;

import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
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

class OsmMatchingRepositoryImplTest {

	private String osmBasisDaten = "src/test/resources/test_freiburg.osm.pbf";
	private String cacheVerzeichnis = "target/test-routing-graph-cache";
	private String externeResourcenBasisPfad = "src/test/resources";
	private String mappingCacheVerzeichnis = "target/test-mapping-cache";

	OsmMatchingRepository osmMatchingRepository;
	private OsmMatchingCacheRepository osmMatchingCacheRepository;

	LineString testLineString;
	LineString vergleichsLineString;
	LineString lineStringAusserhalbGebiet;
	Point start;
	Point ende;

	// @formatter:off
	// LINESTRING(47.99848 7.853072,47.999428 7.853397,47.999909 7.853615,48.000899 7.854052,48.00149 7.85428,48.002129 7.854502,48.00301 7.854731,48.003481 7.854856,48.004431 7.85501,48.004943 7.855117,48.005259 7.855182,48.006173 7.855289,48.006154 7.856491,48.006106 7.857335,48.006068 7.858293,48.006051 7.859434,48.006034 7.859924)
 	// @formatter:on
	private String testLineStringInUTM32 = "LINESTRING (414468.21587206563 5316872.694639614, 414485.2725343314 5316925.912835875, 414519.5058246597 5317035.460292882, 414537.4892169263 5317100.893350286, 414555.1039603605 5317171.668008491, 414573.6400449512 5317269.331718679, 414583.7414200881 5317321.541844417, 414596.7964775269 5317426.95765979, 414605.6225423241 5317483.7446629815, 414610.9923038645 5317518.794050723, 414620.4816484315 5317620.260834424, 414710.1015687146 5317616.818420284, 414772.97230370087 5317610.550001946, 414844.36233679904 5317605.267763674, 414929.43611989124 5317602.118450434, 414965.95494705735 5317599.688345746)";

	private String testLineString2 = "LINESTRING (414491.43793057336006314 5316950.58865212462842464, 414519.48919378232676536 5317042.31751907803118229, 414666.28582668665330857 5316991.57898818794637918)";

	// @formatter:off
	// LINESTRING(47.999782 7.858311,47.999811 7.859116,48.00011 7.860138,48.000861 7.860593,48.001766 7.861084,48.003309 7.862052,48.003283 7.859731,48.004797 7.86119)
 	// @formatter:on
	private String vergleichsLineStringUTM32 = "LINESTRING(414835.357305946 5316906.59438541,414895.453410237 5316908.92854599,414972.180111932 5316941.03273123,415007.354015394 5317023.99981228,415045.465129512 5317124.0435485,415120.201258751 5317294.47183916,414947.036959004 5317294.14037891,415058.348647777 5317460.8031104)";

	private String lineStringAusserhalbGebietUTM32 = "LINESTRING(514835.357305946 6316906.59438541,514895.453410237 6316908.92854599)";
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
			externeResourcenBasisPfad, 60, extent, null, "test", "https://radvis-dev.landbw.de/");
		GraphhopperOsmConfigurationProperties graphhopperOsmConfigurationProperties = new GraphhopperOsmConfigurationProperties(
			cacheVerzeichnis, mappingCacheVerzeichnis, 0.6d);
		GraphhopperDlmConfigurationProperties graphhopperDlmConfigurationProperties = Mockito.mock(
			GraphhopperDlmConfigurationProperties.class);
		graphhopperServiceConfiguration = new MatchingConfiguration(
			new GeoConverterConfiguration(commonConfigurationProperties),
			commonConfigurationProperties,
			graphhopperOsmConfigurationProperties,
			new OsmPbfConfigurationProperties("test_angereichert.osm.pbf", osmBasisDaten, "https://someurl.com",
				0.0),
			graphhopperDlmConfigurationProperties);
		osmMatchingCacheRepository = graphhopperServiceConfiguration.osmMatchingCacheRepository();

		osmMatchingRepository = graphhopperServiceConfiguration
			.osmMatchingRepository(graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository));

		testLineString = createLineString(testLineStringInUTM32, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		vergleichsLineString = createLineString(vergleichsLineStringUTM32,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		lineStringAusserhalbGebiet = createLineString(lineStringAusserhalbGebietUTM32,
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
	void testMatching() throws KeinMatchGefundenException {
		// Arrange

		// Act
		LineString match = osmMatchingRepository.matchGeometry(testLineString, "bike");

		// Assert
		assertThat(match.getCoordinates()).hasSize(48);
	}

	@Test
	void matcheGeometrie_keinMatch() {

		// Act + Assert
		assertThatThrownBy(() -> {
			osmMatchingRepository.matchGeometry(lineStringAusserhalbGebiet, "bike");
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
		LineString match = osmMatchingRepository.matchGeometry(testLineString, "bike");

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
		osmMatchingRepository = new OsmMatchingRepositoryImpl(
			graphhopperServiceConfiguration.graphhopper(osmMatchingCacheRepository),
			coordinateReferenceSystemConverter,
			measurementErrorSigma);

		// Act
		LineString result = osmMatchingRepository.matchGeometry(testLineStringReferenzGeometrie, "bike");

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
			() -> osmMatchingRepository.matchGeometry(testLineStringVerschobenGeometrie, "bike"));
	}

	@Test
	void matchGeometry_gleicheGeometrieLiefertGleicheGeometrie() throws KeinMatchGefundenException {
		// Act
		LineString lineString = osmMatchingRepository.matchGeometry(testLineString, "bike");
		;
		LineString lineString2 = osmMatchingRepository.matchGeometry(testLineString, "bike");

		// Assert
		assertThat(lineString.equalsExact(lineString2)).isTrue();
	}

	@Test
	void matchGeometry_extrahiereWayIdsWithLR() throws KeinMatchGefundenException {
		// Arrange
		LinearReferenziertesOsmMatchResult match = osmMatchingRepository.matchGeometryLinearReferenziert(testLineString,
			"bike");

		// Act
		List<LinearReferenzierteOsmWayId> result = match.getLinearReferenzierteOsmWayIds();

		// Assert
		assertThat(result).isNotEmpty()
			.extracting(LinearReferenzierteOsmWayId::getValue)
			.containsExactlyInAnyOrder(529186113L, 529186112L,
				478639051L, 194244724L, 243995698L, 243995697L,
				243995700L, 245910628L, 245910630L, 245910640L,
				785592161L, 4229285L);
	}

	@Test
	void matchGeometry_LR() throws KeinMatchGefundenException, ParseException {
		// Arrange
		LineString lineString = createLineString(testLineString2,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		LinearReferenziertesOsmMatchResult match = osmMatchingRepository.matchGeometryLinearReferenziert(
			lineString, "foot");

		LineString lineStringReversed = lineString.reverse();
		LinearReferenziertesOsmMatchResult matchReversed = osmMatchingRepository.matchGeometryLinearReferenziert(
			lineStringReversed, "foot");

		// Act
		List<LinearReferenzierteOsmWayId> result = match.getLinearReferenzierteOsmWayIds();
		List<LinearReferenzierteOsmWayId> resultReversed = matchReversed.getLinearReferenzierteOsmWayIds();

		// Assert
		assertThat(result).usingComparatorForType(
			Comparator.comparing(LinearReferenzierteOsmWayId::getValue)
				.thenComparing(LinearReferenzierteOsmWayId::getLinearReferenzierterAbschnitt,
					LineareReferenzTestProvider.comparatorWithTolerance(0.0005)),
			LinearReferenzierteOsmWayId.class).containsExactlyInAnyOrderElementsOf(resultReversed);

		assertThat(result)
			.isNotEmpty()
			.usingComparatorForType(
				Comparator.comparing(LinearReferenzierteOsmWayId::getValue)
					.thenComparing(LinearReferenzierteOsmWayId::getLinearReferenzierterAbschnitt,
						LineareReferenzTestProvider.comparatorWithTolerance(0.0005)),
				LinearReferenzierteOsmWayId.class)
			.containsExactlyInAnyOrder(
				LinearReferenzierteOsmWayId.of(478639051L, LinearReferenzierterAbschnitt.of(0, 0.419)),
				LinearReferenzierteOsmWayId.of(30315551L, LinearReferenzierterAbschnitt.of(0, 0.955))
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
