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
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { KanteDetailView } from 'src/app/shared/models/kante-detail-view';
import { NetzdetailService } from 'src/app/viewer/netz-details/services/netzdetail.service';

@Injectable({
  providedIn: 'root',
})
export class KanteDetailsResolverService implements Resolve<KanteDetailView> {
  constructor(private netzService: NetzdetailService) {}

  public static runGuardsAndResolvers(from: ActivatedRouteSnapshot, to: ActivatedRouteSnapshot): boolean {
    return (
      from.paramMap.get('id') !== to.paramMap.get('id') ||
      from.queryParamMap.get('seite') !== to.queryParamMap.get('seite') ||
      from.queryParamMap.get('position') !== to.queryParamMap.get('position')
    );
  }

  resolve(route: ActivatedRouteSnapshot): Promise<KanteDetailView> {
    const id = route.paramMap.get('id');
    const clickposition = route.queryParamMap.get('position');
    const selektierteSeite = route.queryParamMap.get('seite') ?? '';
    if (id) {
      return this.netzService.getKanteForView(+id, clickposition, selektierteSeite);
    }
    return Promise.reject('ID in der Route nicht gesetzt');
  }
}
