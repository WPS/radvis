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

import { Color } from 'ol/color';
import { BelagArt } from 'src/app/shared/models/belag-art';
import { Radverkehrsfuehrung } from 'src/app/shared/models/radverkehrsfuehrung';

export const belagArtLegende: Map<BelagArt, Color> = new Map([
  [BelagArt.ASPHALT, [64, 0, 75, 1]],
  [BelagArt.BETON, [153, 112, 171, 1]],
  [BelagArt.BETONSTEINPFLASTER_PLATTENBELAG, [194, 165, 207, 1]],
  [BelagArt.NATURSTEINPFLASTER, [231, 212, 232, 1]],
  [BelagArt.WASSERGEBUNDENE_DECKE, [0, 68, 27, 1]],
  [BelagArt.UNGEBUNDENE_DECKE, [27, 120, 55, 1]],
  [BelagArt.SONSTIGER_BELAG, [166, 219, 160, 1]],
  [BelagArt.UNBEKANNT, [217, 240, 211, 1]],
]);

export const flatRadverkehrsfuehrungOptions = [
  ...Radverkehrsfuehrung.optionsSelbststaendig,
  ...Radverkehrsfuehrung.optionsStrassenbegleitend,
  ...Radverkehrsfuehrung.optionsFahrbahnfuehrung,
  Radverkehrsfuehrung.optionUnbekannt,
];

export const radverkehrsfuehrungLegende: Map<string, Color> = new Map([
  [Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_VIER_MEHRSTREIFIGE_FAHRBAHN, [165, 0, 38, 1]],
  [Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN, [165, 0, 38, 1]],

  [Radverkehrsfuehrung.PIKTOGRAMMKETTE_BEIDSEITIG, [215, 48, 39, 1]],
  [Radverkehrsfuehrung.SCHUTZSTREIFEN, [215, 48, 39, 1]],
  [Radverkehrsfuehrung.RADFAHRSTREIFEN, [215, 48, 39, 1]],
  [Radverkehrsfuehrung.RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR, [215, 48, 39, 1]],
  [Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR, [215, 48, 39, 1]],
  [Radverkehrsfuehrung.MEHRZWECKSTREIFEN_BEIDSEITIG, [215, 48, 39, 1]],

  [Radverkehrsfuehrung.FUEHRUNG_IN_T20_ZONE, [244, 109, 67, 1]],
  [Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE, [244, 109, 67, 1]],
  [Radverkehrsfuehrung.FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH, [244, 109, 67, 1]],

  [Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE, [253, 174, 97, 1]],
  [Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADZONE, [253, 174, 97, 1]],

  [Radverkehrsfuehrung.EINBAHNSTRASSE_MIT_FREIGABE_RADVERKEHR_MEHR_WENIGER_30, [254, 224, 144, 1]],
  [Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_ALS_30, [254, 224, 144, 1]],
  [Radverkehrsfuehrung.EINBAHNSTRASSE_OHNE_FREIGABE_RADVERKEHR_MEHR_WENIGER_30, [254, 224, 144, 1]],

  [Radverkehrsfuehrung.BEGEGNUNBSZONE, [255, 255, 191, 1]],
  [Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI, [255, 255, 191, 1]],
  [Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_NICHT_FREI, [255, 255, 191, 1]],
  [Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_ZEITW_FREI, [255, 255, 191, 1]],

  [Radverkehrsfuehrung.OEFFENTLICHE_STRASSE_MIT_FREIGABE_ANLIEGER, [116, 173, 209, 1]],
  [Radverkehrsfuehrung.BETRIEBSWEG_FORST, [116, 173, 209, 1]],
  [Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_SELBSTSTAENDIG, [116, 173, 209, 1]],
  [Radverkehrsfuehrung.BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND, [116, 173, 209, 1]],
  [Radverkehrsfuehrung.BETRIEBSWEG_WASSERWIRTSCHAFT, [116, 173, 209, 1]],
  [Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG, [116, 173, 209, 1]],

  [Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG, [69, 117, 180, 1]],
  [Radverkehrsfuehrung.GEHWEG_RAD_FREI_SELBSTSTAENDIG, [69, 117, 180, 1]],
  [Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_SELBSTSTAENDIG, [69, 117, 180, 1]],
  [Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG, [69, 117, 180, 1]],
  [Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_SELBSTSTAENDIG, [69, 117, 180, 1]],

  [Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND, [49, 54, 149, 1]],
  [Radverkehrsfuehrung.GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND, [49, 54, 149, 1]],
  [Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND, [49, 54, 149, 1]],
  [Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND, [49, 54, 149, 1]],
  [Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND, [49, 54, 149, 1]],

  [Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG, [224, 243, 248, 1]],

  [Radverkehrsfuehrung.UNBEKANNT, [52, 222, 188, 1]],
  ['', [52, 222, 188, 1]],
]);
