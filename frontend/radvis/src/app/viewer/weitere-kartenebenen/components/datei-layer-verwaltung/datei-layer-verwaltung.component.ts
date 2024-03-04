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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { CreateDateiLayerCommand } from 'src/app/viewer/weitere-kartenebenen/models/create-datei-layer-command';
import { DateiLayer } from 'src/app/viewer/weitere-kartenebenen/models/datei-layer';
import { DateiLayerFormat } from 'src/app/viewer/weitere-kartenebenen/models/datei-layer-format';
import { DateiLayerService } from 'src/app/viewer/weitere-kartenebenen/services/datei-layer.service';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { MatDialog } from '@angular/material/dialog';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-datei-layer-verwaltung',
  templateUrl: './datei-layer-verwaltung.component.html',
  styleUrls: ['./datei-layer-verwaltung.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DateiLayerVerwaltungComponent implements OnInit {
  @ViewChild('styleDialogRef')
  styleDialogRef: TemplateRef<any> | undefined;

  public readonly STYLE_DIALOG_ID = 'styleDialog';
  public readonly QUELLANGABE_MAX_LENGTH = 1000;
  public dateiLayerFormatOptions: EnumOption[] = DateiLayerFormat.options;
  public maxFileSizeInMB: number | undefined;
  public saving = false;

  public uploadingStyle = false;

  public dateiLayers$: Observable<DateiLayer[]>;

  formGroupDateiLayer = this.createLayerFormGroup();

  constructor(
    private dateiLayerService: DateiLayerService,
    private weitereKartenebenenService: WeitereKartenebenenService,
    private changeDetectorRef: ChangeDetectorRef,
    private notifyUserService: NotifyUserService,
    public dialog: MatDialog
  ) {
    this.dateiLayers$ = dateiLayerService.allDateiLayers$;
  }

  ngOnInit(): void {
    this.dateiLayerService.getMaxFileSizeInMB().then(maxFileSize => {
      this.maxFileSizeInMB = maxFileSize;
      this.changeDetectorRef.markForCheck();
    });
  }

  public onCreate(): void {
    this.saving = true;
    this.dateiLayerService
      .create(
        {
          name: this.formGroupDateiLayer.get('name')?.value,
          format: this.formGroupDateiLayer.get('format')?.value,
          quellangabe: this.formGroupDateiLayer.get('quellangabe')?.value,
        } as CreateDateiLayerCommand,
        this.formGroupDateiLayer.get('file')?.value
      )
      .then(() => {
        this.notifyUserService.inform('Kartenebene wurde erfolgreich aus hochgeladener Datei erstellt.');
        this.resetCreateLayerFormGroup();
      })
      .catch(reason => {
        let errorMessage =
          'Kartenebene konnte nicht erstellt werden, achten Sie auf korrekte Dateien. Mehr Infos dazu finden Sie im Handbuch.';
        if (reason && reason.error && reason.error.message) {
          errorMessage += '\n\nDetails: ' + reason.error.message;
        }
        this.notifyUserService.warn(errorMessage);
      })
      .finally(() => {
        this.changeDetectorRef.markForCheck();
        this.saving = false;
      });
  }

  public onDeleteStyle(layerId: number): void {
    this.dateiLayerService
      .deleteStyle(layerId)
      .then(() => {
        this.notifyUserService.inform('Style erfolgreich gelöscht.');
        this.dialog.getDialogById(this.STYLE_DIALOG_ID)?.close();
      })
      .catch(reason => {
        let errorMessage = 'Style konnte nicht gelöscht werden';
        if (reason && reason.error && reason.error.message) {
          errorMessage += ': ' + reason.error.message;
        }
        this.notifyUserService.warn(errorMessage);
      })
      .finally(() => {
        this.changeDetectorRef.markForCheck();
      });
  }

  onAddOrChangeStyle(layerId: number, sldFile: File): void {
    this.uploadingStyle = true;
    this.dateiLayerService
      .changeStyle(layerId, sldFile)
      .then(() => {
        this.notifyUserService.inform('Style erfolgreich geändert/hinzugefügt.');
        this.dialog.getDialogById(this.STYLE_DIALOG_ID)?.close();
      })
      .catch(reason => {
        let errorMessage = 'Style konnte nicht geändert/hinzugefügt werden';
        if (reason && reason.error && reason.error.message) {
          errorMessage += ': ' + reason.error.message;
        }
        this.notifyUserService.warn(errorMessage);
      })
      .finally(() => {
        this.uploadingStyle = false;
        this.changeDetectorRef.markForCheck();
      });
  }

  onManageStyles(layer: DateiLayer): void {
    invariant(this.styleDialogRef);
    const formControl = new UntypedFormControl(null);

    this.dialog.open(this.styleDialogRef, { id: this.STYLE_DIALOG_ID, data: { layer, formControl }, width: '30%' });
  }

  public onDeleteLayer(id: number): void {
    this.dateiLayerService
      .delete(id)
      .then(() => this.notifyUserService.inform('Kartenebene erfolgreich gelöscht.'))
      .catch(reason => {
        let errorMessage = 'Kartenebene konnte nicht gelöscht werden';
        if (reason && reason.error && reason.error.message) {
          errorMessage += ': ' + reason.error.message;
        }
        this.notifyUserService.warn(errorMessage);
      })
      .finally(() => this.weitereKartenebenenService.initWeitereKartenebenen());
  }

  public getErlaubteDateiEndung(): string {
    if (!this.formGroupDateiLayer.get('format')) {
      throw new Error('Formfield format unerlaubter Weise leer.');
    }
    const dateiLayerFormat = this.formGroupDateiLayer.get('format')?.value as DateiLayerFormat;
    return DateiLayerFormat.getDateiEndung(dateiLayerFormat);
  }

  public getDisplayTextForDateiFormat(format: DateiLayerFormat): string {
    return DateiLayerFormat.getDisplayText(format);
  }

  private createLayerFormGroup(): UntypedFormGroup {
    return new UntypedFormGroup({
      name: new UntypedFormControl('', [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
      format: new UntypedFormControl(DateiLayerFormat.SHAPE, RadvisValidators.isNotNullOrEmpty),
      quellangabe: new UntypedFormControl(null, [
        RadvisValidators.isNotNullOrEmpty,
        RadvisValidators.maxLength(this.QUELLANGABE_MAX_LENGTH),
      ]),
      file: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
    });
  }

  private resetCreateLayerFormGroup(): void {
    this.formGroupDateiLayer.reset({
      name: null,
      format: DateiLayerFormat.SHAPE,
      quellangabe: null,
      file: null,
    });
  }
}
