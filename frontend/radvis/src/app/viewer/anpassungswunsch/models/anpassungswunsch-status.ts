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

export enum AnpassungswunschStatus {
  OFFEN = 'OFFEN',
  ERLEDIGT = 'ERLEDIGT',
  KLAERUNGSBEDARF = 'KLAERUNGSBEDARF',
  KORRIGIERT = 'KORRIGIERT',
  NACHBEARBEITUNG = 'NACHBEARBEITUNG',
  ABGELEHNT = 'ABGELEHNT',
  ZURUECKGEZOGEN = 'ZURUECKGEZOGEN',
  UMGESETZT = 'UMGESETZT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace AnpassungswunschStatus {
  export const options: EnumOption[] = Object.keys(AnpassungswunschStatus).map((k: string): EnumOption => {
    switch (k) {
      case AnpassungswunschStatus.OFFEN:
        return { name: k, displayText: 'Offen' };
      case AnpassungswunschStatus.ERLEDIGT:
        return { name: k, displayText: 'Erledigt' };
      case AnpassungswunschStatus.KLAERUNGSBEDARF:
        return { name: k, displayText: 'Klärungsbedarf' };
      case AnpassungswunschStatus.KORRIGIERT:
        return { name: k, displayText: 'Im Drittsystem korrigiert' };
      case AnpassungswunschStatus.NACHBEARBEITUNG:
        return { name: k, displayText: 'In Nachbearbeitung' };
      case AnpassungswunschStatus.ABGELEHNT:
        return { name: k, displayText: 'Abgelehnt' };
      case AnpassungswunschStatus.ZURUECKGEZOGEN:
        return { name: k, displayText: 'Zurückgezogen' };
      case AnpassungswunschStatus.UMGESETZT:
        return { name: k, displayText: 'Umgesetzt' };
    }
    throw new Error('Beschreibung für enum AnpassungswunschStatus fehlt: ' + k);
  });

  export const displayTextOf: (anpassungswunschStatus: AnpassungswunschStatus) => string = (
    anpassungswunschStatus: AnpassungswunschStatus
  ): string => {
    const enumOption = options.find(({ name }) => name === anpassungswunschStatus);
    invariant(enumOption, 'Der AnpassungswunschStatus hat kein displayText implementiert');

    return enumOption.displayText;
  };
}
