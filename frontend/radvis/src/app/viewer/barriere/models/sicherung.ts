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
import invariant from 'tiny-invariant';

export enum Sicherung {
  KEINE_BODENMARKIERUNG = 'KEINE_BODENMARKIERUNG',
  BODENMARKIERUNG = 'BODENMARKIERUNG',
  RETROREFLEKTIERENDE_BODENMARKIERUNG = 'RETROREFLEKTIERENDE_BODENMARKIERUNG',
  TAKTILE_BODENMARKIERUNG = 'TAKTILE_BODENMARKIERUNG',
  BAULICHE_SICHERUNG = 'BAULICHE_SICHERUNG',
  BELEUCHTUNG = 'BELEUCHTUNG',
  BELEUCHTUNG_UND_RETROREFLEKTIERENDE_BODENMARKIERUNG = 'BELEUCHTUNG_UND_RETROREFLEKTIERENDE_BODENMARKIERUNG',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Sicherung {
  export const options: EnumOption[] = Object.keys(Sicherung).map((k: string): EnumOption => {
    switch (k) {
      case Sicherung.KEINE_BODENMARKIERUNG:
        return { name: Sicherung.KEINE_BODENMARKIERUNG, displayText: 'Keine Bodenmarkierung vorhanden' };
      case Sicherung.BODENMARKIERUNG:
        return { name: Sicherung.BODENMARKIERUNG, displayText: 'Bodenmarkierung vorhanden' };
      case Sicherung.RETROREFLEKTIERENDE_BODENMARKIERUNG:
        return {
          name: Sicherung.RETROREFLEKTIERENDE_BODENMARKIERUNG,
          displayText: 'Retroreflektierende Bodenmarkierung vorhanden',
        };
      case Sicherung.TAKTILE_BODENMARKIERUNG:
        return { name: Sicherung.TAKTILE_BODENMARKIERUNG, displayText: 'Taktile Bodenmarkierung vorhanden' };
      case Sicherung.BAULICHE_SICHERUNG:
        return {
          name: Sicherung.BAULICHE_SICHERUNG,
          displayText: 'Bauliche Sicherung (z.B. durch Aufpflasterung vorhanden)',
        };
      case Sicherung.BELEUCHTUNG:
        return { name: Sicherung.BELEUCHTUNG, displayText: 'Beleuchtung vorhanden' };
      case Sicherung.BELEUCHTUNG_UND_RETROREFLEKTIERENDE_BODENMARKIERUNG:
        return {
          name: Sicherung.BELEUCHTUNG_UND_RETROREFLEKTIERENDE_BODENMARKIERUNG,
          displayText: 'Beleuchtung und retroreflektierende Bodenmarkierung vorhanden',
        };
    }
    throw new Error('Beschreibung für enum Sicherung fehlt: ' + k);
  });

  export const getDisplayText: (sicherung: Sicherung) => string = (sicherung: Sicherung): string => {
    const enumOption = options.find(({ name }) => name === sicherung);
    invariant(enumOption, 'Die Sicherung hat kein displayText implementiert');

    return enumOption.displayText;
  };
}
