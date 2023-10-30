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

export class ImportAttributeAbschliessenTestAdapter {
  constructor(private component: DebugElement) {}

  doesUebernehmenButtonExist(): boolean {
    const uebernehmenButton = this.component.query(By.css('.buttons'))?.children[2];
    return !!uebernehmenButton && uebernehmenButton.nativeElement.textContent?.trim() === 'Übernehmen';
  }

  isUebernehmenButtonDisabled(): boolean {
    const uebernehmenButton = this.component.query(By.css('.buttons'))?.children[2];
    return !!uebernehmenButton && uebernehmenButton.nativeElement.disabled;
  }

  doesBeendenButtonExist(): boolean {
    const beendenButton = this.component.query(By.css('.buttons'))?.children[0];
    return !!beendenButton && beendenButton.nativeElement.textContent?.trim() === 'Import beenden';
  }

  isBeendenButtonDisabled(): boolean {
    const beendenButton = this.component.query(By.css('.buttons'))?.children[0];
    return !!beendenButton && beendenButton.nativeElement.disabled;
  }

  doesKonflikteViewExists(): boolean {
    const conflictview = this.component.query(By.css('.conflict-detail-view'));
    return !!conflictview;
  }

  pressBackButton(): void {
    const clickEvent = new MouseEvent('click', { bubbles: true });

    const backButton = this.component.query(By.css('.conflict-detail-view')).query(By.css('button')).nativeElement;

    backButton.dispatchEvent(clickEvent);
  }

  doesRowExistInConflict(konfliktIndex: number, key: string, value: string): boolean {
    const conflict = this.component.queryAll(By.css('.konflikt-item'))[konfliktIndex];
    const rows = conflict.queryAll(By.css('tr'));

    return rows.some(row => {
      if (row.nativeElement.hidden) {
        return false;
      }
      const columns = row.queryAll(By.css('td'));
      return (
        columns[0].nativeElement.textContent.trim() === key && columns[1].nativeElement.textContent.trim() === value
      );
    });
  }

  isFehlerShown(fehler: string): boolean {
    const fehlerTexts = this.component.queryAll(By.css('.error > p'));
    return fehlerTexts.some(fehlerText => fehlerText.nativeElement.innerText === fehler);
  }

  doesFehlerExist(): boolean {
    const fehlerTexts = this.component.queryAll(By.css('.error > p'));

    return fehlerTexts.length > 0;
  }
}
