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
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { FahrradrouteDetailView } from 'src/app/viewer/fahrradroute/models/fahrradroute-detail-view';
import { Tourenkategorie } from 'src/app/viewer/fahrradroute/models/tourenkategorie';
import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { FahrradrouteTyp } from 'src/app/viewer/viewer-shared/models/fahrradroute-typ';

export const defaultFahrradroute: FahrradrouteDetailView = {
  id: 1,
  version: 2,
  toubizId: '3',
  fahrradrouteTyp: FahrradrouteTyp.RADVIS_ROUTE,
  canEditAttribute: true,
  canChangeVeroeffentlicht: true,
  name: 'Die SCHÖNSTE Route der Welt!!!',
  kurzbeschreibung: 'Hier könnte Ihre Werbung stehen',
  beschreibung: 'Nein wirklich, hier könnte wirklich Ihre Werbung stehen, das ist kein Schwerz!',
  fahrradrouteKategorie: FahrradrouteKategorie.UEBERREGIONALER_RADWANDERWEG,
  tourenkategorie: Tourenkategorie.GRAVEL_TOUR,
  laengeHauptstrecke: 42.24,
  offizielleLaenge: 42.24,
  homepage: 'https://schoenste-radroute-auf-die-welt.de',
  verantwortlich: {
    id: 213,
    name: 'OR-G ani.sa t.IO-n',
    idUebergeordneteOrganisation: null,
    organisationsArt: OrganisationsArt.GEMEINDE,
  } as Verwaltungseinheit,
  emailAnsprechpartner: 'werbung-schalten@schoenste-radroute-auf-die-welt.de',
  lizenz: 'WTFPL',
  lizenzNamensnennung: 'Hier könnte Ihr Name stehen',
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
      linearReferenzierterAbschnitt: { von: 0, bis: 1 },
    },
  ],
  originalGeometrie: undefined,
  routedOrMatchedGeometry: undefined,
  kehrtwenden: undefined,
  abweichendeSegmente: undefined,
  abbildungDurchRouting: false,
  abstieg: 12,
  anstieg: 2.34,
  info: '<p>Zusätzliche Infos</p><br><span>exklusiv</span>',
  zuletztBearbeitet: new Date('2022-01-10T15:28:09.473406'),
  varianten: [],
  profilEigenschaften: [],
  veroeffentlicht: false,
  customProfileId: 123,
};
