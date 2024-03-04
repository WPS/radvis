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
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ImportRoutes } from 'src/app/import/models/import-routes';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';

export const netzklassenSessionExistsGuard: CanActivateFn = (): Observable<boolean | UrlTree> => {
  const router: Router = inject(Router);
  const netzklassenImportService: NetzklassenImportService = inject(NetzklassenImportService);

  return netzklassenImportService.getImportSession().pipe(
    map(session => {
      if (!session) {
        return router.createUrlTree(
          [
            ImportRoutes.IMPORT_ROUTE,
            ImportRoutes.NETZKLASSEN_IMPORT_ROUTE,
            NetzklassenRoutingService.DATEI_UPLOAD_ROUTE,
          ],
          { queryParamsHandling: 'merge' }
        );
      }

      return true;
    })
  );
};
