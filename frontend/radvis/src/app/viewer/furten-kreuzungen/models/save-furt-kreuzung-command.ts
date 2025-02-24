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
import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { QuerungshilfeDetails } from 'src/app/shared/models/querungshilfe-details';
import { FurtKreuzungTyp } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-typ';
import { LichtsignalAnlageEigenschaften } from 'src/app/viewer/furten-kreuzungen/models/lichtsignal-anlage-eigenschaften';

export interface SaveFurtKreuzungCommand {
  netzbezug: Netzbezug;
  verantwortlicheOrganisation: number;
  typ: FurtKreuzungTyp;
  knotenForm: string;
  radnetzKonform: boolean;
  kommentar: string | null;
  version?: number;
  furtKreuzungMusterloesung: string | null;
  lichtsignalAnlageEigenschaften: LichtsignalAnlageEigenschaften | null;
  querungshilfeDetails: QuerungshilfeDetails | null;
  bauwerksmangel: Bauwerksmangel | null;
  bauwerksmangelArt: BauwerksmangelArt[] | null;
}
