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

export enum FahrradrouteFilterKategorie {
  EINZELNE_FAHRRADROUTE = 'EINZELNE_FAHRRADROUTE',
  ALLE_LRFW = 'ALLE_LRFW',
  ALLE_DROUTEN = 'ALLE_DROUTEN',
  ALLE_FAHRRADROUTEN = 'ALLE_FAHRRADROUTEN',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace FahrradrouteFilterKategorie {
  export const options: EnumOption[] = Object.keys(FahrradrouteFilterKategorie).map((k: string): EnumOption => {
    switch (k) {
      case FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE:
        return { name: k, displayText: 'Einzelne Fahrradroute' };
      case FahrradrouteFilterKategorie.ALLE_LRFW:
        return { name: k, displayText: 'Alle Landesradfernwege' };
      case FahrradrouteFilterKategorie.ALLE_DROUTEN:
        return { name: k, displayText: 'Alle D-Routen' };
      case FahrradrouteFilterKategorie.ALLE_FAHRRADROUTEN:
        return { name: k, displayText: 'Alle Fahrradrouten' };
    }
    throw new Error('Beschreibung für enum MassnahmenTabelleFahrradrouteFilter fehlt: ' + k);
  });
}
