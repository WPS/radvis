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

import Stroke from 'ol/style/Stroke';
import Style from 'ol/style/Style';
import { LayerTypes } from 'src/app/shared/models/layers/layer-types';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Color, RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';

export class LandkreiseLayer extends RadVisLayer {
  public static LAYER_ID = 'LANDKREISE';

  constructor() {
    const color: Color = [0, 0, 0, 1];
    super(
      'LANDKREISE',
      'Landkreise',
      'url an dieser Stelle ignoriert, weil die Landkreise im OrganisationsService geladen werden.',
      RadVisLayerTyp.GEO_JSON,
      (): Style =>
        new Style({
          stroke: new Stroke({
            color,
            width: MapStyles.LINE_WIDTH_THIN,
          }),
        }),
      color,
      LayerTypes.QUELLE
    );
  }
}
