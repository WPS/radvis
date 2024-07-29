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

import { KantenSeitenbezug, Netzbezug } from 'src/app/shared/models/netzbezug';

export const defaultNetzbezug: Netzbezug = {
  kantenBezug: [
    {
      geometrie: {
        coordinates: [
          [0, 1],
          [0, 10],
        ],
        type: 'LineString',
      },
      kanteId: 1,
      linearReferenzierterAbschnitt: {
        bis: 1,
        von: 0,
      },
      kantenSeite: KantenSeitenbezug.BEIDSEITIG,
    },
  ],
  knotenBezug: [
    {
      geometrie: {
        coordinates: [0, 1],
        type: 'Point',
      },
      knotenId: 2,
    },
  ],
  punktuellerKantenBezug: [
    {
      geometrie: {
        coordinates: [10, 12],
        type: 'Point',
      },
      lineareReferenz: 0.5,
      kanteId: 3,
    },
  ],
};
