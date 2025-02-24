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

export enum BarriereFormDetails {
  SPERRPFOSTEN_UNGESICHERT = 'SPERRPFOSTEN_UNGESICHERT',
  SPERRPFOSTEN_GESICHERT = 'SPERRPFOSTEN_GESICHERT',
  SPERRPFOSTEN_GESICHERT_MIT_BODENMARKIERUNG = 'SPERRPFOSTEN_GESICHERT_MIT_BODENMARKIERUNG',
  UMLAUFSPERRE_NICHT_BEFAHRBAR = 'UMLAUFSPERRE_NICHT_BEFAHRBAR',
  UMLAUFSPERRE_REGELKONFORM = 'UMLAUFSPERRE_REGELKONFORM',
  SCHRANKE_NICHT_UMFAHRBAR = 'SCHRANKE_NICHT_UMFAHRBAR',
  SCHRANKE_NICHT_REGELKONFORM = 'SCHRANKE_NICHT_REGELKONFORM',
  SONSTIGE_GEFAHRENSTELLE = 'SONSTIGE_GEFAHRENSTELLE',
  SONSTIGE_BARRIERE = 'SONSTIGE_BARRIERE',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace BarriereFormDetails {
  export const options: EnumOption[] = Object.keys(BarriereFormDetails).map((k: string): EnumOption => {
    switch (k) {
      case BarriereFormDetails.SPERRPFOSTEN_UNGESICHERT:
        return { name: k, displayText: 'Sperrpfosten ungesichert' };
      case BarriereFormDetails.SPERRPFOSTEN_GESICHERT:
        return { name: k, displayText: 'Sperrpfosten gesichert' };
      case BarriereFormDetails.SPERRPFOSTEN_GESICHERT_MIT_BODENMARKIERUNG:
        return { name: k, displayText: 'Sperrpfosten gesichert & mit Bodenmarkierung versehen' };
      case BarriereFormDetails.UMLAUFSPERRE_NICHT_BEFAHRBAR:
        return { name: k, displayText: 'Umlaufsperre nicht-befahrbar' };
      case BarriereFormDetails.UMLAUFSPERRE_REGELKONFORM:
        return { name: k, displayText: 'Umlaufsperre regelkonform' };
      case BarriereFormDetails.SCHRANKE_NICHT_UMFAHRBAR:
        return { name: k, displayText: 'Schranke nicht umfahrbar' };
      case BarriereFormDetails.SCHRANKE_NICHT_REGELKONFORM:
        return { name: k, displayText: 'Schranke regelkonform (es gelten die gleichen Maße wie bei Umlaufgittern)' };
      case BarriereFormDetails.SONSTIGE_GEFAHRENSTELLE:
        return { name: k, displayText: 'sonstige Gefahrenstelle' };
      case BarriereFormDetails.SONSTIGE_BARRIERE:
        return { name: k, displayText: 'sonstige Barriere' };
    }
    throw new Error('Beschreibung für enum BarriereFormDetails fehlt: ' + k);
  });

  export const getOptionsForBarriereForm = (barriereForm: string): EnumOption[] => {
    return options.filter(opt => isValidForBarriereForm(opt.name, barriereForm));
  };

  export const isEnabledForBarriereForm = (barriereForm: string): boolean => {
    return (
      barriereForm === 'SPERRPFOSTEN' ||
      barriereForm === 'UMLAUFSPERREN' ||
      barriereForm === 'SCHRANKE' ||
      barriereForm === 'SONSTIGE_BARRIERE'
    );
  };

  export const isValidForBarriereForm = (
    barriereFormDetails: BarriereFormDetails | string,
    barriereForm: string
  ): boolean => {
    switch (barriereFormDetails) {
      case BarriereFormDetails.SPERRPFOSTEN_GESICHERT:
      case BarriereFormDetails.SPERRPFOSTEN_UNGESICHERT:
      case BarriereFormDetails.SPERRPFOSTEN_GESICHERT_MIT_BODENMARKIERUNG:
        return barriereForm === 'SPERRPFOSTEN';
      case BarriereFormDetails.UMLAUFSPERRE_NICHT_BEFAHRBAR:
      case BarriereFormDetails.UMLAUFSPERRE_REGELKONFORM:
        return barriereForm === 'UMLAUFSPERREN';
      case BarriereFormDetails.SCHRANKE_NICHT_REGELKONFORM:
      case BarriereFormDetails.SCHRANKE_NICHT_UMFAHRBAR:
        return barriereForm === 'SCHRANKE';
      case BarriereFormDetails.SONSTIGE_BARRIERE:
      case BarriereFormDetails.SONSTIGE_GEFAHRENSTELLE:
        return barriereForm === 'SONSTIGE_BARRIERE';
      default:
        return false;
    }
  };
}
