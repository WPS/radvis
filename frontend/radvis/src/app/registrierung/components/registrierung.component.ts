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

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Rolle } from 'src/app/administration/models/rolle';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { RegistriereBenutzerCommand } from 'src/app/registrierung/registriere-benutzer-command';
import { RegistrierungService } from 'src/app/registrierung/registrierung.service';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { createBenutzerForm } from 'src/app/shared/services/benutzer-form.factory';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';

@Component({
  selector: 'rad-registrierung',
  templateUrl: './registrierung.component.html',
  styleUrls: ['./registrierung.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegistrierungComponent {
  registrierungForm: FormGroup;

  public alleBenutzerOrganisationen: Verwaltungseinheit[] = [];

  public agreedTerms = false;

  constructor(
    private benutzerService: RegistrierungService,
    private manualRoutingService: ManualRoutingService,
    private router: Router,
    activatedRoute: ActivatedRoute
  ) {
    this.alleBenutzerOrganisationen = activatedRoute.snapshot.data.organisationen;
    this.registrierungForm = createBenutzerForm();
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

    this.benutzerService.register(command).then(() => {
      this.reloadApplication();
    });
  }

  openManualRollenRechte(): void {
    this.manualRoutingService.openManualRollenRechte();
  }

  get rollen(): EnumOption[] {
    const nichtAngezeigteRollen = [Rolle.RADVIS_ADMINISTRATOR, Rolle.RADNETZ_QUALITAETSSICHERIN];
    return Rolle.options.filter(rolle => !nichtAngezeigteRollen.includes(rolle.name as Rolle));
  }

  private reloadApplication(): void {
    window.location.replace(window.location.origin);
  }
}
