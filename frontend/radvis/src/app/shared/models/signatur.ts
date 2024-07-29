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

import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';

export interface Signatur {
  name: string;
  typ: SignaturTyp;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Signatur {
  export const SIGNATUR_NETZFILTER_INCOMPATIBLE_MESSAGE =
    'Gewählte Signatur und Netzklassen nicht kompatibel: Es wird ggf. kein Netz angezeigt.';

  export const compare = (a: Signatur, b: Signatur): boolean => a && b && a.name === b.name && a.typ === b.typ;

  export const isCompatibleWithNetzklassenfilter = (
    signatur: Signatur,
    netzklassenfilter: Netzklassefilter[]
  ): boolean => {
    if (netzklassenfilter.length === 0) {
      // Wenn keine Netzklasse oder Signatur ausgewählt wurde, kann es auch keine Inkompatibilitäten geben.
      return true;
    }

    const isRadnetzSichtbar = netzklassenfilter.includes(Netzklassefilter.RADNETZ);
    const isKlassifiziertesNetzSichtbar = netzklassenfilter.some(
      filter => filter !== Netzklassefilter.NICHT_KLASSIFIZIERT
    );

    if (signatur.typ !== SignaturTyp.NETZ) {
      // Nicht-Netz Signaturen sind mit allen Netzklassen kompatibel, da ja nicht das Netz, sondern z.B. Maßnahmen gefärbt werden.
      return true;
    }

    if (signatur.name.toLowerCase().includes('radnetz')) {
      // RadNETZ-Signaturen müssen das RadNETZ sichtbar haben
      return isRadnetzSichtbar;
    }

    if (signatur.name.toLowerCase().includes('netzklasse')) {
      // Netzklasse-Signaturen brauchen irgendwelche klassifizierten Kanten
      return isKlassifiziertesNetzSichtbar;
    }

    // Für alle restlichen Signaturen gibt es keine Einschränkungen
    return true;
  };
}
