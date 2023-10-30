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
import { Coordinate } from 'ol/coordinate';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { CreateFahrradrouteCommand } from 'src/app/viewer/fahrradroute/models/create-fahrradroute-command';
import { DeleteFahrradrouteCommand } from 'src/app/viewer/fahrradroute/models/delete-fahrradroute-command';
import { FahrradrouteDetailView } from 'src/app/viewer/fahrradroute/models/fahrradroute-detail-view';
import { FahrradrouteListenView } from 'src/app/viewer/fahrradroute/models/fahrradroute-listen-view';
import { RoutingResult } from 'src/app/viewer/fahrradroute/models/routing-result';
import { SaveFahrradrouteCommand } from 'src/app/viewer/fahrradroute/models/save-fahrradroute-command';
import { ChangeFahrradrouteVeroeffentlichtCommand } from 'src/app/viewer/fahrradroute/models/change-fahrradroute-veroeffentlicht-command';
import { DEFAULT_PROFILE_ID } from 'src/app/viewer/fahrradroute/models/custom-routing-profile';

@Injectable({
  providedIn: 'root',
})
export class FahrradrouteService {
  private readonly api = '/api/fahrradroute';

  constructor(private http: HttpClient) {}

  getFahrradroute(id: number): Promise<FahrradrouteDetailView> {
    return this.http.get<FahrradrouteDetailView>(`${this.api}/${id}`).toPromise();
  }

  saveFahrradroute(command: SaveFahrradrouteCommand): Promise<FahrradrouteDetailView> {
    return this.http.post<FahrradrouteDetailView>(`${this.api}/save`, command).toPromise();
  }

  createFahrradroute(command: CreateFahrradrouteCommand): Promise<number> {
    return this.http.post<number>(`${this.api}/create`, command).toPromise();
  }

  deleteFahrradroute(deleteFahrradrouteCommand: DeleteFahrradrouteCommand): Promise<void> {
    return this.http
      .delete<void>(`${this.api}/${deleteFahrradrouteCommand.id}`, { body: deleteFahrradrouteCommand })
      .toPromise();
  }

  updateVeroeffentlicht(command: ChangeFahrradrouteVeroeffentlichtCommand): Promise<FahrradrouteDetailView> {
    return this.http.post<FahrradrouteDetailView>(`${this.api}`, command).toPromise();
  }

  getAll(): Promise<FahrradrouteListenView[]> {
    return this.http.get<FahrradrouteListenView[]>(`${this.api}/list`).toPromise();
  }

  routeFahrradroutenVerlauf(
    stuetzpunkte: Coordinate[],
    customProfileId: number = DEFAULT_PROFILE_ID,
    fahrtrichtungBeruecksichtigen: boolean = true
  ): Promise<RoutingResult> {
    return this.http
      .post<RoutingResult>(
        `${this.api}/routing`,
        {
          type: 'LineString',
          coordinates: stuetzpunkte,
        } as LineStringGeojson,
        {
          params: {
            mitFahrtrichtung: fahrtrichtungBeruecksichtigen,
            customProfileId,
          },
        }
      )
      .toPromise();
  }
}
