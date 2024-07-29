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
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { FehlerprotokollView } from 'src/app/fehlerprotokoll/models/fehlerprotokoll-view';
import { Konsistenzregel } from 'src/app/shared/models/konsistenzregel';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

@Injectable({
  providedIn: 'root',
})
export class KonsistenzregelService {
  private readonly api = '/api/konsistenzregel';
  private readonly MAX_ANZAHL_VERLETZUNGEN = 300;

  constructor(
    private http: HttpClient,
    private notifyUserService: NotifyUserService
  ) {}

  public getAllKonsistenzRegel(): Observable<Konsistenzregel[]> {
    return this.http.get<Konsistenzregel[]>(`${this.api}/list`);
  }

  public getAlleVerletzungenForTypen(typen: string[], view?: Extent): Observable<FehlerprotokollView[]> {
    let params = new HttpParams().set('typen', typen.join(','));
    if (view) {
      params = params.set('view', view?.join(','));
    }

    return this.http.get<FehlerprotokollView[]>(`${this.api}/verletzung/list`, { params }).pipe(
      tap(result => {
        if (result.length === this.MAX_ANZAHL_VERLETZUNGEN) {
          this.notifyUserService.warn(
            'Die Anzahl der dargestellten Konsistenzregel-Verletzungen wurde beschränkt. Für den vollständigen Datensatz nutzen Sie bitte den WFS-Dienst (s. Handbuch).'
          );
        }
      })
    );
  }
}
