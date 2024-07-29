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
import invariant from 'tiny-invariant';

export enum AnpassungswunschKategorie {
  DLM = 'DLM',
  TOUBIZ = 'TOUBIZ',
  RADVIS = 'RADVIS',
  OSM = 'OSM',
  TT_SIB = 'TT_SIB',
  WEGWEISUNGSSYSTEM = 'WEGWEISUNGSSYSTEM',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace AnpassungswunschKategorie {
  export const options: EnumOption[] = Object.keys(AnpassungswunschKategorie).map((k: string): EnumOption => {
    switch (k) {
      case AnpassungswunschKategorie.DLM:
        return { name: k, displayText: 'DLM' };
      case AnpassungswunschKategorie.TOUBIZ:
        return { name: k, displayText: 'Toubiz' };
      case AnpassungswunschKategorie.RADVIS:
        return { name: k, displayText: 'RadVIS' };
      case AnpassungswunschKategorie.OSM:
        return { name: k, displayText: 'OSM' };
      case AnpassungswunschKategorie.TT_SIB:
        return { name: k, displayText: 'TT-SIB' };
      case AnpassungswunschKategorie.WEGWEISUNGSSYSTEM:
        return { name: k, displayText: 'Wegweisungssystem' };
    }
    throw new Error('Beschreibung für enum AnpassungswunschKategorie fehlt: ' + k);
  });

  export const displayTextOf: (AnpassungswunschKategorie: AnpassungswunschKategorie) => string = (
    anpassungswunschKategorie: AnpassungswunschKategorie
  ): string => {
    const enumOption = options.find(({ name }) => name === anpassungswunschKategorie);
    invariant(enumOption, 'Die AnpassungswunschKategorie hat kein displayText implementiert');

    return enumOption.displayText;
  };
}
