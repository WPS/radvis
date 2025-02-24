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

export enum Richtung {
  BEIDE_RICHTUNGEN = 'BEIDE_RICHTUNGEN',
  IN_RICHTUNG = 'IN_RICHTUNG',
  GEGEN_RICHTUNG = 'GEGEN_RICHTUNG',
  KEINE = 'KEINE',
  UNBEKANNT = 'UNBEKANNT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Richtung {
  export const options: EnumOption[] = Object.keys(Richtung).map((k: string): EnumOption => {
    switch (k) {
      case Richtung.IN_RICHTUNG:
        return { name: k, displayText: 'In Stationierungsrichtung' };
      case Richtung.GEGEN_RICHTUNG:
        return { name: k, displayText: 'Gegen Stationierungsrichtung' };
      case Richtung.BEIDE_RICHTUNGEN:
        return { name: k, displayText: 'Beide Richtungen' };
      case Richtung.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
      case Richtung.KEINE:
        return { name: k, displayText: 'Nicht fahren' };
    }
    throw new Error('Beschreibung für enum Richtung fehlt: ' + k);
  });
}
