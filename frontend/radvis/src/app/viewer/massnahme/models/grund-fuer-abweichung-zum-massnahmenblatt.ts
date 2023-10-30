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

export enum GrundFuerAbweichungZumMassnahmenblatt {
  RADNETZ_WURDE_VERLEGT = 'RADNETZ_WURDE_VERLEGT',
  UMSETZUNG_ALTERNATIVER_MASSNAHME_ERFORDERLICH = 'UMSETZUNG_ALTERNATIVER_MASSNAHME_ERFORDERLICH',
  SONSTIGER_GRUND = 'SONSTIGER_GRUND',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace GrundFuerAbweichungZumMassnahmenblatt {
  export const options: EnumOption[] = Object.keys(GrundFuerAbweichungZumMassnahmenblatt).map(
    (k: string): EnumOption => {
      switch (k) {
        case GrundFuerAbweichungZumMassnahmenblatt.RADNETZ_WURDE_VERLEGT:
          return { name: k, displayText: 'RadNetz wurde verlegt, alternative Maßnahme umgesetzt' };
        case GrundFuerAbweichungZumMassnahmenblatt.UMSETZUNG_ALTERNATIVER_MASSNAHME_ERFORDERLICH:
          return {
            name: k,
            displayText: 'Örtliche Verkehrsschau ergab, dass Umsetzung alternativer Maßnahme erforderlich ist',
          };
        case GrundFuerAbweichungZumMassnahmenblatt.SONSTIGER_GRUND:
          return { name: k, displayText: 'Sonstiger Grund (Erläuterung erfolgt im Anmerkungsfeld)' };
      }
      throw new Error('Beschreibung für enum GrundFuerAbweichungZumMassnahmenblatt fehlt: ' + k);
    }
  );
}
