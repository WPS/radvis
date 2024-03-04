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

import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';

export const umsetzungsstandGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
): Observable<boolean | UrlTree> | boolean => {
  const massnahmeService: MassnahmeService = inject(MassnahmeService);
  const router: Router = inject(Router);
  const massnahmenRoutingService: MassnahmenRoutingService = inject(MassnahmenRoutingService);

  const id = Number(route.parent?.paramMap.get('id'));
  if (isNaN(id)) {
    return false;
  }

  return massnahmeService.hasUmsetzungstand(id).pipe(
    map(hasUmsetzungsstand => {
      if (hasUmsetzungsstand) {
        return hasUmsetzungsstand;
      } else {
        return router.createUrlTree(massnahmenRoutingService.getEigenschaftenRoute(id), {
          queryParams: route.queryParams,
        });
      }
    })
  );
};
