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

import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { Beleuchtung } from 'src/app/editor/kanten/models/beleuchtung';
import { IstStandard } from 'src/app/shared/models/ist-standard';
import { StrassenquerschnittRASt06 } from 'src/app/editor/kanten/models/strassenquerschnittrast06';
import { Umfeld } from 'src/app/editor/kanten/models/umfeld';
import { WegeNiveau } from 'src/app/editor/kanten/models/wege-niveau';

export interface SaveKantenAttributGruppeCommand {
  kanteId: number;

  gruppenId: number;
  gruppenVersion: number;

  wegeNiveau: WegeNiveau | null;
  beleuchtung: Beleuchtung | null;
  umfeld: Umfeld | null;
  strassenquerschnittRASt06: StrassenquerschnittRASt06 | null;
  laengeManuellErfasst: number | null;
  dtvFussverkehr: number | null;
  dtvRadverkehr: number | null;
  dtvPkw: number | null;
  sv: number | null;
  kommentar: string | null;
  gemeinde: number | null;
  status: string | null;

  netzklassen: Netzklasse[];
  istStandards: IstStandard[];
}
