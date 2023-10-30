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
import { Resolve } from '@angular/router';
import { Observable } from 'rxjs';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';

@Injectable({
  providedIn: 'root',
})
export class BenutzerOrganisationenResolverService implements Resolve<Verwaltungseinheit[]> {
  constructor(private http: HttpClient) {}

  resolve(): Verwaltungseinheit[] | Observable<Verwaltungseinheit[]> | Promise<Verwaltungseinheit[]> {
    return this.http.get<Verwaltungseinheit[]>('/api/benutzer/benutzerorganisationen');
  }
}
