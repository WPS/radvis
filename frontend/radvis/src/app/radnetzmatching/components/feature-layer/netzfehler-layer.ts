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
import { LayerTypes } from 'src/app/shared/models/layers/layer-types';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Color, RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';

export class NetzfehlerLayer extends RadVisLayer {
  public static ID_PREFIX = 'NETZFEHLER_';
  public static LAYER_ID = NetzfehlerLayer.ID_PREFIX + 'SONSTIGE';
  public static MIN_ZOOM_LEVEL = 14;

  constructor() {
    const color: Color = [255, 0, 0, 0.2];
    const warningSymbolTextStyle = new Text({
      text: '⚠',
      font: 'bold 20px Roboto',
      fill: new Fill({
        color: 'red',
      }),
      overflow: true,
    });

    const netzfehlerLineStyleThin = new Style({
      stroke: new Stroke({
        color,
        width: MapStyles.LINE_WIDTH_THIN,
      }),
      text: warningSymbolTextStyle,
    });
    const netzfehlerLineStyleMedium = new Style({
      stroke: new Stroke({
        color,
        width: MapStyles.LINE_WIDTH_MEDIUM,
      }),
      text: warningSymbolTextStyle,
    });
    const netzfehlerLineStyleThick = new Style({
      stroke: new Stroke({
        color,
        width: MapStyles.LINE_WIDTH_THICK,
      }),
      text: warningSymbolTextStyle,
    });
    const netzfehlerWarningPointStyle = new Style({
      text: new Text({
        text: '⚠',
        font: 'bold 20px Roboto',
        fill: new Fill({
          color: 'red',
        }),
        offsetY: -12,
      }),
      image: MapStyles.circleWithFill(4, color),
    });
    super(
      NetzfehlerLayer.LAYER_ID,
      'Netzfehler - Sonstige',
      `${NetzausschnittService.BASE_URL}/netzfehler/SONSTIGER_FEHLER,NETZBILDUNG,ATTRIBUT_ABBILDUNG`,
      RadVisLayerTyp.GEO_JSON,
      (feature, resolution): Style => {
        if (feature.getGeometry()?.getType() === 'Point') {
          return netzfehlerWarningPointStyle;
        }
        if (resolution < MapStyles.RESOLUTION_SMALL) {
          return netzfehlerLineStyleThick;
        } else if (resolution < MapStyles.RESOLUTION_MEDIUM) {
          return netzfehlerLineStyleMedium;
        } else {
          return netzfehlerLineStyleThin;
        }
      },
      color,
      LayerTypes.QUELLE,
      NetzfehlerLayer.MIN_ZOOM_LEVEL
    );
  }
}
