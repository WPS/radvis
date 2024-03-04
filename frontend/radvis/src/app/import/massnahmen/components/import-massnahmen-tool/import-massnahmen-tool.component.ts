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
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';

@Component({
  selector: 'rad-import-massnahmen-tool',
  templateUrl: './import-massnahmen-tool.component.html',
  styleUrl: './import-massnahmen-tool.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportMassnahmenToolComponent {
  constructor(
    massnahmenImportService: MassnahmenImportService,
    massnahmenImportRoutingService: MassnahmenImportRoutingService
  ) {
    massnahmenImportService.getImportSession().subscribe(session => {
      if (!session) {
        massnahmenImportRoutingService.navigateToFirst();
        return;
      }

      massnahmenImportRoutingService.navigateToStep(session.schritt);
    });
  }
}