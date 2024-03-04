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
import { KanteDetailView } from 'src/app/shared/models/kante-detail-view';
import { NetzdetailService } from 'src/app/viewer/netz-details/services/netzdetail.service';
import invariant from 'tiny-invariant';

export const kanteDetailsResolver: ResolveFn<KanteDetailView> = (route, state) => {
  const netzService: NetzdetailService = inject(NetzdetailService);
  const id = route.paramMap.get('id');
  const clickposition = route.queryParamMap.get('position');
  const selektierteSeite = route.queryParamMap.get('seite') ?? '';
  invariant(id, 'Kante-ID muss als Parameter id an der Route gesetzt sein.');
  return netzService.getKanteForView(+id, clickposition, selektierteSeite);
};
