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
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { DeleteAbstellanlageCommand } from 'src/app/viewer/abstellanlage/models/delete-abstellanlage-command';
import { SaveAbstellanlageCommand } from 'src/app/viewer/abstellanlage/models/save-abstellanlage-command';
import { AddDokumentCommand } from 'src/app/viewer/dokument/models/add-dokument-command';
import { DokumentListeView } from 'src/app/viewer/dokument/models/dokument-liste-view';
import invariant from 'tiny-invariant';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';

@Injectable({
  providedIn: 'root',
})
export class AbstellanlageService implements Resolve<Abstellanlage> {
  private readonly API = '/api/abstellanlage';

  constructor(private http: HttpClient, private fileHandlingService: FileHandlingService) {}

  public create(command: SaveAbstellanlageCommand): Promise<number> {
    return this.http.post<number>(`${this.API}/new`, command).toPromise();
  }

  public save(id: number, command: SaveAbstellanlageCommand): Promise<Abstellanlage> {
    return this.http.post<Abstellanlage>(`${this.API}/${id}`, command).toPromise();
  }

  public delete(id: number, command: DeleteAbstellanlageCommand): Promise<void> {
    return this.http
      .delete<void>(`${this.API}/${id}`, { body: command })
      .toPromise();
  }

  public getAll(): Promise<Abstellanlage[]> {
    return this.http.get<Abstellanlage[]>(`${this.API}`).toPromise();
  }

  public get(id: number): Promise<Abstellanlage> {
    return this.http.get<Abstellanlage>(`${this.API}/${id}`).toPromise();
  }

  getDokumentListe(id: number): Promise<DokumentListeView> {
    return this.http.get<DokumentListeView>(`${this.API}/${id}/dokumentliste`).toPromise();
  }

  uploadFile(id: number, command: AddDokumentCommand, file: File): Promise<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<void>(`${this.API}/${id}/dokument`, formData).toPromise();
  }

  downloadFile(id: number, dokumentId: number): Promise<Blob> {
    return this.http
      .get<Blob>(`${this.API}/${id}/dokument/${dokumentId}`, {
        responseType: 'blob' as 'json',
      })
      .toPromise();
  }

  deleteFile(id: number, dokumentId: number): Promise<void> {
    return this.http.delete<void>(`${this.API}/${id}/dokument/${dokumentId}`).toPromise();
  }

  // eslint-disable-next-line no-unused-vars
  public resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<Abstellanlage> {
    // Auch fuer die ToolComponent, darum die Veroderung
    const id = route.paramMap.get('id') || route.parent?.paramMap.get('id');
    invariant(id, 'Abstellanlagen-ID muss als Parameter id an der Route gesetzt sein.');
    return this.http.get<Abstellanlage>(`${this.API}/${id}`).toPromise();
  }

  public uploadCsv(formData: FormData): Promise<Blob> {
    return this.http
      .post<Blob>(`${this.API}`, formData, {
        responseType: 'blob' as 'json',
      })
      .toPromise();
  }
}
