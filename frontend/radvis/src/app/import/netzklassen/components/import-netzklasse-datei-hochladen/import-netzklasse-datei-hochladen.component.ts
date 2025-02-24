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
import { Polygon } from 'ol/geom';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { DateiUploadInfo } from 'src/app/import/models/datei-upload-info';
import { ImportTyp } from 'src/app/import/models/import-typ';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { CreateSessionStateService } from 'src/app/import/services/create-session.state.service';
import { ImportService } from 'src/app/import/services/import.service';
import { Organisation } from 'src/app/shared/models/organisation-edit-view';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';

@Component({
  selector: 'rad-import-netzklasse-datei-hochladen',
  templateUrl: './import-netzklasse-datei-hochladen.component.html',
  styleUrls: ['./import-netzklasse-datei-hochladen.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportNetzklasseDateiHochladenComponent {
  private static readonly STEP = 1;
  formGroup: UntypedFormGroup;
  sessionExists = false;
  netzklassenSessionExists = false;

  organisationen$: Promise<Verwaltungseinheit[]>;

  importTyp: ImportTyp = ImportTyp.NETZKLASSE_ZUWEISEN;

  constructor(
    private netzklassenImportService: NetzklassenImportService,
    private netzklassenRoutingService: NetzklassenRoutingService,
    private changeDetectorRef: ChangeDetectorRef,
    private createSessionStateService: CreateSessionStateService,
    private organisationenService: OrganisationenService,
    private olMapService: OlMapService,
    importService: ImportService
  ) {
    this.organisationen$ = organisationenService.getOrganisationen().then(orgs => {
      return orgs.filter(
        orga => orga.organisationsArt === OrganisationsArt.GEMEINDE || orga.organisationsArt === OrganisationsArt.KREIS
      );
    });

    this.formGroup = new UntypedFormGroup({
      organisation: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
      file: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty, c =>
        importService.validateShapefile(c).finally(() => changeDetectorRef.markForCheck())
      ),
    });

    this.formGroup
      .get('organisation')
      ?.valueChanges.subscribe(selectedOrganisation => this.onOrganisationChange(selectedOrganisation));

    netzklassenImportService.existsImportSession().subscribe(exists => {
      this.sessionExists = exists;
      if (exists) {
        this.formGroup.disable();
      }
      this.changeDetectorRef.markForCheck();
    });

    netzklassenImportService.getImportSession().subscribe(session => {
      this.netzklassenSessionExists = !!session;
      changeDetectorRef.markForCheck();
      if (session) {
        this.formGroup.disable();
        this.changeDetectorRef.markForCheck();
        this.organisationen$.then(organisationen => {
          this.formGroup.patchValue({
            organisation: organisationen.find(o => o.id === session.organisationsID),
          });
          this.changeDetectorRef.markForCheck();
        });
      } else {
        if (this.createSessionStateService.dateiUploadInfo) {
          this.formGroup.enable();
          this.formGroup.patchValue({
            file: this.createSessionStateService.dateiUploadInfo?.file,
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

  onNext(): void {
    if (!this.sessionExists) {
      this.createSessionStateService.updateDateiUploadInfo(
        DateiUploadInfo.of(this.importTyp, this.formGroup.value.file, this.formGroup.value.organisation.id)
      );
    }
    this.netzklassenRoutingService.navigateToNext(ImportNetzklasseDateiHochladenComponent.STEP);
  }

  onAbort(): void {
    this.netzklassenImportService.deleteImportSession().subscribe(() => {
      this.formGroup.enable();
      this.formGroup.reset();
      this.sessionExists = false;
      this.netzklassenSessionExists = false;
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
