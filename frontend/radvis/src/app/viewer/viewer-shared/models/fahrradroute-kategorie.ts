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

export enum FahrradrouteKategorie {
  TOURISTISCHE_ROUTE = 'TOURISTISCHE_ROUTE',
  LANDESRADFERNWEG = 'LANDESRADFERNWEG',
  RADFERNWEG = 'RADFERNWEG',
  RADSCHNELLWEG = 'RADSCHNELLWEG',
  UEBERREGIONALER_RADWANDERWEG = 'UEBERREGIONALER_RADWANDERWEG',
  REGIONALER_RADWANDERWEG = 'REGIONALER_RADWANDERWEG',
  VERBINDUNGSRADWANDERWEG = 'VERBINDUNGSRADWANDERWEG',
  RADVERKEHRSNETZ = 'RADVERKEHRSNETZ',
  UNMARKIERTER_RADWANDERVORSCHLAG = 'UNMARKIERTER_RADWANDERVORSCHLAG',
  SONSTIGER_RADWANDERWEG = 'SONSTIGER_RADWANDERWEG',
  D_ROUTE = 'D_ROUTE',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace FahrradrouteKategorie {
  export const options: EnumOption[] = Object.keys(FahrradrouteKategorie)
    .map((k: string): EnumOption => {
      switch (k) {
        case FahrradrouteKategorie.TOURISTISCHE_ROUTE:
          return { name: k, displayText: 'Touristische Route' };
        case FahrradrouteKategorie.LANDESRADFERNWEG:
          return { name: k, displayText: 'Landesradfernweg' };
        case FahrradrouteKategorie.RADFERNWEG:
          return { name: k, displayText: 'Radfernweg' };
        case FahrradrouteKategorie.RADSCHNELLWEG:
          return { name: k, displayText: 'Radschnellweg' };
        case FahrradrouteKategorie.UEBERREGIONALER_RADWANDERWEG:
          return { name: k, displayText: 'Überregionaler Radwanderweg' };
        case FahrradrouteKategorie.REGIONALER_RADWANDERWEG:
          return { name: k, displayText: 'Regionaler Radwanderweg' };
        case FahrradrouteKategorie.VERBINDUNGSRADWANDERWEG:
          return { name: k, displayText: 'Verbindungsradwanderweg' };
        case FahrradrouteKategorie.RADVERKEHRSNETZ:
          return { name: k, displayText: 'Radverkehrsnetz' };
        case FahrradrouteKategorie.UNMARKIERTER_RADWANDERVORSCHLAG:
          return { name: k, displayText: 'Unmarkierter Radwandervorschlag' };
        case FahrradrouteKategorie.SONSTIGER_RADWANDERWEG:
          return { name: k, displayText: 'Sonstiger Radwanderweg' };
        case FahrradrouteKategorie.D_ROUTE:
          return { name: k, displayText: 'D-Route' };
      }
      throw new Error('Beschreibung für enum Kategorie fehlt: ' + k);
    })
    .sort((a, b) => a.displayText.toLowerCase().localeCompare(b.displayText.toLowerCase()));
}
