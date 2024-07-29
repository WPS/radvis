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
import { MassnahmenImportProtokollStats } from 'src/app/import/massnahmen/models/massnahmen-import-protokoll-stats';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';

@Component({
  selector: 'rad-import-massnahmen-fehlerprotokoll-herunterladen',
  templateUrl: './import-massnahmen-fehlerprotokoll-herunterladen.component.html',
  styleUrl: './import-massnahmen-fehlerprotokoll-herunterladen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportMassnahmenFehlerprotokollHerunterladenComponent {
  private static readonly STEP = 5;
  protokollStats?: MassnahmenImportProtokollStats;

  constructor(
    private massnahmenImportService: MassnahmenImportService,
    private massnahmenImportRoutingService: MassnahmenImportRoutingService,
    private fileHandlingService: FileHandlingService,
    changeDetector: ChangeDetectorRef
  ) {
    massnahmenImportService.getProtokollStats().subscribe(protokoll => {
      this.protokollStats = protokoll;
      changeDetector.markForCheck();
    });
  }

  onDownloadFehlerProtokoll(): void {
    this.massnahmenImportService
      .downloadFehlerprotokoll()
      .subscribe(blob => this.fileHandlingService.downloadInBrowser(blob, 'import-protokoll.csv'));
  }

  onDone(): void {
    this.massnahmenImportService.deleteImportSession().subscribe(() => {
      this.massnahmenImportRoutingService.navigateToFirst();
    });
  }
}
