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

import { Feature } from 'ol';
import { Coordinate } from 'ol/coordinate';
import { GeoJSONFeature, GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { GeometryCollection, LineString, MultiLineString, MultiPoint, Point } from 'ol/geom';
import Geometry from 'ol/geom/Geometry';

export interface Geojson {
  type:
    | 'Point'
    | 'MultiPoint'
    | 'LineString'
    | 'MultiLineString'
    | 'Polygon'
    | 'Feature'
    | 'FeatureCollection'
    | 'GeometryCollection';
}

export interface PointGeojson extends Geojson {
  coordinates: Coordinate;
}

export interface LineStringGeojson extends Geojson {
  coordinates: Coordinate[];
}

export interface MultiLineStringGeojson extends Geojson {
  coordinates: Coordinate[][];
}

export interface MultiPointGeojson extends Geojson {
  coordinates: Coordinate[];
}

export interface PolygonGeojson extends Geojson {
  coordinates: Coordinate[][];
}

export interface GeometryCollectionGeojson extends Geojson {
  geometries: Geojson[];
}

export const isPoint = (geometry: Geojson): geometry is PointGeojson => geometry && geometry.type === 'Point';

export const isLineString = (geometry: Geojson): geometry is LineStringGeojson =>
  geometry && geometry.type === 'LineString';

export const isMultiLineString = (geometry: Geojson): geometry is MultiLineStringGeojson =>
  geometry && geometry.type === 'MultiLineString';

export const isMultiPoint = (geometry: Geojson): geometry is MultiPointGeojson =>
  geometry && geometry.type === 'MultiPoint';

export const isGeometryCollection = (geometry: Geojson): geometry is GeometryCollectionGeojson =>
  geometry && geometry.type === 'GeometryCollection';

export const isFeatureCollection = (geometry: Geojson): geometry is GeoJSONFeatureCollection =>
  geometry && geometry.type === 'FeatureCollection';

export const isFeature = (geometry: Geojson): geometry is GeoJSONFeature => geometry && geometry.type === 'Feature';

export const geojsonGeometryToFeature = (geojson: Geojson): Feature | null => {
  if (isLineString(geojson)) {
    return new Feature(new LineString(geojson.coordinates));
  }
  if (isMultiLineString(geojson)) {
    return new Feature(new MultiLineString(geojson.coordinates));
  }
  if (isMultiPoint(geojson)) {
    return new Feature(new MultiPoint(geojson.coordinates));
  }
  if (isPoint(geojson)) {
    return new Feature(new Point(geojson.coordinates));
  }
  if (isGeometryCollection(geojson)) {
    const geometries = geojson.geometries.map(g => geojsonGeometryToFeature(g)?.getGeometry()).filter(g => !!g);
    return new Feature(new GeometryCollection(geometries));
  }
  return null;
};
