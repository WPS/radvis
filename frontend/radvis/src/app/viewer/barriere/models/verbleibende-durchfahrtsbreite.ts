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

export enum VerbleibendeDurchfahrtsbreite {
  KEINE_DURCHFAHRT_MOEGLICH = 'KEINE_DURCHFAHRT_MOEGLICH',
  KLEINER_130CM = 'KLEINER_130CM',
  ZWISCHEN_130CM_160CM = 'ZWISCHEN_130CM_160CM',
  ZWISCHEN_160CM_250CM = 'ZWISCHEN_160CM_250CM',
  GROESSER_250CM = 'GROESSER_250CM',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace VerbleibendeDurchfahrtsbreite {
  export const options: EnumOption[] = Object.keys(VerbleibendeDurchfahrtsbreite).map((k: string): EnumOption => {
    switch (k) {
      case VerbleibendeDurchfahrtsbreite.KEINE_DURCHFAHRT_MOEGLICH:
        return {
          name: VerbleibendeDurchfahrtsbreite.KEINE_DURCHFAHRT_MOEGLICH,
          displayText: 'keine Durchfahrt möglich',
        };
      case VerbleibendeDurchfahrtsbreite.KLEINER_130CM:
        return { name: VerbleibendeDurchfahrtsbreite.KLEINER_130CM, displayText: '< 1,30 m' };
      case VerbleibendeDurchfahrtsbreite.ZWISCHEN_130CM_160CM:
        return { name: VerbleibendeDurchfahrtsbreite.ZWISCHEN_130CM_160CM, displayText: '1,30 m bis 1,60 m' };
      case VerbleibendeDurchfahrtsbreite.ZWISCHEN_160CM_250CM:
        return { name: VerbleibendeDurchfahrtsbreite.ZWISCHEN_160CM_250CM, displayText: '1,60 m bis 2,50 m' };
      case VerbleibendeDurchfahrtsbreite.GROESSER_250CM:
        return { name: VerbleibendeDurchfahrtsbreite.GROESSER_250CM, displayText: '> 2,50 m' };
    }
    throw new Error('Beschreibung für enum VerbleibendeDurchfahrtsbreite fehlt: ' + k);
  });
  // .sort((a, b) => a.displayText.toLowerCase().localeCompare(b.displayText.toLowerCase()));

  export const getDisplayText: (durchfahrtsbreite: VerbleibendeDurchfahrtsbreite) => string = (
    durchfahrtsbreite: VerbleibendeDurchfahrtsbreite
  ): string => {
    const enumOption = options.find(({ name }) => name === durchfahrtsbreite);
    invariant(enumOption, 'Die VerbleibendeDurchfahrtsbreite hat kein displayText implementiert');

    return enumOption.displayText;
  };
}
