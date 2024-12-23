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

import { ActivatedRouteSnapshot, Route } from '@angular/router';
import { KantenDetailViewComponent } from 'src/app/viewer/netz-details/components/kanten-detail-view/kanten-detail-view.component';
import { KnotenDetailViewComponent } from 'src/app/viewer/netz-details/components/knoten-detail-view/knoten-detail-view.component';
import { kanteDetailsResolver } from 'src/app/viewer/netz-details/services/kante-details.resolver';
import { knotenDetailsResolver } from 'src/app/viewer/netz-details/services/knoten-details.resolver';
import { NetzdetailRoutingService } from 'src/app/viewer/netz-details/services/netzdetail-routing.service';

export const NETZ_DETAIL_ROUTES: Route[] = [
  {
    path: NetzdetailRoutingService.KANTE + '/:id',
    component: KantenDetailViewComponent,
    resolve: { kante: kanteDetailsResolver },
    runGuardsAndResolvers: (from: ActivatedRouteSnapshot, to: ActivatedRouteSnapshot): boolean => {
      return (
        from.paramMap.get('id') !== to.paramMap.get('id') ||
        from.queryParamMap.get('seite') !== to.queryParamMap.get('seite') ||
        from.queryParamMap.get('position') !== to.queryParamMap.get('position')
      );
    },
  },
  {
    path: NetzdetailRoutingService.KNOTEN + '/:id',
    component: KnotenDetailViewComponent,
    resolve: { knoten: knotenDetailsResolver },
  },
];
