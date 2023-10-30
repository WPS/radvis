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
import { Kante } from 'src/app/editor/kanten/models/kante';
import { ChangeZuordnungCommand } from 'src/app/radnetzmatching/models/change-zuordnung-command';
import { DeleteZuordnungCommand } from 'src/app/radnetzmatching/models/delete-zuordnung-command';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class ZuordnungService {
  private readonly zuordnungApi = '/api/zuordnung';

  constructor(private http: HttpClient) {}

  changeZuordnung(command: ChangeZuordnungCommand): Promise<Kante[]> {
    invariant(command, 'Kante muss gesetzt sein');
    return this.http.post<Kante[]>(`${this.zuordnungApi}/changeZuordnung`, command).toPromise();
  }

  getZuordnungRadnetz(radNetzKanteId: number): Promise<number[]> {
    return this.http.get<number[]>(`${this.zuordnungApi}/radnetz/${radNetzKanteId}`).toPromise();
  }

  getZuordnungDlm(dlmKanteId: number): Promise<number[]> {
    return this.http.get<number[]>(`${this.zuordnungApi}/dlm/${dlmKanteId}`).toPromise();
  }

  deleteZuordnung(command: DeleteZuordnungCommand): Promise<void> {
    invariant(command, 'Kante muss gesetzt sein');
    return this.http.post<void>(`${this.zuordnungApi}/loescheZuordnung`, command).toPromise();
  }
}
