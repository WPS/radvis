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

import { EnumOption } from 'src/app/form-elements/models/enum-option';

export enum Hoechstgeschwindigkeit {
  KFZ_NICHT_ZUGELASSEN = 'KFZ_NICHT_ZUGELASSEN',
  MAX_9_KMH = 'MAX_9_KMH',
  MAX_20_KMH = 'MAX_20_KMH',
  MAX_30_KMH = 'MAX_30_KMH',
  MAX_40_KMH = 'MAX_40_KMH',
  MAX_50_KMH = 'MAX_50_KMH',
  MAX_60_KMH = 'MAX_60_KMH',
  MAX_70_KMH = 'MAX_70_KMH',
  MAX_80_KMH = 'MAX_80_KMH',
  MAX_90_KMH = 'MAX_90_KMH',
  MAX_100_KMH = 'MAX_100_KMH',
  UEBER_100_KMH = 'UEBER_100_KMH',
  UNBEKANNT = 'UNBEKANNT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Hoechstgeschwindigkeit {
  export const options: EnumOption[] = Object.keys(Hoechstgeschwindigkeit).map(
    (k: string): EnumOption => {
      switch (k) {
        case Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN:
          return { name: k, displayText: 'Kfz nicht zugelassen' };
        case Hoechstgeschwindigkeit.MAX_9_KMH:
          return { name: k, displayText: '9 km/h' };
        case Hoechstgeschwindigkeit.MAX_20_KMH:
          return { name: k, displayText: '20 km/h' };
        case Hoechstgeschwindigkeit.MAX_30_KMH:
          return { name: k, displayText: '30 km/h' };
        case Hoechstgeschwindigkeit.MAX_40_KMH:
          return { name: k, displayText: '40 km/h' };
        case Hoechstgeschwindigkeit.MAX_50_KMH:
          return { name: k, displayText: '50 km/h' };
        case Hoechstgeschwindigkeit.MAX_60_KMH:
          return { name: k, displayText: '60 km/h' };
        case Hoechstgeschwindigkeit.MAX_70_KMH:
          return { name: k, displayText: '70 km/h' };
        case Hoechstgeschwindigkeit.MAX_80_KMH:
          return { name: k, displayText: '80 km/h' };
        case Hoechstgeschwindigkeit.MAX_90_KMH:
          return { name: k, displayText: '90 km/h' };
        case Hoechstgeschwindigkeit.MAX_100_KMH:
          return { name: k, displayText: '100 km/h' };
        case Hoechstgeschwindigkeit.UEBER_100_KMH:
          return { name: k, displayText: '> 100 km/h' };
        case Hoechstgeschwindigkeit.UNBEKANNT:
          return { name: k, displayText: 'Unbekannt' };
      }
      throw new Error('Beschreibung für enum Hoechstgeschwindigkeit fehlt: ' + k);
    }
  );
}
