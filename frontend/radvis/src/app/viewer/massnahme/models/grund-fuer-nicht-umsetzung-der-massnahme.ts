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

export enum GrundFuerNichtUmsetzungDerMassnahme {
  RADNETZ_VERLEGUNG = 'RADNETZ_VERLEGUNG',
  LAUT_VERKEHRSSCHAU_NICHT_ERFORDERLICH = 'LAUT_VERKEHRSSCHAU_NICHT_ERFORDERLICH',
  AUS_SUBJEKTIVER_SICHT_NICHT_ERFORDERLICH = 'AUS_SUBJEKTIVER_SICHT_NICHT_ERFORDERLICH',
  NOCH_IN_PLANUNG_UMSETZUNG = 'NOCH_IN_PLANUNG_UMSETZUNG',
  KAPAZITAETSGRUENDE = 'KAPAZITAETSGRUENDE',
  SONSTIGER_GRUND = 'SONSTIGER_GRUND',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace GrundFuerNichtUmsetzungDerMassnahme {
  export const options: EnumOption[] = Object.keys(GrundFuerNichtUmsetzungDerMassnahme).map(
    (k: string): EnumOption => {
      switch (k) {
        case GrundFuerNichtUmsetzungDerMassnahme.RADNETZ_VERLEGUNG:
          return { name: k, displayText: 'RadNetz Verlegung (keine neue Maßnahme erforderlich)' };
        case GrundFuerNichtUmsetzungDerMassnahme.LAUT_VERKEHRSSCHAU_NICHT_ERFORDERLICH:
          return {
            name: k,
            displayText: 'Maßnahme laut Verkehrsschau nicht erforderlich',
          };
        case GrundFuerNichtUmsetzungDerMassnahme.AUS_SUBJEKTIVER_SICHT_NICHT_ERFORDERLICH:
          return {
            name: k,
            displayText:
              'Maßnahme aus subjektiver Sicht nicht erforderlich (Begründung und Ansprechpartner im Anmerkungsfeld ergänzen)',
          };
        case GrundFuerNichtUmsetzungDerMassnahme.NOCH_IN_PLANUNG_UMSETZUNG:
          return { name: k, displayText: 'Maßnahme ist derzeit noch in der Planung/Umsetzung' };
        case GrundFuerNichtUmsetzungDerMassnahme.KAPAZITAETSGRUENDE:
          return {
            name: k,
            displayText: 'Maßnahme aus Kapazitätsgründen noch nicht angegangen',
          };
        case GrundFuerNichtUmsetzungDerMassnahme.SONSTIGER_GRUND:
          return { name: k, displayText: 'Sonstiger Grund (Erläuterung erfolgt im Anmerkungsfeld)' };
      }
      throw new Error('Beschreibung für enum GrundFuerAbweichungZumMassnahmenblatt fehlt: ' + k);
    }
  );
}
