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
import { Benutzer } from 'src/app/administration/models/benutzer';
import { BenutzerListView } from 'src/app/administration/models/benutzer-list-view';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { SaveBenutzerCommand } from 'src/app/administration/models/save-benutzer-command';

@Injectable({
  providedIn: 'root',
})
export class BenutzerService {
  private readonly benutzerApi = '/api/benutzer';
  constructor(private http: HttpClient) {}

  public getAll(): Promise<BenutzerListView[]> {
    return this.http.get<BenutzerListView[]>(`${this.benutzerApi}/list`).toPromise();
  }

  public get(id: number): Promise<Benutzer> {
    return this.http.get<Benutzer>(`${this.benutzerApi}/${id}`).toPromise();
  }

  public save(command: SaveBenutzerCommand): Promise<Benutzer> {
    return this.http.post<Benutzer>(`${this.benutzerApi}/save`, command).toPromise();
  }

  public aendereBenutzerstatus(id: number, version: number, alterStatus: BenutzerStatus): Promise<Benutzer> {
    if (alterStatus === BenutzerStatus.INAKTIV) {
      return this.http
        .post<Benutzer>(`${this.benutzerApi}/aktiviere-benutzer?id=${id}&version=${version}`, {})
        .toPromise();
    } else {
      return this.http
        .post<Benutzer>(`${this.benutzerApi}/deaktiviere-benutzer?id=${id}&version=${version}`, {})
        .toPromise();
    }
  }
}
