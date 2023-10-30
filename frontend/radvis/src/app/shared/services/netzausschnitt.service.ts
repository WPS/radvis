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
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';

@Injectable({
  providedIn: 'root',
})
export class NetzausschnittService {
  public static BASE_URL = '/api/netzausschnitt';

  constructor(private http: HttpClient) {}

  getKantenForView(
    extent: Extent,
    netzklassen: Netzklassefilter[],
    mitVerlauf: boolean
  ): Observable<GeoJSONFeatureCollection> {
    return this.http.get<GeoJSONFeatureCollection>(NetzausschnittService.BASE_URL + '/kanten?view=' + extent, {
      params: new HttpParams().set('netzklasseFilter', netzklassen.join()).set('mitVerlauf', Boolean(mitVerlauf)),
    });
  }

  getAlleRadNETZKantenForView(mitVerlauf: boolean): Observable<GeoJSONFeatureCollection> {
    return this.http.get<GeoJSONFeatureCollection>(NetzausschnittService.BASE_URL + '/alleRadNETZStrecken', {
      params: new HttpParams().set('mitVerlauf', Boolean(mitVerlauf)),
    });
  }

  getKnotenForView(extent: Extent, netzklassen: Netzklassefilter[]): Observable<GeoJSONFeatureCollection> {
    return this.http.get<GeoJSONFeatureCollection>(NetzausschnittService.BASE_URL + '/knoten?view=' + extent, {
      params: new HttpParams().set('netzklasseFilter', netzklassen.join()),
    });
  }

  getKantenFuerZustaendigkeitsbereich(
    zustaendigeOrganisation: number,
    hasNetzklasse?: Netzklasse
  ): Promise<GeoJSONFeatureCollection> {
    let params = new HttpParams();

    if (hasNetzklasse) {
      params = params.append('hasNetzklasse', hasNetzklasse);
    }

    return this.http
      .get<GeoJSONFeatureCollection>(
        `${NetzausschnittService.BASE_URL}/kanten-in-organisationsbereich/${zustaendigeOrganisation}`,
        {
          params,
        }
      )
      .toPromise();
  }
}
