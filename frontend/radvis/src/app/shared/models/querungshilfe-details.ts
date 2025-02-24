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

export enum QuerungshilfeDetails {
  KEINE_VORHANDEN = 'KEINE_VORHANDEN',
  VORHANDEN_OHNE_FURT = 'VORHANDEN_OHNE_FURT',
  VORHANDEN_MIT_FURT_ZEBRA = 'VORHANDEN_MIT_FURT_ZEBRA',
  VORHANDEN_MIT_FURT = 'VORHANDEN_MIT_FURT',
  VORHANDEN_OHNE_FURT_ZEBRA = 'VORHANDEN_OHNE_FURT_ZEBRA',
  UNBEKANNT = 'UNBEKANNT',
  MITTELINSEL_OK = 'MITTELINSEL_OK',
  AUFSTELLFLAECHE_ZU_SCHMAL = 'AUFSTELLFLAECHE_ZU_SCHMAL',
  UMWEGE = 'UMWEGE',
  AUFSTELLFLAECHE_ZU_SCHMAL_UMWEGE = 'AUFSTELLFLAECHE_ZU_SCHMAL_UMWEGE',
  ANDERE_ANMERKUNG_MITTELINSEL = 'ANDERE_ANMERKUNG_MITTELINSEL',
  AUFSTELLBEREICH_OK = 'AUFSTELLBEREICH_OK',
  AUFSTELLBEREICH_ZU_SCHMAL = 'AUFSTELLBEREICH_ZU_SCHMAL',
  AUSTELLFLAECHE_SCHWER_ERKENNBAR = 'AUSTELLFLAECHE_SCHWER_ERKENNBAR',
  AUFSTELLFLAECHE_ZU_SCHMAL_SCHWER_ERKENNBAR = 'AUFSTELLFLAECHE_ZU_SCHMAL_SCHWER_ERKENNBAR',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace QuerungshilfeDetails {
  export const options: EnumOption[] = Object.keys(QuerungshilfeDetails).map((k: string): EnumOption => {
    switch (k) {
      case QuerungshilfeDetails.KEINE_VORHANDEN:
        return { name: k, displayText: 'Keine baulichen Querungshilfen (mit / ohne Zebra), keine Furt' };
      case QuerungshilfeDetails.VORHANDEN_OHNE_FURT:
        return { name: k, displayText: 'Bauliche Querungshilfen vorhanden, aber ohne Furt' };
      case QuerungshilfeDetails.VORHANDEN_MIT_FURT_ZEBRA:
        return { name: k, displayText: 'Bauliche Querungshilfen und Zebra + Furten vorhanden' };
      case QuerungshilfeDetails.VORHANDEN_MIT_FURT:
        return { name: k, displayText: 'Bauliche Querungshilfen und Furten vorhanden' };
      case QuerungshilfeDetails.VORHANDEN_OHNE_FURT_ZEBRA:
        return { name: k, displayText: 'Bauliche Querungshilfen und Zebra vorhanden (ohne Furt)' };
      case QuerungshilfeDetails.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
      case QuerungshilfeDetails.MITTELINSEL_OK:
        return { name: k, displayText: 'Mittelinsel in Ordnung (Aufstellfläche = 4 x 2,5 m & keine Umwege)' };
      case QuerungshilfeDetails.AUFSTELLFLAECHE_ZU_SCHMAL:
        return { name: k, displayText: 'Aufstellfläche zu schmal (< 4 m und/oder < 2,5 m)' };
      case QuerungshilfeDetails.UMWEGE:
        return { name: k, displayText: 'Mittelinsel nur über Umwege zu nutzen' };
      case QuerungshilfeDetails.AUFSTELLFLAECHE_ZU_SCHMAL_UMWEGE:
        return { name: k, displayText: 'Aufstellfläche zu schmal und Mittelinsel zu umwegig' };
      case QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL:
        return { name: k, displayText: 'Andere Anmerkung zu Mittelinsel(n)' };
      case QuerungshilfeDetails.AUFSTELLBEREICH_OK:
        return { name: k, displayText: 'Aufstellbereich ist mindestens 2,5 m breit' };
      case QuerungshilfeDetails.AUFSTELLBEREICH_ZU_SCHMAL:
        return { name: k, displayText: 'Aufstellbereich zu schmal' };
      case QuerungshilfeDetails.AUSTELLFLAECHE_SCHWER_ERKENNBAR:
        return {
          name: k,
          displayText: 'Aufstellfläche zwischen den Mittelinseln schwer erkennbar (z.B. keine Markierung)',
        };
      case QuerungshilfeDetails.AUFSTELLFLAECHE_ZU_SCHMAL_SCHWER_ERKENNBAR:
        return { name: k, displayText: 'Aufstellfläche zu schmal und zwischen den Mittelinseln schwer erkennbar' };
    }
    throw new Error('Beschreibung für enum QuerungshilfeDetails fehlt: ' + k);
  });

  export const isValidForKontenform = (
    querungshilfeDetails: QuerungshilfeDetails | string,
    knotenForm: string
  ): boolean => {
    if (knotenForm === 'MITTELINSEL_EINFACH') {
      return (
        querungshilfeDetails === QuerungshilfeDetails.MITTELINSEL_OK ||
        querungshilfeDetails === QuerungshilfeDetails.AUFSTELLFLAECHE_ZU_SCHMAL ||
        querungshilfeDetails === QuerungshilfeDetails.UMWEGE ||
        querungshilfeDetails === QuerungshilfeDetails.AUFSTELLFLAECHE_ZU_SCHMAL_UMWEGE ||
        querungshilfeDetails === QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL ||
        querungshilfeDetails === QuerungshilfeDetails.UNBEKANNT
      );
    } else if (knotenForm === 'MITTELINSEL_GETEILT') {
      return (
        querungshilfeDetails === QuerungshilfeDetails.AUFSTELLBEREICH_OK ||
        querungshilfeDetails === QuerungshilfeDetails.AUFSTELLBEREICH_ZU_SCHMAL ||
        querungshilfeDetails === QuerungshilfeDetails.AUSTELLFLAECHE_SCHWER_ERKENNBAR ||
        querungshilfeDetails === QuerungshilfeDetails.AUFSTELLFLAECHE_ZU_SCHMAL_SCHWER_ERKENNBAR ||
        querungshilfeDetails === QuerungshilfeDetails.ANDERE_ANMERKUNG_MITTELINSEL ||
        querungshilfeDetails === QuerungshilfeDetails.UNBEKANNT
      );
    } else if (
      knotenForm === 'KOMPAKTKREISVERKEHR_FUEHRUNG_NUR_UEBER_NEBENANLAGE' ||
      knotenForm === 'KOMPAKTKREISVERKEHR_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE' ||
      knotenForm === 'GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_NUR_UEBER_NEBENANLAGE' ||
      knotenForm === 'GROSSKREISEL_SONDERFORM_MEHRSTREIFIG_TURBOKREISEL_FUEHRUNG_UEBER_KREISFAHRBAHN_UND_NEBENANLAGE'
    ) {
      return (
        querungshilfeDetails === QuerungshilfeDetails.UNBEKANNT ||
        querungshilfeDetails === QuerungshilfeDetails.KEINE_VORHANDEN ||
        querungshilfeDetails === QuerungshilfeDetails.VORHANDEN_OHNE_FURT ||
        querungshilfeDetails === QuerungshilfeDetails.VORHANDEN_MIT_FURT_ZEBRA ||
        querungshilfeDetails === QuerungshilfeDetails.VORHANDEN_MIT_FURT ||
        querungshilfeDetails === QuerungshilfeDetails.VORHANDEN_OHNE_FURT_ZEBRA
      );
    }

    return false;
  };

  export const isEnabledForKnotenform = (knotenform: string): boolean => {
    return getOptionsForKnotenform(knotenform).length > 0;
  };

  export const getOptionsForKnotenform = (knotenform: string): EnumOption[] => {
    return options.filter(opt => {
      return isValidForKontenform(opt.name, knotenform);
    });
  };
}
