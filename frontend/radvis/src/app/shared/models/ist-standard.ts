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

export enum IstStandard {
  RADSCHNELLVERBINDUNG = 'RADSCHNELLVERBINDUNG',
  STARTSTANDARD_RADNETZ = 'STARTSTANDARD_RADNETZ',
  ZIELSTANDARD_RADNETZ = 'ZIELSTANDARD_RADNETZ',
  BASISSTANDARD = 'BASISSTANDARD',
  RADVORRANGROUTEN = 'RADVORRANGROUTEN',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace IstStandard {
  export const options: EnumOption[] = Object.keys(IstStandard).map((k: string): EnumOption => {
    switch (k) {
      case IstStandard.RADSCHNELLVERBINDUNG:
        return { name: k, displayText: 'Radschnellverbindung' };
      case IstStandard.STARTSTANDARD_RADNETZ:
        return { name: k, displayText: 'RadNETZ-Startstandard' };
      case IstStandard.ZIELSTANDARD_RADNETZ:
        return { name: k, displayText: 'RadNETZ-Zielstandard' };
      case IstStandard.BASISSTANDARD:
        return { name: k, displayText: 'Basisstandard' };
      case IstStandard.RADVORRANGROUTEN:
        return { name: k, displayText: 'Radvorrangrouten' };
    }
    throw new Error('Beschreibung für enum IstStandard fehlt: ' + k);
  });
}
