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

import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

export class ImportMassnahmenAttributfehlerUeberpruefenTestAdapter {
  constructor(private component: DebugElement) {}

  get numberOfRows(): number {
    return this.component.queryAll(By.css('tr')).length - 1;
  }

  get attributes(): string[] {
    return this.entriesInColumn(3);
  }

  get fehler(): string[] {
    return this.entriesInColumn(4);
  }

  get ids(): string[] {
    return this.entriesInColumn(2);
  }

  private entriesInColumn(colNum: number): string[] {
    return this.component
      .queryAll(By.css(`td:nth-child(${colNum})`))
      .filter(dE => dE.styles.display !== 'none')
      .map(dE => dE.nativeElement.innerText);
  }
}
