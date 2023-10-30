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

package de.wps.radvis.backend.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.mockito.MockitoAnnotations;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;

import de.wps.radvis.backend.common.domain.exception.ReadGeoJSONException;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.GeoJsonImportRepositoryImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class GeoJsonImportRepositoryTest {
	private GeoJsonImportRepository geoJsonImportRepository;

	@BeforeEach
	void setUp() throws IOException {
		MockitoAnnotations.openMocks(this);
		Envelope badenWuerttembergEnvelope = new Envelope(
			new Coordinate(378073.54, 5255657.09),
			new Coordinate(633191.12, 5534702.95));
		CoordinateReferenceSystemConverter converter = new CoordinateReferenceSystemConverter(
			badenWuerttembergEnvelope);
		geoJsonImportRepository = new GeoJsonImportRepositoryImpl(converter);
	}

	@Test
	void getSimpleFeatures() throws IOException, ReadGeoJSONException {
		// act
		URL url = (new File("src/test/resources/testWegweisendeBeschilderung.geojson")).toURI().toURL();
		String fileContentAsString = geoJsonImportRepository.getFileContentAsString(url);
		List<SimpleFeature> features = geoJsonImportRepository.getSimpleFeatures(fileContentAsString);

		// assert
		assertThat(features).hasSize(2);
		SimpleFeature simpleFeature0 = features.get(0);
		assertThat(simpleFeature0.getAttribute("Knoten")).isEqualTo(711201986);
		Geometry geometry0 = (Geometry) simpleFeature0.getDefaultGeometry();
		assertThat(geometry0.getGeometryType()).isEqualTo("Point");
		assertThat(geometry0.getCoordinates()).containsExactly(new Coordinate(558945.281669753, 5350346.047167036));

		SimpleFeature simpleFeature1 = features.get(1);
		assertThat(simpleFeature1.getAttribute("Knoten")).isEqualTo(710301236);
		Geometry geometry1 = (Geometry) simpleFeature1.getDefaultGeometry();
		assertThat(geometry1.getGeometryType()).isEqualTo("Point");
		assertThat(geometry1.getCoordinates()).containsExactly(new Coordinate(558294.6895211033, 5350546.995618995));
	}

	@Test
	@Disabled
	void getSimpleFeatures_realRemoteUrl() throws IOException, ReadGeoJSONException {
		URL url = new URL("https://filedn.com/lSXztajxYX5m843NUKYXTVH/RadVIS_VPINFO/Pfosten.geojson");
		String fileContentAsString = geoJsonImportRepository.getFileContentAsString(url);
		List<SimpleFeature> features = geoJsonImportRepository.getSimpleFeatures(fileContentAsString);
		log.info(features.size() + " gefunden");
	}

	@Test
	void getSimpleFeatures_fileHasPropertiesThatAreInkonsistent_addAllKeysWithEmptyStrings()
		throws IOException, ReadGeoJSONException {
		// arrange and act
		URL url = (new File("src/test/resources/testWegweisendeBeschilderungInkonsistenteProperties.geojson")).toURI()
			.toURL();
		String fileContentAsString = geoJsonImportRepository.getFileContentAsString(url);
		List<SimpleFeature> features = geoJsonImportRepository.getSimpleFeatures(fileContentAsString);

		// assert
		assertThat(features).hasSize(3);

		SimpleFeature f1 = features.get(0);
		SimpleFeature f2 = features.get(1);
		SimpleFeature f3 = features.get(2);

		assertThat(
			f1.getFeatureType().getTypes().stream().map(t -> t.getName().toString()).collect(Collectors.toList()))
			.containsExactlyInAnyOrder(
				"geometry",
				"Farbe",
				"Knoten",
				"PfostenNr"
			);
		assertThat(
			f2.getFeatureType().getTypes().stream().map(t -> t.getName().toString()).collect(Collectors.toList()))
			.containsExactlyInAnyOrder(
				"geometry",
				"Farbe",
				"Knoten",
				"PfostenNr"
			);
		assertThat(
			f3.getFeatureType().getTypes().stream().map(t -> t.getName().toString()).collect(Collectors.toList()))
			.containsExactlyInAnyOrder(
				"geometry",
				"Farbe",
				"Knoten",
				"PfostenNr"
			);

		// "Farbe" existiert nur im dritten Feature in der Datei
		// → Trotzdem konnte ein Feature mit dem Attribut Farbe importiert werden.
		assertThat(f1.getAttribute("Farbe")).isNull();
		assertThat(f2.getAttribute("Farbe")).isNull();
		assertThat(f3.getAttribute("Farbe")).isEqualTo("Blau");
	}

	@Test
	void getSimpleFeatures_fileHasSomeFeaturesWithoutGeometry_ignoriereFehlerhafte_importiereRichtige()
		throws IOException, ReadGeoJSONException {
		// arrange and act
		URL url = (new File("src/test/resources/testWegweisendeBeschilderungGeometryFehlt.geojson")).toURI()
			.toURL();
		String fileContentAsString = geoJsonImportRepository.getFileContentAsString(url);

		// act
		List<SimpleFeature> simpleFeatures = geoJsonImportRepository.getSimpleFeatures(fileContentAsString);

		// assert
		assertThat(simpleFeatures).hasSize(2);

		SimpleFeature vollstaendigesFeature = simpleFeatures.get(0);

		assertThat(
			vollstaendigesFeature.getFeatureType().getTypes().stream().map(t -> t.getName().toString())
				.collect(Collectors.toList()))
			.containsExactlyInAnyOrder("PfostenNr", "geometry");

		assertThat(vollstaendigesFeature.getAttribute("geometry")).isNotNull();
	}

	@Test
	void validateJSON_invalid() {
		// arrange
		// In folgendem JSON String fehlt eine Klammer hinter properties
		String geoJSONString = "{\n"
			+ "\"type\": \"FeatureCollection\",\n"
			+ "\"name\": \"massnahmenPunktWGS84\",\n"
			+ "\"features\": [\n"
			+ "{ \"type\": \"Feature\", \"properties\": \"Netzklassen\": \"RADNETZ_ALLTAG\" }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ 8.252427342967653, 48.869639621190053 ] } }\n"
			+ "]\n"
			+ "}";

		// act + assert
		assertThat(geoJsonImportRepository.validateJSON(geoJSONString)).isFalse();
	}

	@Test
	void getSimpleFeatures_validJSON_butNotValidGeoJSON() {
		// arrange
		String geoJSONString = "{\n"
			+ "\"type\": \"FeatureCollection\",\n"
			+ "\"name\": \"Wegweiserdaten\",\n"
			+ "\"crs\": { \"type\": \"name\", \"properties\": { \"name\": \"urn:ogc:def:crs:OGC:1.3:CRS84\" } },\n"
			+ "\"features\": [\n"
			+ "{ \"type\": \"Feature\", \"blabla\": { \"Pfost_ID\": 711201993 }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ 9.7948761, 48.3035616 ] } },\n"
			+ "{ \"type\": \"Feature\", \"properties\": { \"Pfost_ID\": 710307420 }, \"geometry\": { \"type\": \"Nichts\", \"coordinates\": [ 10.2185841, 48.8792672 ] } }\n"
			+ "]\n"
			+ "}";
		assertThat(geoJsonImportRepository.validateJSON(geoJSONString)).isTrue();

		// act + assert
		assertThatThrownBy(() -> geoJsonImportRepository.getSimpleFeatures(geoJSONString))
			.isInstanceOf(ReadGeoJSONException.class);
	}

	@Test
	void getSimpleFeatures_validJSON_butNotValidGeoJSON_GeometryNotFittingCoordinates() {
		// arrange
		String geoJSONString = "{\n"
			+ "\"type\": \"FeatureCollection\",\n"
			+ "\"name\": \"Wegweiserdaten\",\n"
			+ "\"crs\": { \"type\": \"name\", \"properties\": { \"name\": \"urn:ogc:def:crs:OGC:1.3:CRS84\" } },\n"
			+ "\"features\": [\n"
			+ "{ \"type\": \"Feature\", \"properties\": { \"Pfost_ID\": 711201993 }, \"geometry\": { \"type\": \"LineString\", \"coordinates\": [ 9.7948761, 48.3035616 ] } },\n"
			+ "{ \"type\": \"Feature\", \"properties\": { \"Pfost_ID\": 710307420 }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ 10.2185841, 48.8792672 ] } }\n"
			+ "]\n"
			+ "}";
		assertThat(geoJsonImportRepository.validateJSON(geoJSONString)).isTrue();

		// act + assert
		assertThatThrownBy(() -> geoJsonImportRepository.getSimpleFeatures(geoJSONString))
			.isInstanceOf(ReadGeoJSONException.class);
	}

	@Test
	void getSimpleFeatures_validGeoJSON_emptyGeometry() throws ReadGeoJSONException {
		// arrange
		String geoJSONString = "{\n"
			+ "\"type\": \"FeatureCollection\",\n"
			+ "\"name\": \"Wegweiserdaten\",\n"
			+ "\"crs\": { \"type\": \"name\", \"properties\": { \"name\": \"urn:ogc:def:crs:OGC:1.3:CRS84\" } },\n"
			+ "\"features\": [\n"
			+ "]\n"
			+ "}";
		assertThat(geoJsonImportRepository.validateJSON(geoJSONString)).isTrue();

		// act
		List<SimpleFeature> features = geoJsonImportRepository.getSimpleFeatures(geoJSONString);

		// assert
		assertThat(features).hasSize(0);
	}

	@Test
	void getSimpleFeatures_validGeoJSON_WGS84_KoordinatenNichtInBW() {
		// arrange
		String geoJSONString = "{\n"
			+ "\"type\": \"FeatureCollection\",\n"
			+ "\"name\": \"Wegweiserdaten\",\n"
			+ "\"crs\": { \"type\": \"name\", \"properties\": { \"name\": \"urn:ogc:def:crs:OGC:1.3:CRS84\" } },\n"
			+ "\"features\": [\n"
			+ "{ \"type\": \"Feature\", \"properties\": { \"Pfost_ID\": 711201993 }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [  445178.29852881265, 5413233.7970963605 ] } }\n"
			+ "]\n"
			+ "}";
		assertThat(geoJsonImportRepository.validateJSON(geoJSONString)).isTrue();

		// act + assert
		assertThatThrownBy(() -> geoJsonImportRepository.getSimpleFeatures(geoJSONString))
			.isInstanceOf(ReadGeoJSONException.class);
	}

	@Test
	void getSimpleFeatures_point_ohneCRS_WGS84() throws ReadGeoJSONException, FactoryException {
		// arrange
		String geoJSONString = "{\n"
			+ "\"type\": \"FeatureCollection\",\n"
			+ "\"name\": \"massnahmenPunktWGS84\",\n"
			+ "\"features\": [\n"
			+ "{ \"type\": \"Feature\", \"properties\": { \"Netzklassen\": \"RADNETZ_ALLTAG\" }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ 8.252427342967653, 48.869639621190053 ] } }\n"
			+ "]\n"
			+ "}";
		assertThat(geoJsonImportRepository.validateJSON(geoJSONString)).isTrue();

		// act
		List<SimpleFeature> features = geoJsonImportRepository.getSimpleFeatures(geoJSONString);

		// assert
		assertThat(features).hasSize(1);
		SimpleFeature punktFeature = features.get(0);
		assertThat(punktFeature.getAttribute("geometry")).isNotNull();
		Geometry geometry = (Geometry) punktFeature.getAttribute("geometry");
		assertThat(geometry.getCoordinates()).containsExactly(new Coordinate(445178.29852881265, 5413233.7970963605));

		assertThat(geometry.getSRID()).isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(KoordinatenReferenzSystem.of(punktFeature.getFeatureType().getCoordinateReferenceSystem()))
			.isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N);
	}

	@Test
	void getSimpleFeatures_point_mitCRS_UTM32() throws ReadGeoJSONException, FactoryException {
		// arrange
		String geoJSONString = "{\n"
			+ "\"type\": \"FeatureCollection\",\n"
			+ "\"name\": \"massnahmenPunkt\",\n"
			+ "\"crs\": { \"type\": \"name\", \"properties\": { \"name\": \"urn:ogc:def:crs:EPSG::25832\" } },\n"
			+ "\"features\": [\n"
			+ "{ \"type\": \"Feature\", \"properties\": { \"Netzklassen\": \"RADNETZ_ALLTAG\" }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ 445178.29852881265, 5413233.7970963605 ] } }\n"
			+ "]\n"
			+ "}";
		assertThat(geoJsonImportRepository.validateJSON(geoJSONString)).isTrue();

		// act
		List<SimpleFeature> features = geoJsonImportRepository.getSimpleFeatures(geoJSONString);

		// assert
		assertThat(features).hasSize(1);
		SimpleFeature punktFeature = features.get(0);
		assertThat(punktFeature.getAttribute("geometry")).isNotNull();
		Geometry geometry = (Geometry) punktFeature.getAttribute("geometry");
		assertThat(geometry.getCoordinates()).containsExactly(new Coordinate(445178.29852881265, 5413233.7970963605));

		assertThat(geometry.getSRID()).isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(KoordinatenReferenzSystem.of(punktFeature.getFeatureType().getCoordinateReferenceSystem()))
			.isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N);
	}

	@Test
	void getSimpleFeatures_point_mitCRS_WGS84() throws ReadGeoJSONException, FactoryException {
		// arrange
		// Das hier ist das gleiche Format wie die wegweisende Beschilderung hat
		// nur dass hier nur noch ein Feature ist und alle properties bis auf ProstenNr rausgenommen wurden
		String geoJSONString = "{\n"
			+ "\"type\": \"FeatureCollection\",\n"
			+ "\"name\": \"Wegweiserdaten\",\n"
			+ "\"crs\": { \"type\": \"name\", \"properties\": { \"name\": \"urn:ogc:def:crs:OGC:1.3:CRS84\" } },\n"
			+ "\"features\": [\n"
			+ "{ \"type\": \"Feature\", \"properties\": { \"Pfost_ID\": 1234 }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ 9.7948761, 48.3035616 ] } }\n"
			+ "]\n"
			+ "}\n";
		assertThat(geoJsonImportRepository.validateJSON(geoJSONString)).isTrue();

		// act
		List<SimpleFeature> features = geoJsonImportRepository.getSimpleFeatures(geoJSONString);

		// assert
		assertThat(features).hasSize(1);
		SimpleFeature punktFeature = features.get(0);
		assertThat(punktFeature.getAttribute("geometry")).isNotNull();
		Geometry geometry = (Geometry) punktFeature.getAttribute("geometry");
		assertThat(geometry.getCoordinates()).containsExactly(new Coordinate(558945.281669753, 5350346.047167036));

		assertThat(geometry.getSRID()).isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		assertThat(KoordinatenReferenzSystem.of(punktFeature.getFeatureType().getCoordinateReferenceSystem()))
			.isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N);

		assertThat(punktFeature.getAttribute("Pfost_ID")).isEqualTo(1234);
	}

}
