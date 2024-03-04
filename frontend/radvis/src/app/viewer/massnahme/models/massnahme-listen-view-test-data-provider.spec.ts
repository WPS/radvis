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
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { MassnahmeListenView } from 'src/app/viewer/massnahme/models/massnahme-listen-view';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';

export const defaultMassnahmeListView: MassnahmeListenView = {
  id: 1,
  bezeichnung: 'testMassnahme',
  massnahmeKonzeptId: 'konzeptId',
  massnahmenkategorien: ['UMWIDMUNG_GEMEINSAMER_RADGEHWEG'],
  umsetzungsstatus: Umsetzungsstatus.IDEE,
  veroeffentlicht: false,
  planungErforderlich: false,
  durchfuehrungszeitraum: { geplanterUmsetzungsstartJahr: 2050 },
  baulastZustaendiger: {
    id: 20,
    name: 'testOrganisation',
    organisationsArt: OrganisationsArt.BUNDESLAND,
    idUebergeordneteOrganisation: null,
    aktiv: true,
  },
  prioritaet: 5000,
  netzklassen: new Set<Netzklasse>([Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT]),
  letzteAenderung: '2022-01-10T15:28:09.473406',
  benutzerLetzteAenderung: {
    nachname: 'Mustermensch',
    vorname: 'M.',
  },
  zustaendiger: null,
  unterhaltsZustaendiger: null,
  geometry: {
    coordinates: [],
    type: 'LineString',
  },
  sollStandard: SollStandard.BASISSTANDARD,
  handlungsverantwortlicher: Handlungsverantwortlicher.BAULASTTRAEGER,
};

export const getTestMassnahmeListenViews = (): MassnahmeListenView[] => {
  return [
    defaultMassnahmeListView,
    {
      id: 2,
      bezeichnung: 'nochEineMassnahme',
      massnahmeKonzeptId: 'zweite_konzeptId',
      massnahmenkategorien: ['STRECKE_FUER_KFZVERKEHR_SPERREN'],
      umsetzungsstatus: Umsetzungsstatus.UMSETZUNG,
      veroeffentlicht: true,
      planungErforderlich: true,
      durchfuehrungszeitraum: { geplanterUmsetzungsstartJahr: 2020 },
      baulastZustaendiger: {
        id: 30,
        name: 'testOrganisation2',
        organisationsArt: OrganisationsArt.BUNDESLAND,
        idUebergeordneteOrganisation: null,
        aktiv: true,
      },
      prioritaet: 4000,
      netzklassen: new Set<Netzklasse>([Netzklasse.KREISNETZ_FREIZEIT]),
      letzteAenderung: '2022-01-10T15:28:09.473406',
      benutzerLetzteAenderung: {
        nachname: 'Musteralien',
        vorname: 'A.',
      },
      zustaendiger: null,
      unterhaltsZustaendiger: null,
      geometry: {
        coordinates: [],
        type: 'LineString',
      },
      sollStandard: SollStandard.KEIN_STANDARD_ERFUELLT,
      handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
    },
    {
      id: 3,
      bezeichnung: 'eine dritte Massnahme',
      massnahmeKonzeptId: 'dritte_konzeptId',
      massnahmenkategorien: ['STRECKE_FUER_KFZVERKEHR_SPERREN'],
      umsetzungsstatus: Umsetzungsstatus.UMSETZUNG,
      veroeffentlicht: true,
      planungErforderlich: true,
      durchfuehrungszeitraum: { geplanterUmsetzungsstartJahr: 2022 },
      baulastZustaendiger: {
        id: 40,
        name: 'testOrganisation4',
        organisationsArt: OrganisationsArt.BUNDESLAND,
        idUebergeordneteOrganisation: null,
        aktiv: true,
      },
      prioritaet: 4000,
      netzklassen: new Set<Netzklasse>([Netzklasse.KREISNETZ_FREIZEIT]),
      letzteAenderung: '2022-01-10T15:28:09.473406',
      benutzerLetzteAenderung: {
        nachname: 'Musteralien',
        vorname: 'A.',
      },
      zustaendiger: null,
      unterhaltsZustaendiger: null,
      geometry: {
        coordinates: [],
        type: 'LineString',
      },
      sollStandard: SollStandard.KEIN_STANDARD_ERFUELLT,
      handlungsverantwortlicher: Handlungsverantwortlicher.VERKEHRSBEHOERDE_TECHNIK,
    },
  ];
};
