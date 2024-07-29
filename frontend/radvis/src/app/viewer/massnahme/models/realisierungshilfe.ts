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

export enum Realisierungshilfe {
  NR_11_1_1 = 'NR_11_1_1',
  NR_11_1_2 = 'NR_11_1_2',
  NR_2_2_1 = 'NR_2_2_1',
  NR_3_2_1 = 'NR_3_2_1',
  NR_3_2_2 = 'NR_3_2_2',
  NR_3_2_3 = 'NR_3_2_3',
  NR_3_2_4 = 'NR_3_2_4',
  NR_3_2_5 = 'NR_3_2_5',
  NR_3_2_6 = 'NR_3_2_6',
  NR_3_2_7 = 'NR_3_2_7',
  NR_3_3_1 = 'NR_3_3_1',
  NR_3_3_2 = 'NR_3_3_2',
  NR_3_4_1 = 'NR_3_4_1',
  NR_3_4_2 = 'NR_3_4_2',
  NR_3_4_3 = 'NR_3_4_3',
  NR_3_4_4 = 'NR_3_4_4',
  NR_3_4_5 = 'NR_3_4_5',
  NR_3_6_1 = 'NR_3_6_1',
  NR_3_6_2 = 'NR_3_6_2',
  NR_3_6_3 = 'NR_3_6_3',
  NR_3_11_1 = 'NR_3_11_1',
  NR_3_11_2 = 'NR_3_11_2',
  NR_4_3_1 = 'NR_4_3_1',
  NR_4_3_2 = 'NR_4_3_2',
  NR_4_4_1 = 'NR_4_4_1',
  NR_4_4_2 = 'NR_4_4_2',
  NR_4_4_3 = 'NR_4_4_3',
  NR_4_4_4 = 'NR_4_4_4',
  NR_4_4_5 = 'NR_4_4_5',
  NR_4_4_6 = 'NR_4_4_6',
  NR_4_4_7 = 'NR_4_4_7',
  NR_4_4_8 = 'NR_4_4_8',
  NR_4_5_1 = 'NR_4_5_1',
  NR_4_5_2 = 'NR_4_5_2',
  NR_4_5_3 = 'NR_4_5_3',
  NR_4_5_4 = 'NR_4_5_4',
  NR_4_5_5 = 'NR_4_5_5',
  NR_4_5_6 = 'NR_4_5_6',
  NR_6_3_1 = 'NR_6_3_1',
  NR_6_3_2 = 'NR_6_3_2',
  NR_6_3_3 = 'NR_6_3_3',
  NR_7_2_1 = 'NR_7_2_1',
  NR_9_2_1 = 'NR_9_2_1',
  NR_9_3_1 = 'NR_9_3_1',
  NR_9_3_2 = 'NR_9_3_2',
  NR_9_3_3 = 'NR_9_3_3',
  NR_9_4_1 = 'NR_9_4_1',
  NR_9_4_2 = 'NR_9_4_2',
  NR_9_4_3 = 'NR_9_4_3',
  NR_9_5_1 = 'NR_9_5_1',
  NR_9_5_2 = 'NR_9_5_2',
  NR_9_5_3 = 'NR_9_5_3',
  NR_9_5_4 = 'NR_9_5_4',
  NR_9_5_5 = 'NR_9_5_5',
  NR_9_5_6 = 'NR_9_5_6',
  NR_9_5_7 = 'NR_9_5_7',
  NR_9_5_8 = 'NR_9_5_8',
  NR_9_5_9 = 'NR_9_5_9',
  NR_9_5_10 = 'NR_9_5_10',
  NR_9_5_11 = 'NR_9_5_11',
  NR_9_5_12 = 'NR_9_5_12',
  NR_10_2_1 = 'NR_10_2_1',
  NR_11_1_3 = 'NR_11_1_3',
  NR_11_1_4 = 'NR_11_1_4',
  RSV_M_1 = 'RSV_M_1',
  RSV_S_1 = 'RSV_S_1',
  RSV_S_2 = 'RSV_S_2',
  RSV_S_3 = 'RSV_S_3',
  RSV_S_4 = 'RSV_S_4',
  RSV_S_5 = 'RSV_S_5',
  RSV_H_1 = 'RSV_H_1',
  RSV_H_2 = 'RSV_H_2',
  RSV_H_3 = 'RSV_H_3',
  RSV_H_4 = 'RSV_H_4',
  RSV_H_5 = 'RSV_H_5',
  RSV_H_6 = 'RSV_H_6',
  RSV_H_7 = 'RSV_H_7',
  RSV_N_1 = 'RSV_N_1',
  RSV_N_2 = 'RSV_N_2',
  RSV_N_3 = 'RSV_N_3',
  RSV_N_4 = 'RSV_N_4',
  RSV_F_1 = 'RSV_F_1',
  RSV_F_2 = 'RSV_F_2',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Realisierungshilfe {
  /**
   * @deprecated Sobald Anpassungen erforderlich werden, bitte stattdessen den Weg wählen wir bei Furt/Kreuzung - Musterlösung
   */
  export const options: EnumOption[] = Object.keys(Realisierungshilfe)
    .map((k: string): EnumOption => {
      switch (k) {
        case Realisierungshilfe.NR_11_1_1:
          return { name: k, displayText: '11.1-1 Markierung des Sinnbildes "Fahrrad"' };
        case Realisierungshilfe.NR_11_1_2:
          return { name: k, displayText: '11.1-2 Markierung des Sinnbildes "Fahrrad" im RadNETZ' };
        case Realisierungshilfe.NR_2_2_1:
          return { name: k, displayText: '2.2-1 Sichtfelder an Knotenpunkten und Querungsstellen' };
        case Realisierungshilfe.NR_3_2_1:
          return { name: k, displayText: '3.2-1 Markierung beidseitiger Schutzstreifen' };
        case Realisierungshilfe.NR_3_2_2:
          return { name: k, displayText: '3.2-2 Furtmarkierung im Zuge von Schutzstreifen an Einmündungen' };
        case Realisierungshilfe.NR_3_2_3:
          return { name: k, displayText: '3.2-3 Furtmarkierung im Zuge von Schutzstreifen an Zufahrt' };
        case Realisierungshilfe.NR_3_2_4:
          return { name: k, displayText: '3.2-4 Furtmarkierung bei Schutzstreifen und Radfahren im Seitenraum (1)' };
        case Realisierungshilfe.NR_3_2_5:
          return { name: k, displayText: '3.2-5 Furtmarkierung bei Schutzstreifen und Radfahren im Seitenraum (2)' };
        case Realisierungshilfe.NR_3_2_6:
          return { name: k, displayText: '3.2-6 Beidseitige Schutzstreifen mit schmaler Kernfahrbahn' };
        case Realisierungshilfe.NR_3_2_7:
          return { name: k, displayText: '3.2-7 Markierung einseitiger Schutzstreifen' };
        case Realisierungshilfe.NR_3_3_1:
          return { name: k, displayText: '3.3-1 Markierung beidseitiger Radfahrstreifen' };
        case Realisierungshilfe.NR_3_3_2:
          return { name: k, displayText: '3.3-2 Furtmarkierung im Zuge von Radfahrstreifen' };
        case Realisierungshilfe.NR_3_4_1:
          return { name: k, displayText: '3.4-1 Anlage beidseitiger Radwege' };
        case Realisierungshilfe.NR_3_4_2:
          return { name: k, displayText: '3.4-2 Baulich geschütztes Radwegende' };
        case Realisierungshilfe.NR_3_4_3:
          return { name: k, displayText: '3.4-3 Sonderform für Radwegende (1)' };
        case Realisierungshilfe.NR_3_4_4:
          return { name: k, displayText: '3.4-4 Sonderform für Radwegende (2)' };
        case Realisierungshilfe.NR_3_4_5:
          return { name: k, displayText: '3.4-5 Furt mit Fahrradweiche' };
        case Realisierungshilfe.NR_3_6_1:
          return { name: k, displayText: '3.6-1 Gemeinsamer Geh- und Radweg' };
        case Realisierungshilfe.NR_3_6_2:
          return { name: k, displayText: '3.6-2 Furt an Fuß- und Radweg/Gehweg mit Zusatzzeichen 1022-10' };
        case Realisierungshilfe.NR_3_6_3:
          return {
            name: k,
            displayText: '3.6-3 Signalisierte Furt an Fuß- und Radweg/Gehweg mit Zusatzzeichen 1022-10',
          };
        case Realisierungshilfe.NR_3_11_1:
          return { name: k, displayText: '3.11-1 Radweg an Bushaltestelle' };
        case Realisierungshilfe.NR_3_11_2:
          return { name: k, displayText: '3.11-2 Schutzstreifen vor Bushaltestelle' };
        case Realisierungshilfe.NR_4_3_1:
          return { name: k, displayText: '4.3-1 Linksabbiegen aus übergeordneten Knotenpunktarmen' };
        case Realisierungshilfe.NR_4_3_2:
          return { name: k, displayText: '4.3-2 Knotenpunkt mit Vorfahrtregelung' };
        case Realisierungshilfe.NR_4_4_1:
          return { name: k, displayText: '4.4-1 Linksabbiegender Radverkehr - indirekte Führung' };
        case Realisierungshilfe.NR_4_4_2:
          return { name: k, displayText: '4.4-2 Fahrradweiche' };
        case Realisierungshilfe.NR_4_4_3:
          return { name: k, displayText: '4.4-3 Vorgezogene Haltlinie - Aufgeweiteter Radaufstellstreifen' };
        case Realisierungshilfe.NR_4_4_4:
          return { name: k, displayText: '4.4-4 Links abbiegender Radverkehr - direkte Führung' };
        case Realisierungshilfe.NR_4_4_5:
          return { name: k, displayText: '4.4-5 Auflösung Zweirichtungsradweg am signalisierten Knotenpunkt (Text)' };
        case Realisierungshilfe.NR_4_4_6:
          return { name: k, displayText: '4.4-6 Beginn Zweirichtungsradweg am signalisierten Knotenpunkt' };
        case Realisierungshilfe.NR_4_4_7:
          return { name: k, displayText: '4.4-7 Ende Zweirichtungsradweg am signalisierten Knotenpunkt' };
        case Realisierungshilfe.NR_4_4_8:
          return { name: k, displayText: '4.4-8 Erweiterte Fußgängersignalisierung zur Sicherung des Radverkehrs' };
        case Realisierungshilfe.NR_4_5_1:
          return { name: k, displayText: '4.5-1 Kreisverkehr - Führung des Radverkehrs auf der Fahrbahn' };
        case Realisierungshilfe.NR_4_5_2:
          return { name: k, displayText: '4.5-2 Kreisverkehr - Führung des Radverkehrs auf Radwegen' };
        case Realisierungshilfe.NR_4_5_3:
          return { name: k, displayText: '4.5-3 Minikreisel' };
        case Realisierungshilfe.NR_4_5_4:
          return { name: k, displayText: '4.5-4 Radweg am Kreisverkehr' };
        case Realisierungshilfe.NR_4_5_5:
          return {
            name: k,
            displayText: '4.5-5 Auflösung Zweirichtungsradweg vor Kreisverkehr - Querungsbedarf am Radweganfang',
          };
        case Realisierungshilfe.NR_4_5_6:
          return {
            name: k,
            displayText: '4.5-6 Auflösung Zweirichtungsradweg vor Kreisverkehr - Querungsbedarf am Radwegende',
          };
        case Realisierungshilfe.NR_6_3_1:
          return { name: k, displayText: '6.3-1 Gestaltung von Fahrradstraßen (1)' };
        case Realisierungshilfe.NR_6_3_2:
          return { name: k, displayText: '6.3-2 Gestaltung von Fahrradstraßen (2)' };
        case Realisierungshilfe.NR_6_3_3:
          return { name: k, displayText: '6.3-3 Gestaltung von Fahrradstraßen (3)' };
        case Realisierungshilfe.NR_7_2_1:
          return { name: k, displayText: '7.2-1 Einbahnstraßen mit Radverkehr in Gegenrichtung' };
        case Realisierungshilfe.NR_9_2_1:
          return { name: k, displayText: '9.2-1 Randmarkierung Fahrradroute (außerorts)' };
        case Realisierungshilfe.NR_9_3_1:
          return { name: k, displayText: '9.3-1 Bevorrechtigter straßenbegleitender Zweirichtungsradweg (1)' };
        case Realisierungshilfe.NR_9_3_2:
          return { name: k, displayText: '9.3-2 Bevorrechtigter straßenbegleitender Zweirichtungsradweg (2)' };
        case Realisierungshilfe.NR_9_3_3:
          return { name: k, displayText: '9.3-3 Untergeordneter straßenbegleitender Zweirichtungsradweg' };
        case Realisierungshilfe.NR_9_4_1:
          return { name: k, displayText: '9.4-1 Querungsstelle Radroute mit wartepflichtigem Radverkehr' };
        case Realisierungshilfe.NR_9_4_2:
          return { name: k, displayText: '9.4-2 Querungshilfen außerorts - großräumige Einbindung' };
        case Realisierungshilfe.NR_9_4_3:
          return {
            name: k,
            displayText:
              '9.4-3 Geteilte Querungshilfe bei Radverkehrsführung im Zuge land- und forstwirtschaftlicher Wege sowie Anliegerstraßen',
          };
        case Realisierungshilfe.NR_9_5_1:
          return {
            name: k,
            displayText:
              '9.5-1 Auflösung Zweirichtungsradweg ohne Mittelinsel Querungsbedarf am Radwegende, Benutzungspflicht',
          };
        case Realisierungshilfe.NR_9_5_2:
          return {
            name: k,
            displayText:
              '9.5-2 Auflösung Zweirichtungsradweg ohne Mittelinsel Querungsbedarf am Radwegende, Benutzungsrecht',
          };
        case Realisierungshilfe.NR_9_5_3:
          return {
            name: k,
            displayText:
              '9.5-3 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radwegende, Benutzungspflicht (1)',
          };
        case Realisierungshilfe.NR_9_5_4:
          return {
            name: k,
            displayText:
              '9.5-4 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radwegende, Benutzungsrecht (1)',
          };
        case Realisierungshilfe.NR_9_5_5:
          return {
            name: k,
            displayText:
              '9.5-5 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radwegende, Benutzungspflicht (2)',
          };
        case Realisierungshilfe.NR_9_5_6:
          return {
            name: k,
            displayText:
              '9.5-6 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radwegende, Benutzungsrecht (2)',
          };
        case Realisierungshilfe.NR_9_5_7:
          return {
            name: k,
            displayText:
              '9.5-7 Auflösung Zweirichtungsradweg ohne Mittelinsel Querungsbedarf am Radweganfang, Benutzungspflicht',
          };
        case Realisierungshilfe.NR_9_5_8:
          return {
            name: k,
            displayText:
              '9.5-8 Auflösung Zweirichtungsradweg ohne Mittelinsel Querungsbedarf am Radweganfang, Benutzungsrecht',
          };
        case Realisierungshilfe.NR_9_5_9:
          return {
            name: k,
            displayText:
              '9.5-9 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radweganfang, Benutzungspflicht (1)',
          };
        case Realisierungshilfe.NR_9_5_10:
          return {
            name: k,
            displayText:
              '9.5-10 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radweganfang, Benutzungsrecht (1)',
          };
        case Realisierungshilfe.NR_9_5_11:
          return {
            name: k,
            displayText:
              '9.5-11 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radweganfang, Benutzungspflicht (2)',
          };
        case Realisierungshilfe.NR_9_5_12:
          return {
            name: k,
            displayText:
              '9.5-12 Auflösung Zweirichtungsradweg mit Mittelinsel Querungsbedarf am Radweganfang, Benutzungsrecht (2)',
          };
        case Realisierungshilfe.NR_10_2_1:
          return { name: k, displayText: '10.2-1 Querungsstelle Radroute mit bevorrechtigtem Radverkehr' };
        case Realisierungshilfe.NR_11_1_3:
          return { name: k, displayText: '11.1-3 Warnmarkierung mit Sperrpfosten' };
        case Realisierungshilfe.NR_11_1_4:
          return {
            name: k,
            displayText: '11.1-4 Querungsstelle/Einmündung Radroute mit Sicherung durch Befahren von Kfz',
          };
        case Realisierungshilfe.RSV_M_1:
          return { name: k, displayText: 'RSV - M 1 Markierungen im Zuge von Radschnellverbindungen' };
        case Realisierungshilfe.RSV_S_1:
          return {
            name: k,
            displayText:
              'RSV - S 1 Bevorrechtigte Querung an einer untergeordneten Straße (ohne FGÜ) - innerorts/ außerorts',
          };
        case Realisierungshilfe.RSV_S_2:
          return {
            name: k,
            displayText: 'RSV - S 2 Bevorrechtigte Querung an einer untergeordneten Straße (mit FGÜ) - innerorts',
          };
        case Realisierungshilfe.RSV_S_3:
          return { name: k, displayText: 'RSV - S 3 Minikreisverkehr - innerorts' };
        case Realisierungshilfe.RSV_S_4:
          return { name: k, displayText: 'RSV - S 4 Wartepflichtige Querung mit Mittelinsel - innerorts/ außerorts' };
        case Realisierungshilfe.RSV_S_5:
          return {
            name: k,
            displayText: 'RSV - S 5 Querungsstelle mit nicht vollständiger Signalisierung - innerorts/ außerorts',
          };
        case Realisierungshilfe.RSV_H_1:
          return {
            name: k,
            displayText: 'RSV - H 1 Führung auf baulichen Radwegen im Einrichtungsverkehr - innerorts',
          };
        case Realisierungshilfe.RSV_H_2:
          return { name: k, displayText: 'RSV - H 2 Führung auf Radfahrstreifen - innerorts' };
        case Realisierungshilfe.RSV_H_3:
          return {
            name: k,
            displayText: 'RSV - H 3 Bevorrechtigte Führung auf baulichen Radwegen im Einrichtungsverkehr - außerorts',
          };
        case Realisierungshilfe.RSV_H_4:
          return {
            name: k,
            displayText: 'RSV - H 4 Bevorrechtigte Führung auf baulichen Radwegen im Zweirichtungsverkehr - außerorts',
          };
        case Realisierungshilfe.RSV_H_5:
          return { name: k, displayText: 'RSV - H 5 Führung an Kreisverkehren (Fahrbahn) - innerorts' };
        case Realisierungshilfe.RSV_H_6:
          return { name: k, displayText: 'RSV - H 6 Führung an Kreisverkehren (Seitenraum) - innerorts' };
        case Realisierungshilfe.RSV_H_7:
          return { name: k, displayText: 'RSV - H 7 Führung an Kreisverkehren (Seitenraum) - außerorts' };
        case Realisierungshilfe.RSV_N_1:
          return {
            name: k,
            displayText:
              'RSV - N 1 Fahrradstraße innerhalb von Tempo-30-Zonen (Bevorrechtigung durch Beschilderung) - innerorts',
          };
        case Realisierungshilfe.RSV_N_2:
          return {
            name: k,
            displayText: 'RSV - N 2 Fahrradstraße innerhalb von Tempo-30-Zonen (Bauliche Bevorrechtigung) - innerorts',
          };
        case Realisierungshilfe.RSV_N_3:
          return { name: k, displayText: 'RSV - N 3 Aufgeweiterer Radaufstellstreifen - innerorts' };
        case Realisierungshilfe.RSV_N_4:
          return { name: k, displayText: 'RSV - N 4 Rechts-vor-Links-Knoten mit Fahrbahnanhebung - innerorts' };
        case Realisierungshilfe.RSV_F_1:
          return {
            name: k,
            displayText:
              'RSV - F 1 Kreuzung mit Gemeinsamen Geh-/ Radweg und Radweg (selbstständig) - innerorts/ außerorts',
          };
        case Realisierungshilfe.RSV_F_2:
          return { name: k, displayText: 'RSV - F 2 Kreuzung mit Gehweg (selbstständig) - innerorts' };
      }
      throw new Error('Beschreibung für enum Realisierungshilfe fehlt: ' + k);
    })
    .sort((a, b) => {
      const aSegements = a.name.split('_').map(i => +i);
      const bSegements = b.name.split('_').map(i => +i);
      const compareFirstSegement = aSegements[1] - bSegements[1];
      if (compareFirstSegement === 0) {
        const compareSecondSegment = aSegements[2] - bSegements[2];
        if (compareSecondSegment === 0) {
          return aSegements[3] - bSegements[3];
        } else {
          return compareSecondSegment;
        }
      } else {
        return compareFirstSegement;
      }
    });
}
