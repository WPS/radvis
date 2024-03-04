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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { MassnahmeUmsetzungsstandDialogComponent } from 'src/app/viewer/massnahme/components/massnahme-umsetzungsstand-dialog/massnahme-umsetzungsstand-dialog.component';
import { UmsetzungsstandStatus } from 'src/app/viewer/massnahme/models/umsetzungsstand-status';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';

@Component({
  selector: 'rad-massnahme-umsetzungsstand-button-menu',
  templateUrl: './massnahme-umsetzungsstand-button-menu.component.html',
  styleUrls: ['./massnahme-umsetzungsstand-button-menu.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MassnahmeUmsetzungsstandButtonMenuComponent {
  @Input()
  isMenuItem = false;

  @Input()
  canStartAbfrage = false;

  @Input()
  canEvaluateAbfragen = false;

  loading = false;

  constructor(
    private dialog: MatDialog,
    private massnahmeService: MassnahmeService,
    private massnahmeFilterService: MassnahmeFilterService,
    private fileHandlingService: FileHandlingService,
    private notifyUserService: NotifyUserService,
    private errorHandlingService: ErrorHandlingService,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  showConfirmationDialog(): void {
    const dialogRef = this.dialog.open(MassnahmeUmsetzungsstandDialogComponent, {
      width: '24rem',
    });
    dialogRef.afterClosed().subscribe(abfrageStarten => {
      if (abfrageStarten) {
        const filteredMassnahmeIds = this.massnahmeFilterService.currentFilteredList.map(m => m.id);
        this.loading = true;
        this.changeDetectorRef.markForCheck();
        this.massnahmeService
          .starteUmsetzungsstandsabfrage(filteredMassnahmeIds)
          .then(() => this.notifyUserService.inform('Umsetzungsstandsabfrage erfolgreich gestartet.'))
          .finally(() => {
            this.loading = false;
            this.changeDetectorRef.markForCheck();
            this.massnahmeFilterService.refetchData();
          });
      }
    });
  }

  downloadAuswertung(): void {
    this.massnahmeService
      .auswertungHerunterladen(this.massnahmeFilterService.currentFilteredList.map(massnahme => massnahme.id))
      .then(res => {
        if (res.body) {
          const filename = res.headers.get('content-disposition')?.split('=')[1] ?? '';
          try {
            this.fileHandlingService.downloadInBrowser(res.body, filename);
          } catch (err) {
            this.notifyUserService.warn('Die heruntergeladene Datei konnte nicht geöffnet werden');
          }
        } else {
          this.notifyUserService.warn('Die heruntergeladene Datei konnte nicht geöffnet werden');
        }
      })
      .catch(err => this.errorHandlingService.handleError(err, 'Die Datei konnte nicht heruntergeladen werden'));
  }

  filterForAbfrageStatus(): void {
    this.massnahmeFilterService.filterField(
      'umsetzungsstandStatus',
      UmsetzungsstandStatus.displayTextOf(UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT)
    );
  }
}
