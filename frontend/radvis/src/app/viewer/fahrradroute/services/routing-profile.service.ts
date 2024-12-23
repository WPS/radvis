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
import { CustomRoutingProfile } from 'src/app/viewer/fahrradroute/models/custom-routing-profile';
import { SaveCustomRoutingProfileCommand } from 'src/app/viewer/fahrradroute/models/save-custom-routing-profile-command';

@Injectable({
  providedIn: 'root',
})
export class RoutingProfileService {
  private readonly api = '/api/custom-routing-profile';

  private _profiles: BehaviorSubject<CustomRoutingProfile[]> = new BehaviorSubject<CustomRoutingProfile[]>([]);

  constructor(private http: HttpClient) {}

  get profiles$(): Observable<CustomRoutingProfile[]> {
    return this._profiles.asObservable();
  }

  public initCustomRoutingProfiles(): Promise<void> {
    return this.http
      .get<CustomRoutingProfile[]>(`${this.api}/list`)
      .toPromise()
      .then(profiles => this._profiles.next(profiles));
  }

  save(commands: SaveCustomRoutingProfileCommand[]): Promise<void> {
    return this.http
      .post<CustomRoutingProfile[]>(`${this.api}/save`, commands)
      .toPromise()
      .then(profiles => this._profiles.next(profiles));
  }
}
