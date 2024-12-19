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

import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { FahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view';
import { FahrradrouteTyp } from 'src/app/viewer/viewer-shared/models/fahrradroute-typ';

export const testFahrradrouteListenView: FahrradrouteListenView[] = [
  {
    id: 1,
    fahrradrouteTyp: FahrradrouteTyp.RADVIS_ROUTE,
    name: 'testFahrradroute',
    verantwortlicheOrganisation: 'verantwortlich1',
    fahrradrouteKategorie: FahrradrouteKategorie.RADSCHNELLWEG,
    anstiegAbstieg: '100,2 km / 200,1 km',
  },
  {
    id: 2,
    fahrradrouteTyp: FahrradrouteTyp.TOUBIZ_ROUTE,
    name: 'andereTestFahrradroute',
    verantwortlicheOrganisation: 'verantwortlich2',
    fahrradrouteKategorie: FahrradrouteKategorie.LANDESRADFERNWEG,
    anstiegAbstieg: '100,2 km / 200,1 km',
  },
  {
    id: 3,
    fahrradrouteTyp: FahrradrouteTyp.TFIS_ROUTE,
    name: 'ganzAndererNameEinerFahrradroute',
    verantwortlicheOrganisation: 'verantwortlich3',
    fahrradrouteKategorie: FahrradrouteKategorie.D_ROUTE,
    anstiegAbstieg: '100,2 km / 200,1 km',
  },
];
