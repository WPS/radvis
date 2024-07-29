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
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { AddDokumentCommand } from 'src/app/viewer/dokument/models/add-dokument-command';
import { DokumentListeView } from 'src/app/viewer/dokument/models/dokument-liste-view';
import { DeleteServicestationCommand } from 'src/app/viewer/servicestation/models/delete-servicestation-command';
import { SaveServicestationCommand } from 'src/app/viewer/servicestation/models/save-servicestation-command';
import { Servicestation } from 'src/app/viewer/servicestation/models/servicestation';

@Injectable({
  providedIn: 'root',
})
export class ServicestationService {
  private readonly api = '/api/servicestation';

  constructor(
    private http: HttpClient,
    private fileHandlingService: FileHandlingService
  ) {}

  public create(command: SaveServicestationCommand): Promise<number> {
    return this.http.post<number>(`${this.api}/new`, command).toPromise();
  }

  public save(id: number, command: SaveServicestationCommand): Promise<Servicestation> {
    return this.http.post<Servicestation>(`${this.api}/${id}`, command).toPromise();
  }

  public delete(id: number, command: DeleteServicestationCommand): Promise<void> {
    return this.http.delete<void>(`${this.api}/${id}`, { body: command }).toPromise();
  }

  public getAll(): Promise<Servicestation[]> {
    return this.http.get<Servicestation[]>(`${this.api}`).toPromise();
  }

  public get(id: number): Promise<Servicestation> {
    return this.http.get<Servicestation>(`${this.api}/${id}`).toPromise();
  }

  getDokumentListe(servicestationId: number): Promise<DokumentListeView> {
    return this.http.get<DokumentListeView>(`${this.api}/${servicestationId}/dokumentliste`).toPromise();
  }

  uploadFile(servicestationId: number, command: AddDokumentCommand, file: File): Promise<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<void>(`${this.api}/${servicestationId}/dokument`, formData).toPromise();
  }

  downloadFile(servicestationId: number, dokumentId: number): Promise<Blob> {
    return this.http
      .get<Blob>(`${this.api}/${servicestationId}/dokument/${dokumentId}`, {
        responseType: 'blob' as 'json',
      })
      .toPromise();
  }

  deleteFile(servicestationId: number, dokumentId: number): Promise<void> {
    return this.http.delete<void>(`${this.api}/${servicestationId}/dokument/${dokumentId}`).toPromise();
  }

  public uploadCsv(file: File): Promise<Blob> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<Blob>(`${this.api}`, formData, {
        responseType: 'blob' as 'json',
      })
      .toPromise();
  }
}
