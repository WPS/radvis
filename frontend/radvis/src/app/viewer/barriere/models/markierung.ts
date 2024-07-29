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

export enum Markierung {
  UNMARKIERTE_ABSPERRANLAGE = 'UNMARKIERTE_ABSPERRANLAGE',
  ROTWEISS_RETROREFLEKTIERENDE_MARKIERUNG = 'ROTWEISS_RETROREFLEKTIERENDE_MARKIERUNG',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Markierung {
  export const options: EnumOption[] = Object.keys(Markierung).map((k: string): EnumOption => {
    switch (k) {
      case Markierung.UNMARKIERTE_ABSPERRANLAGE:
        return { name: Markierung.UNMARKIERTE_ABSPERRANLAGE, displayText: 'Unmarkierte Absperranlage' };
      case Markierung.ROTWEISS_RETROREFLEKTIERENDE_MARKIERUNG:
        return {
          name: Markierung.ROTWEISS_RETROREFLEKTIERENDE_MARKIERUNG,
          displayText: 'Rot-Weiß retroreflektierende Markierung vorhanden',
        };
    }
    throw new Error('Beschreibung für enum Markierung fehlt: ' + k);
  });

  export const getDisplayText: (markierung: Markierung) => string = (markierung: Markierung): string => {
    const enumOption = options.find(({ name }) => name === markierung);
    invariant(enumOption, 'Die Markierung hat kein displayText implementiert');

    return enumOption.displayText;
  };
}
