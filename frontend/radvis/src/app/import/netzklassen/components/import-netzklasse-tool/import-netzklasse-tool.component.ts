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
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';

@Component({
  selector: 'rad-import-netzklasse-tool',
  templateUrl: './import-netzklasse-tool.component.html',
  styleUrl: './import-netzklasse-tool.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportNetzklasseToolComponent {
  constructor(
    netzklassenImportService: NetzklassenImportService,
    netzklassenImportRoutingService: NetzklassenRoutingService
  ) {
    netzklassenImportService.getImportSession().subscribe(session => {
      if (!session) {
        netzklassenImportRoutingService.navigateToFirst();
        return;
      }

      netzklassenImportRoutingService.navigateToStep(session.schritt);
    });
  }
}
