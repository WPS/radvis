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

import { Beleuchtung } from 'src/app/editor/kanten/models/beleuchtung';
import { Benutzungspflicht } from 'src/app/editor/kanten/models/benutzungspflicht';
import { Bordstein } from 'src/app/editor/kanten/models/bordstein';
import { FuehrungsformAttribute } from 'src/app/editor/kanten/models/fuehrungsform-attribute';
import { GeschwindigkeitAttribute } from 'src/app/editor/kanten/models/geschwindigkeit-attribute';
import { Hoechstgeschwindigkeit } from 'src/app/editor/kanten/models/hoechstgeschwindigkeit';
import { Kante } from 'src/app/editor/kanten/models/kante';
import { KantenOrtslage } from 'src/app/editor/kanten/models/kanten-ortslage';
import { KfzParkenForm } from 'src/app/editor/kanten/models/kfz-parken-form';
import { KfzParkenTyp } from 'src/app/editor/kanten/models/kfz-parken-typ';
import { Oberflaechenbeschaffenheit } from 'src/app/editor/kanten/models/oberflaechenbeschaffenheit';
import { Richtung } from 'src/app/editor/kanten/models/richtung';
import { Status } from 'src/app/editor/kanten/models/status';
import { StrassenquerschnittRASt06 } from 'src/app/editor/kanten/models/strassenquerschnittrast06';
import { Umfeld } from 'src/app/editor/kanten/models/umfeld';
import { WegeNiveau } from 'src/app/editor/kanten/models/wege-niveau';
import { StrassenkategorieRIN } from 'src/app/editor/kanten/models/strassenkategorie-rin';
import { ZustaendigkeitAttribute } from 'src/app/editor/kanten/models/zustaendigkeit-attribute';
import { BelagArt } from 'src/app/shared/models/belag-art';
import { IstStandard } from 'src/app/shared/models/ist-standard';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import {
  defaultOrganisation,
  defaultUebergeordneteOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { Radverkehrsfuehrung } from 'src/app/shared/models/radverkehrsfuehrung';

export const defaultFuehrungsformAttribute = {
  belagArt: BelagArt.ASPHALT,
  oberflaechenbeschaffenheit: Oberflaechenbeschaffenheit.GUTER_BIS_MITTLERER_ZUSTAND,
  bordstein: Bordstein.ABSENKUNG_KLEINER_3_ZENTIMETER,
  radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG,
  benutzungspflicht: Benutzungspflicht.UNBEKANNT,
  breite: null,
  parkenTyp: KfzParkenTyp.SCHRAEG_PARKEN,
  parkenForm: KfzParkenForm.GEHWEGPARKEN_UNMARKIERT,
  trennstreifenFormLinks: null,
  trennstreifenTrennungZuLinks: null,
  trennstreifenBreiteLinks: null,
  trennstreifenFormRechts: null,
  trennstreifenTrennungZuRechts: null,
  trennstreifenBreiteRechts: null,

  linearReferenzierterAbschnitt: {
    von: 0,
    bis: 1,
  },
} as FuehrungsformAttribute;

export const defaultZustaendigkeitAttribute = {
  vereinbarungsKennung: 'vereinbarungsKennung',
  baulastTraeger: defaultOrganisation,
  unterhaltsZustaendiger: defaultOrganisation,
  erhaltsZustaendiger: defaultOrganisation,
  linearReferenzierterAbschnitt: {
    von: 0,
    bis: 1,
  },
} as ZustaendigkeitAttribute;

export const defaultGeschwindigkeitAttribute = {
  ortslage: KantenOrtslage.INNERORTS,
  hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_30_KMH,
  abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung: Hoechstgeschwindigkeit.UNBEKANNT,
  linearReferenzierterAbschnitt: {
    von: 0,
    bis: 1,
  },
} as GeschwindigkeitAttribute;

export const defaultKante: Kante = {
  id: 1,
  geometry: {
    coordinates: [[0, 1]],
    type: 'LineString',
  },
  verlaufLinks: { coordinates: [[0, 1]], type: 'LineString' },
  verlaufRechts: { coordinates: [[0, 1]], type: 'LineString' },
  laengeBerechnet: 100.23475,
  zweiseitig: true,
  geometrieAenderungErlaubt: true,
  liegtInZustaendigkeitsbereich: true,
  loeschenErlaubt: true,
  kantenAttributGruppe: {
    id: 1,
    version: 0,
    // richtung: Richtung.BEIDE_RICHTUNGEN,
    wegeNiveau: WegeNiveau.FAHRBAHN,
    beleuchtung: Beleuchtung.VORHANDEN,
    strassenquerschnittRASt06: StrassenquerschnittRASt06.ANBAUFREIE_STRASSE,
    umfeld: Umfeld.GESCHAEFTSSTRASSE,
    strassenkategorieRIN: StrassenkategorieRIN.KLEINRAEUMIG,
    laengeBerechnet: 100.23475,
    laengeManuellErfasst: 100,
    dtvFussverkehr: 1,
    dtvRadverkehr: 2,
    dtvPkw: 2,
    sv: 2,
    kommentar: 'kommentar',
    strassenName: 'ABC-Straße',
    strassenNummer: '1a',
    status: Status.UNTER_VERKEHR,
    gemeinde: defaultOrganisation,
    landkreis: defaultOrganisation,
    netzklassen: [Netzklasse.KREISNETZ_ALLTAG],
    istStandards: [IstStandard.BASISSTANDARD],
  },
  geschwindigkeitAttributGruppe: {
    id: 1,
    version: 0,
    geschwindigkeitAttribute: [
      {
        ortslage: KantenOrtslage.AUSSERORTS,
        hoechstgeschwindigkeit: Hoechstgeschwindigkeit.KFZ_NICHT_ZUGELASSEN,
        abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung: Hoechstgeschwindigkeit.MAX_100_KMH,
        linearReferenzierterAbschnitt: {
          von: 0,
          bis: 1,
        },
      } as GeschwindigkeitAttribute,
    ],
  },
  fuehrungsformAttributGruppe: {
    id: 567,
    version: 1,
    fuehrungsformAttributeLinks: [defaultFuehrungsformAttribute],
    fuehrungsformAttributeRechts: [defaultFuehrungsformAttribute],
  },
  zustaendigkeitAttributGruppe: {
    id: 567,
    version: 1,
    zustaendigkeitAttribute: [defaultZustaendigkeitAttribute],
  },
  fahrtrichtungAttributGruppe: {
    id: 627,
    version: 1,
    fahrtrichtungLinks: Richtung.BEIDE_RICHTUNGEN,
    fahrtrichtungRechts: Richtung.GEGEN_RICHTUNG,
  },
  kantenVersion: 1,
  quelle: QuellSystem.DLM,
};

export const anotherKante: Kante = {
  id: 2,
  geometry: {
    coordinates: [[2, 3]],
    type: 'LineString',
  },
  verlaufLinks: null,
  verlaufRechts: null,
  laengeBerechnet: 50.33,
  zweiseitig: true,
  geometrieAenderungErlaubt: true,
  liegtInZustaendigkeitsbereich: true,
  loeschenErlaubt: true,
  kantenAttributGruppe: {
    id: 2,
    version: 0,
    // richtung: Richtung.GEGEN_RICHTUNG,
    wegeNiveau: WegeNiveau.GEHWEG,
    beleuchtung: Beleuchtung.NICHT_VORHANDEN,
    strassenquerschnittRASt06: StrassenquerschnittRASt06.DOERFLICHE_HAUPTSTRASSE,
    umfeld: Umfeld.GEWERBEGEBIET,
    strassenkategorieRIN: StrassenkategorieRIN.GROSSRAEUMIG,
    laengeBerechnet: 50.33,
    laengeManuellErfasst: 90,
    dtvFussverkehr: 5,
    dtvRadverkehr: 6,
    dtvPkw: 7,
    sv: 8,
    kommentar: 'anderer_kommentar',
    strassenName: 'DEF-Straße',
    strassenNummer: '2b',
    status: Status.UNTER_VERKEHR,
    gemeinde: defaultUebergeordneteOrganisation,
    landkreis: defaultUebergeordneteOrganisation,
    netzklassen: [Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ALLTAG],
    istStandards: [],
  },
  geschwindigkeitAttributGruppe: {
    id: 2,
    version: 0,
    geschwindigkeitAttribute: [
      {
        ortslage: KantenOrtslage.INNERORTS,
        hoechstgeschwindigkeit: Hoechstgeschwindigkeit.MAX_9_KMH,
        abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung: Hoechstgeschwindigkeit.MAX_60_KMH,
        linearReferenzierterAbschnitt: {
          von: 0,
          bis: 1,
        },
      } as GeschwindigkeitAttribute,
    ],
  },
  fuehrungsformAttributGruppe: {
    id: 789,
    version: 1,
    fuehrungsformAttributeLinks: [
      {
        belagArt: BelagArt.BETON,
        oberflaechenbeschaffenheit: Oberflaechenbeschaffenheit.GUTER_BIS_MITTLERER_ZUSTAND,
        bordstein: Bordstein.KOMPLETT_ABGESENKT,
        radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG,
        benutzungspflicht: Benutzungspflicht.VORHANDEN,
        breite: null,
        parkenTyp: KfzParkenTyp.PARKEN_VERBOTEN,
        parkenForm: KfzParkenForm.PARKBUCHTEN,

        linearReferenzierterAbschnitt: {
          von: 0,
          bis: 1,
        },
      } as FuehrungsformAttribute,
    ],
    fuehrungsformAttributeRechts: [
      {
        belagArt: BelagArt.BETON,
        oberflaechenbeschaffenheit: Oberflaechenbeschaffenheit.GUTER_BIS_MITTLERER_ZUSTAND,
        bordstein: Bordstein.KOMPLETT_ABGESENKT,
        radverkehrsfuehrung: Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG,
        benutzungspflicht: Benutzungspflicht.VORHANDEN,
        breite: null,
        parkenTyp: KfzParkenTyp.PARKEN_VERBOTEN,
        parkenForm: KfzParkenForm.PARKBUCHTEN,

        linearReferenzierterAbschnitt: {
          von: 0,
          bis: 1,
        },
      } as FuehrungsformAttribute,
    ],
  },
  zustaendigkeitAttributGruppe: {
    id: 789,
    version: 1,
    zustaendigkeitAttribute: [defaultZustaendigkeitAttribute],
  },
  fahrtrichtungAttributGruppe: {
    id: 628,
    version: 1,
    fahrtrichtungLinks: Richtung.IN_RICHTUNG,
    fahrtrichtungRechts: Richtung.GEGEN_RICHTUNG,
  },
  kantenVersion: 1,
  quelle: QuellSystem.DLM,
};
