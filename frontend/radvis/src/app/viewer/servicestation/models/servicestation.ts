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
import { ServicestationTyp } from 'src/app/viewer/servicestation/models/servicestation-typ';
import { ServicestationStatus } from 'src/app/viewer/servicestation/models/servicestation-status';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { ServicestationQuellSystem } from 'src/app/viewer/servicestation/models/servicestation-quell-system';

export interface Servicestation {
  geometrie: PointGeojson;
  name: string;
  id: number;
  version: number;
  quellSystem: ServicestationQuellSystem;
  darfBenutzerBearbeiten: boolean;

  gebuehren: boolean;
  oeffnungszeiten: string;
  betreiber: string;
  marke: string;
  luftpumpe: boolean;
  kettenwerkzeug: boolean;
  werkzeug: boolean;
  fahrradhalterung: boolean;
  beschreibung: string;
  organisation: Verwaltungseinheit;
  typ: ServicestationTyp;
  status: ServicestationStatus;
}
