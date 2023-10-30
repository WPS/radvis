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

export const KNOTENFORMEN: GroupedEnumOptions = {
  BAUWERK: {
    displayText: 'Bauwerk',
    options: [
      {
        name: 'UEBERFUEHRUNG',
        displayText: 'Überführung',
      },
      {
        name: 'UNTERFUEHRUNG_TUNNEL',
        displayText: 'Unterführung/Tunnel',
      },
    ],
  },
  KNOTEN_MIT_KREISVERKEHR: {
    displayText: 'Knoten mit Kreisverkehr',
    options: [
      {
        name: 'MINIKREISVERKEHR_24_M',
        displayText: 'Minikreisverkehr (< 24 m)',
      },
      {
        name: 'KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_KREISFAHRBAHN',
        displayText: 'Kompaktkreisverkehr (Führung nur über Kreisfahrbahn)',
      },
      {
        name: 'KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE',
        displayText: 'Kompaktkreisverkehr (Führung nur über Nebenanlage)',
      },
      {
        name: 'KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE',
        displayText: 'Kompaktkreisverkehr (Führung über Kreisfahrbahn und Nebenanlage)',
      },
      {
        name: 'GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_KREISFAHRBAHN',
        displayText: 'Großkreisel/Sonderform (mehrstreifig/Turbokreisel) (Führung nur über Kreisfahrbahn)',
      },
      {
        name: 'GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE',
        displayText: 'Großkreisel/Sonderform (mehrstreifig/Turbokreisel) (Führung nur über Nebenanlage)',
      },
      {
        name: 'GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE',
        displayText: 'Großkreisel/Sonderform (mehrstreifig/Turbokreisel) (Führung über Kreisfahrbahn und Nebenanlage)',
      },
    ],
  },
  KNOTEN_MIT_LSA: {
    displayText: 'Knoten mit LSA',
    options: [
      {
        name: 'LSA_KNOTEN_MIT_RADVERKEHRSFUEHRUNG_UEBER_BAULICHE_NEBENANLAGE',
        displayText: 'LSA-Knoten mit Radverkehrsführung über bauliche Nebenanlage',
      },
      {
        name: 'LSA_KNOTEN_MIT_RADVERKEHRSFUEHRUNG_UEBER_FAHRBAHN_MARKIERUNGSTECHN_GESCHUETZT',
        displayText: 'LSA-Knoten mit Radverkehrsführung über Fahrbahn (markierungstechn. geschützt)',
      },
      {
        name: 'LSA_KNOTEN_MIT_RADVERKEHRSFUEHRUNG_UEBER_FAHRBAHN_MISCHVERKEHR',
        displayText: 'LSA-Knoten mit Radverkehrsführung über Fahrbahn (Mischverkehr)',
      },
    ],
  },
  SIGNALISIERTE_QUERUNGSSTELLE: {
    displayText: 'Signalisierte Querungsstelle',
    options: [
      {
        name: 'ERWEITERTE_FUSS_RADFAHRER_LSA',
        displayText: 'erweiterte Fuß-/Radfahrer-LSA',
      },
      {
        name: 'FUSS_RADFAHRER_LSA',
        displayText: 'Fuß-/Radfahrer-LSA',
      },
    ],
  },
  QUERUNG_EINER_UEBERGEORDNETEN_STRASSE: {
    displayText: 'Querung einer übergeordneten Straße',
    options: [
      {
        name: 'MITTELINSEL',
        displayText: 'Mittelinsel',
      },
      {
        name: 'FAHRBAHNEINENGUNG',
        displayText: 'Fahrbahneinengung',
      },
      {
        name: 'QUERUNGSSTELLE_OHNE_SICHERUNG',
        displayText: 'Querungsstelle ohne Sicherung',
      },
      {
        name: 'FUSSGAENGERUEBERWEG',
        displayText: 'Fußgängerüberweg',
      },
    ],
  },
  KNOTEN_MIT_VORFAHRTSREGELNDEN_VERKEHRSZEICHEN: {
    displayText: 'Knoten mit vorfahrtsregelnden Verkehrszeichen',
    options: [
      {
        name: 'RECHTS_VOR_LINKS_REGELUNG',
        displayText: 'rechts-vor-links Regelung',
      },
      {
        name: 'ABKNICKENDE_VORFAHRT',
        displayText: 'Abknickende Vorfahrt',
      },
      {
        name: 'NICHT_ABKNICKENDE_VORFAHRT',
        displayText: 'Nicht-abknickende Vorfahrt',
      },
    ],
  },
  SONSTIGER_KNOTEN: {
    displayText: 'Sonstiger Knoten',
    options: [
      {
        name: 'SONSTIGER_KNOTEN',
        displayText: 'Sonstiger Knoten',
      },
    ],
  },
};

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Knotenformen {
  export const getDisplayText = (key: string): string => {
    const result = '';
    if (key) {
      for (const group of Object.keys(KNOTENFORMEN)) {
        const knotenform = KNOTENFORMEN[group].options.find(t => t.name === key);
        if (knotenform) {
          return knotenform.displayText;
        }
      }
    }
    return result;
  };

  export const isLSAKnotenForm = (knotenForm: string): boolean => {
    return (
      KNOTENFORMEN.KNOTEN_MIT_LSA.options.some(enumOption => enumOption.name === knotenForm) ||
      KNOTENFORMEN.SIGNALISIERTE_QUERUNGSSTELLE.options.some(enumOption => enumOption.name === knotenForm)
    );
  };
}
