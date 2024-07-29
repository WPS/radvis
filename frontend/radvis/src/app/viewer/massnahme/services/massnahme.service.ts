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

import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { AddDokumentCommand } from 'src/app/viewer/dokument/models/add-dokument-command';
import { DokumentListeView } from 'src/app/viewer/dokument/models/dokument-liste-view';
import { AddKommentarCommand } from 'src/app/viewer/kommentare/models/add-kommentar-command';
import { Kommentar } from 'src/app/viewer/kommentare/models/kommentar';
import { CreateMassnahmeCommand } from 'src/app/viewer/massnahme/models/create-massnahme-command';
import { DeleteMassnahmeCommand } from 'src/app/viewer/massnahme/models/delete-massnahme-command';
import { Massnahme } from 'src/app/viewer/massnahme/models/massnahme';
import { MassnahmeListenView } from 'src/app/viewer/massnahme/models/massnahme-listen-view';
import { MassnahmeToolView } from 'src/app/viewer/massnahme/models/massnahme-tool-view';
import { SaveMassnahmeCommand } from 'src/app/viewer/massnahme/models/save-massnahme-command';
import { SaveUmsetzungsstandCommand } from 'src/app/viewer/massnahme/models/save-umsetzungsstand-command';
import { Umsetzungsstand } from 'src/app/viewer/massnahme/models/umsetzungsstand';

@Injectable({
  providedIn: 'root',
})
export class MassnahmeService {
  private readonly api = '/api/massnahme';

  constructor(
    private http: HttpClient,
    private fileHandlingService: FileHandlingService
  ) {}

  createMassnahme(command: CreateMassnahmeCommand): Promise<number> {
    return this.http.post<number>(`${this.api}/create`, command).toPromise();
  }

  getDokumentListe(id: number): Promise<DokumentListeView> {
    return this.http.get<DokumentListeView>(`${this.api}/${id}/dokumentliste`).toPromise();
  }

  getKommentarListe(id: number): Promise<Kommentar[]> {
    return this.http.get<Kommentar[]>(`${this.api}/${id}/kommentarliste`).toPromise();
  }

  addKommentar(massnahmeId: number, command: AddKommentarCommand): Promise<Kommentar[]> {
    return this.http.post<Kommentar[]>(`${this.api}/${massnahmeId}/kommentar`, command).toPromise();
  }

  uploadFileToMassnahme(massnahmeId: number, command: AddDokumentCommand, file: File): Promise<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http.post<void>(`${this.api}/${massnahmeId}/dokument`, formData).toPromise();
  }

  downloadFile(massnahmeId: number, dokumentId: number): Promise<Blob> {
    return this.http
      .get<Blob>(`${this.api}/${massnahmeId}/dokument/${dokumentId}`, {
        responseType: 'blob' as 'json',
      })
      .toPromise();
  }

  deleteFile(massnahmeId: number, dokumentId: number): Promise<void> {
    return this.http.delete<void>(`${this.api}/${massnahmeId}/dokument/${dokumentId}`).toPromise();
  }

  saveMassnahme(command: SaveMassnahmeCommand): Promise<Massnahme> {
    return this.http.post<Massnahme>(`${this.api}/save`, command).toPromise();
  }

  getMassnahme(id: number): Promise<Massnahme> {
    return this.http.get<Massnahme>(`${this.api}/${id}/edit`).toPromise();
  }

  getAll(organisationId: number | null = null): Promise<MassnahmeListenView[]> {
    let params = new HttpParams();
    if (organisationId) {
      params = params.set('organisationId', organisationId);
    }
    return this.http.get<MassnahmeListenView[]>(`${this.api}/list`, { params }).toPromise();
  }

  saveUmsetzungsstand(saveUmsetzungsstandCommand: SaveUmsetzungsstandCommand): Promise<Umsetzungsstand> {
    return this.http.post<Umsetzungsstand>(`${this.api}/saveUmsetzungsstand`, saveUmsetzungsstandCommand).toPromise();
  }

  getUmsetzungstand(massnahmeId: number): Promise<Umsetzungsstand> {
    return this.http.get<Umsetzungsstand>(`${this.api}/${massnahmeId}/editUmsetzungsstand`).toPromise();
  }

  hasUmsetzungstand(massnahmeId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.api}/${massnahmeId}/hasUmsetzungsstand`);
  }

  starteUmsetzungsstandsabfrage(massnahmeIds: number[]): Promise<void> {
    return this.http.post<void>(`${this.api}/starteUmsetzungsstandsabfrage`, massnahmeIds).toPromise();
  }

  auswertungHerunterladen(massnahmenIds: number[]): Promise<HttpResponse<Blob>> {
    return this.http
      .post<Blob>(`${this.api}/umsetzungsstand/auswertung`, massnahmenIds, {
        responseType: 'blob' as 'json',
        observe: 'response',
      })
      .toPromise();
  }

  getBenachrichtigungsFunktion(massnahmeId: number): Promise<boolean> {
    return this.http.get<boolean>(`${this.api}/${massnahmeId}/benachrichtigung`).toPromise();
  }

  stelleBenachrichtigungsFunktionEin(massnahmeId: number, aktiv: boolean): Promise<boolean> {
    return this.http.post<boolean>(`${this.api}/${massnahmeId}/stelleBenachrichtigungsFunktionEin`, aktiv).toPromise();
  }

  delete(deleteMassnahmeCommand: DeleteMassnahmeCommand): Promise<void> {
    return this.http
      .delete<void>(`${this.api}/${deleteMassnahmeCommand.id}`, { body: deleteMassnahmeCommand })
      .toPromise();
  }

  getMassnahmeToolView(massnahmeId: number): Promise<MassnahmeToolView> {
    return this.http.get<MassnahmeToolView>(`${this.api}/${massnahmeId}/massnahmeToolView`).toPromise();
  }
}
