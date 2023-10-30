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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.opengis.feature.simple.SimpleFeature;

import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.GeoPackageExportConverter;

class GeoPackageExportConverterTest {
	private GeoPackageExportConverter geoPackageExportConverter;

	private static GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@BeforeEach
	void setUp() {
		geoPackageExportConverter = new GeoPackageExportConverter();
	}


	@Test
	void convert() throws IOException {
		// arrange

		List<ExportData> exportDataList = new ArrayList<>();
		LineString lineString = geometryFactory.createLineString(new Coordinate[] {
			new Coordinate(300.0, 300.0), new Coordinate(600.0, 600.0)
		});
		LineString lineString2 = geometryFactory.createLineString(new Coordinate[] {
			new Coordinate(100.0, 100.0), new Coordinate(200.0, 200.0)
		});
		MultiLineString multiLineString = geometryFactory.createMultiLineString(
			new LineString[] { lineString, lineString2 });
		Map<String, String> propertyMap = new HashMap<>();
		propertyMap.put("key1", "value1");
		propertyMap.put("key2", "value2");
		ExportData exportData = new ExportData(multiLineString, propertyMap);
		ExportData exportData2 = new ExportData(lineString, propertyMap);
		exportDataList.add(exportData);
		exportDataList.add(exportData2);

		// act
		byte[] byteArray = geoPackageExportConverter.convert(exportDataList);

		// assert
		File directory = Files.createTempDirectory("shape_repo_test").toFile();
		File outputFile = new File(directory, "outputFile.gpkg");
		try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
			outputStream.write(byteArray);
			outputStream.flush();
		}

		List<SimpleFeature> simpleFeatures = new ArrayList<>();

		HashMap<String, Object> map = new HashMap<>();
		map.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
		map.put(GeoPkgDataStoreFactory.DATABASE.key, outputFile.getAbsolutePath());

		DataStore store = DataStoreFinder.getDataStore(map);
		SimpleFeatureSource featureSource = store.getFeatureSource(store.getTypeNames()[0]);
		SimpleFeatureIterator features = featureSource.getFeatures().features();
		while (features.hasNext()) {
			simpleFeatures.add(features.next());
		}
		features.close();
		store.dispose();
		outputFile.delete();
		directory.delete();
		assertThat(simpleFeatures).hasSize(2).extracting(SimpleFeature::getDefaultGeometry)
			.extracting(obj -> ((Geometry) obj).getGeometryType())
			.containsExactlyInAnyOrder(Geometry.TYPENAME_LINESTRING, Geometry.TYPENAME_MULTILINESTRING);
		assertThat(simpleFeatures).extracting(SimpleFeature::getDefaultGeometry)
			.extracting(obj -> ((Geometry) obj).getCoordinates())
			.containsExactlyInAnyOrder(lineString.getCoordinates(), multiLineString.getCoordinates());

	}

}