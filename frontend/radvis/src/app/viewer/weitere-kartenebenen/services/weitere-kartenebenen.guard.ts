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
import { CanActivateFn, Router } from '@angular/router';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import { WeitereKartenebenenRoutingService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen-routing.service';

export const weitereKartenebenenGuard: CanActivateFn = () => {
  const service: WeitereKartenebenenRoutingService = inject(WeitereKartenebenenRoutingService);
  const router: Router = inject(Router);
  const notifyUserService: NotifyUserService = inject(NotifyUserService);
  if (service.currentFeature !== null) {
    return true;
  }
  notifyUserService.inform('Gewähltes Feature nicht mehr verfügbar.');
  return router.createUrlTree([VIEWER_ROUTE]);
};
