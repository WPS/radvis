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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Arrays;
import java.util.List;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class GeoJsonConverter {

	public static FeatureCollection createFeatureCollection() {
		return new FeatureCollection();
	}

	public static Feature createFeature(Geometry geometry) {
		require(geometry, notNullValue());
		require(geometry.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
			"Erwartete SRID '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
				+ "' entspricht nicht geometry.getSRID() '" + geometry.getSRID() + "'");

		Feature feature = new Feature();
		feature.setGeometry(createGeoJsonGeometry(geometry));
		return feature;
	}

	public static GeoJsonObject createGeoJsonGeometry(Geometry geometrie) {
		if (geometrie instanceof Point) {
			return parsePoint((Point) geometrie);
		}
		if (geometrie instanceof MultiPoint) {
			org.geojson.MultiPoint geoJsonMultiPoint = new org.geojson.MultiPoint();
			for (int i = 0; i < geometrie.getNumGeometries(); i++) {
				geoJsonMultiPoint.add(parsePoint((Point) geometrie.getGeometryN(i)).getCoordinates());
			}
			return geoJsonMultiPoint;
		}
		if (geometrie instanceof LineString) {
			LngLatAlt[] lngLatAlts = parseCoordinates(geometrie.getCoordinates());
			return new org.geojson.LineString(lngLatAlts);
		}
		if (geometrie instanceof MultiLineString) {
			org.geojson.MultiLineString geoJsonMultiLineString = new org.geojson.MultiLineString();
			for (int i = 0; i < geometrie.getNumGeometries(); i++) {
				LineString lineString = (LineString) geometrie.getGeometryN(i);
				geoJsonMultiLineString.add(Arrays.asList(parseCoordinates(lineString.getCoordinates())));
			}
			return geoJsonMultiLineString;
		}
		if (geometrie instanceof Polygon) {
			return parsePolygon((Polygon) geometrie);
		}
		if (geometrie instanceof MultiPolygon) {
			return parsePolygon((MultiPolygon) geometrie);
		}
		if (geometrie instanceof GeometryCollection) {
			org.geojson.GeometryCollection geometryCollection = new org.geojson.GeometryCollection();
			for (int i = 0; i < geometrie.getNumGeometries(); i++) {
				geometryCollection.add(createGeoJsonGeometry(geometrie.getGeometryN(i)));
			}
			return geometryCollection;
		}

		throw new RuntimeException("Geometrietyp wird momentan nicht unterstützt: " + geometrie.getClass());
	}

	public static org.locationtech.jts.geom.LineString create3DJtsLineStringFromGeoJson(
		org.geojson.LineString lineString, KoordinatenReferenzSystem koordinatenReferenzSystem) {
		return koordinatenReferenzSystem
			.getGeometryFactory()
			.createLineString(
				lineString.getCoordinates().stream()
					.map(pos -> new Coordinate(pos.getLongitude(), pos.getLatitude(), pos.getAltitude()))
					.toArray(Coordinate[]::new));
	}

	public static org.locationtech.jts.geom.Point create3DJtsPointFromGeoJson(
		org.geojson.Point point, KoordinatenReferenzSystem koordinatenReferenzSystem) {
		return koordinatenReferenzSystem
			.getGeometryFactory()
			.createPoint(new Coordinate(point.getCoordinates().getLongitude(), point.getCoordinates().getLatitude(),
				point.getCoordinates().getAltitude()));
	}

	private static org.geojson.Polygon parsePolygon(Polygon polygon) {
		if (polygon.getNumInteriorRing() > 0) {
			throw new RuntimeException("Polygone mit Löchern werden momentan nicht unterstützt.");
		}
		LngLatAlt[] lngLatAlts = parseCoordinates(polygon.getCoordinates());
		return new org.geojson.Polygon(Arrays.asList(lngLatAlts));
	}

	private static org.geojson.MultiPolygon parsePolygon(MultiPolygon jtsMultiPolygon) {
		org.geojson.MultiPolygon geoJSONMultiPolygon = new org.geojson.MultiPolygon();

		int anzahlGeometrienInMultiPolygon = jtsMultiPolygon.getNumGeometries();
		for (int i = 0; i < anzahlGeometrienInMultiPolygon; i++) {
			org.geojson.Polygon geoJSONPolygon = new org.geojson.Polygon();
			geoJSONMultiPolygon.add(geoJSONPolygon);

			Polygon komplettePolygonJTSGeometry = (Polygon) jtsMultiPolygon.getGeometryN(i);

			List<LngLatAlt> aussenring = Arrays.asList(
				parseCoordinates(komplettePolygonJTSGeometry.getExteriorRing().getCoordinates()));
			geoJSONPolygon.add(aussenring);

			int anzahlInnenRinge = komplettePolygonJTSGeometry.getNumInteriorRing();
			for (int j = 0; j < anzahlInnenRinge; j++) {
				List<LngLatAlt> innenRing = Arrays.asList(
					parseCoordinates(komplettePolygonJTSGeometry.getInteriorRingN(j).getCoordinates()));
				geoJSONPolygon.addInteriorRing(innenRing);
			}
		}

		return geoJSONMultiPolygon;
	}

	private static org.geojson.Point parsePoint(Point geometrie) {
		Coordinate coordinate = geometrie.getCoordinate();
		LngLatAlt lngLatAlt = new LngLatAlt(coordinate.x, coordinate.y, coordinate.z);
		return new org.geojson.Point(lngLatAlt);
	}

	private static LngLatAlt[] parseCoordinates(Coordinate[] coordinates) {
		LngLatAlt[] lngLatAlts = new LngLatAlt[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			lngLatAlts[i] = new LngLatAlt(coordinates[i].x, coordinates[i].y, coordinates[i].z);
		}
		return lngLatAlts;
	}
}
