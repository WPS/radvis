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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MassnahmenDateianhaengeImportSessionView } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-view';
import { StartMassnahmenDateianhaengeImportSessionCommand } from 'src/app/import/massnahmen-dateianhaenge/models/start-massnahmen-dateianhaenge-import-session-command';
import { ImportService } from 'src/app/import/services/import.service';

@Injectable()
export class MassnahmenDateianhaengeService extends ImportService {
  readonly massnahmenDateianhaengeImportApi = '/api/import/massnahmen-dateianhaenge';

  public getImportSession(): Observable<MassnahmenDateianhaengeImportSessionView | null> {
    return this.http.get<MassnahmenDateianhaengeImportSessionView | null>(
      `${this.massnahmenDateianhaengeImportApi}/session`
    );
  }

  public createSessionAndStartMassnahmenDateianhaengeImport(
    command: StartMassnahmenDateianhaengeImportSessionCommand,
    file: File
  ): Observable<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<void>(`${this.massnahmenDateianhaengeImportApi}/start-import`, formData);
  }

  public continueAfterFehlerUeberpruefen(): Observable<void> {
    return this.http.post<void>(`${this.massnahmenDateianhaengeImportApi}/continue-checked-errors`, {});
  }
}
