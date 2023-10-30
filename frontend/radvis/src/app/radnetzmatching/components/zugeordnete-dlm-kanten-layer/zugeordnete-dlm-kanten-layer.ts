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
import { Icon } from 'ol/style';
import Style from 'ol/style/Style';
import { LayerTypes } from 'src/app/shared/models/layers/layer-types';
import { Color, RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';

@Injectable()
export class ZugeordneteDlmKantenLayer extends RadVisLayer {
  public static LAYER_ID = 'ZUGEORDNETE_DLM_KANTEN';
  public static LAYER_NAME = 'Zugeordnete DLM-Kanten';
  public static COLOR = [171, 118, 38, 1];

  constructor() {
    super(
      ZugeordneteDlmKantenLayer.LAYER_ID,
      ZugeordneteDlmKantenLayer.LAYER_NAME,
      `${NetzausschnittService.BASE_URL}/kantenDLMRadNETZZugeordnet`,
      RadVisLayerTyp.GEO_JSON,
      (feature: FeatureLike, resolution: number): Style => {
        return new Style({
          geometry: new Point((feature.getGeometry() as LineString).getCoordinateAt(0.5)),
          image: new Icon({
            anchor: [0.5, 0.5],
            scale: 1.5 / Math.pow(resolution, 1 / 5),
            src: './assets/link.svg',
            color: ZugeordneteDlmKantenLayer.COLOR,
          }),
        });
      },
      ZugeordneteDlmKantenLayer.COLOR as Color,
      LayerTypes.QUELLE,
      13
    );
  }
}
