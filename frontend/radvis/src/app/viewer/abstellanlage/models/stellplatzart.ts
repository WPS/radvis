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

export enum Stellplatzart {
  VORDERRADANSCHLUSS = 'VORDERRADANSCHLUSS',
  ANLEHNBUEGEL = 'ANLEHNBUEGEL',
  FAHRRADBOX = 'FAHRRADBOX',
  DOPPELSTOECKIG = 'DOPPELSTOECKIG',
  SAMMELANLAGE = 'SAMMELANLAGE',
  FAHRRADPARKHAUS = 'FAHRRADPARKHAUS',
  AUTOMATISCHES_PARKSYSTEM = 'AUTOMATISCHES_PARKSYSTEM',
  SONSTIGE = 'SONSTIGE',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Stellplatzart {
  export const options: EnumOption[] = Object.keys(Stellplatzart).map((k: string): EnumOption => {
    switch (k) {
      case Stellplatzart.VORDERRADANSCHLUSS:
        return { name: k, displayText: 'Vorderradanschluss' };
      case Stellplatzart.ANLEHNBUEGEL:
        return { name: k, displayText: 'Anlehnbügel' };
      case Stellplatzart.FAHRRADBOX:
        return { name: k, displayText: 'Fahrradbox' };
      case Stellplatzart.DOPPELSTOECKIG:
        return { name: k, displayText: 'Doppelstöckig' };
      case Stellplatzart.SAMMELANLAGE:
        return { name: k, displayText: 'Sammelanlage' };
      case Stellplatzart.FAHRRADPARKHAUS:
        return { name: k, displayText: 'Fahrradparkhaus' };
      case Stellplatzart.AUTOMATISCHES_PARKSYSTEM:
        return { name: k, displayText: 'Automatisches Parksystem' };
      case Stellplatzart.SONSTIGE:
        return { name: k, displayText: 'Sonstige' };
    }
    throw new Error('Beschreibung für enum Stellplatzart fehlt: ' + k);
  });

  export const getDisplayText: (stellplatzart: Stellplatzart) => string = (stellplatzart: Stellplatzart): string => {
    const enumOption = options.find(({ name }) => name === stellplatzart);
    invariant(enumOption, `Stellplatzart.${stellplatzart} hat keine EnumOption`);

    return enumOption.displayText;
  };
}
