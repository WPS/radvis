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

export interface Verwaltungseinheit {
  id: number;
  name: string;
  organisationsArt: OrganisationsArt;
  idUebergeordneteOrganisation: number | null;
  aktiv: boolean;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace Verwaltungseinheit {
  export const isLandesOderBundesweit = (organisation: Verwaltungseinheit): boolean => {
    return (
      organisation.organisationsArt === OrganisationsArt.BUNDESLAND ||
      organisation.organisationsArt === OrganisationsArt.STAAT
    );
  };

  export const getSortingValueForKey = (organisation: Verwaltungseinheit, key: string): string => {
    if (key === 'status') {
      return organisation.aktiv ? 'aktiv' : 'inaktiv';
    } else {
      return organisation[key as keyof Verwaltungseinheit] as string;
    }
  };

  export const getDisplayName = (organisation: Verwaltungseinheit | null | undefined): string => {
    if (!organisation?.name || !organisation.organisationsArt) {
      return '';
    }

    return `${organisation.name} (${OrganisationsArt.getDisplayName(organisation.organisationsArt)}${
      organisation.aktiv ? '' : ', inaktiv'
    })`;
  };
}
