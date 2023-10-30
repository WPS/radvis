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

import { GroupedEnumOptions } from 'src/app/form-elements/models/grouped-enum-options';
import { EnumOption } from 'src/app/form-elements/models/enum-option';

export const BARRIEREN_FORM = {
  ABSPERRANLAGEN: {
    displayText: 'Absperranlagen',
    options: [
      {
        name: 'SPERRPFOSTEN',
        displayText: 'Sperrpfosten',
      },
      { name: 'VERSENKBARE_SPERRPFOSTEN', displayText: 'Versenkbare Sperrpfosten' },

      { name: 'MOBILE_SPERRPFOSTEN', displayText: 'Mobile Sperrpfosten' },

      { name: 'UMLAUFSPERREN', displayText: 'Umlaufsperren' },

      { name: 'SCHRANKE', displayText: 'Schranke' },
    ],
  },
  BARRIERE_DURCH_ANORDNUNG: {
    displayText: 'Barriere durch Anordnung',
    options: [
      { name: 'ANORDNUNG_VZ_220', displayText: 'Anordnung VZ 220 ohne 1022-10 (Schiebestrecke)' },

      { name: 'ANORDNUNG_VZ_239', displayText: 'Anordnung VZ 239 (Schiebestrecke)' },

      { name: 'ANORDNUNG_VZ_242', displayText: 'Anordnung VZ 242 (Schiebestrecke)' },

      { name: 'ANORDNUNG_VZ_242_1040_42', displayText: 'Anordnung VZ 242 +1040 / 1042 (Schiebestrecke)' },

      { name: 'ANORDNUNG_VZ_250', displayText: 'Anordnung VZ 250 (Schiebestrecke)' },

      { name: 'ANORDNUNG_VZ_254', displayText: 'Anordnung VZ 254 (Schiebestrecke)' },

      { name: 'ANORDNUNG_VZ_1012_32', displayText: 'Anordnung VZ 1012-32 (Schiebestrecke)' },
    ],
  },
  ANDERE_BARRIEREN: {
    displayText: 'Andere Barrieren',
    options: [
      { name: 'TREPPE', displayText: 'Treppe' },

      { name: 'STEILE_RAMPE', displayText: 'Steile Rampe (>6%)' },

      { name: 'LICHTMAST_SIGNALMAST', displayText: 'Lichtmast / Signalmast im Verkehrsraum' },

      { name: 'VERKEHRSZEICHEN', displayText: 'Verkehrszeichen im Verkehrsraum' },

      { name: 'EINENGUNG_HALTESTELLE', displayText: 'Einengung durch Haltestelle' },

      { name: 'EINENGUNG_SONSTIGE', displayText: 'Einengung durch sonstige bauliche Gegebenheit' },

      { name: 'BAUM_BAUMSCHEIBE', displayText: 'Baum / Baumscheibe im Verkehrsraum' },

      { name: 'SONSTIGE_BARRIERE', displayText: 'Sonstige Barriere' },
    ],
  },
} as GroupedEnumOptions;

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace BarrierenForm {
  export const allOptions: EnumOption[] = [
    ...BARRIEREN_FORM.ABSPERRANLAGEN.options,
    ...BARRIEREN_FORM.BARRIERE_DURCH_ANORDNUNG.options,
    ...BARRIEREN_FORM.ANDERE_BARRIEREN.options,
  ];

  export const getDisplayTextForBarrierenForm = (key: string): string => {
    const result = '';
    if (key) {
      for (const group of Object.values(BARRIEREN_FORM)) {
        const kategorie = group.options.find(opt => opt.name === key);
        if (kategorie) {
          return kategorie.displayText;
        }
      }
    }
    return result;
  };
}
