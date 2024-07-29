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

import { Geojson } from 'src/app/shared/models/geojson-geometrie';
import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { MassnahmenImportZuordnungStatus } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-status';

export interface MassnahmenImportZuordnungUeberpruefung {
  id: number;
  massnahmeKonzeptId: string;
  status: MassnahmenImportZuordnungStatus;
  netzbezugHinweise: NetzbezugHinweis[];
  netzbezug?: Netzbezug;
  originalGeometrie: Geojson;
  netzbezugGeometrie?: Geojson;
  selected: boolean;
}

export interface NetzbezugHinweis {
  text: string;
  tooltip: string;
  severity: 'WARN' | 'ERROR';
}