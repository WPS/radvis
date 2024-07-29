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
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Observable } from 'rxjs';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import invariant from 'tiny-invariant';
import { VerwaltungseinheitBereichEnvelopeView } from 'src/app/shared/models/verwaltungseinheit-bereich-envelope-view';

@Injectable({
  providedIn: 'root',
})
export class OrganisationenService {
  private url = `/api/organisationen`;

  constructor(private http: HttpClient) {}

  getAlleOrganisationen(): Observable<Verwaltungseinheit[]> {
    return this.http.get<Verwaltungseinheit[]>(this.url);
  }

  getOrganisationen(): Promise<Verwaltungseinheit[]> {
    return this.http.get<Verwaltungseinheit[]>(this.url).toPromise();
  }

  getGebietskoerperschaften(): Promise<Verwaltungseinheit[]> {
    return this.http.get<Verwaltungseinheit[]>(`${this.url}/gebietskoerperschaften`).toPromise();
  }

  getGemeinden(): Promise<Verwaltungseinheit[]> {
    return this.http.get<Verwaltungseinheit[]>(`${this.url}/gemeinden`).toPromise();
  }

  getKreiseAlsFeatures(): Promise<GeoJSONFeatureCollection> {
    return this.http.get<GeoJSONFeatureCollection>(`${this.url}/kreiseAlsFeatures`).toPromise();
  }

  getOrganisation(id: number): Promise<Verwaltungseinheit> {
    invariant(id, 'Id muss gesetzt sein');
    return this.http.get<Verwaltungseinheit>(`${this.url}/${id}`).toPromise();
  }

  markAsQualitaetsgesichert(id: number): Promise<void> {
    return this.http.post<void>(`${this.url}/markAsQualitaetsgesichert`, { id }).toPromise();
  }

  getBereichVonOrganisationAlsString(id: number | undefined): Promise<string> {
    return this.http.get(`${this.url}/bereichAlsString/${id}`, { responseType: 'text' }).toPromise();
  }

  liegenAlleInQualitaetsgesichertenLandkreisen(kanteIds: number[]): Promise<boolean> {
    return this.http.get<boolean>(`${this.url}/liegenAlleInQualitaetsgesichertenLandkreisen/${kanteIds}`).toPromise();
  }

  getBereichEnvelopeView(id: number | undefined): Promise<VerwaltungseinheitBereichEnvelopeView> {
    return this.http.get<VerwaltungseinheitBereichEnvelopeView>(`${this.url}/bereichEnvelopeView/${id}`).toPromise();
  }
}
