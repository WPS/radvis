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

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { CreateDateiLayerCommand } from 'src/app/viewer/weitere-kartenebenen/models/create-datei-layer-command';
import { DateiLayer } from 'src/app/viewer/weitere-kartenebenen/models/datei-layer';

@Injectable({
  providedIn: 'root',
})
export class DateiLayerService {
  public static readonly API = '/api/datei-layer';
  private _allDateiLayers: BehaviorSubject<DateiLayer[]> = new BehaviorSubject<DateiLayer[]>([]);

  constructor(private http: HttpClient, private fileHandlingService: FileHandlingService) {}

  get allDateiLayers$(): Observable<DateiLayer[]> {
    return this._allDateiLayers.asObservable();
  }

  public refreshDateiLayers(): void {
    this.http
      .get<DateiLayer[]>(`${DateiLayerService.API}/list`)
      .toPromise()
      .then(layers => {
        this._allDateiLayers.next(layers);
      });
  }

  public getMaxFileSizeInMB(): Promise<number> {
    return this.http.get<number>(`${DateiLayerService.API}/max-file-size-mb`).toPromise();
  }

  public create(command: CreateDateiLayerCommand, file: File): Promise<void> {
    const formData = new FormData();
    formData.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
    formData.append('file', file);
    return this.http
      .post<void>(`${DateiLayerService.API}/create`, formData)
      .toPromise()
      .then(() => this.refreshDateiLayers());
  }

  public delete(layerId: number): Promise<void> {
    return this.http
      .delete(`${DateiLayerService.API}/${layerId}/delete`)
      .toPromise()
      .then(() => this.refreshDateiLayers());
  }

  deleteStyle(layerId: number): Promise<void> {
    return this.http
      .delete(`${DateiLayerService.API}/${layerId}/deleteStyle`)
      .toPromise()
      .then(() => this.refreshDateiLayers());
  }

  changeStyle(layerId: number, sldFile: File): Promise<void> {
    const formData = new FormData();
    formData.append('sldFile', sldFile);

    const headers = new HttpHeaders().set(
      'Content-Disposition',
      `form-data; name="sldFile"; filename="${sldFile.name}"`
    );

    return this.http
      .post<void>(`${DateiLayerService.API}/${layerId}/addOrChangeStyle`, formData, { headers })
      .toPromise()
      .then(() => this.refreshDateiLayers());
  }
}
