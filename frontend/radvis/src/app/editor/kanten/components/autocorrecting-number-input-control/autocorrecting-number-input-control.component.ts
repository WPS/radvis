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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  forwardRef,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { FormControl, NG_VALUE_ACCESSOR, ValidationErrors } from '@angular/forms';
import {
  AbstractUndeterminedFormControl,
  UNDETERMINED_LABEL,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

// Diese Eingabekomponente korrigiert invalide, negative und 0,0 automatisch.
// Auf Wunsch (mit dem Input: resetToPreviousValueOnEmpty) werden auch nicht vorhandene Werte korrigiert

// Invalide Eingaben und "0,0" -> Feld wird auf vorherigen Wert zurueckgesetzt
// Negative Zahlen -> Minus wird abgeschnitten
// Nicht vorhandene Werte -> resetToPreviousValueOnEmpty ? zuruecksetzten : null
@Component({
  selector: 'rad-autocorrecting-number-input-control',
  templateUrl: './autocorrecting-number-input-control.component.html',
  styleUrls: ['./autocorrecting-number-input-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AutocorrectingNumberInputControlComponent),
      multi: true,
    },
  ],
})
export class AutocorrectingNumberInputControlComponent
  extends AbstractUndeterminedFormControl<number>
  implements OnChanges
{
  @Input()
  value: number | null = null;
  // Bei direkter Zuweisung des Controls kommt kein neues "enabled" rein, daher binden wir das hier zusätzlich
  @Input()
  isDisabled = false;

  @Input()
  resetToPreviousValueOnEmpty = false;

  @Input()
  touchOnWrite = true;

  @Input()
  errors?: ValidationErrors | null = null;
  errorMessages: string[] = [];

  formControl: FormControl<number | null>;
  previousValue: number | null | UndeterminedValue = null;

  readonly UNDETERMINED_LABEL = UNDETERMINED_LABEL;
  isUndetermined = false;

  constructor(
    private changeDetector: ChangeDetectorRef,
    notifyUserService: NotifyUserService
  ) {
    super();
    this.formControl = new FormControl<number | null>(null, { updateOn: 'blur' });
    this.formControl.valueChanges.subscribe(value => {
      const convertedValue: number | null = value ? +value.toFixed(2) : null;
      let valueToWrite: number | null | UndeterminedValue = convertedValue;
      if (
        this.resetToPreviousValueOnEmpty &&
        (convertedValue === null || isNaN(convertedValue) || convertedValue === 0.0)
      ) {
        valueToWrite = this.previousValue;
        notifyUserService.warn('Fehlerhafter Wert. Eingabe wurde nicht übernommen.');
      }

      // Damit das Form nicht dirty wird bei ungültigen Eingaben, werden Änderungen nur bei tatsächlichen Änderungen nach oben gegeben
      if (convertedValue && this.previousValue !== convertedValue) {
        this.onChange(convertedValue);
      }
      this.writeValue(valueToWrite);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.isDisabled) {
      this.setDisabledState(this.isDisabled);
    }
    if (changes.errors !== undefined) {
      this.formControl.setErrors(this.errors || null);
      this.errorMessages = this.errors ? Object.values<string>(this.errors) : [];
    }
    if (changes.value) {
      this.formControl.reset(this.value ? +this.value.toFixed(2) : null, { emitEvent: false });
      this.isUndetermined = false;
    }
  }

  public setDisabledState(disabled: boolean): void {
    if (disabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  public writeValue(value: number | UndeterminedValue | null): void {
    let convertedValue: number | null | UndeterminedValue = value;
    if (convertedValue instanceof UndeterminedValue) {
      this.formControl.reset(null, { emitEvent: false });
      this.isUndetermined = true;
    } else {
      const fixedValue = convertedValue ? +convertedValue.toFixed(2) : null;
      convertedValue = fixedValue;
      this.formControl.reset(fixedValue, { emitEvent: false });
      this.isUndetermined = false;
    }

    if (this.touchOnWrite) {
      this.formControl.markAsTouched();
    }
    this.changeDetector.markForCheck();
    this.previousValue = convertedValue;
  }
}
