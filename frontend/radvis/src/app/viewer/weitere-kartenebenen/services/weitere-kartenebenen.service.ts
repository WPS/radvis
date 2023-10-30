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
import { BehaviorSubject, Observable } from 'rxjs';
import { SaveWeitereKartenebeneCommand } from 'src/app/viewer/weitere-kartenebenen/models/save-weitere-kartenebene-command';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class WeitereKartenebenenService {
  public static readonly API = '/api/weitere-kartenebenen';
  private _selectedWeitereKartenebenen: BehaviorSubject<WeitereKartenebene[]> = new BehaviorSubject<
    WeitereKartenebene[]
  >([]);

  private _weitereKartenebenen: BehaviorSubject<WeitereKartenebene[]> = new BehaviorSubject<WeitereKartenebene[]>([]);

  constructor(private http: HttpClient) {}

  get weitereKartenebenen(): WeitereKartenebene[] {
    return this._weitereKartenebenen.value;
  }

  get weitereKartenebenen$(): Observable<WeitereKartenebene[]> {
    return this._weitereKartenebenen.asObservable();
  }

  get selectedWeitereKartenebenen$(): Observable<WeitereKartenebene[]> {
    return this._selectedWeitereKartenebenen.asObservable();
  }

  public initWeitereKartenebenen(): Promise<void> {
    return this.http
      .get<WeitereKartenebene[]>(`${WeitereKartenebenenService.API}/list`)
      .toPromise()
      .then((layers: WeitereKartenebene[]) => {
        this._weitereKartenebenen.next(layers);
        // Selektier erneut, was schon selektiert war und entfernt darüber auch entfernte Layer, da die Layer-
        // Komponenten ggf. abgebaut werden, wenn nicht mehr vorhanden und sich damit von der Karte nehmen.
        this._selectedWeitereKartenebenen.next([
          ...layers.filter(l => this._selectedWeitereKartenebenen.value.map(sl => sl.id).includes(l.id)),
        ]);
      });
  }

  public toggleLayerSelection(weitereKartenebenen: WeitereKartenebene): void {
    invariant(this._weitereKartenebenen.value.includes(weitereKartenebenen), 'Selektierter Layer existiert nicht');
    let value = this._selectedWeitereKartenebenen.value;
    if (value.includes(weitereKartenebenen)) {
      value = value.filter(SL => SL !== weitereKartenebenen);
    } else {
      // Preserve Order
      value = this._weitereKartenebenen.value.filter(l => value.includes(l) || l === weitereKartenebenen);
    }

    this._selectedWeitereKartenebenen.next(value);
  }

  public save(commands: SaveWeitereKartenebeneCommand[]): Promise<void> {
    return this.http
      .post<WeitereKartenebene[]>(`${WeitereKartenebenenService.API}/save`, commands)
      .toPromise()
      .then((layers: WeitereKartenebene[]) => {
        this._weitereKartenebenen.next(layers);
        this._selectedWeitereKartenebenen.next([
          // Selektier erneut, was schon selektiert war
          ...layers.filter(l => this._selectedWeitereKartenebenen.value.map(sl => sl.id).includes(l.id)),
          // Selektier neue Layer (neue ids, waren vor der Speicherung nicht vorhanden)
          ...layers.filter(l => !commands.map(c => c.id).includes(l.id)),
        ]);
      });
  }
}
