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
import { TransformShpService } from 'src/app/import/services/transform-shp.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';

@Component({
  templateUrl: './transform-attribute-dialog.component.html',
  styleUrls: ['./transform-attribute-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class TransformAttributeDialogComponent {
  formGroup: UntypedFormGroup;
  shpFileName = '';
  transformationFileName = '';
  loading = false;

  constructor(
    public dialogRef: MatDialogRef<TransformAttributeDialogComponent>,
    private fileHandlingService: FileHandlingService,
    private transformShpService: TransformShpService,
    private changeDetector: ChangeDetectorRef,
    private manualRoutingService: ManualRoutingService
  ) {
    this.formGroup = this.createForm();
  }

  onTransform(): void {
    this.loading = true;
    this.transformShpService
      .startShpTransform(this.formGroup.value.shpFile, this.formGroup.value.transformationFile)
      .then(blob => {
        this.fileHandlingService.downloadInBrowser(blob, 'transformed_' + (this.formGroup.value.shpFile as File).name);
        this.dialogRef.close();
      })
      .finally(() => {
        this.loading = false;
        this.changeDetector.markForCheck();
      });
  }

  openManualImportTransformation(): void {
    this.manualRoutingService.openManualImportTransformation();
  }

  private createForm(): UntypedFormGroup {
    return new UntypedFormGroup({
      transformationFile: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
      shpFile: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
    });
  }
}
