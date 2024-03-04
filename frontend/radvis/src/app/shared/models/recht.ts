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

export enum Recht {
  ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN = 'ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN',
  BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN = 'BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN',
  EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN = 'EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN',
  RADNETZ_ROUTENVERLEGUNGEN = 'RADNETZ_ROUTENVERLEGUNGEN',
  BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT = 'BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT',
  BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN = 'BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN',
  ALLE_ROLLEN = 'ALLE_ROLLEN',
  KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER = 'KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER',
  RADVERKEHRSBEAUFTRAGTER = 'RADVERKEHRSBEAUFTRAGTER',
  BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN = 'BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN',
  BETRACHTER_EXTERNER_DIENSTLEISTER = 'BETRACHTER_EXTERNER_DIENSTLEISTER',
  MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN = 'MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN',
  ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN = 'ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN',
  STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN = 'STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN',
  MANUELLES_MATCHING_ZUORDNEN_UND_BEARBEITEN = 'MANUELLES_MATCHING_ZUORDNEN_UND_BEARBEITEN',
  ANPASSUNGSWUENSCHE_BEARBEITEN = 'ANPASSUNGSWUENSCHE_BEARBEITEN',
  ANPASSUNGSWUENSCHE_ERFASSEN = 'ANPASSUNGSWUENSCHE_ERFASSEN',
  UMSETZUNGSSTANDSABFRAGEN_STARTEN = 'UMSETZUNGSSTANDSABFRAGEN_STARTEN',
  UMSETZUNGSSTANDSABFRAGEN_AUSWERTEN = 'UMSETZUNGSSTANDSABFRAGEN_AUSWERTEN',
  ALLE_RADROUTEN_ERFASSEN_BEARBEITEN = 'ALLE_RADROUTEN_ERFASSEN_BEARBEITEN',
  RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN = 'RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN',
  FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN = 'FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN',
  BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN = 'BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN',
  JOBS_AUSFUEHREN = 'JOBS_AUSFUEHREN',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Recht {
  export const options: EnumOption[] = Object.keys(Recht).map(
    (k: string): EnumOption => {
      switch (k) {
        case Recht.ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN:
          return { name: k, displayText: 'Alle Benutzer und Organisationen bearbeiten' };
        case Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN:
          return { name: k, displayText: 'Benutzer und Organisationen meines Verwaltungsbereichs bearbeiten' };
        case Recht.EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN:
          return {
            name: k,
            displayText:
              'Den Zuständigkeitsbereich meiner Organisation zum Zuständigkeitsbereich einer Organisation hinzufügen',
          };
        case Recht.RADNETZ_ROUTENVERLEGUNGEN:
          return { name: k, displayText: 'RadNETZ Routenverlegungen ' };
        case Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT:
          return { name: k, displayText: 'Bearbeitung von Radwegstrecken des eigenen geographischen Zuständigkeit' };
        case Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN:
          return { name: k, displayText: 'Bearbeitung von allen Radwegstrecken' };
        case Recht.ALLE_ROLLEN:
          return { name: k, displayText: 'Alle Rollen vergeben' };
        case Recht.KREISKOORDINATOREN_RADWEGE_ERFASSERIN_IMPORTE_VERANTWORTLICHER_UND_MASSNAHMEN_VERANTWORLICHER:
          return {
            name: k,
            displayText:
              'Rollen "KreiskoordinatorIn", "Radwege ErfasserIn - Kommune/Kreis", "Importe- VerantwortlicheR" und "Maßnahmen VerantworlicheR" vergeben',
          };
        case Recht.RADVERKEHRSBEAUFTRAGTER:
          return { name: k, displayText: 'Rolle "RadNETZ-ErfasserIn Regierungsbezirk" vergeben' };
        case Recht.BEARBEITERIN_VM_RADNETZ_ADMINISTRATORIN:
          return { name: k, displayText: 'Rolle "BearbeiterIn (VM)/RadNETZ-AdministratorIn" vergeben' };
        case Recht.BETRACHTER_EXTERNER_DIENSTLEISTER:
          return { name: k, displayText: 'Rolle "BetrachterIn", "Externer Dienstleister" vergeben' };
        case Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN:
          return { name: k, displayText: 'Maßnahme im Zuständigkeitsbereich erfassen/bearbeiten/veröffentlichen' };
        case Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN:
          return { name: k, displayText: 'Alle Maßnahmen erfassen/bearbeiten' };
        case Recht.STRECKENDATEN_DES_EIGENEN_ZUSTAENDIGKEITSBEREICHES_IMPORTIEREN:
          return { name: k, displayText: 'Streckendaten des eigenen Zuständigkeitsbereiches importieren' };
        case Recht.MANUELLES_MATCHING_ZUORDNEN_UND_BEARBEITEN:
          return { name: k, displayText: 'Manuelles Matching zuordnen und bearbeiten' };
        case Recht.ANPASSUNGSWUENSCHE_BEARBEITEN:
          return { name: k, displayText: 'Anpassungswünsche bearbeiten' };
        case Recht.ANPASSUNGSWUENSCHE_ERFASSEN:
          return { name: k, displayText: 'Anpassungswünsche erfassen' };
        case Recht.UMSETZUNGSSTANDSABFRAGEN_STARTEN:
          return { name: k, displayText: 'Umsetzungsstandsabfragen starten' };
        case Recht.UMSETZUNGSSTANDSABFRAGEN_AUSWERTEN:
          return { name: k, displayText: 'Umsetzungsstandsabfragen auswerten' };
        case Recht.ALLE_RADROUTEN_ERFASSEN_BEARBEITEN:
          return { name: k, displayText: 'Alle Radrouten erfassen/bearbeiten' };
        case Recht.RADROUTEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN:
          return { name: k, displayText: 'Radrouten im eigenen Zuständigkeitsbereich erfassen/bearbeiten' };
        case Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN:
          return { name: k, displayText: 'Furten und Kreuzungen im eigenen Zuständigkeitsbereich erfassen/bearbeiten' };
        case Recht.BARRIEREN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN:
          return { name: k, displayText: 'Barrieren im eigenen Zuständigkeitsbereich erfassen/bearbeiten' };
        case Recht.JOBS_AUSFUEHREN:
          return { name: k, displayText: 'Jobs ausführen' };
      }
      throw new Error('Beschreibung für enum Recht fehlt: ' + k);
    }
  );
}
