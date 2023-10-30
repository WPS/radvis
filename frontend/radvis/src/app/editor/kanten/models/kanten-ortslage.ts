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

export enum KantenOrtslage {
  INNERORTS = 'INNERORTS',
  AUSSERORTS = 'AUSSERORTS',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace KantenOrtslage {
  export const options: EnumOption[] = Object.keys(KantenOrtslage).map(
    (k: string): EnumOption => {
      switch (k) {
        case KantenOrtslage.INNERORTS:
          return { name: k, displayText: 'Innerorts' };
        case KantenOrtslage.AUSSERORTS:
          return { name: k, displayText: 'Außerorts' };
      }
      throw new Error('Beschreibung für enum KantenOrtslage fehlt: ' + k);
    }
  );
}
