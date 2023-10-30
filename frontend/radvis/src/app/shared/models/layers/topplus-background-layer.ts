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
import { LayerTypes } from 'src/app/shared/models/layers/layer-types';
import { RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';

@Injectable()
export class TopplusBackgroundLayer extends RadVisLayer {
  constructor() {
    super(
      'TOPPLUS-BACKGROUND',
      'TopPlus',
      'https://sgx.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_scale_grau/default/EU_EPSG_25832_TOPPLUS/{z}/{y}/{x}.png',
      RadVisLayerTyp.WMTS_UTM32_TILE,
      undefined,
      undefined,
      LayerTypes.HINTERGRUND,
      undefined,
      undefined,
      [-3803165.98427299, 8805908.08284866],
      '© Bundesamt für Kartographie und Geodäsie (aktuelles Jahr)\nDatenquellen: https://sgx.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf'
    );
  }
}
