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

package de.wps.radvis.backend.common.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.exception.ShapeZipInvalidException;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.ShapeFileRepositoryImpl;

class ShpExportConverterTest {
	private ShpExportConverter shpExportConverter;
	private ShapeZipService shapeZipService;
	private static GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@BeforeEach
	void setUp() {
		shapeZipService = new ShapeZipService();
		CoordinateReferenceSystemConverter coordinateReferenceSystemConverter = new CoordinateReferenceSystemConverter(
			new Envelope(
				new Coordinate(378073.54, 5255657.09),
				new Coordinate(633191.12, 5534702.95))
		);
		shpExportConverter = new ShpExportConverter(
			new ShapeFileRepositoryImpl(coordinateReferenceSystemConverter),
			shapeZipService);
	}

	@TempDir
	public File temp;

	@Test
	void convertLines() throws IOException, ShapeZipInvalidException {
		// arrange

		List<ExportData> exportDataList = new ArrayList<>();
		Coordinate[] coordinates = {
			new Coordinate(300.0, 300.0), new Coordinate(600.0, 600.0)
		};
		LineString lineString = geometryFactory.createLineString(coordinates);
		Coordinate[] coordinates2 = {
			new Coordinate(100.0, 100.0), new Coordinate(200.0, 200.0)
		};
		LineString lineString2 = geometryFactory.createLineString(coordinates2);
		MultiLineString multiLineString = geometryFactory.createMultiLineString(
			new LineString[] { lineString, lineString2 });
		Map<String, String> propertyMap = new HashMap<>();
		propertyMap.put("Baulastträger", "v1");
		propertyMap.put("Ähhh", "v2");
		propertyMap.put("was-auch-immer", "v3");
		propertyMap.put("foo/bar", "v4");
		propertyMap.put("ßäÄöÖüÜ", "v5");
		ExportData exportData = new ExportData(multiLineString, propertyMap);
		ExportData exportData2 = new ExportData(lineString, propertyMap);
		exportDataList.add(exportData);
		exportDataList.add(exportData2);

		// act
		byte[] byteArray = shpExportConverter.convert(exportDataList);

		// assert
		File outputFile = unzip(byteArray);
		List<SimpleFeature> simpleFeatures = new ArrayList<>();

		HashMap<String, Object> map = new HashMap<>();
		map.put("url", new File(outputFile, "export-MultiLineString.shp").toURI().toURL());

		DataStore store = DataStoreFinder.getDataStore(map);
		SimpleFeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);
		SimpleFeatureIterator features = featureSource.getFeatures().features();
		while (features.hasNext()) {
			simpleFeatures.add(features.next());
		}
		features.close();
		store.dispose();
		FileUtils.deleteDirectory(outputFile);
		assertThat(simpleFeatures).hasSize(2).extracting(SimpleFeature::getDefaultGeometry)
			.extracting(obj -> ((Geometry) obj).getGeometryType())
			.containsExactly(Geometry.TYPENAME_MULTILINESTRING, Geometry.TYPENAME_MULTILINESTRING);
		assertThat(simpleFeatures).extracting(SimpleFeature::getDefaultGeometry)
			.extracting(obj -> ((Geometry) obj).getCoordinates())
			.containsExactlyInAnyOrder(lineString.getCoordinates(), multiLineString.getCoordinates());
		simpleFeatures.forEach(f -> {
			assertThat(f.getProperty("Baulasttra").getValue()).isEqualTo("v1");
			assertThat(f.getProperty("Aehhh").getValue()).isEqualTo("v2");
			assertThat(f.getProperty("wasauchimm").getValue()).isEqualTo("v3");
			assertThat(f.getProperty("foobar").getValue()).isEqualTo("v4");
			assertThat(f.getProperty("ssaeAeoeOe").getValue()).isEqualTo("v5");
		});
	}

	@Test
	void unwrapGeometryCollection() throws IOException, ShapeZipInvalidException {
		// arrange
		Coordinate[] coordinates = {
			new Coordinate(300.0, 300.0), new Coordinate(600.0, 600.0)
		};
		LineString lineString = geometryFactory.createLineString(coordinates);
		Coordinate[] coordinates2 = {
			new Coordinate(100.0, 100.0), new Coordinate(200.0, 200.0)
		};
		LineString lineString2 = geometryFactory.createLineString(coordinates2);
		MultiLineString multiLineString = geometryFactory.createMultiLineString(
			new LineString[] { lineString, lineString2 });

		Point point = geometryFactory.createPoint(new Coordinate(100.0, 100.0));

		GeometryCollection geometryCollection = new GeometryCollection(
			new Geometry[] { lineString, multiLineString, point },
			geometryFactory);

		Map<String, String> propertyMap = new HashMap<>();
		propertyMap.put("key1", "value1");
		propertyMap.put("key2", "value2");

		List<ExportData> exportDataList = new ArrayList<>();
		exportDataList.add(new ExportData(geometryCollection, propertyMap));

		// act
		byte[] byteArray = shpExportConverter.convert(exportDataList);

		// assert
		File outputFile = unzip(byteArray);

		// assert - lines are there
		List<SimpleFeature> simpleFeatures = new ArrayList<>();
		HashMap<String, Object> map = new HashMap<>();
		map.put("url", new File(outputFile, "export-MultiLineString.shp").toURI().toURL());

		DataStore store = DataStoreFinder.getDataStore(map);
		SimpleFeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);
		SimpleFeatureIterator features = featureSource.getFeatures().features();
		while (features.hasNext()) {
			simpleFeatures.add(features.next());
		}
		features.close();
		store.dispose();
		assertThat(simpleFeatures).hasSize(2).extracting(SimpleFeature::getDefaultGeometry)
			.extracting(obj -> ((Geometry) obj).getGeometryType())
			.containsExactly(Geometry.TYPENAME_MULTILINESTRING, Geometry.TYPENAME_MULTILINESTRING);
		assertThat(simpleFeatures).extracting(SimpleFeature::getDefaultGeometry)
			.extracting(obj -> ((Geometry) obj).getCoordinates())
			.containsExactlyInAnyOrder(lineString.getCoordinates(), multiLineString.getCoordinates());

		// assert - point is there
		simpleFeatures = new ArrayList<>();
		map = new HashMap<>();
		map.put("url", new File(outputFile, "export-MultiPoint.shp").toURI().toURL());

		store = DataStoreFinder.getDataStore(map);
		featureSource = store.getFeatureSource(store.getTypeNames()[0]);
		features = featureSource.getFeatures().features();
		while (features.hasNext()) {
			simpleFeatures.add(features.next());
		}
		features.close();
		store.dispose();
		assertThat(simpleFeatures).hasSize(1).extracting(SimpleFeature::getDefaultGeometry)
			.extracting(obj -> ((Geometry) obj).getGeometryType())
			.containsExactly(Geometry.TYPENAME_MULTIPOINT);
		assertThat(simpleFeatures).extracting(SimpleFeature::getDefaultGeometry)
			.extracting(obj -> ((Geometry) obj).getCoordinates())
			.containsExactlyInAnyOrder(point.getCoordinates());

		FileUtils.deleteDirectory(outputFile);
	}

	@Test
	void unwrapGeometryCollection_onlyMLS_noEmptyGeometries() throws IOException, ShapeZipInvalidException {
		// arrange
		Coordinate[] coordinates1 = {
			new Coordinate(100.0, 100.0), new Coordinate(200.0, 200.0)
		};
		Coordinate[] coordinates2 = {
			new Coordinate(100.0, 100.0), new Coordinate(200.0, 200.0)
		};

		GeometryCollection geometryCollection = new GeometryCollection(
			new Geometry[] {
				geometryFactory.createMultiLineString(
					new LineString[] {
						geometryFactory.createLineString(coordinates1),
						geometryFactory.createLineString(coordinates2)
					})
			}, geometryFactory);

		Map<String, String> propertyMap = new HashMap<>();
		propertyMap.put("key1", "value1");
		propertyMap.put("key2", "value2");

		List<ExportData> exportDataList = new ArrayList<>();
		exportDataList.add(new ExportData(geometryCollection, propertyMap));

		// act
		byte[] byteArray = shpExportConverter.convert(exportDataList);

		// assert
		File outputFile = unzip(byteArray);

		// assert - lines are there
		List<SimpleFeature> simpleFeatures = new ArrayList<>();
		HashMap<String, Object> map = new HashMap<>();
		map.put("url", new File(outputFile, "export-MultiLineString.shp").toURI().toURL());

		DataStore store = DataStoreFinder.getDataStore(map);
		SimpleFeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);
		SimpleFeatureIterator features = featureSource.getFeatures().features();
		while (features.hasNext()) {
			simpleFeatures.add(features.next());
		}
		features.close();
		store.dispose();
		assertThat(simpleFeatures).hasSize(1)
			.extracting(SimpleFeature::getDefaultGeometry)
			.extracting(obj -> ((Geometry) obj).getGeometryType())
			.containsExactly(Geometry.TYPENAME_MULTILINESTRING);
		assertThat(simpleFeatures)
			.extracting(SimpleFeature::getDefaultGeometry)
			.extracting(obj -> ((Geometry) obj).getCoordinates())
			.containsExactlyInAnyOrder(
				geometryFactory.createMultiLineString(new LineString[] {
					geometryFactory.createLineString(coordinates1),
					geometryFactory.createLineString(coordinates2)
				}).getCoordinates()
			);

		FileUtils.deleteDirectory(outputFile);
	}

	public File unzip(byte[] zipfileContent) throws IOException, ShapeZipInvalidException {
		File shpDirectory = Files
			.createTempDirectory("ShpExportConverterTest")
			.toFile();
		shpDirectory.deleteOnExit();
		try {
			shapeZipService.unzip(zipfileContent, shpDirectory);
		} catch (Exception e) {
			FileUtils.deleteDirectory(shpDirectory);
			throw e;
		}
		return shpDirectory;
	}
}