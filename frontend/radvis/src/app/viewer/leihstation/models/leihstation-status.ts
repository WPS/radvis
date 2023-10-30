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

export enum LeihstationStatus {
  GEPLANT = 'GEPLANT',
  AKTIV = 'AKTIV',
  AUSSER_BETRIEB = 'AUSSER_BETRIEB',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace LeihstationStatus {
  export const options: EnumOption[] = Object.keys(LeihstationStatus)
    .map(
      (o: string): EnumOption => {
        switch (o) {
          case LeihstationStatus.GEPLANT:
            return { name: o, displayText: 'Geplant' };
          case LeihstationStatus.AKTIV:
            return { name: o, displayText: 'Aktiv' };
          case LeihstationStatus.AUSSER_BETRIEB:
            return { name: o, displayText: 'Außer Betrieb' };
        }
        throw new Error('Beschreibung für enum LeihstationStatus fehlt: ' + o);
      }
    )
    .sort((a, b) => a.displayText.toLowerCase().localeCompare(b.displayText.toLowerCase()));

  export const getDisplayText: (leihstationStatus: LeihstationStatus) => string = (
    leihstationStatus: LeihstationStatus
  ): string => {
    const enumOption = options.find(({ name }) => name === leihstationStatus);
    invariant(enumOption, `LeihstationStatus.${leihstationStatus} hat keine EnumOption`);

    return enumOption.displayText;
  };
}
