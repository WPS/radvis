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

import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { Organisation } from 'src/app/shared/models/organisation-edit-view';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';

export const defaultOrganisation: Verwaltungseinheit = {
  id: 1,
  name: 'Eine Organisation',
  organisationsArt: OrganisationsArt.GEMEINDE,
  idUebergeordneteOrganisation: 2,
  aktiv: true,
};

export const defaultUebergeordneteOrganisation: Verwaltungseinheit = {
  id: 2,
  name: 'Eine übergeordnete Organisation',
  organisationsArt: OrganisationsArt.KREIS,
  idUebergeordneteOrganisation: 3,
  aktiv: true,
};

export const defaultBundeslandOrganisation: Verwaltungseinheit = {
  id: 3,
  name: 'Baden-Württemberg',
  organisationsArt: OrganisationsArt.BUNDESLAND,
  idUebergeordneteOrganisation: null,
  aktiv: true,
};

export const defaultGemeinden: Verwaltungseinheit[] = [
  {
    id: 1,
    name: 'Stuttgart',
    organisationsArt: OrganisationsArt.GEMEINDE,
    idUebergeordneteOrganisation: 5,
    aktiv: true,
  },
  {
    id: 2,
    name: 'Bondorf',
    organisationsArt: OrganisationsArt.GEMEINDE,
    idUebergeordneteOrganisation: 6,
    aktiv: true,
  },
  {
    id: 3,
    name: 'Aidlingen',
    organisationsArt: OrganisationsArt.GEMEINDE,
    idUebergeordneteOrganisation: 7,
    aktiv: true,
  },
];

export const defaultEditOrganisation: Organisation = {
  id: 2,
  name: 'Die Organisation, die wir editieren wollen',
  uebergeordneteOrganisation: defaultOrganisation,
  organisationsArt: OrganisationsArt.TOURISMUSVERBAND,
  aktuellerBenutzerDarfBearbeiten: true,
  zustaendigFuerBereichOf: [{ ...defaultOrganisation, id: 2345 }],
  aktiv: defaultOrganisation.aktiv,
  version: 1,
};
