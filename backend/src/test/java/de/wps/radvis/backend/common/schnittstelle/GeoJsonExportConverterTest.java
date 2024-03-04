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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;

class GeoJsonExportConverterTest {

	private GeoJsonExportConverter geoJsonExportConverter;

	private static GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@BeforeEach
	void setUp() {
		geoJsonExportConverter = new GeoJsonExportConverter();
	}

	@Test
	void convert() {
		// arrange

		List<ExportData> exportDataList = new ArrayList<>();

		Point point = GeometryTestdataProvider.createPoint(new Coordinate(100.0, 100.0));
		LineString lineString = GeometryTestdataProvider.createLineString(
			new Coordinate(100.0, 100.0), new Coordinate(200.0, 200.0));
		GeometryCollection collection = new GeometryCollection(new Geometry[] { point, lineString }, geometryFactory);
		Map<String, String> propertyMap = new HashMap<>();
		propertyMap.put("key1", "value1");
		ExportData exportData1 = new ExportData(collection, propertyMap);

		exportDataList.add(exportData1);

		MultiPoint multiPoint = GeometryTestdataProvider.createMultiPoint(new Coordinate(20, 10),
			new Coordinate(200, 100));
		Map<String, String> propertyMap2 = new HashMap<>();
		propertyMap2.put("key2", "value2");
		ExportData exportData2 = new ExportData(new GeometryCollection(new Geometry[] { multiPoint }, geometryFactory),
			propertyMap2);

		exportDataList.add(exportData2);

		String expectedResult =
			"{\"type\":\"FeatureCollection\",\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:25832\"}},\"features\":["
				+ "{\"type\":\"Feature\",\"properties\":{\"key1\":\"value1\"},\"geometry\":{\"type\":\"GeometryCollection\",\"geometries\":["
				+ "{\"type\":\"Point\",\"coordinates\":[100.0,100.0]},"
				+ "{\"type\":\"LineString\",\"coordinates\":[[100.0,100.0],[200.0,200.0]]}"
				+ "]}},"
				+ "{\"type\":\"Feature\",\"properties\":{\"key2\":\"value2\"},\"geometry\":{\"type\":\"GeometryCollection\",\"geometries\":["
				+ "{\"type\":\"MultiPoint\",\"coordinates\":[[20.0,10.0],[200.0,100.0]]}"
				+ "]}}"
				+ "]}";
		// act
		byte[] byteArray = geoJsonExportConverter.convert(exportDataList);

		// assert
		Assert.assertEquals(expectedResult, new String(byteArray, StandardCharsets.UTF_8));
	}
}
