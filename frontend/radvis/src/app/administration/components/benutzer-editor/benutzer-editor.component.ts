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
import { FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { Rolle } from 'src/app/administration/models/rolle';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { BenutzerService } from 'src/app/administration/services/benutzer.service';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { createBenutzerForm } from 'src/app/shared/services/benutzer-form.factory';
import { DiscardGuard } from 'src/app/shared/services/discard-guard.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

@Component({
  selector: 'rad-benutzer-editor',
  templateUrl: './benutzer-editor.component.html',
  styleUrls: ['./benutzer-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BenutzerEditorComponent implements DiscardGuard {
  form: FormGroup;
  benutzerStatus: BenutzerStatus;
  version: number;
  organisationen: Verwaltungseinheit[] = [];
  benutzerRollen: Rolle[] = [];

  fetchingNachSpeicherung = false;
  fetchingNachAktivAenderung = false;

  get isAktiv(): boolean {
    return this.benutzerStatus === BenutzerStatus.AKTIV;
  }

  constructor(
    private route: ActivatedRoute,
    private benutzerService: BenutzerService,
    protected notifyUserService: NotifyUserService,
    private changeDetector: ChangeDetectorRef,
    private administrationRoutingService: AdministrationRoutingService
  ) {
    this.form = createBenutzerForm();
    this.form.reset(route.snapshot.data.benutzer);
    this.organisationen = route.snapshot.data.organisationen;
    this.benutzerStatus = route.snapshot.data.benutzer.status;
    this.benutzerRollen = route.snapshot.data.benutzer.rollen;
    this.version = route.snapshot.data.benutzer.version;
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
        this.form.reset(benutzer);
        this.version = benutzer.version;
        this.notifyUserService.inform('Benutzer wurde erfolgreich gespeichert.');
      })
      .finally(() => {
        this.fetchingNachSpeicherung = false;
        this.changeDetector.markForCheck();
      });
  }

  onStatusAendern(): void {
    this.fetchingNachAktivAenderung = true;
    this.benutzerService
      .aendereBenutzerstatus(this.route.snapshot.params.id, this.version, this.benutzerStatus)
      .then(benutzer => {
        this.benutzerStatus = benutzer.status;
        this.version = benutzer.version;
        const msg = benutzer.status === 'AKTIV' ? 'Benutzer wurde Aktiviert.' : 'Benutzer wurde Deaktiviert.';
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

  onZurueck(): void {
    this.administrationRoutingService.toAdministration();
  }

  get rollen(): EnumOption[] {
    return Rolle.options;
  }
}
