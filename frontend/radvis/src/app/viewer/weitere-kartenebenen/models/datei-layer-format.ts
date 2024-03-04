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

import { EnumOption } from 'src/app/form-elements/models/enum-option';

export enum DateiLayerFormat {
  GEOJSON = 'GEOJSON',
  GEOPACKAGE = 'GEOPACKAGE',
  SHAPE = 'SHAPE',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace DateiLayerFormat {
  export const options: EnumOption[] = Object.keys(DateiLayerFormat).map(
    (k: string): EnumOption => {
      switch (k) {
        case DateiLayerFormat.SHAPE:
          return { name: k, displayText: 'Shapefile (Shape-ZIP)' };
        case DateiLayerFormat.GEOJSON:
          return { name: k, displayText: 'GeoJSON' };
        case DateiLayerFormat.GEOPACKAGE:
          return { name: k, displayText: 'GeoPackage' };
      }
      throw new Error('Beschreibung für enum DateiLayerTyp fehlt: ' + k);
    }
  );

  export const getDisplayText = (dateiLayerFormat: DateiLayerFormat): string => {
    if (!dateiLayerFormat) {
      return '';
    }
    switch (dateiLayerFormat) {
      case DateiLayerFormat.SHAPE:
        return 'Shapefile';
      case DateiLayerFormat.GEOJSON:
        return 'GeoJSON';
      case DateiLayerFormat.GEOPACKAGE:
        return 'GeoPackage';
    }
    throw new Error('getDisplayName für DateiLayerTyp fehlt: ' + dateiLayerFormat);
  };

  export const getDateiEndung = (format: DateiLayerFormat): string => {
    switch (format) {
      case DateiLayerFormat.SHAPE:
        return 'zip';
      case DateiLayerFormat.GEOJSON:
        return 'geojson';
      case DateiLayerFormat.GEOPACKAGE:
        return 'gpkg';
    }
    throw new Error('Beschreibung für enum DateiLayerTyp fehlt: ' + format);
  };
}
