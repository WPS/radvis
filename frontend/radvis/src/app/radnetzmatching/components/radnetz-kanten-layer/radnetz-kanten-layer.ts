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

import { Injectable } from '@angular/core';
import { FeatureLike } from 'ol/Feature';
import { LineString, Point } from 'ol/geom';
import GeometryType from 'ol/geom/GeometryType';
import { Icon } from 'ol/style';
import Stroke from 'ol/style/Stroke';
import Style from 'ol/style/Style';
import { LayerTypes } from 'src/app/shared/models/layers/layer-types';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Color, RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';

@Injectable()
export class RadnetzKantenLayer extends RadVisLayer {
  public static LAYER_ID = 'RadNETZ';
  public static LAYER_NAME = 'RadNETZ-Kanten';

  constructor() {
    const defaultColor: Color = MapStyles.FEATURE_SELECT_COLOR as Color;

    const lightRed = [239, 170, 195, 1];
    const defaultPointLargeStyle = new Style({
      image: MapStyles.circleWithFill(4, lightRed),
    });

    const stylingFunction = (feature: FeatureLike, resolution: number): Style[] => {
      if (feature.getGeometry()?.getType() === GeometryType.POINT && resolution < MapStyles.RESOLUTION_SMALL) {
        return [defaultPointLargeStyle];
      }
      let kantenColor = MapStyles.FEATURE_SELECT_COLOR_TRANSPARENT as Color;
      const styles: Style[] = [];
      if (feature.get('zugeordnet') === true) {
        styles.push(
          new Style({
            geometry: new Point((feature.getGeometry() as LineString).getCoordinateAt(0.5)),
            image: new Icon({
              anchor: [0.5, 0.5],
              scale: 1.3,
              src: './assets/link.svg',
              color: MapStyles.FEATURE_COLOR,
            }),
          })
        );
      }
      if (feature.get('highlighted') === true) {
        kantenColor = defaultColor;
      }
      styles.push(
        new Style({
          stroke: new Stroke({
            color: kantenColor,
            width: MapStyles.LINE_WIDTH_THICK,
          }),
        })
      );
      return styles;
    };

    super(
      RadnetzKantenLayer.LAYER_ID,
      RadnetzKantenLayer.LAYER_NAME,
      `${NetzausschnittService.BASE_URL}/quelle/RadNETZ`,
      RadVisLayerTyp.GEO_JSON,
      stylingFunction,
      defaultColor,
      LayerTypes.QUELLE,
      15.25
    );
  }
}
