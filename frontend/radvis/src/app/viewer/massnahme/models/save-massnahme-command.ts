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
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { Realisierungshilfe } from 'src/app/viewer/massnahme/models/realisierungshilfe';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { Netzbezug } from 'src/app/shared/models/netzbezug';

export interface SaveMassnahmeCommand {
  id: number;
  version: number;
  bezeichnung: string;
  massnahmenkategorien: string[];
  netzbezug: Netzbezug;
  umsetzungsstatus: Umsetzungsstatus;
  veroeffentlicht: boolean;
  planungErforderlich: boolean;
  durchfuehrungszeitraum: Durchfuehrungszeitraum | null;
  baulastZustaendigerId: number | null;
  prioritaet: number | null;
  kostenannahme: number | null;
  netzklassen: Netzklasse[];
  zustaendigerId: number;
  unterhaltsZustaendigerId: number | null;
  maViSID: string | null;
  verbaID: string | null;
  lgvfgid: string | null;
  massnahmeKonzeptID: string | null;
  sollStandard: SollStandard;
  handlungsverantwortlicher: Handlungsverantwortlicher;
  konzeptionsquelle: Konzeptionsquelle;
  sonstigeKonzeptionsquelle: string | null;
  realisierungshilfe: Realisierungshilfe | null;
}
