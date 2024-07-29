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

export enum Umfeld {
  GESCHAEFTSSTRASSE = 'GESCHAEFTSSTRASSE',
  STRASSE_MIT_HOHER_WOHNDICHTE_MISCHNUTZUNG = 'STRASSE_MIT_HOHER_WOHNDICHTE_MISCHNUTZUNG',
  STRASSE_MIT_GERINGER_BIS_MITTLERER_WOHNDICHTE = 'STRASSE_MIT_GERINGER_BIS_MITTLERER_WOHNDICHTE',
  GEWERBEGEBIET = 'GEWERBEGEBIET',
  UNBEKANNT = 'UNBEKANNT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Umfeld {
  export const options: EnumOption[] = Object.keys(Umfeld).map((k: string): EnumOption => {
    switch (k) {
      case Umfeld.GESCHAEFTSSTRASSE:
        return { name: k, displayText: 'Geschäftsstraße' };
      case Umfeld.STRASSE_MIT_HOHER_WOHNDICHTE_MISCHNUTZUNG:
        return { name: k, displayText: 'Straße mit hoher Wohndichte / Mischnutzung' };
      case Umfeld.STRASSE_MIT_GERINGER_BIS_MITTLERER_WOHNDICHTE:
        return { name: k, displayText: 'Straße mit geringer bis mittlerer Wohndichte' };
      case Umfeld.GEWERBEGEBIET:
        return { name: k, displayText: 'Gewerbegebiet' };
      case Umfeld.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
    }
    throw new Error('Beschreibung für enum Umfeld fehlt: ' + k);
  });
}
