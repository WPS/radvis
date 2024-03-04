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
import { Benutzer } from 'src/app/administration/models/benutzer';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { Rolle } from 'src/app/administration/models/rolle';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { BenutzerService } from 'src/app/administration/services/benutzer.service';
import { AutoCompleteOption } from 'src/app/form-elements/components/autocomplete-dropdown/autocomplete-dropdown.component';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

@Component({
  selector: 'rad-benutzer-editor',
  templateUrl: './benutzer-editor.component.html',
  styleUrls: ['./benutzer-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BenutzerEditorComponent implements DiscardableComponent {
  form: UntypedFormGroup;
  benutzerStatus: BenutzerStatus;
  version: number;
  organisationOptions: AutoCompleteOption[] = [];
  benutzerRollen: Rolle[] = [];

  fetchingNachSpeicherung = false;
  fetchingNachAktivAenderung = false;

  get isAktiv(): boolean {
    return this.benutzerStatus === BenutzerStatus.AKTIV;
  }

  get rollen(): EnumOption[] {
    return Rolle.options;
  }

  constructor(
    private route: ActivatedRoute,
    private benutzerService: BenutzerService,
    protected notifyUserService: NotifyUserService,
    private changeDetector: ChangeDetectorRef,
    private administrationRoutingService: AdministrationRoutingService
  ) {
    this.form = new UntypedFormGroup({
      status: new FormControl<string>(''),
      vorname: new FormControl<string>('', [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
      nachname: new FormControl<string>('', [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
      email: new FormControl<string>('', [RadvisValidators.email, RadvisValidators.isNotNullOrEmpty]),
      organisation: new FormControl<AutoCompleteOption | null>(null, [RadvisValidators.isNotNullOrEmpty]),
      rollen: new FormControl<Rolle[]>([], [RadvisValidators.isNotEmpty]),
    });
    this.resetForm(route.snapshot.data.benutzer);
    this.benutzerStatus = route.snapshot.data.benutzer.status;
    this.benutzerRollen = route.snapshot.data.benutzer.rollen;
    this.version = route.snapshot.data.benutzer.version;
    const organisationen: Verwaltungseinheit[] = route.snapshot.data.organisationen;
    this.organisationOptions = organisationen.map<AutoCompleteOption>(value => {
      return { name: value.name, id: value.id, displayText: Verwaltungseinheit.getDisplayName(value) };
    });
  }

  canDiscard = (): boolean => !this.form.dirty;

  onSave(): void {
    if (!this.form.valid) {
      this.notifyUserService.warn('Die Eingabe ist nicht korrekt und kann nicht gespeichert werden.');
      return;
    }

    this.fetchingNachSpeicherung = true;
    this.benutzerService
      .save({
        id: +this.route.snapshot.params.id,
        version: +this.version,
        nachname: this.form.value.nachname.trim(),
        vorname: this.form.value.vorname.trim(),
        email: this.form.value.email,
        organisation: this.form.value.organisation?.id,
        rollen: this.form.value.rollen,
      })
      .then(benutzer => {
        this.resetForm(benutzer);
        this.version = benutzer.version;
        this.notifyUserService.inform('Benutzer wurde erfolgreich gespeichert.');
      })
      .finally(() => {
        this.fetchingNachSpeicherung = false;
        this.changeDetector.markForCheck();
      });
  }

  onZurueck(): void {
    this.administrationRoutingService.toAdministration();
  }

  private resetForm(benutzer: Benutzer): void {
    this.form.reset({
      ...benutzer,
      status: BenutzerStatus.getDisplayName(benutzer.status),
      organisation: {
        name: benutzer.organisation.name,
        id: benutzer.organisation.id,
        displayText: Verwaltungseinheit.getDisplayName(benutzer.organisation),
      },
    });
    this.form.get('status')?.disable();
  }

  onAblehnen(): void {
    this.fetchingNachAktivAenderung = true;
    this.benutzerService
      .aendereBenutzerstatus(this.route.snapshot.params.id, this.version, 'ABGELEHNT')
      .then(benutzer => {
        this.resetForm(benutzer);
        this.benutzerStatus = benutzer.status;
        this.version = benutzer.version;
        const msg = 'Benutzer wurde abgelehnt.';
        this.notifyUserService.inform(msg);
      })
      .catch(() => {
        this.notifyUserService.warn('Der Status des Benutzers konnte nicht geändert werden. Bitte neu laden.');
      })
      .finally(() => {
        this.fetchingNachAktivAenderung = false;
        this.changeDetector.markForCheck();
      });
  }

  onAktivieren(): void {
    this.fetchingNachAktivAenderung = true;
    this.benutzerService
      .aendereBenutzerstatus(this.route.snapshot.params.id, this.version, 'AKTIV')
      .then(benutzer => {
        this.resetForm(benutzer);
        this.benutzerStatus = benutzer.status;
        this.version = benutzer.version;
        const msg = 'Benutzer wurde aktiviert.';
        this.notifyUserService.inform(msg);
      })
      .catch(() => {
        this.notifyUserService.warn('Der Status des Benutzers konnte nicht geändert werden. Bitte neu laden.');
      })
      .finally(() => {
        this.fetchingNachAktivAenderung = false;
        this.changeDetector.markForCheck();
      });
  }
}
