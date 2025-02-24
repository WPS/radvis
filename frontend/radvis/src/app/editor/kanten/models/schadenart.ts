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

export enum Schadenart {
  ABPLATZUNGEN_SCHLAGLOECHER = 'ABPLATZUNGEN_SCHLAGLOECHER',
  ABSACKUNG_SETZUNG = 'ABSACKUNG_SETZUNG',
  AUSSPUELUNGEN_RINNEN = 'AUSSPUELUNGEN_RINNEN',
  GRASEINWUCHS = 'GRASEINWUCHS',
  KANTEN = 'KANTEN',
  NETZRISSE = 'NETZRISSE',
  PFLASTER_KLAPPERT = 'PFLASTER_KLAPPERT',
  PFLASTERBRUECHE = 'PFLASTERBRUECHE',
  PFLASTERSTEINE_FEHLEN = 'PFLASTERSTEINE_FEHLEN',
  RISSE = 'RISSE',
  SONSTIGER_SCHADEN = 'SONSTIGER_SCHADEN',
  STARK_ABSCHUESSIGE_SEITENRAENDER = 'STARK_ABSCHUESSIGE_SEITENRAENDER',
  STARK_WELLENARTIGE_OBERFLAECHE = 'STARK_WELLENARTIGE_OBERFLAECHE',
  WURZELHEBUNG_WURZELBRUECHE_WELLENBILDUNG = 'WURZELHEBUNG_WURZELBRUECHE_WELLENBILDUNG',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Schadenart {
  export const options: EnumOption[] = Object.keys(Schadenart).map((k: string): EnumOption => {
    switch (k) {
      case Schadenart.ABPLATZUNGEN_SCHLAGLOECHER:
        return { name: k, displayText: 'Abplatzungen / Schlaglöcher' };
      case Schadenart.ABSACKUNG_SETZUNG:
        return { name: k, displayText: 'Absackung / Setzung' };
      case Schadenart.AUSSPUELUNGEN_RINNEN:
        return { name: k, displayText: 'Ausspülungen / Rinnen' };
      case Schadenart.GRASEINWUCHS:
        return { name: k, displayText: 'Graseinwuchs' };
      case Schadenart.KANTEN:
        return { name: k, displayText: 'Kanten' };
      case Schadenart.NETZRISSE:
        return { name: k, displayText: 'Netzrisse' };
      case Schadenart.PFLASTER_KLAPPERT:
        return { name: k, displayText: 'Pflaster klappert' };
      case Schadenart.PFLASTERBRUECHE:
        return { name: k, displayText: 'Pflasterbrüche' };
      case Schadenart.PFLASTERSTEINE_FEHLEN:
        return { name: k, displayText: 'Pflastersteine fehlen' };
      case Schadenart.RISSE:
        return { name: k, displayText: 'Risse' };
      case Schadenart.SONSTIGER_SCHADEN:
        return { name: k, displayText: 'sonstiger Schaden' };
      case Schadenart.STARK_ABSCHUESSIGE_SEITENRAENDER:
        return { name: k, displayText: 'stark abschüssige Seitenränder' };
      case Schadenart.STARK_WELLENARTIGE_OBERFLAECHE:
        return { name: k, displayText: 'stark wellenartige Oberfläche (holprig)' };
      case Schadenart.WURZELHEBUNG_WURZELBRUECHE_WELLENBILDUNG:
        return { name: k, displayText: 'Wurzelhebung / -brüche / Wellenbildung' };
    }

    throw new Error('Beschreibung für enum Schadenart fehlt: ' + k);
  });
}
