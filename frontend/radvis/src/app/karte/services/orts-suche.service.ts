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
import { Observable, of } from 'rxjs';
import { OrtsSucheResult } from 'src/app/shared/models/orts-suche-result';

@Injectable({
  providedIn: 'root',
})
export class OrtsSucheService {
  constructor(private http: HttpClient) {}

  sucheOrt(suchBegriff: any): Observable<OrtsSucheResult[]> {
    if (!suchBegriff) {
      return of<OrtsSucheResult[]>([]);
    }

    return this.http.get<OrtsSucheResult[]>(`/api/ortssuche`, {
      params: { suchParameter: suchBegriff },
    });
  }
}