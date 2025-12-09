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
import { ServicestationStatus } from 'src/app/viewer/servicestation/models/servicestation-status';
import { ServicestationTyp } from 'src/app/viewer/servicestation/models/servicestation-typ';

export interface SaveServicestationCommand {
  geometrie: PointGeojson;
  name: string;
  version?: number;

  gebuehren: boolean;
  oeffnungszeiten: string | null;
  betreiber: string;
  marke: string | null;
  luftpumpe: boolean;
  kettenwerkzeug: boolean;
  werkzeug: boolean;
  fahrradhalterung: boolean;
  beschreibung: string | null;
  organisationId: number | null;
  typ: ServicestationTyp;
  status: ServicestationStatus;
  radkultur: boolean;
}
