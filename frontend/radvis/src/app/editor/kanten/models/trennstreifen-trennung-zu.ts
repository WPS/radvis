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

export enum TrennstreifenTrennungZu {
  SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN = 'SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN',
  SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN = 'SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN',
  SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR = 'SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace TrennstreifenTrennungZu {
  export const values: string[] = Object.keys(TrennstreifenTrennungZu);
  export const options: EnumOption[] = values.map((k: string): EnumOption => {
    switch (k) {
      case TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN:
        return { name: k, displayText: 'Sicherheitstrennstreifen zur Fahrbahn' };
      case TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN:
        return { name: k, displayText: 'Sicherheitstrennstreifen zum Parken' };
      case TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR:
        return { name: k, displayText: 'Sicherheitstrennstreifen zum Fußverkehr' };
    }
    throw new Error('Beschreibung für enum TrennstreifenTrennungZu fehlt: ' + k);
  });

  export const isValidForRadverkehrsfuehrung = (
    trennungZu: TrennstreifenTrennungZu | string,
    radverkehrsfuehrung: Radverkehrsfuehrung
  ): boolean => {
    if (!trennungZu) {
      return true;
    }
    if (trennungZu === TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR) {
      return [
        Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
        Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND,
        Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER,
      ].includes(radverkehrsfuehrung);
    } else if (trennungZu === TrennstreifenTrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN) {
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
    } else {
      return [
        Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
        Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND,
        Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
        Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND,
        Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND,
        Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND,
        Radverkehrsfuehrung.RADFAHRSTREIFEN,
        Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR,
        Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR,
        Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER,
      ].includes(radverkehrsfuehrung);
    }
  };

  export const displayText = (value: TrennstreifenTrennungZu): string => {
    return TrennstreifenTrennungZu.options.find(opt => opt.name === value)!.displayText;
  };
}
