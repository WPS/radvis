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
import { FormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Rolle } from 'src/app/administration/models/rolle';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { RegistriereBenutzerCommand } from 'src/app/registrierung/registriere-benutzer-command';
import { RegistrierungService } from 'src/app/registrierung/registrierung.service';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';

@Component({
  selector: 'rad-registrierung',
  templateUrl: './registrierung.component.html',
  styleUrls: ['./registrierung.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class RegistrierungComponent {
  registrierungForm: UntypedFormGroup;

  public alleBenutzerOrganisationen: Verwaltungseinheit[] = [];

  public agreedTerms = false;
  public waiting = false;

  get rollen(): EnumOption[] {
    const nichtAngezeigteRollen = [Rolle.RADVIS_ADMINISTRATOR, Rolle.RADNETZ_QUALITAETSSICHERIN];
    return Rolle.options.filter(rolle => !nichtAngezeigteRollen.includes(rolle.name as Rolle));
  }

  constructor(
    private benutzerService: RegistrierungService,
    private manualRoutingService: ManualRoutingService,
    activatedRoute: ActivatedRoute,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.alleBenutzerOrganisationen = activatedRoute.snapshot.data.organisationen;
    this.registrierungForm = new UntypedFormGroup({
      vorname: new FormControl<string>('', [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
      nachname: new FormControl<string>('', [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
      email: new FormControl<string>('', [RadvisValidators.email, RadvisValidators.isNotNullOrEmpty]),
      organisation: new FormControl<Verwaltungseinheit | null>(null, [RadvisValidators.isNotNullOrEmpty]),
      rollen: new FormControl<Rolle[]>([], [RadvisValidators.isNotEmpty]),
    });
  }

  toggleAgreedTerms(checked: boolean): void {
    this.agreedTerms = checked;
  }

  register(): void {
    this.registrierungForm.markAllAsTouched();

    if (!this.registrierungForm.valid) {
      return;
    }

    const command = {
      vorname: this.registrierungForm.value.vorname,
      nachname: this.registrierungForm.value.nachname,
      email: this.registrierungForm.value.email,
      organisation: this.registrierungForm.value.organisation?.id,
      rollen: this.registrierungForm.value.rollen,
    } as RegistriereBenutzerCommand;

    this.waiting = true;

    this.benutzerService
      .register(command)
      .then(() => {
        this.reloadApplication();
      })
      .finally(() => {
        this.waiting = false;
        this.changeDetectorRef.detectChanges();
      });
  }

  openManualRollenRechte(): void {
    this.manualRoutingService.openManualRollenRechte();
  }

  private reloadApplication(): void {
    window.location.replace(window.location.origin);
  }
}
