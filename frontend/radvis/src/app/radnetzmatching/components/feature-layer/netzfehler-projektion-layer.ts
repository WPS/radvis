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
import Stroke from 'ol/style/Stroke';
import Style from 'ol/style/Style';
import Text from 'ol/style/Text';
import { NetzfehlerLayer } from 'src/app/radnetzmatching/components/feature-layer/netzfehler-layer';
import { LayerTypes } from 'src/app/shared/models/layers/layer-types';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Color, RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';

export class NetzfehlerProjektionLayer extends RadVisLayer {
  public static LAYER_ID = NetzfehlerLayer.ID_PREFIX + 'PROJEKTION';
  public static MIN_ZOOM_LEVEL = 11.2;

  constructor() {
    const color: Color = [67, 165, 71, 1];
    const lineColor: Color = [67, 165, 71, 0.5];
    const highlightColor: Color = [67, 165, 71, 1];

    super(
      NetzfehlerProjektionLayer.LAYER_ID,
      'Attributfehler',
      `${NetzausschnittService.BASE_URL}/netzfehler/ATTRIBUT_PROJEKTION`,
      RadVisLayerTyp.GEO_JSON,
      (feature): Style => {
        const highlighted: boolean = feature.getProperties().highlighted;
        return new Style({
          text: new Text({
            text: '⇛',
            font: 'bold 20px Roboto',
            fill: new Fill({
              color,
            }),
          }),
          image: MapStyles.circleWithFill(8, lineColor),
          zIndex: Infinity,
          stroke: new Stroke({
            color: highlighted ? highlightColor : lineColor,
            width: MapStyles.LINE_WIDTH_THICK,
          }),
        });
      },
      color,
      LayerTypes.QUELLE,
      NetzfehlerProjektionLayer.MIN_ZOOM_LEVEL
    );
  }
}
