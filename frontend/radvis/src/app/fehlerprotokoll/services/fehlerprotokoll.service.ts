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
import { Observable, of } from 'rxjs';
import { FehlerprotokollTyp } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-typ';
import { FehlerprotokollView } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-view';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { GeoserverService } from 'src/app/shared/services/geoserver.service';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class FehlerprotokollService {
  private readonly api = '/api/fehlerprotokoll';

  private readonly geoserverApi = '/api/geoserver/saml/radvis/wfs';

  constructor(private http: HttpClient, private geoserverService: GeoserverService) {}

  public getFehlerprotokolle(selectedTypen: FehlerprotokollTyp[], view?: Extent): Observable<FehlerprotokollView[]> {
    if (selectedTypen.length === 0) {
      return of([]);
    }

    let params = new HttpParams().set('selectedTypen', selectedTypen.map(t => t.name).join());
    if (view) {
      params = params.set('view', view?.join(','));
    }

    return this.http.get<FehlerprotokollView[]>(`${this.api}/list`, {
      params,
    });
  }

  public getFehlerFromManuellerImport(
    organisation: Verwaltungseinheit,
    includeNetzklassenImport: boolean,
    includeAttributeImport: boolean,
    view?: Extent
  ): Observable<FehlerprotokollView[]> {
    invariant(organisation);
    if (!includeAttributeImport && !includeNetzklassenImport) {
      return of([]);
    }

    let params = new HttpParams()
      .set('organisation', organisation.id)
      .set('includeNetzklassenImport', includeNetzklassenImport)
      .set('includeAttributeImport', includeAttributeImport);
    if (view) {
      params = params.set('view', view?.join(','));
    }

    return this.http.get<FehlerprotokollView[]>(`${this.api}/manuellerImport`, {
      params,
    });
  }

  public getFehlerprotokollDownloadLink(organisation: Verwaltungseinheit, typ: 'netzklasse' | 'attribute'): string {
    const httpParams = this.geoserverService
      .getWFSParams('shape-zip')
      .set('typeName', `radvis:manueller_import_fehler_${typ}`)
      .set('CQL_FILTER', `organisation LIKE '${organisation.name}'`);

    return `${GeoserverService.GEOSERVER_API}?${httpParams.toString()}`;
  }
}
