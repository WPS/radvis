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

export enum Konzeptionsquelle {
  RADNETZ_MASSNAHME = 'RADNETZ_MASSNAHME',
  KOMMUNALES_KONZEPT = 'KOMMUNALES_KONZEPT',
  KREISKONZEPT = 'KREISKONZEPT',
  SONSTIGE = 'SONSTIGE',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Konzeptionsquelle {
  export const options: EnumOption[] = Object.keys(Konzeptionsquelle).map(
    (k: string): EnumOption => {
      switch (k) {
        case Konzeptionsquelle.RADNETZ_MASSNAHME:
          return { name: k, displayText: 'RadNETZ-Maßnahme' };
        case Konzeptionsquelle.KOMMUNALES_KONZEPT:
          return { name: k, displayText: 'Kommunales Konzept' };
        case Konzeptionsquelle.KREISKONZEPT:
          return { name: k, displayText: 'Kreiskonzept' };
        case Konzeptionsquelle.SONSTIGE:
          return { name: k, displayText: 'Sonstige' };
      }
      throw new Error('Beschreibung für enum Konzeptionsquelle fehlt: ' + k);
    }
  );
}
