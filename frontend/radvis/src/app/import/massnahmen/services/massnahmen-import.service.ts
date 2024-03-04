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
import { MassnahmenImportAttributeAuswaehlenCommand } from 'src/app/import/massnahmen/models/massnahmen-import-attribute-uebernehmen-command';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { StartMassnahmenImportSessionCommand } from 'src/app/import/massnahmen/models/start-massnahmen-import-session-command';
import { ImportService } from 'src/app/import/services/import.service';
import { MassnahmenImportZuordnung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung';

@Injectable()
export class MassnahmenImportService extends ImportService {
  readonly massnahmenImportApi = '/api/import/massnahmen';

  public getImportSession(): Observable<MassnahmenImportSessionView | null> {
    return this.http.get<MassnahmenImportSessionView | null>(`${this.massnahmenImportApi}/session`);
  }

  public getZuordnungen(): Observable<MassnahmenImportZuordnung[] | null> {
    return this.http.get<MassnahmenImportZuordnung[] | null>(`${this.massnahmenImportApi}/session/zuordnungen`);
  }

  public createSessionAndStartMassnahmenImport(
    command: StartMassnahmenImportSessionCommand,
    file: File
  ): Observable<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<void>(`${this.massnahmenImportApi}/start-import`, formData);
  }

  public attributeAuswaehlen(command: MassnahmenImportAttributeAuswaehlenCommand): Observable<void> {
    return this.http.post<void>(`${this.massnahmenImportApi}/attribute-auswaehlen`, command);
  }
}
