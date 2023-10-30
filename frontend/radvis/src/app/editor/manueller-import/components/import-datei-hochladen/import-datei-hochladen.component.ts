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
import { ActivatedRoute, Router } from '@angular/router';
import { TransformDialogComponent } from 'src/app/editor/manueller-import/components/transform-dialog/transform-dialog.component';
import { DateiUploadInfo } from 'src/app/editor/manueller-import/models/datei-upload-info';
import { ImportSessionView } from 'src/app/editor/manueller-import/models/import-session-view';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { CreateSessionStateService } from 'src/app/editor/manueller-import/services/create-session.state.service';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { AttributeImportFormat } from 'src/app/editor/manueller-import/models/attribute-import-format';
import { AttributeImportSessionView } from 'src/app/editor/manueller-import/models/attribute-import-session-view';

@Component({
  selector: 'rad-datei-upload',
  templateUrl: './import-datei-hochladen.component.html',
  styleUrls: ['./import-datei-hochladen.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportDateiHochladenComponent {
  readonly NETZKLASSE_ZUWEISEN_MODUS = ImportTyp.NETZKLASSE_ZUWEISEN;
  readonly ATTRIBUTE_UEBERNEHMEN_MODUS = ImportTyp.ATTRIBUTE_UEBERNEHMEN;

  formGroup: FormGroup;
  sessionExists = false;

  organisationen$: Promise<Verwaltungseinheit[]>;

  attributeImportFormatOptions = AttributeImportFormat.options;

  get importTyp(): ImportTyp {
    return this.formGroup.value.importTyp;
  }

  constructor(
    private router: Router,
    private manuellerImportRoutingService: ManuellerImportRoutingService,
    private activatedRoute: ActivatedRoute,
    private manuellerImportService: ManuellerImportService,
    private changeDetectorRef: ChangeDetectorRef,
    private createSessionStateService: CreateSessionStateService,
    organisationenService: OrganisationenService,
    public dialog: MatDialog
  ) {
    this.organisationen$ = organisationenService.getOrganisationen().then(orgs => {
      return orgs.filter(
        orga => orga.organisationsArt === OrganisationsArt.GEMEINDE || orga.organisationsArt === OrganisationsArt.KREIS
      );
    });
    this.formGroup = this.createForm();

    manuellerImportService.existsImportSession().then(exists => {
      this.sessionExists = exists;
      changeDetectorRef.markForCheck();
      if (exists) {
        this.manuellerImportService
          .getImportSession()
          .toPromise()
          .then((session: ImportSessionView) => {
            this.formGroup.disable();
            this.formGroup.patchValue({
              importTyp: session.typ,
            });
            if (session.typ === ImportTyp.ATTRIBUTE_UEBERNEHMEN && 'attributeImportFormat' in session) {
              this.formGroup.patchValue({
                attributeImportFormat: (session as AttributeImportSessionView).attributeImportFormat,
              });
            }
            this.changeDetectorRef.markForCheck();
            this.organisationen$.then(organisationen => {
              this.formGroup.patchValue({
                organisation: organisationen.find(o => o.id === session.organisationsID),
              });

              this.changeDetectorRef.markForCheck();
            });
          });
      } else {
        if (this.createSessionStateService.dateiUploadInfo) {
          this.formGroup.patchValue({
            importTyp: this.createSessionStateService.dateiUploadInfo?.importTyp,
            file: this.createSessionStateService.dateiUploadInfo?.file,
            attributeImportFormat: this.createSessionStateService.attributeImportFormat,
          });
          this.changeDetectorRef.markForCheck();
          this.organisationen$.then(organisationen => {
            this.formGroup.patchValue({
              organisation: organisationen.find(
                o => o.id === this.createSessionStateService.dateiUploadInfo?.organisation
              ),
            });
            this.changeDetectorRef.markForCheck();
          });
        }
      }
    });
  }

  onOpenShpTransformDialog(): void {
    this.dialog.open(TransformDialogComponent);
  }

  onNext(): void {
    if (!this.sessionExists) {
      this.createSessionStateService.updateDateiUploadInfo(
        DateiUploadInfo.of(this.importTyp, this.formGroup.value.file, this.formGroup.value.organisation.id)
      );
      if (this.importTyp === ImportTyp.ATTRIBUTE_UEBERNEHMEN) {
        this.createSessionStateService.updateAttributeImportFormat(this.formGroup.value.attributeImportFormat);
      }
    }
    const nextLink =
      '../' +
      this.manuellerImportRoutingService.getRouteForStep(this.activatedRoute.snapshot.data.step + 1, this.importTyp);
    this.router.navigate([nextLink], { relativeTo: this.activatedRoute, queryParamsHandling: 'merge' });
  }

  onAbort(): void {
    this.manuellerImportService.deleteImportSession().then(() => {
      this.formGroup.enable();
      this.formGroup.reset({
        importTyp: ImportTyp.NETZKLASSE_ZUWEISEN,
        attributeImportFormat: AttributeImportFormat.RADVIS,
      });
      this.sessionExists = false;
      this.changeDetectorRef.markForCheck();
    });
    this.createSessionStateService.reset();
  }

  uploadDatenVorhanden(): boolean {
    return (
      this.createSessionStateService.dateiUploadInfo !== null || this.createSessionStateService.parameterInfo !== null
    );
  }

  private createForm(): FormGroup {
    return new FormGroup({
      importTyp: new FormControl(ImportTyp.NETZKLASSE_ZUWEISEN),
      attributeImportFormat: new FormControl(AttributeImportFormat.RADVIS),
      organisation: new FormControl(null, RadvisValidators.isNotNullOrEmpty),
      file: new FormControl(null, RadvisValidators.isNotNullOrEmpty),
    });
  }
}
