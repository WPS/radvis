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

import { WeitereKartenebeneTyp } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene-typ';

export interface WeitereKartenebene {
  id: number;
  name: string;
  url: string;
  weitereKartenebeneTyp: WeitereKartenebeneTyp;
  deckkraft: number;
  zoomstufe: number;
  zindex: number; // muss durchgehend kleingeschrieben werden. zIndex wird nicht korrekt vom BE ans FE uebergeben.
  farbe?: string;
  quellangabe: string;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace WeitereKartenebene {
  export const LAYER_NAME = 'WEITERE_WFS_KARTENEBENEN';
  export const LAYER_ID_KEY = 'WEITERE_KARTENEBENEN_LAYER_ID';
  export const EXTERNE_WMS_FEATURE_ID_PROPERTY_NAME = 'EXTERNE_WMS_FEATURE_ID';
}
