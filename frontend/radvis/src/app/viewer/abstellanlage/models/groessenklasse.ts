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

export enum Groessenklasse {
  BASISANGEBOT_XXS = 'BASISANGEBOT_XXS',
  BASISANGEBOT_XS = 'BASISANGEBOT_XS',
  BASISANGEBOT_S = 'BASISANGEBOT_S',
  STANDARDANGEBOT_M = 'STANDARDANGEBOT_M',
  SCHWERPUNKT_L = 'SCHWERPUNKT_L',
  HOTSPOT_XL = 'HOTSPOT_XL',
  GROSSANLAGE_XXL = 'GROSSANLAGE_XXL',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Groessenklasse {
  export const options: EnumOption[] = Object.keys(Groessenklasse).map(
    (k: string): EnumOption => {
      switch (k) {
        case Groessenklasse.BASISANGEBOT_XXS:
          return { name: k, displayText: 'B+R Basisangebot (XXS)' };
        case Groessenklasse.BASISANGEBOT_XS:
          return { name: k, displayText: 'B+R Basisangebot (XS)' };
        case Groessenklasse.BASISANGEBOT_S:
          return { name: k, displayText: 'B+R Basisangebot (S)' };
        case Groessenklasse.STANDARDANGEBOT_M:
          return { name: k, displayText: 'B+R Standardangebot (M)' };
        case Groessenklasse.SCHWERPUNKT_L:
          return { name: k, displayText: 'B+R Schwerpunkt (L)' };
        case Groessenklasse.HOTSPOT_XL:
          return { name: k, displayText: 'B+R Hotspot (XL)' };
        case Groessenklasse.GROSSANLAGE_XXL:
          return { name: k, displayText: 'B+R Großanlage (XXL)' };
      }
      throw new Error('Beschreibung für enum Groessenklasse fehlt: ' + k);
    }
  );

  export const getDisplayText: (groessenklasse: Groessenklasse) => string = (
    groessenklasse: Groessenklasse
  ): string => {
    const enumOption = options.find(({ name }) => name === groessenklasse);
    invariant(enumOption, `Groessenklasse.${groessenklasse} hat keine EnumOption`);

    return enumOption.displayText;
  };
}
