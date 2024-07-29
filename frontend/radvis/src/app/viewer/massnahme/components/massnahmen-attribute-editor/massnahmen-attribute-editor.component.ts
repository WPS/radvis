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

import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { AbstractControl, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { AutoCompleteOption } from 'src/app/form-elements/components/autocomplete-dropdown/autocomplete-dropdown.component';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Durchfuehrungszeitraum } from 'src/app/shared/models/durchfuehrungszeitraum';
import { NetzklasseFlat } from 'src/app/shared/models/netzklasse-flat';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NetzklassenConverterService } from 'src/app/shared/services/netzklassen-converter.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { convertVerwaltungseinheitToAutocompleteOption } from 'src/app/shared/services/option-converter';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { Handlungsverantwortlicher } from 'src/app/viewer/massnahme/models/handlungsverantwortlicher';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { Massnahme } from 'src/app/viewer/massnahme/models/massnahme';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';
import { Realisierungshilfe } from 'src/app/viewer/massnahme/models/realisierungshilfe';
import { SaveMassnahmeCommand } from 'src/app/viewer/massnahme/models/save-massnahme-command';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmeNetzbezugDisplayService } from 'src/app/viewer/massnahme/services/massnahme-netzbezug-display.service';
import { MassnahmeUpdatedService } from 'src/app/viewer/massnahme/services/massnahme-updated.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-massnahmen-attribute-editor',
  templateUrl: './massnahmen-attribute-editor.component.html',
  styleUrls: [
    './massnahmen-attribute-editor.component.scss',
    '../../../../form-elements/components/attribute-editor/attribut-editor.scss',
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MassnahmenAttributeEditorComponent implements OnDestroy, DiscardableComponent {
  public currentMassnahme: Massnahme | null = null;

  public formGroup: UntypedFormGroup;
  public umsetzungsstatusOptions = Umsetzungsstatus.options;
  public alleOrganisationenOptions: Observable<AutoCompleteOption[]>;
  public sollStandardOptions = SollStandard.options;
  public handlungsverantwortlicherOptions = Handlungsverantwortlicher.options;

  public konzeptionsquelleOptions = Konzeptionsquelle.options;

  public realisierungshilfeOptions: AutoCompleteOption[];

  public MASSNAHMEN = MASSNAHMEN;

  public isFetching = false;
  private subscriptions: Subscription[] = [];

  constructor(
    organisationenService: OrganisationenService,
    private massnahmeService: MassnahmeService,
    private notifyUserService: NotifyUserService,
    private massnahmeFilterService: MassnahmeFilterService,
    private activatedRoute: ActivatedRoute,
    private viewerRoutingService: ViewerRoutingService,
    private massnahmeUpdatedService: MassnahmeUpdatedService,
    private massnahmenRoutingService: MassnahmenRoutingService,
    massnahmeNetzbezugDisplayService: MassnahmeNetzbezugDisplayService
  ) {
    massnahmeNetzbezugDisplayService.showNetzbezug(false);
    this.formGroup = new UntypedFormGroup({
      bezeichnung: new UntypedFormControl(null, [RadvisValidators.isNotNullOrEmpty, RadvisValidators.maxLength(255)]),
      massnahmenkategorien: new UntypedFormControl(
        [],
        [Massnahmenkategorien.isValidMassnahmenKategorienCombination, RadvisValidators.isNotEmpty]
      ),
      netzbezug: new UntypedFormControl(null),
      umsetzungsstatus: new UntypedFormControl(Umsetzungsstatus.IDEE),
      veroeffentlicht: new UntypedFormControl(false),
      planungErforderlich: new UntypedFormControl(false),
      durchfuehrungszeitraum: new UntypedFormControl(null, [
        RadvisValidators.isPositiveInteger,
        RadvisValidators.between(2000, 3000),
      ]),
      baulastZustaendiger: new UntypedFormControl(null),
      prioritaet: new UntypedFormControl(null, [RadvisValidators.isPositiveInteger, RadvisValidators.between(1, 10)]),
      kostenannahme: new UntypedFormControl(null, [
        RadvisValidators.isPositiveInteger,
        RadvisValidators.between(0, 1000000000),
      ]),
      netzklassen: NetzklassenConverterService.createFormGroup(),
      zustaendiger: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
      unterhaltsZustaendiger: new UntypedFormControl(null),
      letzteAenderung: new UntypedFormControl({ value: '', disabled: true }),
      benutzerLetzteAenderung: new UntypedFormControl({ value: '', disabled: true }),
      maViSID: new UntypedFormControl(null, RadvisValidators.maxLength(255)),
      verbaID: new UntypedFormControl(null, RadvisValidators.maxLength(255)),
      lgvfgid: new UntypedFormControl(null, RadvisValidators.maxLength(255)),
      massnahmeKonzeptID: new UntypedFormControl(null, [
        RadvisValidators.isValidMassnahmeKonzeptId,
        RadvisValidators.maxLength(255),
      ]),
      sollStandard: new UntypedFormControl(null),
      handlungsverantwortlicher: new UntypedFormControl(null),
      konzeptionsquelle: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
      sonstigeKonzeptionsquelle: new UntypedFormControl(null),
      realisierungshilfe: new UntypedFormControl(null),
    });

    this.realisierungshilfeOptions = Realisierungshilfe.options.map(option => ({
      name: option.name,
      displayText: option.displayText,
    }));

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
      ),
      this.activatedRoute.data.subscribe(data => {
        this.currentMassnahme = data.massnahme;
        invariant(this.currentMassnahme);
        this.resetForm(this.currentMassnahme);
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  onReset(): void {
    invariant(this.currentMassnahme);
    this.resetForm(this.currentMassnahme);
  }

  onSave(): void {
    invariant(this.currentMassnahme);
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

    const newNetzklassen = NetzklassenConverterService.convertFlatToNetzklassen(
      this.formGroup.getRawValue().netzklassen
    );

    const command: SaveMassnahmeCommand = {
      id: this.currentMassnahme.id,
      version: this.currentMassnahme.version,
      bezeichnung: this.formGroup.get('bezeichnung')?.value,
      massnahmenkategorien: this.formGroup.get('massnahmenkategorien')?.value,
      netzbezug: this.formGroup.get('netzbezug')?.value,
      umsetzungsstatus: this.formGroup.get('umsetzungsstatus')?.value,
      veroeffentlicht: this.formGroup.get('veroeffentlicht')?.value || false,
      planungErforderlich: this.formGroup.get('planungErforderlich')?.value || false,
      durchfuehrungszeitraum,
      baulastZustaendigerId: this.formGroup.get('baulastZustaendiger')?.value?.id || null,

      prioritaet: this.formGroup.get('prioritaet')?.value || null,
      kostenannahme: this.formGroup.get('kostenannahme')?.value || null,
      netzklassen: newNetzklassen,
      zustaendigerId: this.formGroup.get('zustaendiger')?.value.id,
      unterhaltsZustaendigerId: this.formGroup.get('unterhaltsZustaendiger')?.value?.id || null,
      maViSID: this.formGroup.get('maViSID')?.value,
      verbaID: this.formGroup.get('verbaID')?.value,
      lgvfgid: this.formGroup.get('lgvfgid')?.value,
      massnahmeKonzeptID: this.formGroup.get('massnahmeKonzeptID')?.value,
      sollStandard: this.formGroup.get('sollStandard')?.value,
      handlungsverantwortlicher: this.formGroup.get('handlungsverantwortlicher')?.value,
      konzeptionsquelle: this.formGroup.get('konzeptionsquelle')?.value,
      sonstigeKonzeptionsquelle: this.formGroup.get('sonstigeKonzeptionsquelle')?.value,
      realisierungshilfe: this.formGroup.get('realisierungshilfe')?.value?.name || null,
    };

    this.isFetching = true;

    this.massnahmeService
      .saveMassnahme(command)
      .then((massnahme: Massnahme) => {
        let umsetzungsstandabfrageAktualisieren = false;
        umsetzungsstandabfrageAktualisieren =
          this.currentMassnahme?.umsetzungsstatus !== massnahme.umsetzungsstatus &&
          Konzeptionsquelle.isRadNetzMassnahme(massnahme.konzeptionsquelle) &&
          (massnahme.umsetzungsstatus === Umsetzungsstatus.STORNIERT ||
            massnahme.umsetzungsstatus === Umsetzungsstatus.UMGESETZT);

        this.notifyUserService.inform('Maßnahme wurde erfolgreich gespeichert.');
        this.massnahmeUpdatedService.updateMassnahme();
        this.currentMassnahme = massnahme;
        this.resetForm(massnahme);

        if (umsetzungsstandabfrageAktualisieren) {
          this.massnahmenRoutingService.toUmsetzungstandEditor(this.currentMassnahme.id);
        }
      })
      .finally(() => {
        this.isFetching = false;
        this.massnahmeFilterService.refetchData();
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

  canDiscard(): boolean {
    return this.formGroup.pristine;
  }

  get isRadNETZMassnahme(): boolean {
    if (this.currentMassnahme) {
      return Konzeptionsquelle.isRadNetzMassnahme(this.currentMassnahme.konzeptionsquelle);
    }

    return false;
  }

  private resetForm(massnahme: Massnahme): void {
    const benutzer = massnahme.benutzerLetzteAenderung;
    const realisierungshilfe: AutoCompleteOption | null = massnahme.realisierungshilfe
      ? {
          displayText: Realisierungshilfe.options.find(value => value.name === massnahme.realisierungshilfe)!
            .displayText,
          name: massnahme.realisierungshilfe,
        }
      : null;
    this.formGroup.reset({
      ...massnahme,
      zustaendiger: convertVerwaltungseinheitToAutocompleteOption(massnahme.zustaendiger),
      baulastZustaendiger: massnahme.baulastZustaendiger
        ? convertVerwaltungseinheitToAutocompleteOption(massnahme.baulastZustaendiger)
        : null,
      unterhaltsZustaendiger: massnahme.unterhaltsZustaendiger
        ? convertVerwaltungseinheitToAutocompleteOption(massnahme.unterhaltsZustaendiger)
        : null,
      realisierungshilfe: realisierungshilfe,
      massnahmenkategorien: [...massnahme.massnahmenkategorien],
      benutzerLetzteAenderung: benutzer.vorname + ' ' + benutzer.nachname,
      letzteAenderung: new DatePipe('en-US').transform(new Date(massnahme.letzteAenderung), 'dd.MM.yy HH:mm') as string,
      durchfuehrungszeitraum: massnahme.durchfuehrungszeitraum?.geplanterUmsetzungsstartJahr,
    });

    this.formGroup.patchValue({
      netzklassen: NetzklassenConverterService.convertToFlat<NetzklasseFlat>(massnahme.netzklassen),
    });

    if (massnahme.canEdit) {
      this.formGroup.enable({ emitEvent: false });
    }

    this.formGroup.get('letzteAenderung')?.disable({ emitEvent: false });
    this.formGroup.get('benutzerLetzteAenderung')?.disable({ emitEvent: false });

    if (Konzeptionsquelle.isRadNetzMassnahme(massnahme.konzeptionsquelle)) {
      this.formGroup.get('konzeptionsquelle')?.disable({ emitEvent: false });
    } else {
      this.formGroup.get('konzeptionsquelle')?.enable({ emitEvent: false });
    }

    if (!massnahme.canEdit) {
      this.formGroup.disable({ emitEvent: false });
    }
  }

  private onUmsetzungsstatusChanged = (newValue: Umsetzungsstatus): void => {
    const durchfuehrungszeitraumControl = this.formGroup.get('durchfuehrungszeitraum') as AbstractControl;
    const baulastZustaendigerControl = this.formGroup.get('baulastZustaendiger') as AbstractControl;
    const handlungsverantwortlicherControl = this.formGroup.get('handlungsverantwortlicher') as AbstractControl;
    if ([Umsetzungsstatus.UMSETZUNG, Umsetzungsstatus.UMGESETZT, Umsetzungsstatus.PLANUNG].includes(newValue)) {
      durchfuehrungszeitraumControl.setValidators([
        RadvisValidators.isPositiveInteger,
        RadvisValidators.isNotNullOrEmpty,
        RadvisValidators.between(2000, 3000),
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
      sonstigeKonzeptionsquelleControl.addValidators(RadvisValidators.isNotNullOrEmpty);
    } else {
      sonstigeKonzeptionsquelleControl.removeValidators(RadvisValidators.isNotNullOrEmpty);
    }
    sonstigeKonzeptionsquelleControl.updateValueAndValidity();
  };
}
