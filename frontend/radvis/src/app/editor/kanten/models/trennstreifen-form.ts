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
import { Radverkehrsfuehrung } from 'src/app/shared/models/radverkehrsfuehrung';

export enum TrennstreifenForm {
  UNBEKANNT = 'UNBEKANNT',
  KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN = 'KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN',
  TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM = 'TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM',
  TRENNUNG_DURCH_SPERRPFOSTEN = 'TRENNUNG_DURCH_SPERRPFOSTEN',
  TRENNUNG_DURCH_GRUENSTREIFEN = 'TRENNUNG_DURCH_GRUENSTREIFEN',
  TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG = 'TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG',
  TRENNUNG_DURCH_ANDERE_ART = 'TRENNUNG_DURCH_ANDERE_ART',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace TrennstreifenForm {
  export const options: EnumOption[] = Object.keys(TrennstreifenForm).map((k: string): EnumOption => {
    switch (k) {
      case TrennstreifenForm.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
      case TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN:
        return { name: k, displayText: 'Kein Sicherheitstrennstreifen vorhanden' };
      case TrennstreifenForm.TRENNUNG_DURCH_FAHRZEUGRUEKHALTESYSTEM:
        return { name: k, displayText: 'Trennung durch Fahrzeugrückhaltesystem' };
      case TrennstreifenForm.TRENNUNG_DURCH_SPERRPFOSTEN:
        return { name: k, displayText: 'Trennung durch Sperrpfosten' };
      case TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN:
        return { name: k, displayText: 'Trennung durch Grünstreifen' };
      case TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG:
        return { name: k, displayText: 'Trennung durch markierungstechnische oder bauliche Trennung' };
      case TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART:
        return { name: k, displayText: 'Trennung durch andere Art' };
    }
    throw new Error('Beschreibung für enum TrennstreifenForm fehlt: ' + k);
  });

  export const isEnabledForRadverkehrsfuehrung = (radverkehrsfuehrung: Radverkehrsfuehrung): boolean => {
    return [
      Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND,
      Radverkehrsfuehrung.SCHUTZSTREIFEN,
      Radverkehrsfuehrung.RADFAHRSTREIFEN,
      Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR,
      Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR,
      Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE,
      Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADZONE,
      Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG,
      Radverkehrsfuehrung.MEHRZWECKSTREIFEN_EINSEITIG,
      Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER,
    ].includes(radverkehrsfuehrung);
  };
}
