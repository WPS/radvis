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

package de.wps.radvis.backend.common;

import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.SimpleFeatureTypeFactory;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;

public class SimpleFeatureTestDataProvider {
	public static SimpleFeatureTypeBuilder defaultType() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("Flag");
		b.setCRS(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeotoolsCRS());
		b.add(SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_THE_GEOM, LineString.class);
		return b;
	}

	public static SimpleFeatureTypeBuilder typeWithGeometry(Class<? extends Geometry> geom) {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("Flag");
		b.setCRS(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeotoolsCRS());
		b.add(SimpleFeatureTypeFactory.GEOMETRY_ATTRIBUTE_KEY_THE_GEOM, geom);
		return b;
	}

	public static SimpleFeature defaultFeature() {
		return withLineString(new Coordinate(0, 1), new Coordinate(1, 10));
	}

	public static SimpleFeature withLineString(Coordinate... coordinates) {
		GeometryFactory geomFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

		LineString geometry1 = geomFactory.createLineString(coordinates);

		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(defaultType().buildFeatureType());
		f1.add(geometry1);
		return f1.buildFeature("id");
	}

	public static SimpleFeature withMultiLineStringAndAttributes(Map<String, String> attributes,
		Coordinate... coordinates) {
		GeometryFactory geomFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

		MultiLineString geometry = geomFactory.createMultiLineString(new LineString[] {
			GeometryTestdataProvider.createLineString(coordinates) });

		return withGeometryAndAttributes(attributes, geometry);
	}

	public static SimpleFeature withGeometryAndAttributes(Map<String, String> attributes,
		Geometry geometry) {

		SimpleFeatureTypeBuilder typeBuilder = typeWithGeometry(geometry.getClass());
		attributes.keySet().forEach(key -> {
			typeBuilder.add(key, String.class);
		});

		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		f1.add(geometry);
		attributes.keySet().forEach(key -> {
			f1.add(attributes.get(key));
		});
		return f1.buildFeature("id");
	}

	public static SimpleFeature withPointAndAttributes(Map<String, String> attributes,
		Coordinate coordinate) {

		Point geometry1 = GeometryTestdataProvider.createPoint(coordinate);

		SimpleFeatureTypeBuilder typeBuilder = typeWithGeometry(Point.class);
		attributes.keySet().forEach(key -> {
			typeBuilder.add(key, String.class);
		});

		SimpleFeatureBuilder f1 = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		f1.add(geometry1);
		attributes.keySet().forEach(key -> {
			f1.add(attributes.get(key));
		});
		return f1.buildFeature("id");
	}

	public static SimpleFeature withAttributes(Map<String, String> attributes) {
		SimpleFeature simpleFeature = withMultiLineStringAndAttributes(attributes, new Coordinate(0, 0),
			new Coordinate(10, 10));

		return simpleFeature;
	}
}
