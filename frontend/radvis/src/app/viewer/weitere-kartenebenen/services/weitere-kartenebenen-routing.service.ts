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

/* eslint-disable no-unused-vars */
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Resolve, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class WeitereKartenebenenRoutingService implements Resolve<RadVisFeature>, CanActivate {
  public static ROUTE_LAYER = 'extern';
  public static ROUTE_FEATURE = 'detail';

  private currentFeature: RadVisFeature | null = null;

  constructor(private router: Router, private notifyUserService: NotifyUserService) {}

  public canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean | UrlTree | Observable<boolean | UrlTree> | Promise<boolean | UrlTree> {
    if (this.currentFeature !== null) {
      return true;
    }
    this.notifyUserService.inform('Gewähltes Feature nicht mehr verfügbar.');
    return this.router.createUrlTree([VIEWER_ROUTE]);
  }

  public resolve(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): RadVisFeature | Observable<RadVisFeature> | Promise<RadVisFeature> {
    invariant(this.currentFeature);
    return this.currentFeature;
  }

  public routeToFeature(feature: RadVisFeature): void {
    this.currentFeature = feature;
    const layerId = feature.attribute.find(attr => attr.key === WeitereKartenebene.LAYER_ID_KEY)?.value;
    const id =
      feature.id ||
      feature.attribute.find(attr => attr.key === WeitereKartenebene.EXTERNE_WMS_FEATURE_ID_PROPERTY_NAME)?.value;
    this.router.navigate(
      [
        VIEWER_ROUTE,
        WeitereKartenebenenRoutingService.ROUTE_LAYER,
        layerId,
        WeitereKartenebenenRoutingService.ROUTE_FEATURE,
        id,
      ],
      { queryParamsHandling: 'merge' }
    );
  }
}