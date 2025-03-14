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

import { Bauwerksmangel } from 'src/app/shared/models/bauwerksmangel';
import { BauwerksmangelArt } from 'src/app/shared/models/bauwerksmangel-art';
import { PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { QuellSystem } from 'src/app/shared/models/quell-system';
import { QuerungshilfeDetails } from 'src/app/shared/models/querungshilfe-details';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';

export interface Knoten {
  id: number;
  geometry: PointGeojson;
  ortslage: string | null;
  gemeinde: Verwaltungseinheit | null;
  landkreis: Verwaltungseinheit | null;
  kommentar: string | null;
  zustandsbeschreibung: string | null;
  knotenForm: string | null;
  knotenVersion: number;
  quelle: QuellSystem;
  liegtInZustaendigkeitsbereich: boolean;
  querungshilfeDetails: QuerungshilfeDetails | null;
  bauwerksmangel: Bauwerksmangel | null;
  bauwerksmangelArt: BauwerksmangelArt[] | null;
}
