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

import { Injectable } from '@angular/core';
import { Route, Router } from '@angular/router';
import { AbstractInfrastrukturenRoutingService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-routing.service';
import { FAHRRADZAEHLSTELLE } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle.infrastruktur';
import { FahrradzaehlstelleDetailViewComponent } from 'src/app/viewer/fahrradzaehlstelle/components/fahrradzaehlstelle-detail-view/fahrradzaehlstelle-detail-view.component';
import { FahrradzaehlstelleStatistikComponent } from 'src/app/viewer/fahrradzaehlstelle/components/fahrradzaehlstelle-statistik/fahrradzaehlstelle-statistik.component';

@Injectable({
  providedIn: 'root',
})
export class FahrradzaehlstelleRoutingService extends AbstractInfrastrukturenRoutingService {
  public static EIGENSCHAFTEN = 'eigenschaften';
  public static STATISTIK = 'statistik';

  constructor(router: Router) {
    super(router, FAHRRADZAEHLSTELLE);
  }

  public static getChildRoutes(): Route[] {
    return [
      { path: '', redirectTo: this.EIGENSCHAFTEN, pathMatch: 'full' },
      {
        path: this.EIGENSCHAFTEN,
        component: FahrradzaehlstelleDetailViewComponent,
      },
      {
        path: this.STATISTIK,
        component: FahrradzaehlstelleStatistikComponent,
      },
    ];
  }
}
