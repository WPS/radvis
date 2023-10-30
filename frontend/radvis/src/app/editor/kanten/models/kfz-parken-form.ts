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

export enum KfzParkenForm {
  UNBEKANNT = 'UNBEKANNT',
  PARKBUCHTEN = 'PARKBUCHTEN',
  FAHRBAHNPARKEN_MARKIERT = 'FAHRBAHNPARKEN_MARKIERT',
  FAHRBAHNPARKEN_UNMARKIERT = 'FAHRBAHNPARKEN_UNMARKIERT',
  GEHWEGPARKEN_MARKIERT = 'GEHWEGPARKEN_MARKIERT',
  GEHWEGPARKEN_UNMARKIERT = 'GEHWEGPARKEN_UNMARKIERT',
  HALBES_GEHWEGPARKEN_MARKIERT = 'HALBES_GEHWEGPARKEN_MARKIERT',
  HALBES_GEHWEGPARKEN_UNMARKIERT = 'HALBES_GEHWEGPARKEN_UNMARKIERT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace KfzParkenForm {
  export const options: EnumOption[] = Object.keys(KfzParkenForm).map(
    (k: string): EnumOption => {
      switch (k) {
        case KfzParkenForm.PARKBUCHTEN:
          return { name: k, displayText: 'Parkbuchten' };
        case KfzParkenForm.FAHRBAHNPARKEN_MARKIERT:
          return { name: k, displayText: 'Fahrbahnparken (markiert)' };
        case KfzParkenForm.FAHRBAHNPARKEN_UNMARKIERT:
          return { name: k, displayText: 'Fahrbahnparken (unmarkiert)' };
        case KfzParkenForm.GEHWEGPARKEN_MARKIERT:
          return { name: k, displayText: 'Gehwegparken (markiert)' };
        case KfzParkenForm.GEHWEGPARKEN_UNMARKIERT:
          return { name: k, displayText: 'Gehwegparken (unmarkiert)' };
        case KfzParkenForm.HALBES_GEHWEGPARKEN_MARKIERT:
          return { name: k, displayText: 'Halbes Gehwegparken (markiert)' };
        case KfzParkenForm.HALBES_GEHWEGPARKEN_UNMARKIERT:
          return { name: k, displayText: 'Halbes Gehwegparken (unmarkiert)' };
        case KfzParkenForm.UNBEKANNT:
          return { name: k, displayText: 'Unbekannt' };
      }
      throw new Error('Beschreibung für enum KfzParkenForm fehlt: ' + k);
    }
  );
}
