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
import { FahrradrouteImportprotokoll } from 'src/app/viewer/importprotokolle/models/fahrradroute-importprotokoll';
import { ImportprotokollService } from 'src/app/viewer/importprotokolle/services/importprotokoll.service';
import invariant from 'tiny-invariant';

export const fahrradrouteImportprotokollResolver: ResolveFn<FahrradrouteImportprotokoll> = (route, state) => {
  const importprotokollService: ImportprotokollService = inject(ImportprotokollService);
  const id = route.paramMap.get('id');
  invariant(id);
  return importprotokollService.getFahrradrouteImportprotokoll(+id);
};
