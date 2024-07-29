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

export enum PruefungQualitaetsstandardsErfolgt {
  JA_STANDARDS_EINGEHALTEN = 'JA_STANDARDS_EINGEHALTEN',
  JA_STANDARDS_NICHT_EINGEHALTEN = 'JA_STANDARDS_NICHT_EINGEHALTEN',
  NEIN_ERFOLGT_NOCH = 'NEIN_ERFOLGT_NOCH',
  NEIN = 'NEIN',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace PruefungQualitaetsstandardsErfolgt {
  export const options: EnumOption[] = Object.keys(PruefungQualitaetsstandardsErfolgt).map((k: string): EnumOption => {
    switch (k) {
      case PruefungQualitaetsstandardsErfolgt.JA_STANDARDS_EINGEHALTEN:
        return { name: k, displayText: 'Ja, Standards werden eingehalten' };
      case PruefungQualitaetsstandardsErfolgt.JA_STANDARDS_NICHT_EINGEHALTEN:
        return { name: k, displayText: 'Ja, aber Standards werden nicht eingehalten' };
      case PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH:
        return { name: k, displayText: 'Nein, erfolgt noch' };
      case PruefungQualitaetsstandardsErfolgt.NEIN:
        return { name: k, displayText: 'Nein' };
    }
    throw new Error('Beschreibung für enum PruefungQualitaetsstandardsErfolgt fehlt: ' + k);
  });
}
