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
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { SystembenachrichtigungService } from 'src/app/services/systembenachrichtigung.service';
export interface Systemnachricht {
  vom: Date;
  text: string;
}
@Component({
  selector: 'rad-systembenachrichtigung',
  templateUrl: './systembenachrichtigung.component.html',
  styleUrl: './systembenachrichtigung.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SystembenachrichtigungComponent {
  protected readonly WIRD_GELADEN = 'wird geladen ...';
  protected readonly KEINE_NACHRICHT = 'Keine Systemnachricht vorhanden';
  systemnachricht: Systemnachricht | null = null;
  systemnachrichtVisible = false;
  loading: boolean;

  constructor(systembenachrichtigungService: SystembenachrichtigungService, changeDetector: ChangeDetectorRef) {
    this.loading = true;
    systembenachrichtigungService
      .fetch()
      .then(systemnachricht => {
        this.systemnachricht = systemnachricht;
        this.systemnachrichtVisible = this.systemnachrichtVisible || Boolean(systemnachricht);
      })
      .finally(() => {
        this.loading = false;
        changeDetector.markForCheck();
      });
  }
}
