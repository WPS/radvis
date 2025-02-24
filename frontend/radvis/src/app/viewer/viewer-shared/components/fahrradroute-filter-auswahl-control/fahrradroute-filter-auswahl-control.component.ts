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
import {
  AbstractControl,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import { Subscription } from 'rxjs';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { FahrradrouteFilter } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';
import { FahrradrouteKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-kategorie';
import { FahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view';
import { FahrradroutenProviderService } from 'src/app/viewer/viewer-shared/services/fahrradrouten-provider.service';

@Component({
  selector: 'rad-fahrradroute-filter-auswahl-control',
  templateUrl: './fahrradroute-filter-auswahl-control.component.html',
  styleUrl: './fahrradroute-filter-auswahl-control.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: FahrradrouteFilterAuswahlControlComponent,
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: FahrradrouteFilterAuswahlControlComponent,
      multi: true,
    },
  ],
  standalone: false,
})
export class FahrradrouteFilterAuswahlControlComponent
  extends AbstractFormControl<FahrradrouteFilter>
  implements Validator
{
  formGroup = new FormGroup({
    fahrradrouteFilterKategorie: new FormControl<FahrradrouteFilterKategorie | null>(null),
    fahrradroute: new FormControl<FahrradrouteListenView | null>(null, RadvisValidators.isNotNullOrEmpty),
  });
  fahrradrouteFilterKategorieOptions = FahrradrouteFilterKategorie.options;
  alleFahrradrouten: FahrradrouteListenView[] = [];
  fahrradrouteFilterKategorieValueChangeSubscription: Subscription;

  constructor(
    fahrradrouteProviderService: FahrradroutenProviderService,
    private changeDetector: ChangeDetectorRef
  ) {
    super();

    this.formGroup.valueChanges.subscribe(() => this.onChange(this.readForm()));

    fahrradrouteProviderService.getAll().then(fahrradrouten => {
      this.alleFahrradrouten = fahrradrouten;
      changeDetector.markForCheck();
    });

    this.fahrradrouteFilterKategorieValueChangeSubscription =
      this.formGroup.controls.fahrradrouteFilterKategorie.valueChanges.subscribe(newValue =>
        this.onFahrradrouteFilterKategorieChanged(newValue)
      );
  }

  // eslint-disable-next-line no-unused-vars
  validate(control: AbstractControl): ValidationErrors | null {
    if (this.formGroup.valid) {
      return null;
    }

    return { fahrradrouteFilterError: 'Fahrradroute-Filter ist invalid' };
  }

  public override writeValue(value: FahrradrouteFilter | null): void {
    this.fahrradrouteFilterKategorieValueChangeSubscription.unsubscribe();

    this.formGroup.reset(value ?? undefined, { emitEvent: false });

    this.updateFahrradrouteAuswahlEnabled(value?.fahrradrouteFilterKategorie ?? null);
    this.changeDetector.markForCheck();

    this.fahrradrouteFilterKategorieValueChangeSubscription =
      this.formGroup.controls.fahrradrouteFilterKategorie.valueChanges.subscribe(newValue =>
        this.onFahrradrouteFilterKategorieChanged(newValue)
      );
  }

  private updateFahrradrouteAuswahlEnabled(fahrradrouteFilterKategorie: FahrradrouteFilterKategorie | null): void {
    const isFahrradrouteEnabled = fahrradrouteFilterKategorie === FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE;
    if (isFahrradrouteEnabled) {
      this.formGroup.controls.fahrradroute.enable({ emitEvent: false });
    } else {
      this.formGroup.controls.fahrradroute.disable({ emitEvent: false });
    }
  }

  public override setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formGroup.disable({ emitEvent: false });
    } else {
      this.formGroup.enable({ emitEvent: false });
      this.updateFahrradrouteAuswahlEnabled(this.formGroup.value.fahrradrouteFilterKategorie ?? null);
    }

    this.changeDetector.markForCheck();
  }

  private readForm(): FahrradrouteFilter | null {
    const value = this.formGroup.getRawValue();

    let matchingFahrradrouten: FahrradrouteListenView[] = [];

    switch (value.fahrradrouteFilterKategorie) {
      case FahrradrouteFilterKategorie.EINZELNE_FAHRRADROUTE:
        matchingFahrradrouten = value.fahrradroute ? [value.fahrradroute] : []; // falls form invalid
        break;
      case FahrradrouteFilterKategorie.ALLE_LRFW:
        matchingFahrradrouten = this.alleFahrradrouten.filter(
          f => f.fahrradrouteKategorie === FahrradrouteKategorie.LANDESRADFERNWEG
        );
        break;
      case FahrradrouteFilterKategorie.ALLE_DROUTEN:
        matchingFahrradrouten = this.alleFahrradrouten.filter(
          f => f.fahrradrouteKategorie === FahrradrouteKategorie.D_ROUTE
        );
        break;
      case FahrradrouteFilterKategorie.ALLE_FAHRRADROUTEN:
        matchingFahrradrouten = this.alleFahrradrouten;
        break;
    }

    return {
      fahrradroute: value.fahrradroute ?? null,
      fahrradrouteFilterKategorie: value.fahrradrouteFilterKategorie ?? null,
      fahrradroutenIds: matchingFahrradrouten.map(item => item.id),
    };
  }

  private onFahrradrouteFilterKategorieChanged(newValue: FahrradrouteFilterKategorie | null): void {
    this.formGroup.controls.fahrradroute.reset();
    this.updateFahrradrouteAuswahlEnabled(newValue);
  }
}
