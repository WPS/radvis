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

export enum ArtDerAuswertung {
  DURCHSCHNITT_PRO_STUNDE = 'DURCHSCHNITT_PRO_STUNDE',
  DURCHSCHNITT_PRO_WOCHENTAG = 'DURCHSCHNITT_PRO_WOCHENTAG',
  DURCHSCHNITT_PRO_MONAT = 'DURCHSCHNITT_PRO_MONAT',
  SUMME_PRO_JAHR = 'SUMME_PRO_JAHR',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace ArtDerAuswertung {
  export const options: EnumOption[] = Object.keys(ArtDerAuswertung).map(
    (o: string): EnumOption => {
      switch (o) {
        case ArtDerAuswertung.DURCHSCHNITT_PRO_STUNDE:
          return { name: o, displayText: 'Durchschnitt pro Stunde' };
        case ArtDerAuswertung.DURCHSCHNITT_PRO_WOCHENTAG:
          return { name: o, displayText: 'Durchschnitt pro Wochentag' };
        case ArtDerAuswertung.DURCHSCHNITT_PRO_MONAT:
          return { name: o, displayText: 'Durchschnitt pro Monat' };
        case ArtDerAuswertung.SUMME_PRO_JAHR:
          return { name: o, displayText: 'Summe pro Jahr' };
      }
      throw new Error('Beschreibung für enum ArtDerAuswertung fehlt: ' + o);
    }
  );
}
