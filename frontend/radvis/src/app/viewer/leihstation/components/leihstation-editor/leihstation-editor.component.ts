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
import { FormControl, FormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { DeleteLeihstationCommand } from 'src/app/viewer/leihstation/models/delete-leihstation-command';
import { Leihstation } from 'src/app/viewer/leihstation/models/leihstation';
import { LeihstationQuellSystem } from 'src/app/viewer/leihstation/models/leihstation-quell-system';
import { LeihstationStatus } from 'src/app/viewer/leihstation/models/leihstation-status';
import { LEIHSTATIONEN } from 'src/app/viewer/leihstation/models/leihstation.infrastruktur';
import { LeihstationFilterService } from 'src/app/viewer/leihstation/services/leihstation-filter.service';
import { LeihstationRoutingService } from 'src/app/viewer/leihstation/services/leihstation-routing.service';
import { LeihstationService } from 'src/app/viewer/leihstation/services/leihstation.service';
import { SimpleEditorCreatorComponent } from 'src/app/viewer/viewer-shared/components/simple-editor-creator.component';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-leihstation-editor',
  templateUrl: './leihstation-editor.component.html',
  styleUrls: ['./leihstation-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeihstationEditorComponent extends SimpleEditorCreatorComponent<Leihstation> {
  public MOBIDATA_DATENSATZ_URL = 'https://www.mobidata-bw.de/dataset/bikesh';

  entityName = 'Leihstation';

  iconName = LEIHSTATIONEN.iconFileName;
  currentLeihstation: Leihstation | null = null;
  leihstationStatusOptions = LeihstationStatus.options;
  leihstationQuellSystemOptions = LeihstationQuellSystem.options;

  constructor(
    activatedRoute: ActivatedRoute,
    changeDetector: ChangeDetectorRef,
    private viewerRoutingService: ViewerRoutingService,
    private leihstationService: LeihstationService,
    notifyUserService: NotifyUserService,
    private leihstationRoutingService: LeihstationRoutingService,
    private filterService: LeihstationFilterService,
    private olMapService: OlMapService,
    infrastrukturSelektionService: InfrastrukturenSelektionService,
    private dialog: MatDialog,
    private errorHandlingService: ErrorHandlingService
  ) {
    super(
      new FormGroup({
        geometrie: new FormControl(null, RadvisValidators.isNotNullOrEmpty),
        betreiber: new FormControl(null, [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
        quellSystem: new FormControl({ value: null, disabled: true }),
        anzahlFahrraeder: new FormControl(null, [
          RadvisValidators.isPositiveInteger,
          RadvisValidators.isSmallerThanIntegerMaxValue,
        ]),
        anzahlPedelecs: new FormControl(null, [
          RadvisValidators.isPositiveInteger,
          RadvisValidators.isSmallerThanIntegerMaxValue,
        ]),
        anzahlAbstellmoeglichkeiten: new FormControl(null, [
          RadvisValidators.isPositiveInteger,
          RadvisValidators.isSmallerThanIntegerMaxValue,
        ]),
        freiesAbstellen: new FormControl(null),
        buchungsUrl: new FormControl(null),
        status: new FormControl(null, RadvisValidators.isNotNullOrEmpty),
      }),
      notifyUserService,
      changeDetector,
      filterService
    );
    activatedRoute.data.subscribe(d => {
      this.isCreator = d.isCreator;
      this.currentLeihstation = d.leihstation ?? null;
      this.resetForm(this.currentLeihstation);
      if (this.currentLeihstation) {
        this.olMapService.scrollIntoViewByCoordinate(this.currentLeihstation.geometrie.coordinates);
      }
      changeDetector.markForCheck();
    });
    infrastrukturSelektionService.selectInfrastrukturen(LEIHSTATIONEN);
  }

  onSave(): void {
    super.save();
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  onReset(): void {
    this.resetForm(this.currentLeihstation);
  }

  onDeleteLeihstation(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question: `Möchten Sie die Leihstation wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.`,
        labelYes: 'Löschen',
        labelNo: 'Abbrechen',
        inverseButtonColorCoding: true,
        title: 'Achtung',
      } as QuestionYesNo,
    });
    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        invariant(this.currentLeihstation);
        const id = this.currentLeihstation.id;
        const deleteLeihstationCommand: DeleteLeihstationCommand = {
          version: this.currentLeihstation.version,
        };
        this.leihstationService
          .delete(id, deleteLeihstationCommand)
          .then(() => {
            this.filterService.onLeihstationDeleted(id);
            // zusaetzlich refetchData, um insgesamt die Massnahmen aktuell zu halten
            this.infrastrukturFilterService.refetchData();
            this.onClose();
          })
          .catch(error => this.errorHandlingService.handleHttpError(error));
      }
    });
  }

  focusLeihstationIntoView(): void {
    const toFocus = this.currentLeihstation?.geometrie.coordinates;

    if (toFocus) {
      this.olMapService.scrollIntoViewByCoordinate(toFocus);
    }
  }

  protected doSave(formGroup: FormGroup): Promise<void> {
    const coordinate = formGroup.value.geometrie;
    const currentId = this.currentLeihstation?.id;
    invariant(currentId);

    const {
      buchungsUrl,
      anzahlFahrraeder,
      anzahlPedelecs,
      anzahlAbstellmoeglichkeiten,
      freiesAbstellen,
      status,
      betreiber,
    } = this.formGroup.value;

    return this.leihstationService
      .save(currentId, {
        anzahlPedelecs: anzahlPedelecs || null,
        anzahlFahrraeder: anzahlFahrraeder || null,
        anzahlAbstellmoeglichkeiten: anzahlAbstellmoeglichkeiten || null,
        freiesAbstellen,
        status,
        betreiber,
        geometrie: {
          coordinates: coordinate,
          type: 'Point',
        },
        buchungsUrl: buchungsUrl || null,
        version: this.currentLeihstation?.version,
      })
      .then(saved => {
        this.currentLeihstation = saved;
        this.resetForm(this.currentLeihstation);
      });
  }

  protected doCreate(formGroup: FormGroup): Promise<void> {
    const coordinate = formGroup.value.geometrie;
    return this.leihstationService
      .create({
        ...formGroup.value,
        geometrie: {
          coordinates: coordinate,
          type: 'Point',
        },
      })
      .then(newId => {
        this.formGroup.markAsPristine();
        this.leihstationRoutingService.toInfrastrukturEditor(newId);
      });
  }

  public get canEdit(): boolean {
    return (this.isCreator || !!this.currentLeihstation?.darfBenutzerBearbeiten) && !this.isQuellsystemMobiData;
  }

  public get isQuellsystemMobiData(): boolean {
    return this.currentLeihstation?.quellSystem === LeihstationQuellSystem.MOBIDATABW;
  }

  private resetForm(leihstation: Leihstation | null): void {
    if (leihstation) {
      this.formGroup.reset({
        ...leihstation,
        geometrie: leihstation?.geometrie.coordinates,
      });
    } else {
      this.formGroup.reset({
        quellSystem: LeihstationQuellSystem.RADVIS,
      });
    }
    if (this.canEdit) {
      this.formGroup.enable();
    } else {
      this.formGroup.disable();
    }

    if (this.canEdit) {
      this.formGroup.enable();
    } else {
      this.formGroup.disable();
    }
    this.formGroup.get('quellSystem')?.disable();
  }
}
