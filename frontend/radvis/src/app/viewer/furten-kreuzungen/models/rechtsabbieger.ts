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

export enum Rechtsabbieger {
  GRUENPFEIL_ALLE = 'GRUENPFEIL_ALLE',
  GRUENPFEIL_RAD = 'GRUENPFEIL_RAD',
  RECHTSABBIEGER = 'RECHTSABBIEGER',
  UNBEKANNT = 'UNBEKANNT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Rechtsabbieger {
  export const options: EnumOption[] = Object.keys(Rechtsabbieger).map((k: string): EnumOption => {
    switch (k) {
      case Rechtsabbieger.GRUENPFEIL_ALLE:
        return { name: k, displayText: 'Grünpfeil (Alle)' };
      case Rechtsabbieger.GRUENPFEIL_RAD:
        return { name: k, displayText: 'Grünpfeil (Rad)' };
      case Rechtsabbieger.RECHTSABBIEGER:
        return { name: k, displayText: 'Rechtsabbieger' };
      case Rechtsabbieger.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
    }
    throw new Error('Beschreibung für enum Rechtsabbieger fehlt: ' + k);
  });
}
