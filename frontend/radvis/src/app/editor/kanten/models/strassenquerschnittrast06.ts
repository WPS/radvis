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

export enum StrassenquerschnittRASt06 {
  WOHNWEG = 'WOHNWEG',
  WOHNSTRASSE = 'WOHNSTRASSE',
  SAMMELSTRASSE = 'SAMMELSTRASSE',
  QUARTIERSSTRASSE = 'QUARTIERSSTRASSE',
  DOERFLICHE_HAUPTSTRASSE = 'DOERFLICHE_HAUPTSTRASSE',
  OERTLICHE_EINFAHRTSSTRASSE = 'OERTLICHE_EINFAHRTSSTRASSE',
  OERTLICHE_GESCHAEFTSSTRASSE = 'OERTLICHE_GESCHAEFTSSTRASSE',
  HAUPTGESCHAEFTSSTRASSE = 'HAUPTGESCHAEFTSSTRASSE',
  GEWERBESTRASSE = 'GEWERBESTRASSE',
  INDUSTRIESTRASSE = 'INDUSTRIESTRASSE',
  VERBINDUNGSSTRASSE = 'VERBINDUNGSSTRASSE',
  ANBAUFREIE_STRASSE = 'ANBAUFREIE_STRASSE',
  UNBEKANNT = 'UNBEKANNT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace StrassenquerschnittRASt06 {
  export const options: EnumOption[] = Object.keys(StrassenquerschnittRASt06).map((k: string): EnumOption => {
    switch (k) {
      case StrassenquerschnittRASt06.WOHNWEG:
        return { name: k, displayText: 'Wohnweg' };
      case StrassenquerschnittRASt06.WOHNSTRASSE:
        return { name: k, displayText: 'Wohnstraße' };
      case StrassenquerschnittRASt06.SAMMELSTRASSE:
        return { name: k, displayText: 'Sammelstraße' };
      case StrassenquerschnittRASt06.QUARTIERSSTRASSE:
        return { name: k, displayText: 'Quartiersstraße' };
      case StrassenquerschnittRASt06.DOERFLICHE_HAUPTSTRASSE:
        return { name: k, displayText: 'Dörfliche Hauptstraße' };
      case StrassenquerschnittRASt06.OERTLICHE_EINFAHRTSSTRASSE:
        return { name: k, displayText: 'Örtliche Einfahrtsstraße' };
      case StrassenquerschnittRASt06.OERTLICHE_GESCHAEFTSSTRASSE:
        return { name: k, displayText: 'Örtliche Geschäftsstraße' };
      case StrassenquerschnittRASt06.HAUPTGESCHAEFTSSTRASSE:
        return { name: k, displayText: 'Hauptgeschäftsstraße' };
      case StrassenquerschnittRASt06.GEWERBESTRASSE:
        return { name: k, displayText: 'Gewerbestraße' };
      case StrassenquerschnittRASt06.INDUSTRIESTRASSE:
        return { name: k, displayText: 'Industriestraße' };
      case StrassenquerschnittRASt06.VERBINDUNGSSTRASSE:
        return { name: k, displayText: 'Verbindungsstraße' };
      case StrassenquerschnittRASt06.ANBAUFREIE_STRASSE:
        return { name: k, displayText: 'Anbaufreie Straße' };
      case StrassenquerschnittRASt06.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
    }
    throw new Error('Beschreibung für enum Strassenquerschnitt nach RASt 06 fehlt: ' + k);
  });
}
