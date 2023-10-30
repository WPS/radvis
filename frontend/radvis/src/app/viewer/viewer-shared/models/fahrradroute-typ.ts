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

export enum FahrradrouteTyp {
  RADVIS_ROUTE = 'RADVIS_ROUTE',
  TOUBIZ_ROUTE = 'TOUBIZ_ROUTE',
  TFIS_ROUTE = 'TFIS_ROUTE',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace FahrradrouteTyp {
  export const options: EnumOption[] = Object.keys(FahrradrouteTyp).map(
    (k: string): EnumOption => {
      switch (k) {
        case FahrradrouteTyp.RADVIS_ROUTE:
          return { name: k, displayText: 'RadVIS-Route' };
        case FahrradrouteTyp.TOUBIZ_ROUTE:
          return { name: k, displayText: 'Toubiz-Route' };
        case FahrradrouteTyp.TFIS_ROUTE:
          return { name: k, displayText: 'TFIS-Route' };
      }
      throw new Error('Beschreibung für enum FahrradrouteTyp fehlt: ' + k);
    }
  );

  export const getQuelleName = (typ: FahrradrouteTyp): string => {
    if (typ === FahrradrouteTyp.TFIS_ROUTE) {
      return 'TFIS';
    } else if (typ === FahrradrouteTyp.TOUBIZ_ROUTE) {
      return 'Toubiz';
    }
    return '';
  };
}
