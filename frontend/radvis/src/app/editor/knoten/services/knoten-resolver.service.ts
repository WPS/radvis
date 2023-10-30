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
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { Knoten } from 'src/app/editor/knoten/models/knoten';

@Injectable({
  providedIn: 'root',
})
export class KnotenResolverService implements Resolve<Promise<Knoten>> {
  constructor(private netzService: NetzService) {}

  resolve(route: ActivatedRouteSnapshot): Promise<Knoten> {
    const id = route.paramMap.get('id');
    if (id) {
      return this.netzService.getKnotenForEdit(+id);
    }
    return Promise.reject('ID in der Route nicht gesetzt');
  }
}
