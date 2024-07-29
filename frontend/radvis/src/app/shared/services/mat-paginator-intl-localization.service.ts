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
import { MatPaginatorIntl } from '@angular/material/paginator';

@Injectable({
  providedIn: 'root',
})
export class MatPaginatorIntlLocalizationService extends MatPaginatorIntl {
  // eslint-disable-next-line prettier/prettier
  override itemsPerPageLabel = 'Einträge pro Seite';
  override nextPageLabel = 'Nächste Seite';
  override previousPageLabel = 'Vorherige Seite';
  override firstPageLabel = 'Erste Seite';
  override lastPageLabel = 'Letzte Seite';

  override getRangeLabel: (page: number, pageSize: number, length: number) => string = (
    page: number,
    pageSize: number,
    length: number
  ) => {
    if (length === 0 || pageSize === 0) {
      return `0 von ${length}`;
    }

    const startIndex = page * pageSize;
    const endIndex = Math.min(startIndex + pageSize, length);

    return `${startIndex + 1} – ${endIndex} von ${length}`;
  };
}
