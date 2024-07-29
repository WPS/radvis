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
import { MassnahmenDateianhaengeImportProtokollStats } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-protokoll-stats';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';

@Component({
  selector: 'rad-massnahmen-dateianhaenge-fehlerprotokoll-herunterladen',
  templateUrl: './massnahmen-dateianhaenge-fehlerprotokoll-herunterladen.component.html',
  styleUrl: './massnahmen-dateianhaenge-fehlerprotokoll-herunterladen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MassnahmenDateianhaengeFehlerprotokollHerunterladenComponent {
  protokollStats?: MassnahmenDateianhaengeImportProtokollStats;

  constructor(
    private service: MassnahmenDateianhaengeService,
    private routingService: MassnahmenDateianhaengeRoutingService,
    private fileHandlingService: FileHandlingService,
    changeDetector: ChangeDetectorRef
  ) {
    service.getProtokollStats().subscribe(protokoll => {
      this.protokollStats = protokoll;
      changeDetector.markForCheck();
    });
  }

  onDownloadFehlerProtokoll(): void {
    this.service
      .downloadFehlerprotokoll()
      .subscribe(blob => this.fileHandlingService.downloadInBrowser(blob, 'import-protokoll.csv'));
  }

  onDone(): void {
    this.service.deleteImportSession().subscribe(() => {
      this.routingService.navigateToFirst();
    });
  }
}
