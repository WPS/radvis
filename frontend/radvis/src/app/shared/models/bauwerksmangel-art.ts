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
import { Bauwerksmangel } from 'src/app/shared/models/bauwerksmangel';
export enum BauwerksmangelArt {
  GELAENDER_ZU_NIEDRIG = 'GELAENDER_ZU_NIEDRIG',
  ZU_SCHMAL = 'ZU_SCHMAL',
  RAMPE_MANGELHAFT = 'RAMPE_MANGELHAFT',
  ANDERER_MANGEL = 'ANDERER_MANGEL',
  ZU_NIEDRIG = 'ZU_NIEDRIG',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace BauwerksmangelArt {
  export const options: EnumOption[] = Object.keys(BauwerksmangelArt).map((k: string): EnumOption => {
    switch (k) {
      case BauwerksmangelArt.GELAENDER_ZU_NIEDRIG:
        return { name: k, displayText: 'Geländer zu niedrig (< 1,30m)' };
      case BauwerksmangelArt.ZU_SCHMAL:
        return { name: k, displayText: 'zu schmal (< 4m)' };
      case BauwerksmangelArt.RAMPE_MANGELHAFT:
        return { name: k, displayText: 'Rampe zu steil (> 6%) und/oder verwinkelt' };
      case BauwerksmangelArt.ANDERER_MANGEL:
        return { name: k, displayText: 'Anderer Mangel' };
      case BauwerksmangelArt.ZU_NIEDRIG:
        return { name: k, displayText: 'zu niedrig (< 2,5m)' };
    }
    throw new Error('Beschreibung für enum BauwerksmangelArt fehlt: ' + k);
  });

  export const isValidForKontenform = (bauweksmangelArt: BauwerksmangelArt | string, knotenForm: string): boolean => {
    if (!Bauwerksmangel.isEnabledForKnotenform(knotenForm)) {
      return false;
    }

    switch (bauweksmangelArt) {
      case BauwerksmangelArt.GELAENDER_ZU_NIEDRIG:
        return knotenForm === 'UEBERFUEHRUNG';
      case BauwerksmangelArt.ZU_NIEDRIG:
        return knotenForm === 'UNTERFUEHRUNG_TUNNEL';
      case BauwerksmangelArt.ZU_SCHMAL:
      case BauwerksmangelArt.RAMPE_MANGELHAFT:
      case BauwerksmangelArt.ANDERER_MANGEL:
        return true;
      default:
        return false;
    }
  };

  export const getOptionsForKnotenform = (knotenform: string): EnumOption[] => {
    return options.filter(opt => {
      return isValidForKontenform(opt.name, knotenform);
    });
  };
}
