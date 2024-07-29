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
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { Observable } from 'rxjs';
import { FileValidationResultView } from 'src/app/import/models/file-validation-result-view';

@Injectable()
export class ImportService {
  public static readonly POLLING_INTERVALL_IN_MILLISECONDS = 2500;
  public static readonly MAX_POLLING_CALLS = 3600000 / ImportService.POLLING_INTERVALL_IN_MILLISECONDS;

  readonly manuellerImportApi = '/api/import/common';

  constructor(protected http: HttpClient) {}

  public existsImportSession(): Observable<boolean> {
    return this.http.get<boolean>(`${this.manuellerImportApi}/exists-session`);
  }

  public deleteImportSession(): Observable<void> {
    return this.http.delete<void>(`${this.manuellerImportApi}/delete-session`);
  }

  private requestValidationResult(file: File): Promise<FileValidationResultView> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<FileValidationResultView>(`${this.manuellerImportApi}/validate-shapefile`, formData)
      .toPromise();
  }

  public validateShapefile(control: AbstractControl): Promise<ValidationErrors | null> {
    if (control.value) {
      return this.requestValidationResult(control.value)
        .then(result => {
          if (result.valid) {
            return null;
          }

          return { fileInvalid: result.reason };
        })
        .catch(() => ({ fileInvalid: 'Fehler beim Prüfen der Datei. Bitte versuchen Sie es später erneut.' }));
    }

    return Promise.resolve(null);
  }
}
