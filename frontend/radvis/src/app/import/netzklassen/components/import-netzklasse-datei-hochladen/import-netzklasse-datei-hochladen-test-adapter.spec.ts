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

export class ImportNetzklasseDateiHochladenTestAdapter {
  constructor(private componentElement: DebugElement) {}

  getWeiterButtonText(): string {
    return (
      (this.componentElement.query(By.css('.weiter-button')).nativeElement as HTMLElement).textContent?.trim() || ''
    );
  }

  isWeiterButtonDisabled(): boolean {
    return (
      (this.componentElement.query(By.css('.weiter-button')).nativeElement as HTMLElement).attributes.getNamedItem(
        'ng-reflect-disabled'
      )?.value === 'true'
    );
  }

  getAbbrechenButtonText(): string {
    return (
      (this.componentElement.query(By.css('.abbrechen-button')).nativeElement as HTMLElement).textContent?.trim() || ''
    );
  }

  isAbbrechenButtonDisabled(): boolean {
    return (
      (this.componentElement.query(By.css('.abbrechen-button')).nativeElement as HTMLElement).attributes.getNamedItem(
        'ng-reflect-disabled'
      )?.value === 'true'
    );
  }
}
