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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { AbstractControl, FormControl, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { AutoCompleteOption } from 'src/app/form-elements/components/autocomplete-dropdown/autocomplete-dropdown.component';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Durchfuehrungszeitraum } from 'src/app/shared/models/durchfuehrungszeitraum';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { convertVerwaltungseinheitToAutocompleteOption } from 'src/app/shared/services/option-converter';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { CreateMassnahmeCommand } from 'src/app/viewer/massnahme/models/create-massnahme-command';
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { MASSNAHMENKATEGORIEN, Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';

@Component({
  selector: 'rad-massnahmen-creator',
  templateUrl: './massnahmen-creator.component.html',
  styleUrls: ['./massnahmen-creator.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MassnahmenCreatorComponent implements OnDestroy, DiscardableComponent {
  public formGroup: UntypedFormGroup;
  public umsetzungsstatusOptions = Umsetzungsstatus.options;
  public massnahmenkategorieOptions = MASSNAHMENKATEGORIEN;
  public alleOrganisationenOptions: Observable<AutoCompleteOption[]>;
  public sollStandardOptions = SollStandard.options;
  public handlungsverantwortlicherOptions = Handlungsverantwortlicher.options;
  public konzeptionsquelleOptions = Konzeptionsquelle.options.filter(
    o => o.name !== Konzeptionsquelle.RADNETZ_MASSNAHME
  );

  public isFetching = false;

  public MASSNAHMEN = MASSNAHMEN;

  private subscriptions: Subscription[] = [];
  private saved = false;

  constructor(
    private massnahmenRoutingService: MassnahmenRoutingService,
    private viewerRoutingService: ViewerRoutingService,
    organisationenService: OrganisationenService,
    private massnahmeService: MassnahmeService,
    private changeDetectorRef: ChangeDetectorRef,
    private notifyUserService: NotifyUserService,
    private massnahmeFilterService: MassnahmeFilterService,
    private benutzerDetailsService: BenutzerDetailsService
  ) {
    this.formGroup = new UntypedFormGroup({
      netzbezug: new UntypedFormControl(null),
      umsetzungsstatus: new UntypedFormControl(Umsetzungsstatus.IDEE),
      massnahmenkategorien: new UntypedFormControl(
        [],
        [Massnahmenkategorien.isValidMassnahmenKategorienCombination, RadvisValidators.isNotEmpty]
      ),
      bezeichnung: new UntypedFormControl(null, [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
      veroeffentlicht: new UntypedFormControl(false),
      planungErforderlich: new UntypedFormControl(false),
      durchfuehrungszeitraum: new UntypedFormControl(null, [
        RadvisValidators.isPositiveInteger,
        RadvisValidators.between(2000, 3000),
      ]),
      baulastZustaendiger: new UntypedFormControl(null),
      zustaendiger: new FormControl<AutoCompleteOption | null>(
        this.getAktuellerBenutzerOrganisationAutoCompleteOption(),
        RadvisValidators.isNotNullOrEmpty
      ),
      sollStandard: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
      handlungsverantwortlicher: new UntypedFormControl(null),
      konzeptionsquelle: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
      sonstigeKonzeptionsquelle: new UntypedFormControl(null),
    });
    this.alleOrganisationenOptions = organisationenService
      .getAlleOrganisationen()
      .pipe(
        map(verwaltungseinheiten =>
          verwaltungseinheiten.map(value => convertVerwaltungseinheitToAutocompleteOption(value))
        )
      );

    this.subscriptions.push(
      (this.formGroup.get('umsetzungsstatus') as AbstractControl).valueChanges.subscribe(
        this.onUmsetzungsstatusChanged
      ),
      (this.formGroup.get('konzeptionsquelle') as AbstractControl).valueChanges.subscribe(
        this.onKonzeptionsquelleChanged
      )
    );
  }

  canDiscard(): boolean {
    return this.saved || this.formGroup.pristine;
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  onReset(): void {
    this.formGroup.reset();
    this.formGroup.get('umsetzungsstatus')?.setValue(Umsetzungsstatus.IDEE);
    this.formGroup.get('veroeffentlicht')?.setValue(false);
    this.formGroup.get('planungErforderlich')?.setValue(false);
    this.formGroup.get('massnahmenkategorien')?.setValue([]);
    this.formGroup.get('zustaendiger')?.setValue(this.getAktuellerBenutzerOrganisationAutoCompleteOption());
  }

  onSave(): void {
    let durchfuehrungszeitraum = null;
    if (this.formGroup.get('durchfuehrungszeitraum')?.value) {
      durchfuehrungszeitraum = {
        geplanterUmsetzungsstartJahr: Number(this.formGroup.get('durchfuehrungszeitraum')?.value),
      } as Durchfuehrungszeitraum;
    }

    if (this.formGroup.pristine) {
      return;
    }

    if (this.formGroup.invalid) {
      this.notifyUserService.warn('Das Formular kann nicht gespeichert werden, weil es ungültige Einträge enthält.');
      return;
    }

    const command: CreateMassnahmeCommand = {
      bezeichnung: this.formGroup.get('bezeichnung')?.value,
      massnahmenkategorien: this.formGroup.get('massnahmenkategorien')?.value,
      netzbezug: this.formGroup.get('netzbezug')?.value,
      umsetzungsstatus: this.formGroup.get('umsetzungsstatus')?.value,
      veroeffentlicht: this.formGroup.get('veroeffentlicht')?.value || false,
      planungErforderlich: this.formGroup.get('planungErforderlich')?.value || false,
      durchfuehrungszeitraum,
      baulastZustaendigerId: this.formGroup.get('baulastZustaendiger')?.value?.id || null,
      zustaendigerId: this.formGroup.get('zustaendiger')?.value.id,
      sollStandard: this.formGroup.get('sollStandard')?.value,
      handlungsverantwortlicher: this.formGroup.get('handlungsverantwortlicher')?.value,
      konzeptionsquelle: this.formGroup.get('konzeptionsquelle')?.value,
      sonstigeKonzeptionsquelle: this.formGroup.get('sonstigeKonzeptionsquelle')?.value,
    };

    this.isFetching = true;

    this.massnahmeService
      .createMassnahme(command)
      .then((id: number) => {
        this.saved = true;
        this.notifyUserService.inform('Maßnahme wurde erfolgreich gespeichert.');
        this.massnahmenRoutingService.toInfrastrukturEditor(id);
      })
      .finally(() => {
        this.isFetching = false;
        this.massnahmeFilterService.refetchData();
        this.changeDetectorRef.markForCheck();
      });
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  umsetzungsstatusAbPlanung(): boolean {
    return this.formGroup.get('umsetzungsstatus')?.value !== Umsetzungsstatus.IDEE;
  }

  sonstigeKonzeptionsquelle(): boolean {
    return this.formGroup.get('konzeptionsquelle')?.value === Konzeptionsquelle.SONSTIGE;
  }

  private getAktuellerBenutzerOrganisationAutoCompleteOption(): AutoCompleteOption | null {
    const aktuellerBenutzerOrganisation = this.benutzerDetailsService.aktuellerBenutzerOrganisation();
    return aktuellerBenutzerOrganisation
      ? convertVerwaltungseinheitToAutocompleteOption(aktuellerBenutzerOrganisation)
      : null;
  }

  private onUmsetzungsstatusChanged = (newValue: Umsetzungsstatus): void => {
    const durchfuehrungszeitraumControl = this.formGroup.get('durchfuehrungszeitraum') as AbstractControl;
    const baulastZustaendigerControl = this.formGroup.get('baulastZustaendiger') as AbstractControl;
    const handlungsverantwortlicherControl = this.formGroup.get('handlungsverantwortlicher') as AbstractControl;
    if (newValue !== Umsetzungsstatus.IDEE) {
      durchfuehrungszeitraumControl.setValidators([
        RadvisValidators.isPositiveInteger,
        RadvisValidators.between(2000, 3000),
        RadvisValidators.isNotNullOrEmpty,
      ]);
      baulastZustaendigerControl.setValidators(RadvisValidators.isNotNullOrEmpty);
      handlungsverantwortlicherControl.setValidators(RadvisValidators.isNotNullOrEmpty);
    } else {
      durchfuehrungszeitraumControl.setValidators([
        RadvisValidators.isPositiveInteger,
        RadvisValidators.between(2000, 3000),
      ]);
      baulastZustaendigerControl.setValidators(null);
      handlungsverantwortlicherControl.setValidators(null);
    }
    durchfuehrungszeitraumControl.updateValueAndValidity();
    baulastZustaendigerControl.updateValueAndValidity();
    handlungsverantwortlicherControl.updateValueAndValidity();
  };

  private onKonzeptionsquelleChanged = (newValue: Konzeptionsquelle): void => {
    const sonstigeKonzeptionsquelleControl = this.formGroup.get('sonstigeKonzeptionsquelle') as AbstractControl;
    if (newValue === Konzeptionsquelle.SONSTIGE) {
      sonstigeKonzeptionsquelleControl.setValidators(RadvisValidators.isNotNullOrEmpty);
    } else {
      sonstigeKonzeptionsquelleControl.setValidators(null);
    }
    sonstigeKonzeptionsquelleControl.updateValueAndValidity();
  };
}
