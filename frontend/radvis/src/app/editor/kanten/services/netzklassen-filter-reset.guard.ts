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
import { ActivatedRouteSnapshot, CanDeactivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { KantenCreatorComponent } from 'src/app/editor/kanten/components/kanten-creator/kanten-creator.component';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';

export const netzklassenFilterResetGuard: CanDeactivateFn<KantenCreatorComponent> = (
  component: KantenCreatorComponent,
  currentRoute: ActivatedRouteSnapshot,
  currentState: RouterStateSnapshot,
  nextState: RouterStateSnapshot
) => {
  const mapQueryParamsService = inject(MapQueryParamsService);
  const router = inject(Router);
  const urlTree = router.parseUrl(nextState.url);
  const targetNetzklassenFilter = MapQueryParams.fromRoute(urlTree.queryParams).netzklassen;
  if (
    targetNetzklassenFilter.length === component.previousNetzklassenfilter.length &&
    component.previousNetzklassenfilter.every(n => targetNetzklassenFilter.includes(n))
  ) {
    return true;
  }
  urlTree.queryParams = mapQueryParamsService
    .updateQueryParams({ netzklassen: component.previousNetzklassenfilter }, currentRoute, true)
    .replaceIn(urlTree.queryParams);

  return urlTree;
};
