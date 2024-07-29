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
import { KanteDetailView } from 'src/app/shared/models/kante-detail-view';
import { KnotenDetailView } from 'src/app/shared/models/knoten-detail-view';

@Injectable({
  providedIn: 'root',
})
export class NetzdetailService {
  private readonly api = '/api/netz';

  constructor(private http: HttpClient) {}

  getKanteForView(id: number, clickposition: string | null, selektierteSeite: string): Promise<KanteDetailView> {
    let params = new HttpParams();
    if (clickposition) {
      params = params.append('position', clickposition);
    }
    params = params.append('seite', selektierteSeite ?? '');

    return this.http.get<KanteDetailView>(`${this.api}/kante/${id}`, { params }).toPromise();
  }

  getKnotenForView(id: number): Promise<KnotenDetailView> {
    return this.http.get<KnotenDetailView>(`${this.api}/knoten/${id}`).toPromise();
  }
}
