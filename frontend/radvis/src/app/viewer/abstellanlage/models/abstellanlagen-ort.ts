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

export enum AbstellanlagenOrt {
  SCHULE = 'SCHULE',
  OEFFENTLICHE_EINRICHTUNG = 'OEFFENTLICHE_EINRICHTUNG',
  BILDUNGSEINRICHTUNG = 'BILDUNGSEINRICHTUNG',
  BIKE_AND_RIDE = 'BIKE_AND_RIDE',
  STRASSENRAUM = 'STRASSENRAUM',
  SONSTIGES = 'SONSTIGES',
  UNBEKANNT = 'UNBEKANNT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace AbstellanlagenOrt {
  export const options: EnumOption[] = Object.keys(AbstellanlagenOrt).map((k: string): EnumOption => {
    switch (k) {
      case AbstellanlagenOrt.SCHULE:
        return { name: k, displayText: 'Schule' };
      case AbstellanlagenOrt.OEFFENTLICHE_EINRICHTUNG:
        return { name: k, displayText: 'Öffentliche Einrichtung' };
      case AbstellanlagenOrt.BILDUNGSEINRICHTUNG:
        return { name: k, displayText: 'Bildungseinrichtung' };
      case AbstellanlagenOrt.BIKE_AND_RIDE:
        return { name: k, displayText: 'B+R' };
      case AbstellanlagenOrt.STRASSENRAUM:
        return { name: k, displayText: 'Straßenraum' };
      case AbstellanlagenOrt.SONSTIGES:
        return { name: k, displayText: 'Sonstiges' };
      case AbstellanlagenOrt.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
    }
    throw new Error('Beschreibung für enum AbstellanlagenOrt fehlt: ' + k);
  });

  export const getDisplayText: (abstellanlagenOrt: AbstellanlagenOrt) => string = (
    abstellanlagenOrt: AbstellanlagenOrt
  ): string => {
    const enumOption = options.find(({ name }) => name === abstellanlagenOrt);
    invariant(enumOption, `AbstellanlagenOrt.${abstellanlagenOrt} hat keine EnumOption`);

    return enumOption.displayText;
  };
}
