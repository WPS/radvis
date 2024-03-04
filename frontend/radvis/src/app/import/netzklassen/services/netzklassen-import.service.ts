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
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Observable } from 'rxjs';
import { NetzklassenImportSessionView } from 'src/app/import/netzklassen/models/netzklassen-import-session-view';
import { StartNetzklassenImportSessionCommand } from 'src/app/import/netzklassen/models/start-netzklassen-import-session-command';
import { ImportService } from 'src/app/import/services/import.service';

@Injectable()
export class NetzklassenImportService extends ImportService {
  readonly manuellerNetzklasseImportApi = '/api/import/netzklassen';

  public getImportSession(): Observable<NetzklassenImportSessionView | null> {
    return this.http.get<NetzklassenImportSessionView | null>(`${this.manuellerNetzklasseImportApi}/session`);
  }

  public bearbeitungAbschliessen(): Observable<void> {
    return this.http.post<void>(`${this.manuellerNetzklasseImportApi}/bearbeitung-abschliessen`, null);
  }

  public executeNetzklassenZuweisen(): Promise<void> {
    return this.http.get<void>(`${this.manuellerNetzklasseImportApi}/execute-zuweisen`).toPromise();
  }

  public getKanteIdsMitNetzklasse(): Promise<number[]> {
    return this.http.get<number[]>(`${this.manuellerNetzklasseImportApi}/kante-ids`).toPromise();
  }

  public getNetzklassenSackgassen(): Promise<GeoJSONFeatureCollection> {
    return this.http.get<GeoJSONFeatureCollection>(`${this.manuellerNetzklasseImportApi}/sackgassen`).toPromise();
  }

  public toggleNetzklassenzugehoerigkeit(kanteId: number): Observable<number[]> {
    return this.http.post<number[]>(`${this.manuellerNetzklasseImportApi}/toggle-zugehoerigkeit/${kanteId}`, null);
  }

  public createSessionAndStartNetzklassenImport(
    command: StartNetzklassenImportSessionCommand,
    file: File
  ): Promise<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<void>(`${this.manuellerNetzklasseImportApi}/start-import`, formData).toPromise();
  }
}
