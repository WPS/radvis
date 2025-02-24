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
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';

@Component({
  selector: 'rad-massnahmen-dateianhaenge-tool',
  templateUrl: './massnahmen-dateianhaenge-tool.component.html',
  styleUrl: './massnahmen-dateianhaenge-tool.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class MassnahmenDateianhaengeToolComponent {
  constructor(
    dateianhaengeService: MassnahmenDateianhaengeService,
    routingService: MassnahmenDateianhaengeRoutingService
  ) {
    dateianhaengeService.getImportSession().subscribe(session => {
      if (!session) {
        routingService.navigateToFirst();
        return;
      }
      routingService.navigateToStep(session.schritt);
    });
  }
}
