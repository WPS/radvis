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
import { GeoJSONFeature, GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Observable } from 'rxjs';
import { AttributeImportSessionView } from 'src/app/import/attribute/models/attribute-import-session-view';
import { DeleteMappedGrundnetzkanteCommand } from 'src/app/import/attribute/models/delete-mapped-grundnetzkante-command';
import { ImportierbaresAttribut } from 'src/app/import/attribute/models/importierbares-attribut';
import { StartAttributeImportSessionCommand } from 'src/app/import/attribute/models/start-attribute-import-session-command';
import { UpdateFeatureMappingCommand } from 'src/app/import/attribute/models/update-feature-mapping-command';
import { ValidateAttributeImportCommand } from 'src/app/import/attribute/models/validate-attribute-import-command';
import { ImportService } from 'src/app/import/services/import.service';

@Injectable()
export class AttributeImportService extends ImportService {
  readonly manuellerAttributeImportApi = '/api/import/attribute';

  public getImportSession(): Observable<AttributeImportSessionView | null> {
    return this.http.get<AttributeImportSessionView | null>(`${this.manuellerAttributeImportApi}/session`);
  }

  public getImportierbareAttribute(
    command: ValidateAttributeImportCommand,
    file: File
  ): Promise<ImportierbaresAttribut[]> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http
      .post<ImportierbaresAttribut[]>(`${this.manuellerAttributeImportApi}/validate-attribute`, formData)
      .toPromise();
  }

  public executeAttributeUebernehmen(): Promise<void> {
    return this.http.post<void>(`${this.manuellerAttributeImportApi}/execute-uebernehmen`, null).toPromise();
  }

  public bearbeitungAbschliessen(): Observable<void> {
    return this.http.post<void>(`${this.manuellerAttributeImportApi}/bearbeitung-abschliessen`, null);
  }

  public createSessionAndStartAttributeImport(command: StartAttributeImportSessionCommand, file: File): Promise<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<void>(`${this.manuellerAttributeImportApi}/start-import`, formData).toPromise();
  }

  public getFeatureMappings(): Promise<GeoJSONFeatureCollection> {
    return this.http.get<GeoJSONFeatureCollection>(`${this.manuellerAttributeImportApi}/feature-mappings`).toPromise();
  }

  public getKonfliktprotokolle(): Promise<GeoJSONFeatureCollection> {
    return this.http
      .get<GeoJSONFeatureCollection>(`${this.manuellerAttributeImportApi}/konflikt-protokolle`)
      .toPromise();
  }

  public deleteMappedGrundnetzkanten(
    deleteMappedGrundnetzkanteCommands: DeleteMappedGrundnetzkanteCommand[]
  ): Promise<GeoJSONFeatureCollection> {
    return this.http
      .post<GeoJSONFeatureCollection>(
        `${this.manuellerAttributeImportApi}/delete-mapped-grundnetzkanten`,
        deleteMappedGrundnetzkanteCommands
      )
      .toPromise();
  }

  public updateMappedGrundnetzkanten(
    updateFeatureMappingCommand: UpdateFeatureMappingCommand
  ): Observable<GeoJSONFeature> {
    return this.http.post<GeoJSONFeature>(
      `${this.manuellerAttributeImportApi}/update-feature-mapping`,
      updateFeatureMappingCommand
    );
  }
}
