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
import { FahrradzaehlstelleListenView } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle-listen-view';
import { FahrradzaehlstelleDetailView } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle-detail-view';
import { ArtDerAuswertung } from 'src/app/viewer/fahrradzaehlstelle/models/art-der-auswertung';
import { FahrradzaehlstelleAuswertung } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle-auswertung';

@Injectable({
  providedIn: 'root',
})
export class FahrradzaehlstelleService {
  private readonly api = '/api/fahrradzaehlstelle';

  constructor(private http: HttpClient) {}

  getAll(): Promise<FahrradzaehlstelleListenView[]> {
    return this.http.get<FahrradzaehlstelleListenView[]>(`${this.api}/list`).toPromise();
  }

  getFahrradzaehlstelleForView(id: number): Promise<FahrradzaehlstelleDetailView> {
    return this.http.get<FahrradzaehlstelleDetailView>(`${this.api}/${id}`).toPromise();
  }

  getDataForChannel(
    channelIds: number[],
    artDerAuswertung: ArtDerAuswertung,
    startDate: Date,
    endDate: Date
  ): Promise<FahrradzaehlstelleAuswertung> {
    const params = new HttpParams()
      .set('channelIds', channelIds.join())
      .set('startDate', startDate.toISOString().split('.')[0] + 'Z')
      .set('endDate', endDate.toISOString().split('.')[0] + 'Z')
      .set('artDerAuswertung', artDerAuswertung);

    return this.http
      .get<FahrradzaehlstelleAuswertung>(`${this.api}/channelData`, { params })
      .toPromise();
  }
}
