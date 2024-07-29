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

export enum Netzklasse {
  RADSCHNELLVERBINDUNG = 'RADSCHNELLVERBINDUNG',
  RADVORRANGROUTEN = 'RADVORRANGROUTEN',
  RADNETZ_FREIZEIT = 'RADNETZ_FREIZEIT',
  RADNETZ_ALLTAG = 'RADNETZ_ALLTAG',
  RADNETZ_ZIELNETZ = 'RADNETZ_ZIELNETZ',
  KREISNETZ_FREIZEIT = 'KREISNETZ_FREIZEIT',
  KREISNETZ_ALLTAG = 'KREISNETZ_ALLTAG',
  KOMMUNALNETZ_FREIZEIT = 'KOMMUNALNETZ_FREIZEIT',
  KOMMUNALNETZ_ALLTAG = 'KOMMUNALNETZ_ALLTAG',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Netzklasse {
  export const options: EnumOption[] = Object.keys(Netzklasse).map((k: string): EnumOption => {
    switch (k) {
      case Netzklasse.RADSCHNELLVERBINDUNG:
        return { name: k, displayText: 'Radschnellverbindung' };
      case Netzklasse.RADVORRANGROUTEN:
        return { name: k, displayText: 'Radvorrangrouten' };
      case Netzklasse.RADNETZ_FREIZEIT:
        return { name: k, displayText: 'Freizeit (RadNETZ)' };
      case Netzklasse.RADNETZ_ALLTAG:
        return { name: k, displayText: 'Alltag (RadNETZ)' };
      case Netzklasse.RADNETZ_ZIELNETZ:
        return { name: k, displayText: 'Zielnetz (RadNETZ)' };
      case Netzklasse.KREISNETZ_FREIZEIT:
        return { name: k, displayText: 'Freizeit (Kreisnetz)' };
      case Netzklasse.KREISNETZ_ALLTAG:
        return { name: k, displayText: 'Alltag (Kreisnetz)' };
      case Netzklasse.KOMMUNALNETZ_FREIZEIT:
        return { name: k, displayText: 'Freizeit (Kommunalnetz)' };
      case Netzklasse.KOMMUNALNETZ_ALLTAG:
        return { name: k, displayText: 'Alltag (Kommunalnetz)' };
    }
    throw new Error('Beschreibung für enum Netzklasse fehlt: ' + k);
  });
}
