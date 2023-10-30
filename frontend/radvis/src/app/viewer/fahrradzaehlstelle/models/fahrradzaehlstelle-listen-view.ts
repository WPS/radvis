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

import { PointGeojson } from 'src/app/shared/models/geojson-geometrie';

export interface FahrradzaehlstelleListenView {
  id: number;
  geometrie: PointGeojson;
  betreiberEigeneId: string;
  fahrradzaehlstelleGebietskoerperschaft: string | null;
  fahrradzaehlstelleBezeichnung: string | null;
  seriennummer: string | null;
  zaehlintervall: string | null;
  neusterZeitstempel: string | null;
  neusterZeitstempelEpochSecs: number | null;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace FahrradzaehlstelleListenView {
  const EMPTY_FIELD_INDICATOR = '';

  export const getSortingValueForKey = (
    item: FahrradzaehlstelleListenView,
    key: string
  ): string | number | string[] => {
    if (key === 'neusterZeitstempel') {
      return item.neusterZeitstempelEpochSecs ?? EMPTY_FIELD_INDICATOR;
    } else {
      return (item[key as keyof FahrradzaehlstelleListenView] as string) ?? EMPTY_FIELD_INDICATOR;
    }
  };
}
