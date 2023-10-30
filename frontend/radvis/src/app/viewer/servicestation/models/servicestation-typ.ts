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

export enum ServicestationTyp {
  RADSERVICE_PUNKT_KLEIN = 'RADSERVICE_PUNKT_KLEIN',
  RADSERVICE_PUNKT_GROSS = 'RADSERVICE_PUNKT_GROSS',
  SONSTIGER = 'SONSTIGER',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace ServicestationTyp {
  export const options: EnumOption[] = Object.keys(ServicestationTyp).map(
    (k: string): EnumOption => {
      switch (k) {
        case ServicestationTyp.RADSERVICE_PUNKT_KLEIN:
          return { name: k, displayText: 'RadSERVICE-Punkt (klein)' };
        case ServicestationTyp.RADSERVICE_PUNKT_GROSS:
          return { name: k, displayText: 'RadSERVICE-Punkt (groß)' };
        case ServicestationTyp.SONSTIGER:
          return { name: k, displayText: 'Sonstiger' };
      }
      throw new Error('Beschreibung für enum ServicestationTyp fehlt: ' + k);
    }
  );

  export const getDisplayText: (servicestationTyp: ServicestationTyp) => string = (
    servicestationTyp: ServicestationTyp
  ): string => {
    const enumOption = options.find(({ name }) => name === servicestationTyp);
    invariant(enumOption, `ServicestationTyp ${servicestationTyp} hat kein displayText implementiert`);

    return enumOption.displayText;
  };
}
