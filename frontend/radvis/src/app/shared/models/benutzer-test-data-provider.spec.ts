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

import { BenutzerListView } from 'src/app/administration/models/benutzer-list-view';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { Rolle } from 'src/app/administration/models/rolle';

export const defaultBenutzer: BenutzerListView = {
  email: 'test@test.de',
  id: 1346,
  nachname: 'Nachname',
  organisation: defaultOrganisation.name,
  status: BenutzerStatus.AKTIV,
  vorname: 'Vorname',
  rollen: [Rolle.RADWEGE_ERFASSERIN, Rolle.RADROUTEN_BEARBEITERIN],
};
