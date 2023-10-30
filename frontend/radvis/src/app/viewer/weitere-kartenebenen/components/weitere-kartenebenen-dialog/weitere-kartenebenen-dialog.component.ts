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

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';

@Component({
  selector: 'rad-weitere-kartenebenen-dialog',
  templateUrl: './weitere-kartenebenen-dialog.component.html',
  styleUrls: ['./weitere-kartenebenen-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeitereKartenebenenDialogComponent {
  public showLayerAusDateiVerwaltung: boolean;

  constructor(
    private benutzerDetailsService: BenutzerDetailsService,
    private featureTogglzService: FeatureTogglzService
  ) {
    this.showLayerAusDateiVerwaltung =
      this.benutzerDetailsService.showLayerAusDateiVerwaltung() &&
      this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_DATEILAYER_HOCHLADEN_ANZEIGEN);
  }
}
