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

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Anpassungswunsch } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch';
import { AnpassungswunschListenView } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-listen-view';
import { SaveAnpassungswunschCommand } from 'src/app/viewer/anpassungswunsch/models/save-anpassungswunsch-command';
import { AddKommentarCommand } from 'src/app/viewer/kommentare/models/add-kommentar-command';
import { Kommentar } from 'src/app/viewer/kommentare/models/kommentar';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class AnpassungswunschService {
  private readonly api = '/api/anpassungswunsch';

  constructor(private http: HttpClient) {}

  createAnpassungswunsch(command: SaveAnpassungswunschCommand): Promise<Anpassungswunsch> {
    return this.http.post<Anpassungswunsch>(`${this.api}/create`, command).toPromise();
  }

  getAnpassungswunsch(id: number): Promise<Anpassungswunsch> {
    invariant(id, 'Id muss gesetzt sein');
    return this.http.get<Anpassungswunsch>(`${this.api}/${id}`).toPromise();
  }

  delete(id: number): Promise<void> {
    invariant(id, 'Id muss gesetzt sein');
    return this.http.delete<void>(`${this.api}/${id}`).toPromise();
  }

  getAlleAnpassungswuensche(
    abgeschlosseneAnpassungswuenscheAusblenden: boolean
  ): Promise<AnpassungswunschListenView[]> {
    let params = new HttpParams();
    params = params.set('abgeschlosseneAusblenden', abgeschlosseneAnpassungswuenscheAusblenden);
    return this.http
      .get<AnpassungswunschListenView[]>(`${this.api}/list`, { params })
      .toPromise();
  }

  updateAnpassungswunsch(id: number, command: SaveAnpassungswunschCommand): Promise<Anpassungswunsch> {
    return this.http.post<Anpassungswunsch>(`${this.api}/${id}/update`, command).toPromise();
  }

  getKommentarListe(id: number): Promise<Kommentar[]> {
    return this.http.get<Kommentar[]>(`${this.api}/${id}/kommentarliste`).toPromise();
  }

  addKommentar(massnahmeId: number, command: AddKommentarCommand): Promise<Kommentar[]> {
    return this.http.post<Kommentar[]>(`${this.api}/${massnahmeId}/kommentar`, command).toPromise();
  }
}
