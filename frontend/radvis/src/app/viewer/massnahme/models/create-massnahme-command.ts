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

import { Durchfuehrungszeitraum } from 'src/app/shared/models/durchfuehrungszeitraum';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { Netzbezug } from 'src/app/viewer/viewer-shared/models/netzbezug';

export interface CreateMassnahmeCommand {
  bezeichnung: string;
  massnahmenkategorien: string[];
  netzbezug: Netzbezug;
  umsetzungsstatus: Umsetzungsstatus;
  veroeffentlicht: boolean;
  planungErforderlich: boolean;
  durchfuehrungszeitraum: Durchfuehrungszeitraum | null;
  baulastZustaendigerId: number | null;
  sollStandard: SollStandard;
  handlungsverantwortlicher: Handlungsverantwortlicher;
  konzeptionsquelle: Konzeptionsquelle;
  sonstigeKonzeptionsquelle: string | null;
}
