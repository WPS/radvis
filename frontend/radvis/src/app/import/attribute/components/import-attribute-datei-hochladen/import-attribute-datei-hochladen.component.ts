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
import { MatDialog } from '@angular/material/dialog';
import { Polygon } from 'ol/geom';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { TransformAttributeDialogComponent } from 'src/app/import/attribute/components/transform-attribute-dialog/transform-attribute-dialog.component';
import { AttributeImportFormat } from 'src/app/import/attribute/models/attribute-import-format';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { AttributeRoutingService } from 'src/app/import/attribute/services/attribute-routing.service';
import { DateiUploadInfo } from 'src/app/import/models/datei-upload-info';
import { ImportTyp } from 'src/app/import/models/import-typ';
import { CreateSessionStateService } from 'src/app/import/services/create-session.state.service';
import { ImportService } from 'src/app/import/services/import.service';
import { Organisation } from 'src/app/shared/models/organisation-edit-view';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';

@Component({
  selector: 'rad-import-attribute-datei-hochladen',
  templateUrl: './import-attribute-datei-hochladen.component.html',
  styleUrls: ['./import-attribute-datei-hochladen.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportAttributeDateiHochladenComponent {
  private static readonly STEP = 1;

  formGroup: UntypedFormGroup;
  sessionExists = false;
  attributeSessionExists = false;

  organisationen$: Promise<Verwaltungseinheit[]>;

  attributeImportFormatOptions = AttributeImportFormat.options;

  readonly importTyp: ImportTyp = ImportTyp.ATTRIBUTE_UEBERNEHMEN;

  constructor(
    private attributeImportService: AttributeImportService,
    private attributeRoutingService: AttributeRoutingService,
    private changeDetectorRef: ChangeDetectorRef,
    private createSessionStateService: CreateSessionStateService,
    private organisationenService: OrganisationenService,
    private olMapService: OlMapService,
    public dialog: MatDialog,
    importService: ImportService
  ) {
    this.organisationen$ = organisationenService.getOrganisationen().then(orgs => {
      return orgs.filter(
        orga => orga.organisationsArt === OrganisationsArt.GEMEINDE || orga.organisationsArt === OrganisationsArt.KREIS
      );
    });

    this.formGroup = new UntypedFormGroup({
      attributeImportFormat: new UntypedFormControl(AttributeImportFormat.RADVIS),
      organisation: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
      file: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty, c =>
        importService.validateShapefile(c).finally(() => changeDetectorRef.markForCheck())
      ),
    });

    this.formGroup
      .get('organisation')
      ?.valueChanges.subscribe(selectedOrganisation => this.onOrganisationChange(selectedOrganisation));

    attributeImportService.existsImportSession().subscribe(exists => {
      this.sessionExists = exists;
      if (exists) {
        this.formGroup.disable();
      }
      this.changeDetectorRef.markForCheck();
    });

    this.attributeImportService.getImportSession().subscribe(session => {
      this.attributeSessionExists = !!session;
      changeDetectorRef.markForCheck();
      if (session) {
        this.formGroup.disable();

        this.formGroup.patchValue({ attributeImportFormat: session.attributeImportFormat });
        this.changeDetectorRef.markForCheck();
        this.organisationen$.then(organisationen => {
          this.formGroup.patchValue({
            organisation: organisationen.find(o => o.id === session.organisationsID),
          });
          this.changeDetectorRef.markForCheck();
        });
      } else if (!this.sessionExists) {
        if (this.createSessionStateService.dateiUploadInfo) {
          this.formGroup.enable();
          this.formGroup.patchValue({
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
    this.dialog.open(TransformAttributeDialogComponent);
  }

  onNext(): void {
    if (!this.sessionExists) {
      this.createSessionStateService.updateDateiUploadInfo(
        DateiUploadInfo.of(this.importTyp, this.formGroup.value.file, this.formGroup.value.organisation.id)
      );
      this.createSessionStateService.updateAttributeImportFormat(this.formGroup.value.attributeImportFormat);
    }
    this.attributeRoutingService.navigateToNext(ImportAttributeDateiHochladenComponent.STEP);
  }

  onAbort(): void {
    this.attributeImportService.deleteImportSession().subscribe(() => {
      this.formGroup.enable();
      this.formGroup.reset({
        attributeImportFormat: AttributeImportFormat.RADVIS,
      });
      this.sessionExists = false;
      this.attributeSessionExists = false;
      this.changeDetectorRef.markForCheck();
    });
    this.createSessionStateService.reset();
  }

  uploadDatenVorhanden(): boolean {
    return (
      this.createSessionStateService.dateiUploadInfo !== null || this.createSessionStateService.parameterInfo !== null
    );
  }

  onOrganisationChange(selectedOrganisation: Organisation): void {
    if (selectedOrganisation) {
      this.organisationenService.getBereichEnvelopeView(selectedOrganisation.id).then(view => {
        if (view.bereich) {
          this.olMapService.scrollIntoViewByGeometry(new Polygon(view.bereich.coordinates));
        }
      });
    }
  }
}
