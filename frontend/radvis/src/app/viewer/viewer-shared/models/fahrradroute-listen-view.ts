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
import { FahrradrouteTyp } from 'src/app/viewer/viewer-shared/models/fahrradroute-typ';

export interface FahrradrouteListenView {
  id: number;
  fahrradrouteTyp: FahrradrouteTyp;
  name: string;
  fahrradrouteKategorie: FahrradrouteKategorie;
  verantwortlicheOrganisation: string;

  anstiegAbstieg: string;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace FahrradrouteListenView {
  const EMPTY_FIELD_INDICATOR = '';

  export const getDisplayValueForKey = (item: FahrradrouteListenView, key: string): string | string[] => {
    switch (key) {
      case 'name':
        return item.name ?? EMPTY_FIELD_INDICATOR;
      case 'verantwortlicheOrganisation':
        return item.verantwortlicheOrganisation ?? EMPTY_FIELD_INDICATOR;
      case 'fahrradrouteKategorie':
        return (
          FahrradrouteKategorie.options.find(option => option.name === item.fahrradrouteKategorie)?.displayText ??
          EMPTY_FIELD_INDICATOR
        );
      case 'fahrradrouteTyp':
        return (
          FahrradrouteTyp.options.find(option => option.name === item.fahrradrouteTyp)?.displayText ??
          EMPTY_FIELD_INDICATOR
        );
      case 'anstiegAbstieg':
        return item.anstiegAbstieg ?? EMPTY_FIELD_INDICATOR;
      default:
        throw Error(`Key ${key} nicht gefunden`);
    }
  };
  export const getDisplayName = (fahrradrouteListenView: FahrradrouteListenView | null | undefined): string => {
    if (!fahrradrouteListenView?.name) {
      return '';
    }

    return fahrradrouteListenView.name;
  };
}
