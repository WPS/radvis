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

export enum LeihstationQuellSystem {
  RADVIS = 'RADVIS',
  MOBIDATABW = 'MOBIDATABW',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace LeihstationQuellSystem {
  export const options: EnumOption[] = Object.keys(LeihstationQuellSystem).map((k: string): EnumOption => {
    switch (k) {
      case LeihstationQuellSystem.RADVIS:
        return { name: k, displayText: 'RadVIS' };
      case LeihstationQuellSystem.MOBIDATABW:
        return { name: k, displayText: 'MobiDataBW' };
    }
    throw new Error('Beschreibung für enum LeihstationQuellSystem fehlt: ' + k);
  });

  export const getDisplayText: (leihstationQuellSystem: LeihstationQuellSystem) => string = (
    leihstationQuellSystem: LeihstationQuellSystem
  ): string => {
    const enumOption = options.find(({ name }) => name === leihstationQuellSystem);
    invariant(enumOption, `LeihstationQuellSystem.${leihstationQuellSystem} hat keine EnumOption`);

    return enumOption.displayText;
  };
}
