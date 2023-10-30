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
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import invariant from 'tiny-invariant';
import { WegweisendeBeschilderungListenView } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung-listen-view';
import { WegweisendeBeschilderungService } from 'src/app/viewer/wegweisende-beschilderung/services/wegweisende-beschilderung.service';

@Injectable({
  providedIn: 'root',
})
export class WegweisendeBeschilderungResolver implements Resolve<WegweisendeBeschilderungListenView> {
  constructor(private wegweisendeBeschilderungService: WegweisendeBeschilderungService) {}

  // eslint-disable-next-line no-unused-vars
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<WegweisendeBeschilderungListenView> {
    const idFromRoute = route.paramMap.get('id');
    invariant(idFromRoute);
    return this.wegweisendeBeschilderungService.getWegweisendeBeschilderung(+idFromRoute);
  }
}
