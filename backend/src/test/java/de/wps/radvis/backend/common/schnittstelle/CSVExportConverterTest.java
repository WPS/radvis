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
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.valueObject.ExportData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.repositoryImpl.CsvRepositoryImpl;

class CSVExportConverterTest {
	private CSVExportConverter csvExportConverter;

	private static GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@BeforeEach
	void setUp() {
		csvExportConverter = new CSVExportConverter(new CsvRepositoryImpl());
	}

	@Test
	void convert() {
		// arrange

		List<ExportData> exportDataList = new ArrayList<>();
		Point point = geometryFactory.createPoint(new Coordinate(100.0, 100.0));
		LineString lineString = geometryFactory.createLineString(new Coordinate[] {
			new Coordinate(100.0, 100.0), new Coordinate(200.0, 200.0)
		});
		Geometry[] geometryArray = new Geometry[2];
		geometryArray[0] = point;
		geometryArray[1] = lineString;
		GeometryCollection collection = new GeometryCollection(geometryArray, geometryFactory);
		Map<String, String> propertyMap = new HashMap<>();
		propertyMap.put("key1", "value1");
		propertyMap.put("key2", "value2");
		ExportData exportData = new ExportData(collection, propertyMap);

		exportDataList.add(exportData);

		String expectedResult = "\"key1\";\"key2\"\n\"value1\";\"value2\"\n";
		// act
		byte[] byteArray = csvExportConverter.convert(exportDataList);

		// assert
		// convert byte[] to String and remove BOM
		String actual = new String(byteArray, StandardCharsets.UTF_8).replace("\uFEFF", "");
		Assert.assertEquals(expectedResult, actual);
	}

	@Test
	void convert_respectesHeaderReihenfolge() {
		// arrange

		List<ExportData> exportDataList = new ArrayList<>();
		Point point = geometryFactory.createPoint(new Coordinate(100.0, 100.0));
		LineString lineString = geometryFactory.createLineString(new Coordinate[] {
			new Coordinate(100.0, 100.0), new Coordinate(200.0, 200.0)
		});
		Geometry[] geometryArray = new Geometry[2];
		geometryArray[0] = point;
		geometryArray[1] = lineString;
		GeometryCollection collection = new GeometryCollection(geometryArray, geometryFactory);
		Map<String, String> propertyMap = new HashMap<>();
		propertyMap.put("key1", "value1");
		propertyMap.put("key2", "value2");
		ExportData exportData = new ExportData(collection, propertyMap, List.of("key2", "key1"));

		exportDataList.add(exportData);

		String expectedResult = "\"key2\";\"key1\"\n\"value2\";\"value1\"\n";
		// act
		byte[] byteArray = csvExportConverter.convert(exportDataList);

		// assert
		// convert byte[] to String and remove BOM
		String actual = new String(byteArray, StandardCharsets.UTF_8).replace("\uFEFF", "");
		Assert.assertEquals(expectedResult, actual);
	}
}