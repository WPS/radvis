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

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Coordinate } from 'ol/coordinate';
import { RadVisFeatureAttribut } from 'src/app/shared/models/rad-vis-feature-attribut';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class NetzFeatureDetailsService {
  public static BASE_URL = '/api/netz-feature-details';

  constructor(private http: HttpClient) {}

  /**
   * @deprecated Wird entfernt sobald RadNETZ Matching entfernt wird
   */
  getAttributeFuerKante(id: number, clickposition: Coordinate): Promise<RadVisFeatureAttribut[]> {
    invariant(id, 'Id muss gesetzt sein');

    let params = new HttpParams();
    params = params.append('position', clickposition.toString());

    return this.http
      .get<RadVisFeatureAttribut[]>(`${NetzFeatureDetailsService.BASE_URL}/kante-feature-details/${id}`, { params })
      .toPromise();
  }
}
