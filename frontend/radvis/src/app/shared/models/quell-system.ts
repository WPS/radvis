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

/* eslint-disable @typescript-eslint/naming-convention */
import { EnumOption } from 'src/app/form-elements/models/enum-option';

export enum QuellSystem {
  RadNETZ = 'RadNETZ',
  DLM = 'DLM',
  LGL = 'LGL',
  RadwegeDB = 'RadwegeDB',
  RvkEsslingen = 'RvkEsslingen',
  BietigheimBissingen = 'BietigheimBissingen',
  GisGoeppingen = 'GisGoeppingen',
  RadVis = 'RadVis',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace QuellSystem {
  export const options: EnumOption[] = Object.keys(QuellSystem).map((k: string): EnumOption => {
    switch (k) {
      case QuellSystem.RadNETZ:
        return { name: k, displayText: 'RadNETZ' };
      case QuellSystem.DLM:
        return { name: k, displayText: 'DLM' };
      case QuellSystem.LGL:
        return { name: k, displayText: 'LGL' };
      case QuellSystem.RadwegeDB:
        return { name: k, displayText: 'RadwegeDB' };
      case QuellSystem.RvkEsslingen:
        return { name: k, displayText: 'RvkEsslingen' };
      case QuellSystem.BietigheimBissingen:
        return { name: k, displayText: 'BietigheimBissingen' };
      case QuellSystem.GisGoeppingen:
        return { name: k, displayText: 'GisGoeppingen' };
      case QuellSystem.RadVis:
        return { name: k, displayText: 'RadVis' };
    }
    throw new Error('Beschreibung für enum QuellSystem fehlt: ' + k);
  });
}
