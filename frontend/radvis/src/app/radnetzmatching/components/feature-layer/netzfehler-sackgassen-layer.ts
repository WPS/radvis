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

import Fill from 'ol/style/Fill';
import Style from 'ol/style/Style';
import Text from 'ol/style/Text';
import { NetzfehlerLayer } from 'src/app/radnetzmatching/components/feature-layer/netzfehler-layer';
import { LayerTypes } from 'src/app/shared/models/layers/layer-types';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Color, RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';

export class NetzfehlerSackgassenLayer extends RadVisLayer {
  public static LAYER_ID = NetzfehlerLayer.ID_PREFIX + 'SACKGASSE';
  public static MIN_ZOOM_LEVEL = 11.2;

  constructor() {
    const color: Color = [0, 0, 255, 1];
    const highlightColor: Color = [216, 27, 96, 1];

    super(
      NetzfehlerSackgassenLayer.LAYER_ID,
      'Sackgassen',
      `${NetzausschnittService.BASE_URL}/netzfehler/RADNETZ_SACKGASSE`,
      RadVisLayerTyp.GEO_JSON,
      (feature): Style => {
        const highlighted: boolean = feature.getProperties().highlighted;
        return new Style({
          text: new Text({
            text: 'T',
            font: 'bold 14px Roboto',
            fill: new Fill({
              color: 'white',
            }),
            rotation: 3.14,
            offsetY: 2,
          }),
          image: MapStyles.circleWithFill(8, highlighted ? highlightColor : color),
          zIndex: Infinity,
        });
      },
      color,
      LayerTypes.QUELLE,
      NetzfehlerSackgassenLayer.MIN_ZOOM_LEVEL
    );
  }
}
