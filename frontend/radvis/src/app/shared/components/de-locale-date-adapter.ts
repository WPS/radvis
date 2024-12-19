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

import { NativeDateAdapter } from '@angular/material/core';
import { Injectable } from '@angular/core';

@Injectable()
export class DeLocaleDateAdapter extends NativeDateAdapter {
  // eslint-disable-next-line prettier/prettier
  override parse(value: string): Date | null {
    const it = value.split('.');
    if (it.length === 3) return new Date(Date.UTC(+it[2], +it[1] - 1, +it[0], 12));
    else return null;
  }

  override getFirstDayOfWeek(): number {
    return 1;
  }
}
