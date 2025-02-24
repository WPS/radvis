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

export enum Absenkung {
  UNBEKANNT = 'UNBEKANNT',
  GRUNDSTUECKSZUFAHRTEN_NICHT_ABGESENKT = 'GRUNDSTUECKSZUFAHRTEN_NICHT_ABGESENKT',
  GRUNDSTUECKSZUFAHRTEN_ABGESENKT = 'GRUNDSTUECKSZUFAHRTEN_ABGESENKT',
  KEINE_GRUNDSTUECKSZUFAHRTEN_VORHANDEN = 'KEINE_GRUNDSTUECKSZUFAHRTEN_VORHANDEN',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Absenkung {
  export const options: EnumOption[] = Object.keys(Absenkung).map((k: string): EnumOption => {
    switch (k) {
      case Absenkung.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
      case Absenkung.GRUNDSTUECKSZUFAHRTEN_NICHT_ABGESENKT:
        return { name: k, displayText: 'nein, Grundstückzufahrten sind (überwiegend) nicht abgesenkt' };
      case Absenkung.GRUNDSTUECKSZUFAHRTEN_ABGESENKT:
        return { name: k, displayText: 'ja, Grundstückzufahrten sind (überwiegend) abgesenkt' };
      case Absenkung.KEINE_GRUNDSTUECKSZUFAHRTEN_VORHANDEN:
        return { name: k, displayText: 'Keine Grundstückszufahrten auf Streckenabschnitt' };
    }

    throw new Error('Beschreibung für enum Absenkung fehlt: ' + k);
  });
}
