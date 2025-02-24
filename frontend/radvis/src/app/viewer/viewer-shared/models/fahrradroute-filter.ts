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
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';
import { FahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view';

export interface FahrradrouteFilter {
  fahrradrouteFilterKategorie: FahrradrouteFilterKategorie | null;
  fahrradroute: FahrradrouteListenView | null;
  fahrradroutenIds: number[];
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace FahrradrouteFilter {
  export const equal = (filter: FahrradrouteFilter | null, other: FahrradrouteFilter | null): boolean => {
    if (filter && other) {
      return (
        filter.fahrradrouteFilterKategorie === other.fahrradrouteFilterKategorie &&
        filter.fahrradroute?.id === other.fahrradroute?.id
      );
    }

    return filter == other;
  };
}
