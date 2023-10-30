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
import { CreateOrganisationCommand } from 'src/app/administration/models/create-organisation-command';
import { SaveOrganisationCommand } from 'src/app/administration/models/save-organisation-command';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { Organisation } from 'src/app/shared/models/organisation-edit-view';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class OrganisationenVerwaltungService {
  private url = `/api/administration/organisationen`;

  constructor(private http: HttpClient) {}

  getAlleFuerBearbeitung(): Promise<Verwaltungseinheit[]> {
    return this.http.get<Verwaltungseinheit[]>(`${this.url}`).toPromise();
  }

  getAlleZuweisbaren(): Promise<Verwaltungseinheit[]> {
    return this.http.get<Verwaltungseinheit[]>(`${this.url}/zuweisbar`).toPromise();
  }

  getFuerBearbeitung(id: number): Promise<Organisation> {
    invariant(id, 'Id muss gesetzt sein');
    return this.http.get<Organisation>(`${this.url}/${id}`).toPromise();
  }

  save(command: SaveOrganisationCommand): Promise<Organisation> {
    return this.http.post<Organisation>(`${this.url}/save`, command).toPromise();
  }

  create(command: CreateOrganisationCommand): Promise<Organisation> {
    return this.http.post<Organisation>(`${this.url}/create`, command).toPromise();
  }

  aendereOrganisationAktiv(id: number, aktiv: boolean): Promise<Organisation> {
    return this.http
      .post<Organisation>(`${this.url}/${aktiv ? '' : 'de'}aktiviere-organisation?id=${id}`, {})
      .toPromise();
  }
}
