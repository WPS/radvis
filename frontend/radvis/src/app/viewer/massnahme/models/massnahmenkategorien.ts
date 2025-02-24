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

import { AbstractControl, ValidationErrors } from '@angular/forms';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import {
  MassnahmenkategorieGruppe,
  MassnahmenkategorieOptionGroup,
} from 'src/app/viewer/massnahme/models/massnahmenkategorie-option-group';
import invariant from 'tiny-invariant';

export class Massnahmenkategorien {
  public static ALL: MassnahmenkategorieOptionGroup[] = [
    {
      name: 'STRECKENMASSNAHMENKATEGORIEN',
      displayText: 'Streckenmaßnahmenkategorien',
      options: [
        {
          name: 'STVO_BESCHILDERUNG',
          displayText: 'StVO Beschilderung / Änderung der verkehrsrechtlichen Anordnung',
          gewichtung: 5,
          options: [
            {
              name: 'STRECKE_FUER_KFZVERKEHR_SPERREN',
              displayText: 'Strecke für Kfz-Verkehr sperren, Anlieger frei',
            },
            {
              name: 'EINRICHTUNG_MODALFILTER_PRUEFEN',
              displayText: 'Einrichtung von Modalfiltern prüfen',
            },
            {
              name: 'UMWIDMUNG_GEMEINSAMER_RADGEHWEG',
              displayText: 'Umwidmung in gemeinsamen Rad-/Gehweg',
            },
            {
              name: 'BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN',
              displayText: 'Benutzungspflicht für den Radverkehr aufheben',
            },
            {
              name: 'BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN_RADFAHRER_FREI',
              displayText: 'Benutzungspflicht für den Radverkehr aufheben, Radfahrer frei',
            },
            {
              name: 'BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN_RADFAHRER_FREI_REDUZIERUNG_HOECHSTGESCHWINDIGKEIT',
              displayText:
                'Benutzungspflicht für den Radverkehr aufheben, Reduzierung der vorgeschriebenen Höchstgeschwindigkeit',
            },
            {
              name: 'BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN_RADFAHRER_FREI_REDUZIERUNG_HOECHSTGESCHWINDIGKEIT_RADFAHRER_FREI',
              displayText:
                'Benutzungspflicht für den Radverkehr aufheben, Reduzierung der vorgeschriebenen Höchstgeschwindigkeit, Radfahrer frei',
            },
            { name: 'ZWEIRICHTUNGSFUEHRUNG_AUFHEBEN', displayText: 'Zweirichtungsführung aufheben' },
            {
              name: 'REDUZIERUNG_DER_VORGESCHRIEBENEN_HOECHSTGESCHWINDIGKEIT',
              displayText: 'Reduzierung der vorgeschriebenen Höchstgeschwindigkeit',
            },
            {
              name: 'OEFFNUNG_EINBAHNSTRASSE_RADVERKEHR_BEIDE_RICHTUNGEN',
              displayText: 'Öffnung der Einbahnstraße für den Radverkehr in beide Richtungen',
            },
            { name: 'EINRICHTUNG_FAHRRADSTRASSE', displayText: 'Einrichtung einer Fahrradstraße' },
            { name: 'RADFAHRER_FREI', displayText: 'Radfahrer frei' },
            { name: 'SONSTIGE_STVO_BESCHILDERUNG', displayText: 'Sonstige StVO Beschilderung' },
          ],
        },
        {
          name: 'MARKIERUNG',
          displayText: 'Markierung',
          gewichtung: 3,
          options: [
            {
              name: 'MARKIERUNG_PIKTOGRAMMKETTE',
              displayText: 'Piktogrammkette markieren',
            },
            {
              name: 'NEUMARKIERUNG_PIKTOGRAMMKETTE',
              displayText: 'Piktogrammkette anpassen',
            },
            {
              name: 'NEUMARKIERUNG_SCHUTZSTREIFEN',
              displayText: 'Neumarkierung Schutzstreifen (inkl. Neuordnung Straßenraum)',
            },
            {
              name: 'NEUMARKIERUNG_RADFAHRSTREIFEN',
              displayText: 'Neumarkierung Radfahrstreifen (inkl. Neuordnung Straßenraum)',
            },
            {
              name: 'MARKIERUNG_SICHERHEITSTRENNSTREIFEN',
              displayText: 'Markierung Sicherheitstrennstreifen',
            },
            {
              name: 'MARKIERUNG_RADFAHRSTREIFEN',
              displayText: 'Markierung Radfahrstreifen (inkl. Neuordnung Straßenraum)',
            },
            {
              name: 'MARKIERUNG_SCHUTZSTREIFEN',
              displayText: 'Markierung Schutzstreifen (inkl. Neuordnung Straßenraum)',
            },
            {
              name: 'DEMARKIERUNG',
              displayText: 'Demarkierung',
            },
            {
              name: 'FURT_STVO_KONFORM',
              displayText: 'Furt StVO konform herstellen oder umgestalten',
            },
            {
              name: 'SONSTIGE_MARKIERUNG',
              displayText: 'Sonstige Markierung',
            },
          ],
        },
        {
          name: 'AUSBAU',
          displayText: 'Ausbau',
          gewichtung: 2,
          options: [
            {
              name: 'STRASSENRAUM_BAULICH_NEU_ORDNEN',
              displayText: 'Straßenraum baulich neu ordnen',
            },
            {
              name: 'AUSBAU_BESTEHENDEN_WEGES_NACH_QUALITAETSSTANDARD',
              displayText: 'Ausbau des bestehenden Weges nach Qualitätsstandard',
            },
            {
              name: 'AUSBAU_BESTEHENDEN_WEGES_MIT_GERINGEREM_QUALITAETSSTANDARD',
              displayText: 'Ausbau des bestehenden Weges mit geringerem Qualitätsstandard',
            },
          ],
        },
        {
          name: 'NEUBAU',
          displayText: 'Neubau',
          gewichtung: 1,
          options: [
            {
              name: 'NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_160CM',
              displayText:
                'Neubau einer baulichen Radverkehrsanlage ≥ 1,60m (Fußverkehrsanlage muss ≥ 1,80m Breite beibehalten)',
            },
            {
              name: 'NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_200CM',
              displayText: 'Radweg neu bauen',
            },
            {
              name: 'NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_250CM',
              displayText: 'Neubau einer baulichen Radverkehrsanlage ≥ 2,50m',
            },
            {
              name: 'NEUBAU_WEG_NACH_RADNETZ_QUALITAETSSTANDARD',
              displayText: 'Neubau eines Weges nach RadNETZ-Qualitätsstandard (Stand 2016)',
            },
            {
              name: 'NEUBAU_WEG_NACH_QUALITAETSSTANDARD',
              displayText: 'Neubau eines Weges nach Qualitätsstandard',
            },
            {
              name: 'NEUBAU_BAULICHE_ANLAGE',
              displayText: 'Selbstständigen (Rad-)Weg neu bauen',
            },
          ],
        },
        {
          name: 'BELAG',
          displayText: 'Belag',
          gewichtung: 4,
          options: [
            {
              name: 'WASSERGEBUNDENE_DECKE_ANLEGEN',
              displayText: 'Wassergebundene Decke herstellen',
            },
            {
              name: 'OBERFLAECHE_ASPHALTIEREN',
              displayText: 'Oberfläche asphaltieren',
            },
            {
              name: 'SPURBAHN_MIT_DURCHGAENGIGEM_BELAG_ERSETZEN',
              displayText: 'Spurbahn mit durchgängigem Belag ersetzen',
            },
            {
              name: 'BELAG_ABSCHNITTSWEISE_ERNEUERN',
              displayText: 'Belag abschnittsweise erneuern',
            },
            {
              name: 'PUNKTUELLE_DECKENERNEUERUNG',
              displayText: 'Punktuelle Deckenerneuerung',
            },
            {
              name: 'SONSTIGE_SANIERUNGSMASSNAHME',
              displayText: 'Sonstige Sanierungsmaßnahme',
            },
          ],
        },
        {
          name: 'SICHERUNG_RADWEGANFANG_ENDE',
          displayText: 'Sicherung Radweganfang/Ende',
          gewichtung: 6,
          options: [
            {
              name: 'ANFANG_UND_ENDE_RADWEG_SICHERN',
              displayText: 'Anfang und Ende Radweg sichern',
            },
            {
              name: 'EIN_ENDE_DES_RADWEGES_SICHERN',
              displayText: 'Ein Ende des Radweges sichern',
            },
            {
              name: 'QUERUNGSMOEGLICHKEIT_HERSTELLEN',
              displayText: 'Querungsmöglichkeit herstellen',
            },
            {
              name: 'SONSTIGE_MASSNAHME_AN_RADWEGANFANG_ENDE',
              displayText: 'Sonstige Maßnahme an Radweganfang-/ende',
            },
          ],
        },
        {
          name: 'FURTEN_ERNEUERN',
          displayText: 'Furten erneuern',
          gewichtung: 7,
          hidden: true,
          options: [
            {
              name: 'FURTEN_ERNEUERN',
              displayText: 'Furten erneuern',
            },
          ],
        },
        {
          name: 'FURTEN_HERSTELLEN',
          displayText: 'Furten herstellen',
          gewichtung: 8,
          hidden: true,
          options: [
            {
              name: 'FURTEN_HERSTELLEN',
              displayText: 'Furten herstellen',
            },
          ],
        },
        {
          name: 'HERSTELLUNG_RANDMARKIERUNG_BELEUCHTUNG',
          displayText: 'Herstellung Randmarkierung/Beleuchtung',
          gewichtung: 6,
          options: [
            {
              name: 'BELEUCHTUNG_HERSTELLEN',
              displayText: 'Beleuchtung herstellen',
            },
            {
              name: 'RANDMARKIERUNG_HERSTELLEN',
              displayText: 'Randmarkierung herstellen',
            },
          ],
        },
        {
          name: 'HERSTELLUNG_ABSENKUNG',
          displayText: 'Herstellung von Absenkung',
          gewichtung: 7,
          options: [
            {
              name: 'RADWEG_AN_ZUFAHRTEN_NIVEAUGLEICH_AUSBAUEN',
              displayText: '(Rad-)Weg an Zufahrten niveaugleich ausbauen',
            },
            {
              name: 'RADWEGABSENKUNGEN_AN_ZUFAHRTEN_AUFHEBEN_INNERORTS',
              displayText: 'Radwegabsenkungen an Grundstückszufahrten aufheben (innerorts)',
            },
            {
              name: 'RADWEGABSENKUNGEN_AN_ZUFAHRTEN_AUFHEBEN_AUSSERORTS',
              displayText: 'Radwegabsenkungen an Grundstückszufahrten aufheben (außerorts)',
            },
          ],
        },
        {
          name: 'ABSENKEN_VON_BORDEN',
          displayText: 'Absenken von Borden',
          gewichtung: 8,
          options: [
            {
              name: 'BORDABSENKUNGEN_HERSTELLEN_INNERORTS',
              displayText: 'Bordabsenkungen herstellen (innerorts)',
            },
            {
              name: 'BORDABSENKUNGEN_HERSTELLEN_AUSSERORTS',
              displayText: 'Bordabsenkungen herstellen (außerorts)',
            },
          ],
        },
        {
          name: 'SONSTIGE_BAUMASSNAHME',
          displayText: 'Sonstige Baumaßnahme',
          gewichtung: 9,
          hidden: true,
          options: [{ name: 'SONSTIGE_BAUMASSNAHME', displayText: 'Sonstige Baumaßnahme' }],
        },
        {
          name: 'SONSTIGE_STRECKENMASSNAHME',
          displayText: 'Sonstige Streckenmaßnahme',
          gewichtung: 10,
          hidden: true,
          options: [{ name: 'SONSTIGE_STRECKENMASSNAHME', displayText: 'Sonstige Streckenmaßnahme(n)' }],
        },
        {
          name: 'PLANUNGEN_RSV_PVR',
          displayText: 'Planungen RSV/RVR',
          gewichtung: 8,
          options: [
            { name: 'MASSNAHME_RADSCHNELLVERBINDUNG', displayText: 'Maßnahmen im Zuge Radschnellverbindung' },
            { name: 'MASSNAHME_RADVORRANGROUTE', displayText: 'Maßnahmen im Zuge Radvorrangroute' },
          ],
        },
      ],
    },
    {
      name: 'KNOTENMASSNAHMENKATEGORIEN',
      displayText: 'Knotenmaßnahmenkategorien',
      options: [
        {
          name: 'AUS_UMBAU',
          displayText: 'Aus-/Umbau',
          gewichtung: 2,
          options: [
            {
              name: 'BELEUCHTUNG_HERSTELLEN_PUNKTUELL',
              displayText: 'Kleinräumig Beleuchtung herstellen',
            },
            {
              name: 'ANPASSUNG_RADWEGENDE_ANFANG_GESCHUETZT',
              displayText: 'Radweganfang bzw. -ende anpassen',
            },
            {
              name: 'ANPASSUNG_EINMUENDUNG_ZUFAHRT',
              displayText: 'Einmündungs-/Zufahrtsbereich anpassen',
            },
            {
              name: 'ERSATZBAUWERK_ERRICHTEN',
              displayText: 'Ersatzbauwerk errichten',
            },
            {
              name: 'ANPASSUNG_AN_BAUWERK_GERINGFUEGIG',
              displayText: 'Bauwerk geringfügig anpassen',
            },
            {
              name: 'AENDERUNG_DER_VORFAHRT_AM_KNOTENPUNKT',
              displayText: 'Vorfahrtsregelung ändern',
            },
            {
              name: 'AENDERUNG_DER_VERKEHRSRECHTLICHEN_ANORDNUNG',
              displayText: 'Änderung der verkehrsrechtlichen Anordnung',
            },
            {
              name: 'ANPASSUNG_AN_BESTEHENDEN_KREISVERKEHR',
              displayText: 'Anpassung an bestehenden Kreisverkehr',
            },
            {
              name: 'ANPASSUNG_AN_BESTEHENDER_QUERUNGSHILFE',
              displayText: 'Anpassung an bestehender Querungshilfe',
            },
            {
              name: 'ANPASSUNG_EINER_LSA',
              displayText: 'Anpassung einer LSA',
            },
            {
              name: 'ANPASSUNG_EINER_FAHRBAHNEINENGUNG',
              displayText: 'Anpassung einer Fahrbahneinengung',
            },
            {
              name: 'ANPASSUNG_AN_BAUWERK',
              displayText: 'Anpassung an Bauwerk',
            },
          ],
        },
        {
          name: 'NEUBAU_KNOTEN',
          displayText: 'Neubau',
          gewichtung: 1,
          options: [
            {
              name: 'FURT_PUNKTUELL_HERSTELLEN',
              displayText: 'Furt punktuell herstellen',
            },
            {
              name: 'BAU_RADWEGENDE_GESCHUETZT',
              displayText: 'Übergang Fahrbahn-Radwegende baulich sichern',
            },
            {
              name: 'BAU_RADWEGANFANG_GESCHUETZT',
              displayText: 'Übergang Fahrbahn-Radweganfang baulich sichern',
            },
            {
              name: 'BAU_EINER_FAHRBAHNEINENGUNG',
              displayText: 'Bau einer Fahrbahneinengung',
            },
            {
              name: 'BAU_EINER_FAHRBAHNEINENGUNG_UEBERGANG_ZWEIRICHTUNGSRADWEG',
              displayText: 'Übergang Fahrbahn-Radweg sichern: Einengung',
            },
            {
              name: 'BAU_EINER_NEUEN_LSA',
              displayText: 'Überquerungsstelle sichern: LSA',
            },
            {
              name: 'BAU_EINER_QUERUNGSHILFE',
              displayText: 'Überquerungsstelle sichern: Querungshilfe',
            },
            {
              name: 'BAU_EINER_QUERUNGSHILFE_UEBERGANG_ZWEIRICHTUNGSRADWEG',
              displayText: 'Übergang Fahrbahn-Radweg sichern: Querungshilfe',
            },
            {
              name: 'BAU_EINER_UEBERFUEHRUNG',
              displayText: 'Bau einer Überführung',
            },
            {
              name: 'BAU_EINER_UNTERFUEHRUNG',
              displayText: 'Bau einer Unterführung',
            },
            {
              name: 'BAU_KOMPAKT_TURBO_KREISVERKEHR',
              displayText: 'Bau eines Kompakt-Kreisverkehrs / Turbo-Kreisverkehr',
            },
            {
              name: 'BAU_MINIKREISVERKEHRS',
              displayText: 'Bau eines Minikreisverkehrs',
            },
          ],
        },
        {
          name: 'MARKIERUNGSTECHNISCHE_MASSNAHME',
          displayText: 'Markierungstechnische Maßnahme',
          gewichtung: 3,
          hidden: true,
          options: [{ name: 'MARKIERUNGSTECHNISCHE_MASSNAHME', displayText: 'Markierungstechnische Maßnahme' }],
        },
        {
          name: 'SONSTIGE_MASSNAHME_KNOTENPUNKT',
          displayText: 'Sonstige Maßnahme am Knotenpunkt',
          gewichtung: 9,
          hidden: true,
          options: [{ name: 'SONSTIGE_MASSNAHME_KNOTENPUNKT', displayText: 'Sonstige Maßnahme am Knotenpunkt' }],
        },
      ],
    },
    {
      name: 'BARRIERENMASSNAHMENKATEGORIEN',
      displayText: 'Barrierenmaßnahmenkategorien',
      options: [
        {
          name: 'BARRIERENMASSNAHMENKATEGORIEN',
          displayText: 'Barrierenmaßnahmenkategorien',
          gewichtung: 4,
          hidden: true,
          options: [
            {
              name: 'AENDERUNG_DER_VERKEHRSRECHTLICHEN_ANORDUNG',
              displayText: 'Änderung der verkehrsrechtlichen Anordung',
            },
            {
              name: 'BARRIERE_SICHERN_BZW_PRUEFUNG_AUF_VERZICHT',
              displayText: 'Barriere sichern bzw. Prüfung auf Verzicht der Barriere',
            },
            { name: 'SONSTIGE_MASSNAHME_AN_BARRIERE', displayText: 'Sonstige Maßnahme an Barriere' },
            { name: 'ABBAU_BZW_ERSATZ_BARRIERE', displayText: 'Abbau bzw. Ersatz Barriere' },
          ],
        },
      ],
    },
  ];

  public static kategorienNotAllowedForRadnetz2024: string[] = [
    'NEUBAU_WEG_NACH_RADNETZ_QUALITAETSSTANDARD',
    'NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_250CM',
    'NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_160CM',
    'FURT_STVO_KONFORM',
    'BENUTZUNGSPFLICHT_RADVERKEHR_AUFHEBEN_RADFAHRER_FREI_REDUZIERUNG_HOECHSTGESCHWINDIGKEIT_RADFAHRER_FREI',
    'ANFANG_UND_ENDE_RADWEG_SICHERN',
    'EIN_ENDE_DES_RADWEGES_SICHERN',
    'QUERUNGSMOEGLICHKEIT_HERSTELLEN',
    'SONSTIGE_MASSNAHME_AN_RADWEGANFANG_ENDE',
    'RADWEGABSENKUNGEN_AN_ZUFAHRTEN_AUFHEBEN_INNERORTS',
    'RADWEGABSENKUNGEN_AN_ZUFAHRTEN_AUFHEBEN_AUSSERORTS',
    'ABBAU_BZW_ERSATZ_BARRIERE',
    'AENDERUNG_DER_VERKEHRSRECHTLICHEN_ANORDNUNG',
  ];

  public static RADNETZ_2024_KATEGORIEN_ONLY = Massnahmenkategorien.ALL.map(optGroup => {
    return {
      ...optGroup,
      options: optGroup.options.map(subGroup => {
        return {
          ...subGroup,
          options: subGroup.options.filter(option => !this.kategorienNotAllowedForRadnetz2024.includes(option.name)),
        };
      }),
    };
  });

  public static isValidMassnahmenKategorienCombination = (ctrl: AbstractControl): ValidationErrors | null => {
    const value: string[] = ctrl.value;
    if (value === null) {
      return null;
    }
    const oberKategorien = value.map(kat => Massnahmenkategorien.getMassnahmeOberkategorie(kat));
    if (oberKategorien.length === new Set(oberKategorien).size) {
      return null;
    }
    return { isValidMassnahmeKategorien: 'Es darf nur eine Kategorie pro Oberkategorie gewählt werden' };
  };

  public static getDisplayTextForMassnahmenKategorie = (key: string): string => {
    const result = '';
    if (key) {
      for (const firstLevelGroup of Massnahmenkategorien.ALL) {
        for (const secondLevelGroup of firstLevelGroup.options) {
          const kategorie = secondLevelGroup.options.find(opt => opt.name === key);
          if (kategorie) {
            return kategorie.displayText;
          }
        }
      }
    }
    return result;
  };

  public static getDisplayTextForKategorieArtVonKategorie = (kategorie: string): string => {
    for (const firstLevelGroup of Massnahmenkategorien.ALL) {
      for (const secondLevelGroup of firstLevelGroup.options) {
        const value = secondLevelGroup.options.find(opt => opt.name === kategorie);
        if (value) {
          return firstLevelGroup.displayText;
        }
      }
    }
    return '';
  };

  public static getMassnahmeOberkategorieGruppe = (kategorie: string): MassnahmenkategorieGruppe => {
    for (const firstLevelGroup of Massnahmenkategorien.ALL) {
      for (const secondLevelGroup of firstLevelGroup.options) {
        const value = secondLevelGroup.options.find(opt => opt.name === kategorie);
        if (value) {
          return secondLevelGroup;
        }
      }
    }

    throw new Error(`Massnahmenkategorie ${kategorie} fehlt`);
  };

  public static validateKonzeptionsquelle = (
    currentMassnahmenkategorien: string[] | null,
    currentKonzeptionsquelle: Konzeptionsquelle | null
  ): null | ValidationErrors => {
    if (!currentKonzeptionsquelle || currentKonzeptionsquelle !== Konzeptionsquelle.RADNETZ_MASSNAHME_2024) {
      return null;
    }

    if (!currentMassnahmenkategorien) {
      return null;
    }

    const unerlaubteKategorien = currentMassnahmenkategorien?.filter(kat =>
      Massnahmenkategorien.kategorienNotAllowedForRadnetz2024.includes(kat)
    );

    if (unerlaubteKategorien?.length === 0) {
      return null;
    }

    return {
      invalidKategorieForKonzeptionsquelle:
        'Folgende Kategorien sind für die gewählte Konzeptionsquelle nicht erlaubt: ' +
        unerlaubteKategorien?.map(kat => Massnahmenkategorien.getDisplayTextForMassnahmenKategorie(kat)).join(', '),
    };
  };

  public static getDisplayTextForOberkategorieVonKategorie = (kategorie: string): string =>
    Massnahmenkategorien.getMassnahmeOberkategorieGruppe(kategorie).displayText;

  public static getMassnahmeOberkategorie = (kategorie: string): string =>
    Massnahmenkategorien.getMassnahmeOberkategorieGruppe(kategorie).name;

  public static getOberkategorieGewichtet = (massnahmenkategorien: string[]): MassnahmenkategorieGruppe => {
    invariant(massnahmenkategorien.length > 0);

    // Wenn die Massnahme nur eine Kategorie hat, muss die Gewichtung nicht beruecksichtigt werden.
    if (massnahmenkategorien.length === 1) {
      return Massnahmenkategorien.getMassnahmeOberkategorieGruppe(massnahmenkategorien[0]);
    }

    return massnahmenkategorien
      .map(Massnahmenkategorien.getMassnahmeOberkategorieGruppe)
      .sort((a: MassnahmenkategorieGruppe, b: MassnahmenkategorieGruppe) => a.gewichtung - b.gewichtung)[0];
  };
}
