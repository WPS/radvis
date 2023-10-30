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
import { ActivatedRoute } from '@angular/router';
import { CreateOrganisationCommand } from 'src/app/administration/models/create-organisation-command';
import { SaveOrganisationCommand } from 'src/app/administration/models/save-organisation-command';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { OrganisationenVerwaltungService } from 'src/app/administration/services/organisationen-verwaltung.service';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Organisation } from 'src/app/shared/models/organisation-edit-view';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { DiscardGuard } from 'src/app/shared/services/discard-guard.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-organisation-editor',
  templateUrl: './organisation-editor.component.html',
  styleUrls: ['./organisation-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganisationEditorComponent implements DiscardGuard {
  form: FormGroup = new FormGroup({
    name: new FormControl(null, [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
    uebergeordneteOrganisation: new FormControl(null),
    organisationsArt: new FormControl(null, [RadvisValidators.isNotNullOrEmpty]),
    zustaendigFuerBereichOf: new FormControl([]),
  });

  fetchingNachSpeicherung = false;
  fetchingNachAktivAenderung = false;
  bearbeitenFuerBenutzerVerboten = false;
  organisationAktiv = false;

  organisationsArten: EnumOption[] = OrganisationsArt.options.filter(
    enumOption => !OrganisationsArt.istGebietskoerperschaft(enumOption.name)
  );
  zuweisbareOrganisationen$: Promise<Verwaltungseinheit[]>;
  isCreator = false;
  private currentVersion: number | null = null;

  get currentId(): number | null {
    const idFromRoute = this.route.snapshot.paramMap.get('id');
    if (idFromRoute) {
      return +idFromRoute;
    }
    return null;
  }

  constructor(
    private route: ActivatedRoute,
    protected notifyUserService: NotifyUserService,
    private changeDetector: ChangeDetectorRef,
    private administrationRoutingService: AdministrationRoutingService,
    private organisationVerwaltungService: OrganisationenVerwaltungService
  ) {
    this.zuweisbareOrganisationen$ = this.organisationVerwaltungService.getAlleZuweisbaren();
    route.data.subscribe(data => {
      this.isCreator = data.isCreator;
      let organisation: Organisation | null = null;
      if (!this.isCreator) {
        organisation = data.organisation as Organisation;
        this.organisationAktiv = organisation.aktiv;
        invariant(organisation);
        this.bearbeitenFuerBenutzerVerboten = !organisation.aktuellerBenutzerDarfBearbeiten;
        this.currentVersion = organisation.version;
      } else {
        this.bearbeitenFuerBenutzerVerboten = false;
        this.currentVersion = null;
      }

      this.resetForm(organisation);
    });
  }

  canDiscard = (): boolean => !this.form.dirty;

  onSave(): void {
    if (!this.form.dirty) {
      return;
    }

    if (!this.form.valid) {
      this.notifyUserService.warn('Die Eingabe ist nicht korrekt und kann nicht gespeichert werden.');
      return;
    }

    let savePromise: Promise<void>;

    if (!this.isCreator) {
      invariant(this.currentId);
      invariant(this.currentVersion !== null);
      const formValue = this.form.getRawValue();
      const command: SaveOrganisationCommand = {
        id: this.currentId,
        name: formValue.name.trim(),
        organisationsArt: formValue.organisationsArt,
        zustaendigFuerBereichOf: formValue.zustaendigFuerBereichOf.map((org: Verwaltungseinheit) => org.id),
        version: this.currentVersion,
      };
      savePromise = this.organisationVerwaltungService.save(command).then(organisation => {
        this.notifyUserService.inform('Organisation wurde erfolgreich gespeichert.');
        this.resetForm(organisation);
        this.currentVersion = organisation.version;
      });
    } else {
      const command: CreateOrganisationCommand = {
        name: this.form.value.name.trim(),
        organisationsArt: this.form.value.organisationsArt,
        zustaendigFuerBereichOf: this.form.value.zustaendigFuerBereichOf.map((org: Verwaltungseinheit) => org.id),
      };
      savePromise = this.organisationVerwaltungService.create(command).then(v => {
        this.form.markAsPristine();
        this.notifyUserService.inform('Organisation wurde erfolgreich erstellt.');
        this.administrationRoutingService.toOrganisationEditor(v.id);
      });
    }

    this.fetchingNachSpeicherung = true;
    savePromise.finally(() => {
      this.fetchingNachSpeicherung = false;
      this.changeDetector.markForCheck();
    });
  }

  onToggleAktiv(): void {
    invariant(this.currentId);
    this.fetchingNachAktivAenderung = true;
    this.organisationVerwaltungService
      .aendereOrganisationAktiv(this.currentId, !this.organisationAktiv)
      .then(organisation => {
        this.resetForm(organisation);
        this.currentVersion = organisation.version;
        this.organisationAktiv = organisation.aktiv;
        this.notifyUserService.inform(`Die Organisation wurde ${this.organisationAktiv ? '' : 'de'}aktiviert.`);
      })
      .catch(() => {
        this.notifyUserService.warn(
          `Die Organisation konnte nicht ${this.organisationAktiv ? 'de' : ''}aktiviert werden. Bitte neu laden.`
        );
      })
      .finally(() => {
        this.fetchingNachAktivAenderung = false;
        this.changeDetector.markForCheck();
      });
  }

  onZurueck(): void {
    this.administrationRoutingService.toOrganisationListe();
  }

  private resetForm(organisation: Organisation | null): void {
    this.form.reset({
      name: organisation?.name,
      uebergeordneteOrganisation: Verwaltungseinheit.getDisplayName(organisation?.uebergeordneteOrganisation),
      organisationsArt: organisation?.organisationsArt,
      zustaendigFuerBereichOf: organisation?.zustaendigFuerBereichOf ?? [],
    });

    this.form.get('uebergeordneteOrganisation')?.disable();

    if (this.bearbeitenFuerBenutzerVerboten) {
      this.form.get('name')?.disable();
      this.form.get('organisationsArt')?.disable();
    } else {
      this.form.get('name')?.enable();
      this.form.get('organisationsArt')?.enable();
    }
  }
}
