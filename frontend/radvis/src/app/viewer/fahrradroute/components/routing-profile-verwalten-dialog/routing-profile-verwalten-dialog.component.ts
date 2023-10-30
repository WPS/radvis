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

import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { AbstractControl, FormArray, FormControl, FormGroup, ValidationErrors } from '@angular/forms';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { RoutingProfileService } from 'src/app/viewer/fahrradroute/services/routing-profile.service';
import { CustomRoutingProfile } from 'src/app/viewer/fahrradroute/models/custom-routing-profile';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
// @ts-ignore
import { create } from 'custom-model-editor/src/index';
import { BelagArt } from 'src/app/shared/models/belag-art';
import { Beleuchtung } from 'src/app/editor/kanten/models/beleuchtung';
import { Radverkehrsfuehrung } from 'src/app/shared/models/radverkehrsfuehrung';
import { Oberflaechenbeschaffenheit } from 'src/app/editor/kanten/models/oberflaechenbeschaffenheit';
import { Subscription } from 'rxjs';
import { BarrierenForm } from 'src/app/viewer/barriere/models/barrieren-form';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';

@Component({
  selector: 'rad-routing-profile-verwalten-dialog',
  templateUrl: './routing-profile-verwalten-dialog.component.html',
  styleUrls: ['./routing-profile-verwalten-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoutingProfileVerwaltenDialogComponent implements AfterViewInit, OnDestroy {
  public profileFormArray: FormArray;
  public disableAnimation = true;

  public saving = false;

  private customProfileEditor: any;

  private subscriptions: Subscription[] = [];

  constructor(
    private routingProfileService: RoutingProfileService,
    private manualRoutingService: ManualRoutingService,
    private changeDetectorRef: ChangeDetectorRef,
    private notifyUserService: NotifyUserService
  ) {
    this.customProfileEditor = create(encodedValues, () => {});

    this.profileFormArray = new FormArray([]);
    this.subscriptions.push(
      this.routingProfileService.profiles$.subscribe(profiles => {
        this.resetForm(profiles);
        this.changeDetectorRef.markForCheck();
        this.profileFormArray.markAsPristine();
      })
    );
  }

  ngAfterViewInit(): void {
    // workaround for https://github.com/angular/components/issues/14759
    setTimeout(() => (this.disableAnimation = false));
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  public get formArrayAsFormGroupArray(): FormGroup[] {
    return this.profileFormArray.controls as FormGroup[];
  }

  public onDeleteProfile(atIndex: number): void {
    this.profileFormArray.removeAt(atIndex);
    this.profileFormArray.markAsDirty();
  }

  onAddProfile(): void {
    const newControl = this.createProfileFormGroup();

    this.profileFormArray.push(newControl);
    this.profileFormArray.markAsDirty();
  }

  onSave(): void {
    if (this.profileFormArray.invalid) {
      this.notifyUserService.warn('Die Eingabe ist nicht korrekt und konnte nicht gespeichert werden.');
      return;
    }

    this.saving = true;

    for (let i = 0; i < this.profileFormArray.length; i++) {
      this.prettyPrint(i);
    }

    this.routingProfileService
      .save(this.profileFormArray.value)
      .catch(reason => this.notifyUserService.warn(`Die Änderungen konnten nicht gespeichert werden: ${reason}`))
      .finally(() => {
        this.saving = false;
        this.changeDetectorRef.markForCheck();
      });
  }

  public prettyPrint(index: number): void {
    const profilJsonControl = this.profileFormArray.at(index).get('profilJson');
    let parsed;
    try {
      parsed = JSON.parse(profilJsonControl?.value);
    } catch (e) {}
    if (parsed) {
      const pretty = JSON.stringify(parsed, null, 2);
      if (pretty !== profilJsonControl?.value) {
        profilJsonControl?.patchValue(pretty);
        this.profileFormArray.markAsDirty();
      }
    }
  }

  public onOpenHandbuch(): void {
    this.manualRoutingService.openRoutingProfile();
  }

  private createProfileFormGroup(): FormGroup {
    return new FormGroup({
      id: new FormControl(null),
      name: new FormControl('', [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
      profilJson: new FormControl('', [RadvisValidators.isNotNullOrEmpty, this.customProfileValidator]),
    });
  }

  private resetForm(profiles: CustomRoutingProfile[]): void {
    this.profileFormArray.clear();
    profiles.forEach(p => {
      const fg = this.createProfileFormGroup();
      fg.patchValue(p);
      this.profileFormArray.push(fg);
    });
  }

  private customProfileValidator = (ctrl: AbstractControl): ValidationErrors | null => {
    if (ctrl.value) {
      this.customProfileEditor.cm.setValue(ctrl.value);
      const currentErrors: {
        from: { line: number; ch: number };
        message: string;
        severity: string;
      }[] = this.customProfileEditor.getCurrentErrors(ctrl.value, this.customProfileEditor.cm).errors;
      if (!currentErrors.length) {
        return null;
      }
      const errorMessage = currentErrors
        .map((err, index) => `Fehler ${index + 1}: ${err.message} ( Zeile: ${err.from.line}, Spalte: ${err.from.ch} )`)
        .join('| ');

      return {
        customProfileInvalid: errorMessage,
      };
    }
    return null;
  };
}

const encodedValues = {
  barriere: { type: 'enum', values: BarrierenForm.allOptions.map(opt => `\\"${opt.name}\\"`).sort() },
  belagart: { type: 'enum', values: BelagArt.options.map(opt => `\\"${opt.name}\\"`).sort() },
  beleuchtung: { type: 'enum', values: Beleuchtung.options.map(opt => `\\"${opt.name}\\"`).sort() },
  breite: { type: 'numeric' },
  dtv_pkw: { type: 'numeric' },
  fuehrung: { type: 'enum', values: Radverkehrsfuehrung.all.map(f => `\\"${f}\\"`).sort() },
  oberflaeche: { type: 'enum', values: Oberflaechenbeschaffenheit.options.map(opt => `\\"${opt.name}\\"`).sort() },
  netzklasse_kommunalnetz_alltag: { type: 'boolean' },
  netzklasse_kreisnetz_alltag: { type: 'boolean' },
  netzklasse_radnetz_alltag: { type: 'boolean' },
  netzklasse_radnetz_freizeit: { type: 'boolean' },
  netzklasse_radnetz_zielnetz: { type: 'boolean' },
  netzklasse_radschnellverbindung: { type: 'boolean' },
  netzklasse_radvorrangrouten: { type: 'boolean' },
};
