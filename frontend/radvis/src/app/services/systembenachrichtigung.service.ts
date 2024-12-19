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
import { Systemnachricht } from 'src/app/components/systembenachrichtigung/systembenachrichtigung.component';

@Injectable({
  providedIn: 'root',
})
export class SystembenachrichtigungService {
  private readonly API = '/api/systemnachricht';
  constructor(private http: HttpClient) {
    ((window || globalThis) as any).createSystemnachricht = this.create;
    ((window || globalThis) as any).deleteSystemnachricht = this.delete;
  }

  private create = (text: string): void => {
    console.info('Systemnachricht wird erstellt, bitte warten ...');
    this.http
      .post(this.API, { text })
      .toPromise()
      .then(() => {
        console.info('Systemnachricht erfolgreich erstellt');
      });
  };

  private delete = (): void => {
    console.info('Systemnachricht wird gelöscht, bitte warten ...');
    this.http
      .delete(this.API)
      .toPromise()
      .then(() => {
        console.info('Systemnachricht erfolgreich gelöscht');
      });
  };

  fetch(): Promise<Systemnachricht | null> {
    return this.http.get<Systemnachricht | null>(this.API).toPromise();
  }
}
