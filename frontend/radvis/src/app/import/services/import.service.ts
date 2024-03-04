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
import { Observable } from 'rxjs';

@Injectable()
export class ImportService {
  public static readonly POLLING_INTERVALL_IN_MILLISECONDS = 2500;
  public static readonly MAX_POLLING_CALLS = 3600000 / ImportService.POLLING_INTERVALL_IN_MILLISECONDS;
  private static readonly MILLISECONDS_IN_HOUR = 3600000;

  readonly manuellerImportApi = '/api/import/common';

  constructor(protected http: HttpClient) {}

  public existsImportSession(): Observable<boolean> {
    return this.http.get<boolean>(`${this.manuellerImportApi}/exists-session`);
  }

  public deleteImportSession(): Observable<void> {
    return this.http.delete<void>(`${this.manuellerImportApi}/delete-session`);
  }
}
