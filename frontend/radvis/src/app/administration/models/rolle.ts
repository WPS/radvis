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

export enum Rolle {
  RADWEGE_ERFASSERIN = 'RADWEGE_ERFASSERIN',
  KREISKOORDINATOREN = 'KREISKOORDINATOREN',
  RADVERKEHRSBEAUFTRAGTER = 'RADVERKEHRSBEAUFTRAGTER',
  BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN = 'BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN',
  RADVIS_BETRACHTER = 'RADVIS_BETRACHTER',
  RADROUTEN_BEARBEITERIN = 'RADROUTEN_BEARBEITERIN',
  EXTERNER_DIENSTLEISTER = 'EXTERNER_DIENSTLEISTER',
  LGL_MITARBEITERIN = 'LGL_MITARBEITERIN',
  RADNETZ_QUALITAETSSICHERIN = 'RADNETZ_QUALITAETSSICHERIN',
  RADVIS_ADMINISTRATOR = 'RADVIS_ADMINISTRATOR',
  LOG_SICHTER = 'LOG_SICHTER',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Rolle {
  export const options: EnumOption[] = Object.keys(Rolle).map(
    (k: string): EnumOption => {
      switch (k) {
        case Rolle.RADVIS_ADMINISTRATOR:
          return { name: k, displayText: 'RadVIS AdministratorIn' };
        case Rolle.KREISKOORDINATOREN:
          return { name: k, displayText: 'KreiskoordinatorIn' };
        case Rolle.RADWEGE_ERFASSERIN:
          return { name: k, displayText: 'Radwege ErfasserIn - Kommune/Kreis/Regierungsbezirk' };
        case Rolle.RADVERKEHRSBEAUFTRAGTER:
          return { name: k, displayText: 'RadverkehrsbeauftragteR Regierungsbezirk' };
        case Rolle.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN:
          return { name: k, displayText: 'BearbeiterIn (VM)/RadNETZ-AdministratorIn' };
        case Rolle.EXTERNER_DIENSTLEISTER:
          return { name: k, displayText: 'Externer Dienstleister' };
        case Rolle.RADVIS_BETRACHTER:
          return { name: k, displayText: 'BetrachterIn' };
        case Rolle.RADNETZ_QUALITAETSSICHERIN:
          return { name: k, displayText: 'RadNETZ-QualitätssicherIn' };
        case Rolle.LGL_MITARBEITERIN:
          return { name: k, displayText: 'LGL-MitarbeiterIn' };
        case Rolle.RADROUTEN_BEARBEITERIN:
          return { name: k, displayText: 'RadroutenbearbeiterIn' };
        case Rolle.LOG_SICHTER:
          return { name: k, displayText: 'Log-SichterIn' };
      }
      throw new Error('Beschreibung für enum Rolle fehlt: ' + k);
    }
  );
}
