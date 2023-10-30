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
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { FurtKreuzung } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung';
import { FurtKreuzungListenView } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-listen-view';
import { SaveFurtKreuzungCommand } from 'src/app/viewer/furten-kreuzungen/models/save-furt-kreuzung-command';

@Injectable({
  providedIn: 'root',
})
export class FurtenKreuzungenService {
  private readonly api = '/api/furtkreuzung';

  constructor(private http: HttpClient) {}

  createFurtKreuzung(command: SaveFurtKreuzungCommand): Promise<number> {
    return this.http.post<number>(`${this.api}/new`, command).toPromise();
  }

  updateFurtKreuzung(id: number, command: SaveFurtKreuzungCommand): Promise<FurtKreuzung> {
    return this.http.post<FurtKreuzung>(`${this.api}/${id}`, command).toPromise();
  }

  getFurtKreuzung(id: number): Promise<FurtKreuzung> {
    return this.http.get<FurtKreuzung>(`${this.api}/${id}`).toPromise();
  }

  getAlleFurtenKreuzungen(): Promise<FurtKreuzungListenView[]> {
    return this.http.get<FurtKreuzungListenView[]>(`${this.api}/list`).toPromise();
  }

  getAllMusterloesungen(): Promise<EnumOption[]> {
    return this.http.get<EnumOption[]>(`${this.api}/musterloesung/list`).toPromise();
  }
}
