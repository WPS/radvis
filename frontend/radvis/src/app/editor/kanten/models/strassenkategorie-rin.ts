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

export enum StrassenkategorieRIN {
  KONTINENTAL = 'KONTINENTAL',
  GROSSRAEUMIG = 'GROSSRAEUMIG',
  UEBERREGIONAL = 'UEBERREGIONAL',
  REGIONAL = 'REGIONAL',
  NAHRAEUMIG = 'NAHRAEUMIG',
  KLEINRAEUMIG = 'KLEINRAEUMIG',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace StrassenkategorieRIN {
  export const options: EnumOption[] = Object.keys(StrassenkategorieRIN).map(
    (k: string): EnumOption => {
      switch (k) {
        case StrassenkategorieRIN.KONTINENTAL:
          return { name: k, displayText: '  0 - kontinental' };
        case StrassenkategorieRIN.GROSSRAEUMIG:
          return { name: k, displayText: '  I - großräumig' };
        case StrassenkategorieRIN.UEBERREGIONAL:
          return { name: k, displayText: ' II - überregional' };
        case StrassenkategorieRIN.REGIONAL:
          return { name: k, displayText: 'III - regional' };
        case StrassenkategorieRIN.NAHRAEUMIG:
          return { name: k, displayText: ' IV - nahräumig' };
        case StrassenkategorieRIN.KLEINRAEUMIG:
          return { name: k, displayText: '  V - kleinräumig' };
      }
      throw new Error('Beschreibung für enum StrassenkategorieRIN fehlt: ' + k);
    }
  );
}
