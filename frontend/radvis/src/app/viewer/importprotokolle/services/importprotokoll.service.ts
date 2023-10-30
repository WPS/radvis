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
import { FahrradrouteImportprotokoll } from 'src/app/viewer/importprotokolle/models/fahrradroute-importprotokoll';
import { ImportprotokollListView } from 'src/app/viewer/importprotokolle/models/importprotokoll-list-view';
import { WegweiserImportprotokoll } from 'src/app/viewer/importprotokolle/models/wegweiser-importprotokoll';

@Injectable({
  providedIn: 'root',
})
export class ImportprotokollService {
  constructor(private http: HttpClient) {}

  findAll(): Promise<ImportprotokollListView[]> {
    return this.http.get<ImportprotokollListView[]>('/api/importprotokoll').toPromise();
  }

  getFahrradrouteImportprotokoll(id: number): Promise<FahrradrouteImportprotokoll> {
    return this.http.get<FahrradrouteImportprotokoll>(`/api/fahrradroute/importprotokoll/${id}`).toPromise();
  }

  getWegweiserImportprotokoll(id: number): Promise<WegweiserImportprotokoll> {
    return this.http.get<WegweiserImportprotokoll>(`/api/wegweisendebeschilderung/importprotokoll/${id}`).toPromise();
  }
}
