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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostBinding, Optional } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { Servicestation } from 'src/app/viewer/servicestation/models/servicestation';
import { SERVICESTATIONEN } from 'src/app/viewer/servicestation/models/servicestation.infrastruktur';
import { ServicestationFilterService } from 'src/app/viewer/servicestation/services/servicestation-filter.service';
import { ServicestationRoutingService } from 'src/app/viewer/servicestation/services/servicestation-routing.service';
import { ServicestationService } from 'src/app/viewer/servicestation/services/servicestation.service';
import { SimpleEditorCreatorComponent } from 'src/app/viewer/viewer-shared/components/simple-editor-creator.component';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { ServicestationTyp } from 'src/app/viewer/servicestation/models/servicestation-typ';
import { ServicestationStatus } from 'src/app/viewer/servicestation/models/servicestation-status';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import invariant from 'tiny-invariant';
import { SaveServicestationCommand } from 'src/app/viewer/servicestation/models/save-servicestation-command';
import { ServicestationUpdatedService } from 'src/app/viewer/servicestation/services/servicestation-updated.service';
import { ServicestationQuellSystem } from 'src/app/viewer/servicestation/models/servicestation-quell-system';

@Component({
  selector: 'rad-servicestation-editor',
  templateUrl: './servicestation-editor.component.html',
  styleUrls: ['./servicestation-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServicestationEditorComponent extends SimpleEditorCreatorComponent<Servicestation> {
  // Setzt je nachdem ob es Creator oder Editor ist die entsprechende CSS Klasse an :host,
  // welches in .scss dann verwendet werden kann.
  @HostBinding('class') get hostClasses(): string {
    return this.isCreator ? 'is-creator' : 'is-editor';
  }

  entityName = 'Servicestation';

  iconName = SERVICESTATIONEN.iconFileName;
  currentServicestation: Servicestation | null = null;
  alleOrganisationen$: Promise<Verwaltungseinheit[]>;
  servicestationQuellSystemOptions = ServicestationQuellSystem.options;
  servicestationTypOptions = ServicestationTyp.options;
  servicestationStatusOptions = ServicestationStatus.options;
  organisationAktuellerBenutzer: Verwaltungseinheit | undefined;

  constructor(
    activatedRoute: ActivatedRoute,
    changeDetector: ChangeDetectorRef,
    private viewerRoutingService: ViewerRoutingService,
    private servicestationService: ServicestationService,
    @Optional() private servicestationUpdatedService: ServicestationUpdatedService,
    notifyUserService: NotifyUserService,
    private servicestationRoutingService: ServicestationRoutingService,
    filterService: ServicestationFilterService,
    organisationenService: OrganisationenService,
    benutzerDetailsService: BenutzerDetailsService
  ) {
    super(
      new UntypedFormGroup({
        geometrie: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
        name: new UntypedFormControl(null, [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
        quellSystem: new UntypedFormControl({ value: null, disabled: true }),
        gebuehren: new UntypedFormControl(null),
        oeffnungszeiten: new UntypedFormControl(null, RadvisValidators.maxLength(2000)),
        betreiber: new UntypedFormControl(null, [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
        marke: new UntypedFormControl(null, RadvisValidators.maxLength(255)),
        luftpumpe: new UntypedFormControl(null),
        kettenwerkzeug: new UntypedFormControl(null),
        werkzeug: new UntypedFormControl(null),
        fahrradhalterung: new UntypedFormControl(null),
        beschreibung: new UntypedFormControl(null, RadvisValidators.maxLength(2000)),
        organisation: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
        typ: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
        status: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
      }),
      notifyUserService,
      changeDetector,
      filterService
    );

    this.alleOrganisationen$ = organisationenService.getOrganisationen();
    this.organisationAktuellerBenutzer = benutzerDetailsService.aktuellerBenutzerOrganisation();
    activatedRoute.data.subscribe(d => {
      this.isCreator = d.isCreator;
      this.currentServicestation = d.servicestation ?? null;
      this.resetForm(this.currentServicestation);
      changeDetector.markForCheck();
    });
  }

  onSave(): void {
    super.save();
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  onReset(): void {
    this.resetForm(this.currentServicestation);
  }

  public get isQuellsystemMobiData(): boolean {
    return this.currentServicestation?.quellSystem === ServicestationQuellSystem.MOBIDATABW;
  }

  protected doSave(formGroup: UntypedFormGroup): Promise<void> {
    const currentId = this.currentServicestation?.id;
    invariant(currentId);
    return this.servicestationService
      .save(currentId, {
        ...this.readCommand(formGroup),
        version: this.currentServicestation?.version,
      })
      .then(saved => {
        this.currentServicestation = saved;
        this.resetForm(this.currentServicestation);
        this.servicestationUpdatedService?.updateServicestation();
      });
  }

  protected doCreate(formGroup: UntypedFormGroup): Promise<void> {
    return this.servicestationService.create(this.readCommand(formGroup)).then(newId => {
      this.formGroup.markAsPristine();
      this.servicestationRoutingService.toInfrastrukturEditor(newId);
    });
  }

  public get canEdit(): boolean {
    return this.isCreator || !!this.currentServicestation?.darfBenutzerBearbeiten;
  }

  private resetForm(servicestation: Servicestation | null): void {
    if (servicestation) {
      this.formGroup.reset({
        ...servicestation,
        geometrie: servicestation?.geometrie.coordinates,
      });
    } else {
      this.formGroup.reset({
        organisation: this.organisationAktuellerBenutzer,
        quellSystem: ServicestationQuellSystem.RADVIS,
      });
    }

    if (this.canEdit) {
      this.formGroup.enable();
    } else {
      this.formGroup.disable();
    }

    this.formGroup.get('quellSystem')?.disable();
  }

  private readCommand(formGroup: UntypedFormGroup): SaveServicestationCommand {
    const coordinate = formGroup.value.geometrie;
    return {
      name: formGroup.value.name,
      betreiber: formGroup.value.betreiber,
      geometrie: {
        coordinates: coordinate,
        type: 'Point',
      },
      marke: formGroup.value.marke || null,
      beschreibung: formGroup.value.beschreibung || null,
      organisationId: formGroup.value.organisation?.id || null,
      oeffnungszeiten: formGroup.value.oeffnungszeiten || null,
      gebuehren: formGroup.value.gebuehren || false,
      fahrradhalterung: formGroup.value.fahrradhalterung || false,
      kettenwerkzeug: formGroup.value.kettenwerkzeug || false,
      werkzeug: formGroup.value.werkzeug || false,
      luftpumpe: formGroup.value.luftpumpe || false,
      typ: formGroup.value.typ,
      status: formGroup.value.status,
    };
  }
}
