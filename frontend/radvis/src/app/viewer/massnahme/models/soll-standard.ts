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

export enum SollStandard {
  RADSCHNELLVERBINDUNG = 'RADSCHNELLVERBINDUNG',
  STARTSTANDARD_RADNETZ = 'STARTSTANDARD_RADNETZ',
  ZIELSTANDARD_RADNETZ = 'ZIELSTANDARD_RADNETZ',
  BASISSTANDARD = 'BASISSTANDARD',
  RADVORRANGROUTEN = 'RADVORRANGROUTEN',
  KEIN_STANDARD_ERFUELLT = 'KEIN_STANDARD_ERFUELLT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace SollStandard {
  export const options: EnumOption[] = Object.keys(SollStandard).map(
    (k: string): EnumOption => {
      switch (k) {
        case SollStandard.RADSCHNELLVERBINDUNG:
          return { name: k, displayText: 'Radschnellverbindung' };
        case SollStandard.STARTSTANDARD_RADNETZ:
          return { name: k, displayText: 'RadNETZ-Startstandard' };
        case SollStandard.ZIELSTANDARD_RADNETZ:
          return { name: k, displayText: 'RadNETZ-Zielstandard' };
        case SollStandard.BASISSTANDARD:
          return { name: k, displayText: 'Basisstandard' };
        case SollStandard.RADVORRANGROUTEN:
          return { name: k, displayText: 'Radvorrangrouten' };
        case SollStandard.KEIN_STANDARD_ERFUELLT:
          return { name: k, displayText: 'Kein Standard erfüllt' };
      }
      throw new Error('Beschreibung für enum SollStandard fehlt: ' + k);
    }
  );
}
