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
import { ResolveFn } from '@angular/router';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { Knoten } from 'src/app/editor/knoten/models/knoten';

export const knotenResolver: ResolveFn<Knoten> = (route, state) => {
  const netzService: NetzService = inject(NetzService);
  const id = route.paramMap.get('id');
  if (id) {
    return netzService.getKnotenForEdit(+id);
  }
  return Promise.reject(new Error('ID in der Route nicht gesetzt'));
};
