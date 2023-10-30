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

import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { GrundFuerAbweichungZumMassnahmenblatt } from 'src/app/viewer/massnahme/models/grund-fuer-abweichung-zum-massnahmenblatt';
import { GrundFuerNichtUmsetzungDerMassnahme } from 'src/app/viewer/massnahme/models/grund-fuer-nicht-umsetzung-der-massnahme';
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { Massnahme } from 'src/app/viewer/massnahme/models/massnahme';
import { PruefungQualitaetsstandardsErfolgt } from 'src/app/viewer/massnahme/models/pruefung-qualitaetsstandards-erfolgt';
import { Realisierungshilfe } from 'src/app/viewer/massnahme/models/realisierungshilfe';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { Umsetzungsstand } from 'src/app/viewer/massnahme/models/umsetzungsstand';
import { UmsetzungsstandStatus } from 'src/app/viewer/massnahme/models/umsetzungsstand-status';
import { KantenSeitenbezug } from 'src/app/viewer/viewer-shared/models/netzbezug';
import { defaultNetzbezug } from 'src/app/viewer/viewer-shared/models/netzbezug-test-data-provider.spec';

const eineTestOrga = {
  id: 5,
  name: 'Orgablaaaa',
  organisationsArt: OrganisationsArt.GEMEINDE,
  idUebergeordneteOrganisation: null,
} as Verwaltungseinheit;

export const defaultUmsetzungsstand: Umsetzungsstand = {
  id: 1,
  version: 0,
  massnahmeUmsetzungsstatus: Umsetzungsstatus.IDEE,
  umsetzungsstandStatus: UmsetzungsstandStatus.AKTUALISIERT,
  letzteAenderung: new Date(),
  umsetzungGemaessMassnahmenblatt: false,
  grundFuerAbweichungZumMassnahmenblatt:
    GrundFuerAbweichungZumMassnahmenblatt.UMSETZUNG_ALTERNATIVER_MASSNAHME_ERFORDERLICH,
  pruefungQualitaetsstandardsErfolgt: PruefungQualitaetsstandardsErfolgt.NEIN_ERFOLGT_NOCH,
  beschreibungAbweichenderMassnahme: 'Freitext Beschreibung abweichender Massnahme',
  kostenDerMassnahme: 10000,
  grundFuerNichtUmsetzungDerMassnahme: GrundFuerNichtUmsetzungDerMassnahme.LAUT_VERKEHRSSCHAU_NICHT_ERFORDERLICH,
  anmerkung: 'Freitext Anmerkung',
  canEdit: true,
};

export const defaultMassnahme: Massnahme = {
  id: 1,
  version: 0,
  bezeichnung: 'Bezeichnung',
  massnahmenkategorien: ['STRECKE_FUER_KFZVERKEHR_SPERREN'],
  netzbezug: {
    kantenBezug: [
      {
        kanteId: 7,
        geometrie: {
          type: 'LineString',
          coordinates: [
            [4, 4],
            [5, 5],
          ],
        },
        seitenbezug: KantenSeitenbezug.BEIDSEITIG,
        linearReferenzierterAbschnitt: { von: 0, bis: 1 },
      },
    ],
    knotenBezug: [],
    punktuellerKantenBezug: defaultNetzbezug.punktuellerKantenBezug,
  },
  umsetzungsstatus: Umsetzungsstatus.IDEE,
  veroeffentlicht: false,
  planungErforderlich: true,
  durchfuehrungszeitraum: { geplanterUmsetzungsstartJahr: 2042 },
  baulastZustaendiger: eineTestOrga,
  prioritaet: 5,
  kostenannahme: 500,
  netzklassen: [Netzklasse.RADNETZ_ALLTAG],
  letzteAenderung: new Date(2022, 1, 1),
  benutzerLetzteAenderung: {
    vorname: 'Olaf',
    nachname: 'Müller',
  },
  markierungsZustaendiger: eineTestOrga,
  unterhaltsZustaendiger: eineTestOrga,
  maViSID: 'eineMaViSID',
  verbaID: 'eineVerbaID',
  lgvfgid: 'eineLgvfgid',
  geometry: {
    coordinates: [
      [0, 1],
      [0, 5],
    ],
    type: 'LineString',
  },
  massnahmeKonzeptID: 'konzeptId',
  sollStandard: SollStandard.BASISSTANDARD,
  handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
  konzeptionsquelle: Konzeptionsquelle.SONSTIGE,
  sonstigeKonzeptionsquelle: 'konzeptionsQuelle',
  canEdit: true,
  realisierungshilfe: Realisierungshilfe.NR_2_2_1,
};
