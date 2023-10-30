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
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Leihstation } from 'src/app/viewer/leihstation/models/leihstation';
import { SaveLeihstationCommand } from 'src/app/viewer/leihstation/models/save-leihstation-command';
import { DeleteLeihstationCommand } from 'src/app/viewer/leihstation/models/delete-leihstation-command';

@Injectable({
  providedIn: 'root',
})
export class LeihstationService implements Resolve<Leihstation> {
  private readonly API = '/api/leihstation';

  constructor(private httpClient: HttpClient) {}

  public create(command: SaveLeihstationCommand): Promise<number> {
    return this.httpClient.post<number>(`${this.API}/new`, command).toPromise();
  }

  public save(id: number, command: SaveLeihstationCommand): Promise<Leihstation> {
    return this.httpClient.post<Leihstation>(`${this.API}/${id}`, command).toPromise();
  }

  public delete(id: number, command: DeleteLeihstationCommand): Promise<void> {
    return this.httpClient
      .delete<void>(`${this.API}/${id}`, { body: command })
      .toPromise();
  }

  public getAll(): Promise<Leihstation[]> {
    return this.httpClient.get<Leihstation[]>(`${this.API}`).toPromise();
  }

  // eslint-disable-next-line no-unused-vars
  public resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<Leihstation> {
    const id = route.paramMap.get('id');
    return this.httpClient.get<Leihstation>(`${this.API}/${id}`).toPromise();
  }

  public uploadCsv(file: File): Promise<Blob> {
    const formData = new FormData();
    formData.append('file', file);
    return this.httpClient
      .post<Blob>(`${this.API}`, formData, {
        responseType: 'blob' as 'json',
      })
      .toPromise();
  }
}
