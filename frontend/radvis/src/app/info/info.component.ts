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

import { AfterViewInit, ChangeDetectionStrategy, Component } from '@angular/core';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';

@Component({
  selector: 'rad-info',
  templateUrl: './info.component.html',
  styleUrls: ['./info.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class InfoComponent implements AfterViewInit {
  // workaround for https://github.com/angular/components/issues/14759
  disableAnimation = true;

  constructor(
    private manualRoutingService: ManualRoutingService,
    private featureTogglzService: FeatureTogglzService
  ) {}

  ngAfterViewInit(): void {
    setTimeout(() => (this.disableAnimation = false));
  }

  openManualWeitereHilfe(): void {
    this.manualRoutingService.openManualWeitereHilfe();
  }

  get isDefaultVersionsinfoDialogToggleOn(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_DEFAULT_VERSIONSINFO_DIALOG);
  }
}
