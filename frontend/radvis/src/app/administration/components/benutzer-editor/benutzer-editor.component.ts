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
import { FormControl, FormGroup, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { DateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatDatepickerIntl } from '@angular/material/datepicker';
import { ActivatedRoute } from '@angular/router';
import { Benutzer } from 'src/app/administration/models/benutzer';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { Rolle } from 'src/app/administration/models/rolle';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { BenutzerService } from 'src/app/administration/services/benutzer.service';
import { AutoCompleteOption } from 'src/app/form-elements/components/autocomplete-dropdown/autocomplete-dropdown.component';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { DeLocaleDateAdapter } from 'src/app/shared/components/de-locale-date-adapter';
import { RadvisMatDatepickerIntl } from 'src/app/shared/components/radvis-mat-datepicker-intl';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { MatomoTracker } from 'ngx-matomo-client';

@Component({
  selector: 'rad-benutzer-editor',
  templateUrl: './benutzer-editor.component.html',
  styleUrls: ['./benutzer-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: DateAdapter,
      useClass: DeLocaleDateAdapter,
    },
    { provide: MAT_DATE_LOCALE, useValue: 'de-DE' },
    { provide: MatDatepickerIntl, useClass: RadvisMatDatepickerIntl },
  ],
})
export class BenutzerEditorComponent implements DiscardableComponent {
  formGroup: UntypedFormGroup;
  benutzerStatus: BenutzerStatus;
  version: number;
  isAblaufdatumEnabled: boolean;
  organisationOptions: AutoCompleteOption[] = [];

  readonly today = new Date();
  fetchingNachSpeicherung = false;
  fetchingNachAktivAenderung = false;

  get canAktivieren(): boolean {
    return BenutzerStatus.canAktivieren(this.benutzerStatus);
  }

  get canAblehnen(): boolean {
    return BenutzerStatus.canAblehnen(this.benutzerStatus);
  }

  get rollen(): EnumOption[] {
    return Rolle.options;
  }

  private get benutzerId(): number {
    return +this.route.snapshot.params.id;
  }

  constructor(
    private route: ActivatedRoute,
    private benutzerService: BenutzerService,
    protected notifyUserService: NotifyUserService,
    private changeDetector: ChangeDetectorRef,
    private administrationRoutingService: AdministrationRoutingService,
    private matomoTracker: MatomoTracker
  ) {
    const benutzer: Benutzer = route.snapshot.data.benutzer;
    this.isAblaufdatumEnabled = benutzer.ablaufdatum !== null;

    this.formGroup = new FormGroup({
      status: new FormControl<string>(''),
      ablaufdatum: new FormControl<Date | null>(
        {
          value: null,
          disabled: !this.isAblaufdatumEnabled,
        },
        this.isNotNullIfEnabled
      ),
      vorname: new FormControl<string>('', [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
      nachname: new FormControl<string>('', [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
      email: new FormControl<string>('', [RadvisValidators.email, RadvisValidators.isNotNullOrEmpty]),
      organisation: new FormControl<AutoCompleteOption | null>(null, [RadvisValidators.isNotNullOrEmpty]),
      rollen: new FormControl<Rolle[]>([], [RadvisValidators.isNotEmpty]),
    });
    this.resetForm(benutzer);
    this.benutzerStatus = benutzer.status;
    this.version = benutzer.version;
    const organisationen: Verwaltungseinheit[] = route.snapshot.data.organisationen;
    this.organisationOptions = organisationen.map<AutoCompleteOption>(value => {
      return { name: value.name, id: value.id, displayText: Verwaltungseinheit.getDisplayName(value) };
    });
  }

  private isNotNullIfEnabled: ValidatorFn = ctrl => {
    if (this.isAblaufdatumEnabled && !ctrl.value) {
      return { isNotNullIfEnabled: 'Das Feld darf nicht leer sein' };
    }
    return null;
  };

  canDiscard = (): boolean => !this.formGroup.dirty;

  onToggleAblaufdatum(): void {
    if (this.isAblaufdatumEnabled) {
      this.disableAblaufdatum();
    } else {
      this.isAblaufdatumEnabled = true;
      this.formGroup.get('ablaufdatum')?.enable();
    }

    this.formGroup.markAsDirty();
  }

  disableAblaufdatum(): void {
    this.isAblaufdatumEnabled = false;
    const ablaufDatumFormControl = this.formGroup.get('ablaufdatum');
    ablaufDatumFormControl?.disable();
    ablaufDatumFormControl?.reset();
  }

  onSave(): void {
    if (!this.formGroup.valid) {
      this.notifyUserService.warn('Die Eingabe ist nicht korrekt und kann nicht gespeichert werden.');
      return;
    }

    this.fetchingNachSpeicherung = true;
    this.benutzerService
      .save({
        id: this.benutzerId,
        version: +this.version,
        ablaufdatum: this.isAblaufdatumEnabled ? this.convertAblaufdatumToIsoString() : null,
        nachname: this.formGroup.value.nachname.trim(),
        vorname: this.formGroup.value.vorname.trim(),
        email: this.formGroup.value.email,
        organisation: this.formGroup.value.organisation?.id,
        rollen: this.formGroup.value.rollen,
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

  private convertAblaufdatumToIsoString(): string {
    const date = new Date(this.formGroup.value.ablaufdatum);
    return new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate())).toISOString();
  }

  onZurueck(): void {
    this.administrationRoutingService.toAdministration();
  }

  private resetForm(benutzer: Benutzer): void {
    this.formGroup.reset({
      ...benutzer,
      status: BenutzerStatus.getDisplayName(benutzer.status),
      organisation: {
        name: benutzer.organisation.name,
        id: benutzer.organisation.id,
        displayText: Verwaltungseinheit.getDisplayName(benutzer.organisation),
      },
    });
    this.formGroup.get('status')?.disable();
  }

  onAblehnen(): void {
    this.fetchingNachAktivAenderung = true;
    this.benutzerService
      .aendereBenutzerstatus(this.route.snapshot.params.id, this.version, 'ABGELEHNT')
      .then(benutzer => {
        this.resetForm(benutzer);
        this.benutzerStatus = benutzer.status;
        this.version = benutzer.version;
        this.disableAblaufdatum();
        const msg = 'Benutzer wurde abgelehnt.';
        this.notifyUserService.inform(msg);
      })
      .finally(() => {
        this.fetchingNachAktivAenderung = false;
        this.changeDetector.markForCheck();
      });
  }

  onAktivieren(): void {
    this.matomoTracker.trackEvent('Editor', 'Freischalten', 'Benutzer');

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
