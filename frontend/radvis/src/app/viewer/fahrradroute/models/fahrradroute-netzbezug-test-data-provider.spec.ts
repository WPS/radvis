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

import { BelagArt } from 'src/app/shared/models/belag-art';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { FahrradrouteNetzbezug } from 'src/app/viewer/fahrradroute/models/fahrradroute.netzbezug';

export const defaultFahrradrouteNetzbezug = {
  geometrie: {
    type: 'LineString',
    coordinates: [
      [10, 10, 10],
      [20, 20, 20],
    ],
  } as LineStringGeojson,
  stuetzpunkte: [
    [10, 10],
    [20, 20],
  ],
  kantenIDs: [8],
  profilEigenschaften: [{ vonLR: 0, bisLR: 0, belagArt: BelagArt.ASPHALT }],
} as FahrradrouteNetzbezug;
