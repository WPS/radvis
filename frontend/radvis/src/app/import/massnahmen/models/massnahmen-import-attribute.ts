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

export enum MassnahmenImportAttribute {
  // Pflichtattribute
  UMSETZUNGSSTATUS = 'UMSETZUNGSSTATUS',
  BEZEICHNUNG = 'BEZEICHNUNG',
  KATEGORIEN = 'KATEGORIEN',
  ZUSTAENDIGER = 'ZUSTAENDIGER',
  SOLL_STANDARD = 'SOLL_STANDARD',
  DURCHFUEHRUNGSZEITRAUM = 'DURCHFUEHRUNGSZEITRAUM',
  BAULASTTRAEGER = 'BAULASTTRAEGER',
  HANDLUNGSVERANTWORTLICHER = 'HANDLUNGSVERANTWORTLICHER',

  // optionale Attribute
  PRIORITAET = 'PRIORITAET',
  KOSTENANNAHME = 'KOSTENANNAHME',
  UNTERHALTSZUSTAENDIGER = 'UNTERHALTSZUSTAENDIGER',
  MAVIS_ID = 'MAVIS_ID',
  VERBA_ID = 'VERBA_ID',
  LGVFG_ID = 'LGVFG_ID',
  REALISIERUNGSHILFE = 'REALISIERUNGSHILFE',
  NETZKLASSEN = 'NETZKLASSEN',
  PLANUNG_ERFORDERLICH = 'PLANUNG_ERFORDERLICH',
  VEROEFFENTLICHT = 'VEROEFFENTLICHT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace MassnahmenImportAttribute {
  export const pflichtAttribute: MassnahmenImportAttribute[] = [
    MassnahmenImportAttribute.UMSETZUNGSSTATUS,
    MassnahmenImportAttribute.BEZEICHNUNG,
    MassnahmenImportAttribute.KATEGORIEN,
    MassnahmenImportAttribute.ZUSTAENDIGER,
    MassnahmenImportAttribute.SOLL_STANDARD,
    MassnahmenImportAttribute.DURCHFUEHRUNGSZEITRAUM,
    MassnahmenImportAttribute.BAULASTTRAEGER,
    MassnahmenImportAttribute.HANDLUNGSVERANTWORTLICHER,
  ];
  export const pflichtAttributeOptions: EnumOption[] = pflichtAttribute.map((k: string): EnumOption => {
    switch (k) {
      case MassnahmenImportAttribute.UMSETZUNGSSTATUS:
        return { name: k, displayText: 'Umsetzungsstatus' };
      case MassnahmenImportAttribute.BEZEICHNUNG:
        return { name: k, displayText: 'Bezeichnung' };
      case MassnahmenImportAttribute.KATEGORIEN:
        return { name: k, displayText: 'Kategorien' };
      case MassnahmenImportAttribute.ZUSTAENDIGER:
        return { name: k, displayText: 'Zuständige/r' };
      case MassnahmenImportAttribute.SOLL_STANDARD:
        return { name: k, displayText: 'Soll-Standard' };
      case MassnahmenImportAttribute.DURCHFUEHRUNGSZEITRAUM:
        return { name: k, displayText: 'Durchführungszeitraum *' };
      case MassnahmenImportAttribute.BAULASTTRAEGER:
        return { name: k, displayText: 'Baulastträger *' };
      case MassnahmenImportAttribute.HANDLUNGSVERANTWORTLICHER:
        return { name: k, displayText: 'Wer soll tätig werden? *' };
    }
    throw new Error('Beschreibung für enum MassnahmenImportAttribute fehlt: ' + k);
  });

  export const optionaleAttribute: MassnahmenImportAttribute[] = [
    MassnahmenImportAttribute.PRIORITAET,
    MassnahmenImportAttribute.KOSTENANNAHME,
    MassnahmenImportAttribute.UNTERHALTSZUSTAENDIGER,
    MassnahmenImportAttribute.MAVIS_ID,
    MassnahmenImportAttribute.VERBA_ID,
    MassnahmenImportAttribute.LGVFG_ID,
    MassnahmenImportAttribute.REALISIERUNGSHILFE,
    MassnahmenImportAttribute.NETZKLASSEN,
    MassnahmenImportAttribute.PLANUNG_ERFORDERLICH,
    MassnahmenImportAttribute.VEROEFFENTLICHT,
  ];
  export const optionaleAttributeOptions: EnumOption[] = optionaleAttribute.map((k: string): EnumOption => {
    switch (k) {
      case MassnahmenImportAttribute.PRIORITAET:
        return { name: k, displayText: 'Priorität' };
      case MassnahmenImportAttribute.KOSTENANNAHME:
        return { name: k, displayText: 'Kostenannahme' };
      case MassnahmenImportAttribute.UNTERHALTSZUSTAENDIGER:
        return { name: k, displayText: 'Unterhaltszuständige/r' };
      case MassnahmenImportAttribute.MAVIS_ID:
        return { name: k, displayText: 'MaViS-ID' };
      case MassnahmenImportAttribute.VERBA_ID:
        return { name: k, displayText: 'Verba-ID (Vereinbarungsdatenbank)' };
      case MassnahmenImportAttribute.LGVFG_ID:
        return { name: k, displayText: 'LGVFG-ID' };
      case MassnahmenImportAttribute.REALISIERUNGSHILFE:
        return { name: k, displayText: 'Realisierungshilfe' };
      case MassnahmenImportAttribute.NETZKLASSEN:
        return { name: k, displayText: 'Netzklassen' };
      case MassnahmenImportAttribute.PLANUNG_ERFORDERLICH:
        return { name: k, displayText: 'Planung erforderlich' };
      case MassnahmenImportAttribute.VEROEFFENTLICHT:
        return { name: k, displayText: 'Veröffentlicht' };
    }
    throw new Error('Beschreibung für enum MassnahmenImportAttribute fehlt: ' + k);
  });

  export const alleAttribute = [...pflichtAttribute, ...optionaleAttribute];
}
