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

package de.wps.radvis.backend.shapetransformation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.shapetransformation.domain.valueObject.AttributTransformation;
import de.wps.radvis.backend.shapetransformation.domain.valueObject.TransformationsKonfiguration;

class TransformationsServiceTest {

	private TransformationsService transformationsService;

	@BeforeEach
	void setup() {
		transformationsService = new TransformationsService();
	}

	@Test
	void transformiere_simpleMapping() throws SchemaException {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("Flag");
		b.setCRS(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeotoolsCRS());
		b.add("the_geom", MultiLineString.class);
		String quellAttributName = "attr1";
		b.add(quellAttributName, String.class);
		SimpleFeatureType type = b.buildFeatureType();

		GeometryFactory geomFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

		MultiLineString geometry1 = geomFactory.createMultiLineString(new LineString[] {
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100)) });

		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(type);
		f1.add(geometry1);
		f1.add("wert1");
		SimpleFeature simpleFeature1 = f1.buildFeature("id");

		MultiLineString geometry2 = geomFactory.createMultiLineString(new LineString[] {
			GeometryTestdataProvider.createLineString(new Coordinate(30, 0), new Coordinate(50, 100)) });

		SimpleFeatureBuilder f2 = new SimpleFeatureBuilder(type);
		f2.add(geometry2);
		f2.add("wert2");
		SimpleFeature simpleFeature2 = f2.buildFeature("id");

		Map<String, String> quellToZielAttributwert = new HashMap<>();
		quellToZielAttributwert.put("wert1", "newWert1");
		quellToZielAttributwert.put("wert2", "newWert2");
		String zielAttributName = "newAttr1";
		TransformationsKonfiguration konfiguration = new TransformationsKonfiguration(Map.of(
			quellAttributName, new AttributTransformation(zielAttributName, quellToZielAttributwert)));

		Stream<SimpleFeature> features = Stream.of(simpleFeature1, simpleFeature2);
		List<SimpleFeature> result = transformationsService.transformiere(features,
			konfiguration).collect(Collectors.toList());

		assertThat(result).hasSize(2);

		SimpleFeature resultFeature1 = result.get(0);
		assertThat(resultFeature1.getAttribute(zielAttributName)).isEqualTo("newWert1");
		Geometry defaultGeometry = (Geometry) resultFeature1.getDefaultGeometry();
		assertThat(defaultGeometry.getCoordinates()).containsExactly(geometry1.getCoordinates());

		SimpleFeature resultFeature2 = result.get(1);
		assertThat(resultFeature2.getAttribute(zielAttributName)).isEqualTo("newWert2");
		defaultGeometry = (Geometry) resultFeature2.getDefaultGeometry();
		assertThat(defaultGeometry.getCoordinates()).containsExactly(geometry2.getCoordinates());
	}

	@Test
	void transformiere_keepsUnmappedAttribut() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("Flag");
		b.setCRS(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeotoolsCRS());
		b.add("the_geom", MultiLineString.class);
		String quellAttributName = "attr1";
		b.add(quellAttributName, String.class);
		String unmappedAttributName = "attr2";
		b.add(unmappedAttributName, String.class);
		SimpleFeatureType type = b.buildFeatureType();

		GeometryFactory geomFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

		MultiLineString geometry1 = geomFactory.createMultiLineString(new LineString[] {
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100)) });

		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(type);
		f1.add(geometry1);
		f1.add("wert1");
		String unmappedAttributWert = "wert2";
		f1.add(unmappedAttributWert);
		SimpleFeature simpleFeature1 = f1.buildFeature("id");

		Map<String, String> quellToZielAttributwert = new HashMap<>();
		quellToZielAttributwert.put("wert1", "newWert1");
		String zielAttributName = "newAttr1";
		TransformationsKonfiguration konfiguration = new TransformationsKonfiguration(Map.of(
			quellAttributName, new AttributTransformation(zielAttributName, quellToZielAttributwert)));

		Stream<SimpleFeature> features = Stream.of(simpleFeature1);
		List<SimpleFeature> result = transformationsService.transformiere(features,
			konfiguration).collect(Collectors.toList());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getAttribute(unmappedAttributName)).isEqualTo(unmappedAttributWert);
	}

	@Test
	void transformiere_keepsUnmappedWert() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("Flag");
		b.setCRS(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeotoolsCRS());
		b.add("the_geom", MultiLineString.class);
		String quellAttributName = "attr1";
		b.add(quellAttributName, String.class);
		SimpleFeatureType type = b.buildFeatureType();

		GeometryFactory geomFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

		MultiLineString geometry1 = geomFactory.createMultiLineString(new LineString[] {
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100)) });

		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(type);
		f1.add(geometry1);
		String unmappedAttributWert = "wert2";
		f1.add(unmappedAttributWert);
		SimpleFeature simpleFeature1 = f1.buildFeature("id");

		Map<String, String> quellToZielAttributwert = new HashMap<>();
		quellToZielAttributwert.put("wert1", "newWert1");
		String zielAttributName = "newAttr1";
		TransformationsKonfiguration konfiguration = new TransformationsKonfiguration(Map.of(
			quellAttributName, new AttributTransformation(zielAttributName, quellToZielAttributwert)));

		Stream<SimpleFeature> features = Stream.of(simpleFeature1);
		List<SimpleFeature> result = transformationsService.transformiere(features,
			konfiguration).collect(Collectors.toList());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getAttribute(zielAttributName)).isEqualTo(unmappedAttributWert);
	}

	@Test
	void transformiere_wertHatAnderenTyp() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("Flag");
		b.setCRS(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeotoolsCRS());
		b.add("the_geom", MultiLineString.class);
		String quellAttributName = "attr1";
		b.add(quellAttributName, Long.class);
		SimpleFeatureType type = b.buildFeatureType();

		GeometryFactory geomFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

		MultiLineString geometry1 = geomFactory.createMultiLineString(new LineString[] {
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100)) });

		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(type);
		f1.add(geometry1);
		f1.add(2l);
		SimpleFeature simpleFeature1 = f1.buildFeature("id");

		Map<String, String> quellToZielAttributwert = new HashMap<>();
		String zielWert = "newWert1";
		quellToZielAttributwert.put("2", zielWert);
		String zielAttributName = quellAttributName;
		TransformationsKonfiguration konfiguration = new TransformationsKonfiguration(Map.of(
			quellAttributName, new AttributTransformation(zielAttributName, quellToZielAttributwert)));

		Stream<SimpleFeature> features = Stream.of(simpleFeature1);
		List<SimpleFeature> result = transformationsService.transformiere(features,
			konfiguration).collect(Collectors.toList());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getAttribute(zielAttributName)).isEqualTo(zielWert);
	}

	@Test
	void transformiere_wertIsNull() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("Flag");
		b.setCRS(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeotoolsCRS());
		b.add("the_geom", MultiLineString.class);
		String quellAttributName = "attr1";
		b.add(quellAttributName, String.class);
		SimpleFeatureType type = b.buildFeatureType();

		GeometryFactory geomFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

		MultiLineString geometry1 = geomFactory.createMultiLineString(new LineString[] {
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100)) });

		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(type);
		f1.add(geometry1);
		SimpleFeature simpleFeature1 = f1.buildFeature("id");

		Map<String, String> quellToZielAttributwert = new HashMap<>();
		quellToZielAttributwert.put("wert1", "newWert1");
		String zielAttributName = "newAttr1";
		TransformationsKonfiguration konfiguration = new TransformationsKonfiguration(Map.of(
			quellAttributName, new AttributTransformation(zielAttributName, quellToZielAttributwert)));

		Stream<SimpleFeature> features = Stream.of(simpleFeature1);
		List<SimpleFeature> result = transformationsService.transformiere(features,
			konfiguration).collect(Collectors.toList());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getAttribute(zielAttributName)).isNull();
	}

	@Test
	void transformiere_mappedAttributNameMissing() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("Flag");
		b.setCRS(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeotoolsCRS());
		b.add("the_geom", MultiLineString.class);
		SimpleFeatureType type = b.buildFeatureType();

		GeometryFactory geomFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

		MultiLineString geometry1 = geomFactory.createMultiLineString(new LineString[] {
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100)) });

		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(type);
		f1.add(geometry1);
		SimpleFeature simpleFeature1 = f1.buildFeature("id");

		String quellAttributName = "attr1";
		Map<String, String> quellToZielAttributwert = new HashMap<>();
		quellToZielAttributwert.put("wert1", "newWert1");
		String zielAttributName = "newAttr1";
		TransformationsKonfiguration konfiguration = new TransformationsKonfiguration(Map.of(
			quellAttributName, new AttributTransformation(zielAttributName, quellToZielAttributwert)));

		Stream<SimpleFeature> features = Stream.of(simpleFeature1);
		assertDoesNotThrow(() -> transformationsService.transformiere(features,
			konfiguration).collect(Collectors.toList()));
	}
}