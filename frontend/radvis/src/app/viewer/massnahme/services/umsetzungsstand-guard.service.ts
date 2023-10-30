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
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';

@Injectable({ providedIn: 'root' })
export class UmsetzungsstandGuardService implements CanActivate {
  constructor(
    private massnahmeService: MassnahmeService,
    private router: Router,
    private massnahmenRoutingService: MassnahmenRoutingService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    const id = Number(route.parent?.paramMap.get('id'));
    if (isNaN(id)) {
      return false;
    }
    return this.massnahmeService.hasUmsetzungstand(id).then(hasUmsetzungsstand => {
      if (hasUmsetzungsstand) {
        return hasUmsetzungsstand;
      } else {
        return this.router.createUrlTree(this.massnahmenRoutingService.getEigenschaftenRoute(id), {
          queryParams: route.queryParams,
        });
      }
    });
  }
}
