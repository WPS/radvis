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

export enum RadNetzKategorie {
  FREIZEIT = 'FREIZEIT',
  ALLTAG = 'ALLTAG',
  ZIELNETZ = 'ZIELNETZ',
  ALLTAG_FREIZEIT = 'ALLTAG_FREIZEIT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace RadNetzKategorie {
  export const options: EnumOption[] = Object.keys(RadNetzKategorie).map(
    (k: string): EnumOption => {
      switch (k) {
        case RadNetzKategorie.FREIZEIT:
          return { name: k, displayText: 'Freizeit' };
        case RadNetzKategorie.ALLTAG:
          return { name: k, displayText: 'Alltag' };
        case RadNetzKategorie.ZIELNETZ:
          return { name: k, displayText: 'Zielnetz' };
        case RadNetzKategorie.ALLTAG_FREIZEIT:
          return { name: k, displayText: 'Alltag und Freizeit' };
      }
      throw new Error('Beschreibung für enum RadNetzKategorie fehlt: ' + k);
    }
  );
}
