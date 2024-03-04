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
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { CsvImportService } from 'src/app/viewer/viewer-shared/services/csv-import.service';

@Component({
  selector: 'rad-csv-import-dialog',
  templateUrl: './csv-import-dialog.component.html',
  styleUrls: ['./csv-import-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CsvImportDialogComponent {
  formGroup: UntypedFormGroup;

  loading = false;

  constructor(
    public dialogRef: MatDialogRef<CsvImportDialogComponent>,
    private fileHandlingService: FileHandlingService,
    private changeDetector: ChangeDetectorRef,
    private notifyUserService: NotifyUserService,
    private csvImportService: CsvImportService
  ) {
    this.formGroup = new UntypedFormGroup({
      csvImportFile: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
    });
  }

  public onImport(): void {
    this.loading = true;
    this.csvImportService
      .uploadCsv(this.formGroup.value.csvImportFile)
      .then(blob => {
        this.fileHandlingService.downloadInBrowser(
          blob,
          'Importprotokoll_' + (this.formGroup.value.csvImportFile as File).name
        );
        this.dialogRef.close();
      })
      .finally(() => {
        this.loading = false;
        this.csvImportService.afterUpload();
        this.changeDetector.markForCheck();
      });
  }

  onOpenManual(): void {
    this.csvImportService.openManual();
  }
}
