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
import { Router } from '@angular/router';
import { BARRIEREN } from 'src/app/viewer/barriere/models/barriere.infrastruktur';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import { AbstractInfrastrukturenRoutingService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-routing.service';

@Injectable({
  providedIn: 'root',
})
export class BarriereRoutingService extends AbstractInfrastrukturenRoutingService {
  public static CREATOR_ROUTE = 'new';

  constructor(router: Router) {
    super(router, BARRIEREN);
  }

  public toCreator(): void {
    this.router.navigate([VIEWER_ROUTE, BARRIEREN.pathElement, BarriereRoutingService.CREATOR_ROUTE], {
      queryParamsHandling: 'merge',
    });
  }
}
