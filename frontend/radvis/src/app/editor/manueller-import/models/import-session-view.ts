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

import { AutomatischerImportSchritt } from 'src/app/editor/manueller-import/models/automatischer-import-schritt';
import { ImportSessionStatus } from 'src/app/editor/manueller-import/models/import-session-status';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';

export enum Severity {
  WARN = 'WARN',
  ERROR = 'ERROR',
}

export interface ImportLogEintrag {
  fehlerBeschreibung: string;
  severity: Severity;
}

export interface ImportSessionView {
  typ: ImportTyp;
  aktuellerImportSchritt: AutomatischerImportSchritt;
  log: ImportLogEintrag[];
  status: ImportSessionStatus;
  organisationsID: number;
  anzahlFeaturesOhneMatch: number | null;
}
