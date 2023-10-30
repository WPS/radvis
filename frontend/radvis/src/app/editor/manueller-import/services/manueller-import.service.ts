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

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { GeoJSONFeature, GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Observable } from 'rxjs';
import { DeleteMappedGrundnetzkanteCommand } from 'src/app/editor/manueller-import/models/delete-mapped-grundnetzkante-command';
import { ImportSessionView } from 'src/app/editor/manueller-import/models/import-session-view';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { ImportierbaresAttribut } from 'src/app/editor/manueller-import/models/importierbares-attribut';
import { UpdateFeatureMappingCommand } from 'src/app/editor/manueller-import/models/update-feature-mapping-command';
import { ValidateAttributeImportCommand } from 'src/app/editor/manueller-import/models/validate-attribute-import-command';
import { StartAttributeImportSessionCommand } from 'src/app/editor/manueller-import/models/start-attribute-import-session-command';
import { StartNetzklassenImportSessionCommand } from 'src/app/editor/manueller-import/models/start-netzklassen-import-session-command';

@Injectable({
  providedIn: 'root',
})
export class ManuellerImportService {
  public static readonly MILLISECONDS_IN_HOUR = 3600000;
  public static readonly POLLING_INTERVALL_IN_MILLISECONDS = 2500;

  readonly manuellerImportApi = '/api/import/common';
  readonly manuellerImportAbfrageApi = '/api/import/abfrage';
  readonly manuellerNetzklasseImportApi = '/api/import/netzklassen';
  readonly manuellerAttributeImportApi = '/api/import/attribute';

  constructor(private http: HttpClient) {}

  getImportSession(): Observable<ImportSessionView> {
    return this.http.get<ImportSessionView>(`${this.manuellerImportAbfrageApi}/session`);
  }

  existsImportSession(importTyp?: ImportTyp): Promise<boolean> {
    let url = `${this.manuellerImportAbfrageApi}/exists-session`;
    if (importTyp) {
      url += `?importTyp=${importTyp}`;
    }
    return this.http.get<boolean>(url).toPromise();
  }

  deleteImportSession(): Promise<void> {
    return this.http.delete<void>(`${this.manuellerImportApi}/delete-session`).toPromise();
  }

  getImportierbareAttribute(command: ValidateAttributeImportCommand, file: File): Promise<ImportierbaresAttribut[]> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http
      .post<ImportierbaresAttribut[]>(`${this.manuellerAttributeImportApi}/validate-attribute`, formData)
      .toPromise();
  }

  executeNetzklassenZuweisen(): Promise<void> {
    return this.http.get<void>(`${this.manuellerNetzklasseImportApi}/execute-zuweisen`).toPromise();
  }

  getKanteIdsMitNetzklasse(): Promise<number[]> {
    return this.http.get<number[]>(`${this.manuellerNetzklasseImportApi}/kante-ids`).toPromise();
  }

  getNetzklassenSackgassen(): Promise<GeoJSONFeatureCollection> {
    return this.http.get<GeoJSONFeatureCollection>(`${this.manuellerNetzklasseImportApi}/sackgassen`).toPromise();
  }

  toggleNetzklassenzugehoerigkeit(kanteId: number): Observable<number[]> {
    return this.http.post<number[]>(`${this.manuellerNetzklasseImportApi}/toggle-zugehoerigkeit/${kanteId}`, null);
  }

  createSessionAndStartNetzklassenImport(command: StartNetzklassenImportSessionCommand, file: File): Promise<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<void>(`${this.manuellerNetzklasseImportApi}/start-import`, formData).toPromise();
  }

  executeAttributeUebernehmen(): Promise<void> {
    return this.http.post<void>(`${this.manuellerAttributeImportApi}/execute-uebernehmen`, null).toPromise();
  }

  createSessionAndStartAttributeImport(command: StartAttributeImportSessionCommand, file: File): Promise<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<void>(`${this.manuellerAttributeImportApi}/start-import`, formData).toPromise();
  }

  getFeatureMappings(): Promise<GeoJSONFeatureCollection> {
    return this.http.get<GeoJSONFeatureCollection>(`${this.manuellerAttributeImportApi}/feature-mappings`).toPromise();
  }

  getKonfliktprotokolle(): Promise<GeoJSONFeatureCollection> {
    return this.http
      .get<GeoJSONFeatureCollection>(`${this.manuellerAttributeImportApi}/konflikt-protokolle`)
      .toPromise();
  }

  deleteMappedGrundnetzkanten(
    deleteMappedGrundnetzkanteCommands: DeleteMappedGrundnetzkanteCommand[]
  ): Promise<GeoJSONFeatureCollection> {
    return this.http
      .post<GeoJSONFeatureCollection>(
        `${this.manuellerAttributeImportApi}/delete-mapped-grundnetzkanten`,
        deleteMappedGrundnetzkanteCommands
      )
      .toPromise();
  }

  updateMappedGrundnetzkanten(updateFeatureMappingCommand: UpdateFeatureMappingCommand): Observable<GeoJSONFeature> {
    return this.http.post<GeoJSONFeature>(
      `${this.manuellerAttributeImportApi}/update-feature-mapping`,
      updateFeatureMappingCommand
    );
  }
}
