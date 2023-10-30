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

export enum Status {
  FIKTIV = 'FIKTIV',
  KONZEPTION = 'KONZEPTION',
  IN_BAU = 'IN_BAU',
  UNTER_VERKEHR = 'UNTER_VERKEHR',
  NICHT_FUER_RADVERKEHR_FREIGEGEBEN = 'NICHT_FUER_RADVERKEHR_FREIGEGEBEN',
  NICHT_MIT_RAD_BEFAHRBAR = 'NICHT_MIT_RAD_BEFAHRBAR',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Status {
  export const options: EnumOption[] = Object.keys(Status).map(
    (k: string): EnumOption => {
      switch (k) {
        case Status.FIKTIV:
          return { name: k, displayText: 'Fiktiv' };
        case Status.IN_BAU:
          return { name: k, displayText: 'In Bau' };
        case Status.KONZEPTION:
          return { name: k, displayText: 'Konzeption' };
        case Status.NICHT_FUER_RADVERKEHR_FREIGEGEBEN:
          return { name: k, displayText: 'Nicht für Radverkehr freigegeben' };
        case Status.NICHT_MIT_RAD_BEFAHRBAR:
          return { name: k, displayText: 'Nicht mit dem Rad befahrbar' };
        case Status.UNTER_VERKEHR:
          return { name: k, displayText: 'Unter Verkehr' };
      }
      throw new Error('Beschreibung für enum Status fehlt: ' + k);
    }
  );
}
