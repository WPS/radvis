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

export enum UmsetzungsstandStatus {
  NEU_ANGELEGT = 'NEU_ANGELEGT',
  IMPORTIERT = 'IMPORTIERT',
  AKTUALISIERT = 'AKTUALISIERT',
  AKTUALISIERUNG_ANGEFORDERT = 'AKTUALISIERUNG_ANGEFORDERT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace UmsetzungsstandStatus {
  const options: EnumOption[] = Object.keys(UmsetzungsstandStatus).map((k: string): EnumOption => {
    switch (k) {
      case UmsetzungsstandStatus.NEU_ANGELEGT:
        return { name: k, displayText: 'Neu angelegt' };
      case UmsetzungsstandStatus.IMPORTIERT:
        return { name: k, displayText: 'Importiert' };
      case UmsetzungsstandStatus.AKTUALISIERT:
        return { name: k, displayText: 'Aktualisiert' };
      case UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT:
        return { name: k, displayText: 'Aktualisierung angefordert' };
    }
    throw new Error('Beschreibung für enum UmsetzungsstandStatus fehlt: ' + k);
  });

  export const displayTextOf: (umsetzungsstandStatus: UmsetzungsstandStatus) => string = (
    umsetzungsstandStatus: UmsetzungsstandStatus
  ): string => {
    const enumOption = options.find(({ name }) => name === umsetzungsstandStatus);
    invariant(enumOption, 'Der UmsetzungsstandStatus hat kein displayText implementiert');

    return enumOption.displayText;
  };
}
