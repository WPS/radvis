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

export enum BelagArt {
  ASPHALT = 'ASPHALT',
  ASPHALT_MIT_GRUENSTREIFEN = 'ASPHALT_MIT_GRUENSTREIFEN',
  BETON = 'BETON',
  BETON_MIT_GRUENSTREIFEN = 'BETON_MIT_GRUENSTREIFEN',
  NATURSTEINPFLASTER = 'NATURSTEINPFLASTER',
  BETONSTEINPFLASTER_PLATTENBELAG = 'BETONSTEINPFLASTER_PLATTENBELAG',
  WASSERGEBUNDENE_DECKE = 'WASSERGEBUNDENE_DECKE',
  WASSERGEBUNDENE_DECKE_MIT_GRUENSTREIFEN = 'WASSERGEBUNDENE_DECKE_MIT_GRUENSTREIFEN',
  UNGEBUNDENE_DECKE = 'UNGEBUNDENE_DECKE',
  SONSTIGER_BELAG = 'SONSTIGER_BELAG',
  UNBEKANNT = 'UNBEKANNT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace BelagArt {
  export const options: EnumOption[] = Object.keys(BelagArt).map((k: string): EnumOption => {
    switch (k) {
      case BelagArt.ASPHALT:
        return { name: k, displayText: 'Asphalt' };
      case BelagArt.BETON:
        return { name: k, displayText: 'Beton' };
      case BelagArt.NATURSTEINPFLASTER:
        return { name: k, displayText: 'Natursteinpflaster' };
      case BelagArt.BETONSTEINPFLASTER_PLATTENBELAG:
        return { name: k, displayText: 'Betonsteinpflaster oder Plattenbelag' };
      case BelagArt.WASSERGEBUNDENE_DECKE:
        return { name: k, displayText: 'Wassergebundene Decke' };
      case BelagArt.UNGEBUNDENE_DECKE:
        return { name: k, displayText: 'Ungebundene Decke' };
      case BelagArt.SONSTIGER_BELAG:
        return { name: k, displayText: 'Sonstiger Belag' };
      case BelagArt.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
      case BelagArt.ASPHALT_MIT_GRUENSTREIFEN:
        return { name: k, displayText: 'Asphalt mit Rasengittersteinen / Grünstreifen mittig' };
      case BelagArt.BETON_MIT_GRUENSTREIFEN:
        return { name: k, displayText: 'Beton mit Rasengittersteinen / Grünstreifen mittig' };
      case BelagArt.WASSERGEBUNDENE_DECKE_MIT_GRUENSTREIFEN:
        return { name: k, displayText: 'Wassergebundene Decke mit Grünstreifen mittig' };
    }
    throw new Error('Beschreibung für enum BelagArt fehlt: ' + k);
  });
}
