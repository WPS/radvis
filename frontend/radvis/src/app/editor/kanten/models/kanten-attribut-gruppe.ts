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

import { Beleuchtung } from 'src/app/editor/kanten/models/beleuchtung';
import { Status } from 'src/app/editor/kanten/models/status';
import { StrassenquerschnittRASt06 } from 'src/app/editor/kanten/models/strassenquerschnittrast06';
import { Umfeld } from 'src/app/editor/kanten/models/umfeld';
import { WegeNiveau } from 'src/app/editor/kanten/models/wege-niveau';
import { IstStandard } from 'src/app/shared/models/ist-standard';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { VersionierteEntitaet } from 'src/app/shared/models/versionierte-entitaet';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { StrassenkategorieRIN } from 'src/app/editor/kanten/models/strassenkategorie-rin';

export interface KantenAttributGruppe extends VersionierteEntitaet {
  wegeNiveau: WegeNiveau | null;
  beleuchtung: Beleuchtung | null;
  umfeld: Umfeld;
  strassenkategorieRIN: StrassenkategorieRIN | null;
  strassenquerschnittRASt06: StrassenquerschnittRASt06;
  laengeBerechnet: number;
  laengeManuellErfasst: number | null;
  dtvFussverkehr: number | null;
  dtvRadverkehr: number | null;
  dtvPkw: number | null;
  sv: number | null;
  kommentar: string | null;
  strassenName: string | null;
  strassenNummer: string | null;
  gemeinde: Verwaltungseinheit | null;
  landkreis: Verwaltungseinheit | null;
  status: Status;
  netzklassen: Netzklasse[];
  istStandards: IstStandard[];
}
