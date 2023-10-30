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
import { Extent } from 'ol/extent';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Observable } from 'rxjs';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { Signatur } from 'src/app/shared/models/signatur';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class SignaturService {
  private readonly signaturApi = '/api/signaturen';

  constructor(private http: HttpClient) {}

  getStylingForSignatur(signatur: Signatur): Promise<string> {
    invariant(signatur, 'Signatur muss gesetzt sein');
    return this.http
      .get(`${this.signaturApi}/style/${signatur.typ}/${signatur.name}`, { responseType: 'text' })
      .toPromise();
  }

  getFeaturesForView(
    attributnamen: string[],
    extent: Extent,
    selectedNetzklassen: Netzklassefilter[]
  ): Observable<GeoJSONFeatureCollection> {
    return this.http.get<GeoJSONFeatureCollection>(`${this.signaturApi}/geojson`, {
      params: new HttpParams()
        .set('netzklasseFilter', selectedNetzklassen.join())
        .set('attribute', attributnamen.join(','))
        .set('view', extent.toString()),
    });
  }

  getStreckenForNetzklasse(
    attributnamen: string[],
    extent: Extent,
    selectedNetzklassen: Netzklassefilter[]
  ): Observable<GeoJSONFeatureCollection> {
    return this.http.get<GeoJSONFeatureCollection>(`${this.signaturApi}/geojson/netzklassen/strecken`, {
      params: new HttpParams()
        .set('netzklasseFilter', selectedNetzklassen.join())
        .set('attribute', attributnamen.join(','))
        .set('view', extent.toString()),
    });
  }

  getSignaturen(): Observable<Signatur[]> {
    return this.http.get<Signatur[]>(`${this.signaturApi}`);
  }
}
