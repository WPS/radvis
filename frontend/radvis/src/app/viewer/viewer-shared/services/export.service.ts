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
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';

@Injectable({
  providedIn: 'root',
})
export class ExportService {
  private readonly api = '/api/export';

  constructor(
    private http: HttpClient,
    private fileHandlingService: FileHandlingService,
    private errorHandlingService: ErrorHandlingService,
    private notifyUserService: NotifyUserService
  ) {}

  public exportInfrastruktur(
    typ: string,
    format: ExportFormat,
    filteredIds: number[],
    fieldsToExclude: string[] = []
  ): Promise<void> {
    return this.http
      .post<Blob>(
        `${this.api}/${format}/infrastruktur/${typ}`,
        { ids: filteredIds, fieldsToExclude },
        {
          responseType: 'blob' as 'json',
          observe: 'response',
        }
      )
      .toPromise()
      .then(res => {
        if (res.body) {
          const filename = res.headers.get('content-disposition')?.split('=')[1] ?? '';
          try {
            this.fileHandlingService.downloadInBrowser(res.body, filename);
          } catch (err) {
            this.notifyUserService.warn('Die heruntergeladene Datei konnte nicht geöffnet werden');
          }
        } else {
          this.notifyUserService.warn('Die heruntergeladene Datei konnte nicht geöffnet werden');
        }
      })
      .catch(err => this.errorHandlingService.handleError(err, 'Die Datei konnte nicht heruntergeladen werden'));
  }
}
