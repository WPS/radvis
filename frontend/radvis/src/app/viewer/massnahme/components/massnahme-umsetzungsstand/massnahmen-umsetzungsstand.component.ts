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

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { MatomoTracker } from 'ngx-matomo-client';
import { Subscription } from 'rxjs';
import { AbstractEventTrackedEditor } from 'src/app/form-elements/components/abstract-event-tracked-editor';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { HinweisDialogComponent } from 'src/app/shared/components/hinweis-dialog/hinweis-dialog.component';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { GrundFuerAbweichungZumMassnahmenblatt } from 'src/app/viewer/massnahme/models/grund-fuer-abweichung-zum-massnahmenblatt';
import { GrundFuerNichtUmsetzungDerMassnahme } from 'src/app/viewer/massnahme/models/grund-fuer-nicht-umsetzung-der-massnahme';
import { PruefungQualitaetsstandardsErfolgt } from 'src/app/viewer/massnahme/models/pruefung-qualitaetsstandards-erfolgt';
import { SaveUmsetzungsstandCommand } from 'src/app/viewer/massnahme/models/save-umsetzungsstand-command';
import { Umsetzungsstand } from 'src/app/viewer/massnahme/models/umsetzungsstand';
import { UmsetzungsstandStatus } from 'src/app/viewer/massnahme/models/umsetzungsstand-status';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmeNetzbezugDisplayService } from 'src/app/viewer/massnahme/services/massnahme-netzbezug-display.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-massnahmen-umsetzungsstand',
  templateUrl: './massnahmen-umsetzungsstand.component.html',
  styleUrls: [
    './massnahmen-umsetzungsstand.component.scss',
    '../../../../form-elements/components/attribute-editor/attribut-editor.scss',
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class MassnahmenUmsetzungsstandComponent
  extends AbstractEventTrackedEditor
  implements OnDestroy, DiscardableComponent
{
  public isFetching = false;

  public formGroup: UntypedFormGroup;

  public grundFuerAbweichungZumMassnahmenblattOptions = GrundFuerAbweichungZumMassnahmenblatt.options;
  public pruefungQualitaetsstandardsErfolgtOptions = PruefungQualitaetsstandardsErfolgt.options;
  public grundFuerNichtUmsetzungDerMassnahmeOptions = GrundFuerNichtUmsetzungDerMassnahme.options;

  public umsetzungsstand: Umsetzungsstand | null = null;

  private subscriptions: Subscription[] = [];

  constructor(
    private massnahmeService: MassnahmeService,
    private massnahmeFilterService: MassnahmeFilterService,
    private notifyUserService: NotifyUserService,
    public dialog: MatDialog,
    matomoTracker: MatomoTracker,
    activatedRoute: ActivatedRoute,
    massnahmeNetzbezugDisplayService: MassnahmeNetzbezugDisplayService
  ) {
    super(matomoTracker, activatedRoute);

    massnahmeNetzbezugDisplayService.showNetzbezug(true);
    this.formGroup = new UntypedFormGroup({
      umsetzungGemaessMassnahmenblatt: new UntypedFormControl(null),
      grundFuerAbweichungZumMassnahmenblatt: new UntypedFormControl(null),
      pruefungQualitaetsstandardsErfolgt: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
      beschreibungAbweichenderMassnahme: new UntypedFormControl(null, [RadvisValidators.maxLength(3000)]),
      kostenDerMassnahme: new UntypedFormControl(null, [
        RadvisValidators.isPositiveInteger,
        RadvisValidators.between(0, 1000000000),
      ]),
      grundFuerNichtUmsetzungDerMassnahme: new UntypedFormControl(
        null,
        this.validateGrundFuerNichtUmsetzungDerMassnahme
      ),
      anmerkung: new UntypedFormControl('', [RadvisValidators.maxLength(3000)]),
    });

    this.subscriptions.push(
      this.activatedRoute.data.subscribe(data => {
        this.umsetzungsstand = data.umsetzungsstand;
        invariant(this.umsetzungsstand);
        this.resetForm(this.umsetzungsstand);
      })
    );
  }

  private validateGrundFuerNichtUmsetzungDerMassnahme: ValidatorFn = (
    control: AbstractControl
  ): ValidationErrors | null => {
    if (
      (this.umsetzungsstand?.massnahmeUmsetzungsstatus === Umsetzungsstatus.IDEE ||
        this.umsetzungsstand?.massnahmeUmsetzungsstatus === Umsetzungsstatus.STORNIERUNG_ANGEFRAGT ||
        Umsetzungsstatus.isStorniert(this.umsetzungsstand?.massnahmeUmsetzungsstatus)) &&
      this.umsetzungsstand?.umsetzungsstandStatus === UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT &&
      !control.value
    ) {
      return {
        grundFuerNichtUmsetzungDerMassnahmeNichtVorhanden:
          'Das Feld darf nicht leer sein, wenn die Maßnahme im Status "Idee" oder storniert ist',
      };
    }
    return null;
  };

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    invariant(this.umsetzungsstand);
  }

  onSave(): void {
    if (this.isFetching) {
      return;
    }

    if (this.formGroup.invalid) {
      this.notifyUserService.warn('Das Formular kann nicht gespeichert werden, weil es ungültige Einträge enthält.');
      return;
    }

    invariant(this.umsetzungsstand);

    this.isFetching = true;

    this.trackSpeichernEvent();

    const { id, version } = this.umsetzungsstand;

    this.massnahmeService
      .saveUmsetzungsstand({
        id,
        version,
        umsetzungGemaessMassnahmenblatt: this.formGroup.get('umsetzungGemaessMassnahmenblatt')?.value,
        grundFuerAbweichungZumMassnahmenblatt: this.formGroup.get('grundFuerAbweichungZumMassnahmenblatt')?.value,
        pruefungQualitaetsstandardsErfolgt: this.formGroup.get('pruefungQualitaetsstandardsErfolgt')?.value,
        beschreibungAbweichenderMassnahme: this.formGroup.get('beschreibungAbweichenderMassnahme')?.value || '',
        kostenDerMassnahme: this.formGroup.get('kostenDerMassnahme')?.value,
        grundFuerNichtUmsetzungDerMassnahme: this.formGroup.get('grundFuerNichtUmsetzungDerMassnahme')?.value,
        anmerkung: this.formGroup.get('anmerkung')?.value,
      } as SaveUmsetzungsstandCommand)
      .then((umsetzungsstand: Umsetzungsstand) => {
        this.notifyUserService.inform('Maßnahme-Umsetzungsstand wurde erfolgreich gespeichert.');
        this.umsetzungsstand = umsetzungsstand;
        this.resetForm(umsetzungsstand);
        this.massnahmeFilterService.refetchData();
      })
      .finally(() => {
        this.isFetching = false;
      });
  }

  onReset(): void {
    invariant(this.umsetzungsstand);
    this.resetForm(this.umsetzungsstand);
  }

  canDiscard(): boolean {
    return this.formGroup.pristine;
  }

  get aktualisierungAngefordert(): boolean {
    return this.umsetzungsstand?.umsetzungsstandStatus === UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT;
  }

  get aktualisiert(): boolean {
    return this.umsetzungsstand?.umsetzungsstandStatus === UmsetzungsstandStatus.AKTUALISIERT;
  }

  get importiert(): boolean {
    return this.umsetzungsstand?.umsetzungsstandStatus === UmsetzungsstandStatus.IMPORTIERT;
  }

  get istMassnahmeUmgesetzt(): boolean {
    return this.umsetzungsstand?.massnahmeUmsetzungsstatus === Umsetzungsstatus.UMGESETZT;
  }

  get istBearbeitungGesperrt(): boolean {
    return (
      (Umsetzungsstatus.isStorniert(this.umsetzungsstand?.massnahmeUmsetzungsstatus) ||
        this.umsetzungsstand?.massnahmeUmsetzungsstatus === Umsetzungsstatus.UMGESETZT) &&
      this.umsetzungsstand?.umsetzungsstandStatus !== UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT
    );
  }

  private get mussAbfrageFinalBestaetigtWerden(): boolean {
    const finaleUmsetzungsstatus = [
      Umsetzungsstatus.UMGESETZT,
      Umsetzungsstatus.STORNIERT,
      Umsetzungsstatus.STORNIERUNG_ANGEFRAGT,
    ];
    return (
      Boolean(this.umsetzungsstand) &&
      finaleUmsetzungsstatus.includes(this.umsetzungsstand!.massnahmeUmsetzungsstatus) &&
      this.umsetzungsstand!.umsetzungsstandStatus === UmsetzungsstandStatus.AKTUALISIERUNG_ANGEFORDERT
    );
  }

  private resetForm(umsetzungsstand: Umsetzungsstand | null): void {
    invariant(umsetzungsstand);
    this.formGroup.reset(umsetzungsstand);
    if (this.istBearbeitungGesperrt || !umsetzungsstand.canEdit) {
      this.formGroup.disable({ emitEvent: false });
    } else {
      this.formGroup.enable({ emitEvent: false });
    }

    if (this.mussAbfrageFinalBestaetigtWerden) {
      this.dialog.open(HinweisDialogComponent, {
        data:
          'Die Maßnahme wurde umgesetzt/storniert. \n\n' +
          'Bitte bestätigen Sie ein letztes Mal die Umsetzungsstandsabfrage! \n' +
          'Spätere Änderungen an der Abfrage sind nicht mehr möglich.',
      });
    }
  }
}
