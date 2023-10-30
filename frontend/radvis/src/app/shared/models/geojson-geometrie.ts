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

import { Coordinate } from 'ol/coordinate';

export interface Geojson {
  type: 'LineString' | 'Point' | 'FeatureCollection' | 'Feature' | 'MultiLineString';
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

export const isPoint = (geometry: Geojson): geometry is PointGeojson => geometry && geometry.type === 'Point';

export const isLineString = (geometry: Geojson): geometry is LineStringGeojson =>
  geometry && geometry.type === 'LineString';

export const isMultiLineString = (geometry: Geojson): geometry is MultiLineStringGeojson =>
  geometry && geometry.type === 'MultiLineString';
