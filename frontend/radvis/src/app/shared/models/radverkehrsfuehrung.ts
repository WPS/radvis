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
import { GroupedEnumOptions } from 'src/app/form-elements/models/grouped-enum-options';

export enum Radverkehrsfuehrung {
  // Selbstständig
  SONDERWEG_RADWEG_SELBSTSTAENDIG = 'SONDERWEG_RADWEG_SELBSTSTAENDIG',
  GEH_RADWEG_GETRENNT_SELBSTSTAENDIG = 'GEH_RADWEG_GETRENNT_SELBSTSTAENDIG',
  GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG = 'GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG',
  GEHWEG_RAD_FREI_SELBSTSTAENDIG = 'GEHWEG_RAD_FREI_SELBSTSTAENDIG',
  GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG = 'GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG',
  BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG = 'BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG',
  BETRIEBSWEG_FORST = 'BETRIEBSWEG_FORST',
  BETRIEBSWEG_WASSERWIRTSCHAFT = 'BETRIEBSWEG_WASSERWIRTSCHAFT',
  SONSTIGER_BETRIEBSWEG = 'SONSTIGER_BETRIEBSWEG',
  OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER = 'OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER',

  // Straßenbegleitend
  SONDERWEG_RADWEG_STRASSENBEGLEITEND = 'SONDERWEG_RADWEG_STRASSENBEGLEITEND',
  GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND = 'GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND',
  GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND = 'GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND',
  GEHWEG_RAD_FREI_STRASSENBEGLEITEND = 'GEHWEG_RAD_FREI_STRASSENBEGLEITEND',
  GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND = 'GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND',
  BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND = 'BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND',

  // FAHRBAHNFUEHRUNG
  PIKTOGRAMMKETTE_BEIDSEITIG = 'PIKTOGRAMMKETTE_BEIDSEITIG',
  PIKTOGRAMMKETTE_EINSEITIG = 'PIKTOGRAMMKETTE_EINSEITIG',
  SCHUTZSTREIFEN = 'SCHUTZSTREIFEN',
  SCHUTZSTREIFEN_IN_TEMPOZONE = 'SCHUTZSTREIFEN_IN_TEMPOZONE',
  SCHUTZSTREIFEN_MIT_GEHWEG = 'SCHUTZSTREIFEN_MIT_GEHWEG',
  SCHUTZSTREIFEN_MIT_GEHWEG_IN_TEMPOZONE = 'SCHUTZSTREIFEN_MIT_GEHWEG_IN_TEMPOZONE',
  RADFAHRSTREIFEN = 'RADFAHRSTREIFEN',
  RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR = 'RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR',
  BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR = 'BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR',
  MEHRZWECKSTREIFEN_BEIDSEITIG = 'MEHRZWECKSTREIFEN_BEIDSEITIG',
  MEHRZWECKSTREIFEN_EINSEITIG = 'MEHRZWECKSTREIFEN_EINSEITIG',
  FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN = 'FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN',
  FUEHRUNG_AUF_FAHRBAHN_VIER_MEHRSTREIFIGE_FAHRBAHN = 'FUEHRUNG_AUF_FAHRBAHN_VIER_MEHRSTREIFIGE_FAHRBAHN',
  FUEHRUNG_IN_T30_ZONE = 'FUEHRUNG_IN_T30_ZONE',
  FUEHRUNG_IN_T30_ZONE_VORFAHRTSGEREGELT = 'FUEHRUNG_IN_T30_ZONE_VORFAHRTSGEREGELT',
  FUEHRUNG_IN_T20_ZONE = 'FUEHRUNG_IN_T20_ZONE',
  FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH = 'FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH',
  FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI = 'FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI',
  FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI = 'FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI',
  FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI = 'FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI',
  BEGEGNUNBSZONE = 'BEGEGNUNBSZONE',
  FUEHRUNG_IN_FAHRRADSTRASSE = 'FUEHRUNG_IN_FAHRRADSTRASSE',
  FUEHRUNG_IN_FAHRRADZONE = 'FUEHRUNG_IN_FAHRRADZONE',
  EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30 = 'EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30',
  EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30 = 'EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30',
  EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30 = 'EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30',

  GEMEINDEVERBINDUNGSSTRASSE = 'GEMEINDEVERBINDUNGSSTRASSE',
  SONSTIGE_STRASSE_WEG = 'SONSTIGE_STRASSE_WEG',

  // Unbekannt
  UNBEKANNT = 'UNBEKANNT',
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Radverkehrsfuehrung {
  const toOption = (k: Radverkehrsfuehrung): EnumOption => {
    switch (k) {
      case Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG:
        return { name: k, displayText: 'Sonderweg Radweg (selbstständig)' };
      case Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_SELBSTSTAENDIG:
        return { name: k, displayText: 'Geh-/Radweg getrennt (selbstständig)' };
      case Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG:
        return { name: k, displayText: 'Geh-/Radweg gemeinsam (selbstständig)' };
      case Radverkehrsfuehrung.GEHWEG_RAD_FREI_SELBSTSTAENDIG:
        return { name: k, displayText: 'Gehweg (Rad frei) (selbstständig)' };
      case Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG:
        return { name: k, displayText: 'Gem. Rad-/Gehweg mit Gehweg (Rad frei in Gegenrichtung) (selbstständig)' };
      case Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG:
        return { name: k, displayText: 'Betriebsweg Landwirtschaft (selbstständig)' };
      case Radverkehrsfuehrung.BETRIEBSWEG_FORST:
        return { name: k, displayText: 'Betriebsweg Forst' };
      case Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT:
        return { name: k, displayText: 'Betriebsweg Wasserwirtschaft' };
      case Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG:
        return { name: k, displayText: 'Sonstiger Betriebsweg' };
      case Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER:
        return { name: k, displayText: 'Öffentliche Straße / Weg (mit Freigabe Anlieger)' };
      case Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND:
        return { name: k, displayText: 'Sonderweg Radweg (straßenbegleitend)' };
      case Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND:
        return { name: k, displayText: 'Geh-/Radweg getrennt (straßenbegleitend)' };
      case Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND:
        return { name: k, displayText: 'Geh-/Radweg gemeinsam (straßenbegleitend)' };
      case Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND:
        return { name: k, displayText: 'Gehweg (Rad frei) (straßenbegleitend)' };
      case Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND:
        return { name: k, displayText: 'Gem. Rad-/Gehweg mit Gehweg (Rad frei in Gegenrichtung) (straßenbegleitend)' };
      case Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND:
        return { name: k, displayText: 'Betriebsweg Landwirtschaft (straßenbegleitend)' };
      case Radverkehrsfuehrung.PIKTOGRAMMKETTE_BEIDSEITIG:
        return { name: k, displayText: 'Piktogrammkette (beidseitig)' };
      case Radverkehrsfuehrung.PIKTOGRAMMKETTE_EINSEITIG:
        return { name: k, displayText: 'Piktogrammkette (einseitig)' };
      case Radverkehrsfuehrung.SCHUTZSTREIFEN:
        return { name: k, displayText: 'Schutzstreifen' };
      case Radverkehrsfuehrung.RADFAHRSTREIFEN:
        return { name: k, displayText: 'Radfahrstreifen' };
      case Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR:
        return { name: k, displayText: 'Radfahrstreifen (mit Freigabe Busverkehr)' };
      case Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR:
        return { name: k, displayText: 'Busfahrstreifen (mit Freigabe Radverkehr)' };
      case Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG:
        return { name: k, displayText: 'Mehrzweckstreifen (beidseitig)' };
      case Radverkehrsfuehrung.MEHRZWECKSTREIFEN_EINSEITIG:
        return { name: k, displayText: 'Mehrzweckstreifen (einseitig)' };
      case Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN:
        return { name: k, displayText: 'Führung auf Fahrbahn (30 - 100 km/h) zweistreifige Fahrbahn' };
      case Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_VIER_MEHRSTREIFIGE_FAHRBAHN:
        return { name: k, displayText: 'Führung auf Fahrbahn (30 - 100 km/h) vier- / mehrstreifige Fahrbahn' };
      case Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE:
        return { name: k, displayText: 'Führung in T30-Zone' };
      case Radverkehrsfuehrung.FUEHRUNG_IN_T20_ZONE:
        return { name: k, displayText: 'Führung in T20-Zone' };
      case Radverkehrsfuehrung.FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH:
        return { name: k, displayText: 'Führung in Verkehrsberuhigter Bereich' };
      case Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI:
        return { name: k, displayText: 'Führung in Fußg.-Zone (Rad frei)' };
      case Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI:
        return { name: k, displayText: 'Führung in Fußg.-Zone (Rad zeitw. frei)' };
      case Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI:
        return { name: k, displayText: 'Führung in Fußg.-Zone (Rad nicht frei)' };
      case Radverkehrsfuehrung.BEGEGNUNBSZONE:
        return { name: k, displayText: 'Begegnungszone' };
      case Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE:
        return { name: k, displayText: 'Führung in Fahrradstraße' };
      case Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADZONE:
        return { name: k, displayText: 'Führung in Fahrradzone' };
      case Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30:
        return { name: k, displayText: 'Einbahnstraße (ohne Freigabe Radverkehr > 30 km/h)' };
      case Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30:
        return { name: k, displayText: 'Einbahnstraße (ohne Freigabe Radverkehr bei ≤ 30 km/h)' };
      case Radverkehrsfuehrung.EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30:
        return { name: k, displayText: 'Einbahnstraße (mit Freigabe Radverkehr bei ≤ 30 km/h)' };
      case Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG:
        return { name: k, displayText: 'Sonstige Straße / Weg' };
      case Radverkehrsfuehrung.UNBEKANNT:
        return { name: k, displayText: 'Unbekannt' };
      case Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE_VORFAHRTSGEREGELT:
        return { name: k, displayText: 'Führung in T30-Zone (vorfahrtsgeregelt)' };
      case Radverkehrsfuehrung.GEMEINDEVERBINDUNGSSTRASSE:
        return { name: k, displayText: 'Gemeindeverbindungsstraße' };
      case Radverkehrsfuehrung.SCHUTZSTREIFEN_IN_TEMPOZONE:
        return { name: k, displayText: 'Schutzstreifen in Tempozone' };
      case Radverkehrsfuehrung.SCHUTZSTREIFEN_MIT_GEHWEG:
        return { name: k, displayText: 'Schutzstreifen mit Gehweg (Rad frei)' };
      case Radverkehrsfuehrung.SCHUTZSTREIFEN_MIT_GEHWEG_IN_TEMPOZONE:
        return { name: k, displayText: 'Schutzstreifen mit Gehweg (Rad frei) in Tempozone' };
    }
  };

  export const allSelbststaendig: Radverkehrsfuehrung[] = [
    Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG,
    Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_SELBSTSTAENDIG,
    Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG,
    Radverkehrsfuehrung.GEHWEG_RAD_FREI_SELBSTSTAENDIG,
    Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG,
    Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG,
    Radverkehrsfuehrung.BETRIEBSWEG_FORST,
    Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT,
    Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG,
    Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER,
  ];

  export const allStrassenbegleitend: Radverkehrsfuehrung[] = [
    Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND,
    Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND,
  ];

  export const allFahrbahnbegleitend: Radverkehrsfuehrung[] = [
    Radverkehrsfuehrung.PIKTOGRAMMKETTE_BEIDSEITIG,
    Radverkehrsfuehrung.PIKTOGRAMMKETTE_EINSEITIG,
    Radverkehrsfuehrung.SCHUTZSTREIFEN,
    Radverkehrsfuehrung.RADFAHRSTREIFEN,
    Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR,
    Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR,
    Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG,
    Radverkehrsfuehrung.MEHRZWECKSTREIFEN_EINSEITIG,
    Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN,
    Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_VIER_MEHRSTREIFIGE_FAHRBAHN,
    Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE,
    Radverkehrsfuehrung.FUEHRUNG_IN_T20_ZONE,
    Radverkehrsfuehrung.FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH,
    Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI,
    Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI,
    Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI,
    Radverkehrsfuehrung.BEGEGNUNBSZONE,
    Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE,
    Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADZONE,
    Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30,
    Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30,
    Radverkehrsfuehrung.EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30,
    Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG,
    Radverkehrsfuehrung.GEMEINDEVERBINDUNGSSTRASSE,
    Radverkehrsfuehrung.SCHUTZSTREIFEN_IN_TEMPOZONE,
    Radverkehrsfuehrung.SCHUTZSTREIFEN_MIT_GEHWEG,
    Radverkehrsfuehrung.SCHUTZSTREIFEN_MIT_GEHWEG_IN_TEMPOZONE,
    Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE_VORFAHRTSGEREGELT,
  ];

  export const all: Radverkehrsfuehrung[] = [
    ...allSelbststaendig,
    ...allFahrbahnbegleitend,
    ...allStrassenbegleitend,
    Radverkehrsfuehrung.UNBEKANNT,
  ];

  export const optionsSelbststaendig: EnumOption[] = allSelbststaendig.map(toOption);
  export const optionsStrassenbegleitend: EnumOption[] = allStrassenbegleitend.map(toOption);
  export const optionsFahrbahnfuehrung: EnumOption[] = allFahrbahnbegleitend.map(toOption);
  export const optionUnbekannt: EnumOption = toOption(Radverkehrsfuehrung.UNBEKANNT);

  export const options: GroupedEnumOptions = {
    SELBSTSTAENDIG: {
      displayText: 'Selbstständig',
      options: optionsSelbststaendig,
    },
    STRASSENBEGLEITEND: {
      displayText: 'Straßenbegleitend',
      options: optionsStrassenbegleitend,
    },
    FAHRBAHNFUEHRUNG: {
      displayText: 'Fahrbahnführung',
      options: optionsFahrbahnfuehrung,
    },
    UNBEKANNT: {
      displayText: 'Unbekannt',
      options: [
        {
          name: 'UNBEKANNT',
          displayText: 'Unbekannt',
        },
      ],
    },
  } as GroupedEnumOptions;
}
