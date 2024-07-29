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

export enum Tourenkategorie {
  RADFERNWEG = 'RADFERNWEG',
  RADTOUR = 'RADTOUR',
  GRAVEL_TOUR = 'GRAVEL_TOUR',
  MTB_TOUR = 'MTB_TOUR',
  RENNRADTOUR = 'RENNRADTOUR',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Tourenkategorie {
  export const options: EnumOption[] = Object.keys(Tourenkategorie)
    .map((o: string): EnumOption => {
      switch (o) {
        case Tourenkategorie.RADFERNWEG:
          return { name: o, displayText: 'Radfernweg' };
        case Tourenkategorie.RADTOUR:
          return { name: o, displayText: 'Radtour' };
        case Tourenkategorie.GRAVEL_TOUR:
          return { name: o, displayText: 'Gravel-Tour' };
        case Tourenkategorie.MTB_TOUR:
          return { name: o, displayText: 'MTB-Tour' };
        case Tourenkategorie.RENNRADTOUR:
          return { name: o, displayText: 'Rennradtour' };
      }
      throw new Error('Beschreibung für enum Tourenkategorie fehlt: ' + o);
    })
    .sort((a, b) => a.displayText.toLowerCase().localeCompare(b.displayText.toLowerCase()));
}
