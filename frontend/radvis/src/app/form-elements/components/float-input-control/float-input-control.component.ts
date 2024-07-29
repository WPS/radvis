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
  Input,
  OnChanges,
  SimpleChanges,
  forwardRef,
} from '@angular/core';
import { FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';
import {
  AbstractUndeterminedFormControl,
  UNDETERMINED_INVALID_LABEL,
  UNDETERMINED_LABEL,
  UndeterminedInvalidValue,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';

@Component({
  selector: 'rad-float-input-control',
  templateUrl: './float-input-control.component.html',
  styleUrl: './float-input-control.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FloatInputControlComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: FloatInputControlComponent,
      multi: true,
    },
  ],
})
export class FloatInputControlComponent
  extends AbstractUndeterminedFormControl<number>
  implements OnChanges, Validator
{
  @Input()
  value: number | null = null;
  // Bei direkter Zuweisung des Controls kommt kein neues "enabled" rein, daher binden wir das hier zusätzlich
  @Input()
  isDisabled = false;

  @Input()
  anzahlNachkommastellen = 2;

  @Input()
  errors?: ValidationErrors | null = null;
  errorMessages: string[] = [];

  formControl: FormControl<string | null>;

  isUndetermined = false;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl('', RadvisValidators.isPositiveFloatString);
    this.formControl.valueChanges.subscribe(value => {
      if (value === undefined || value === null || value === '') {
        this.onChange(null);
      } else {
        const convertedValue: number | null = +Number.parseFloat(value.replace(',', '.')).toFixed(
          this.anzahlNachkommastellen
        );
        this.onChange(convertedValue);
      }
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
  }

  public setDisabledState(disabled: boolean): void {
    if (disabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  public writeValue(value: number | UndeterminedValue | UndeterminedInvalidValue | null): void {
    this.isUndetermined = false;
    if (value instanceof UndeterminedValue) {
      this.formControl.reset(UNDETERMINED_LABEL, { emitEvent: false });
      this.isUndetermined = true;
    } else if (value instanceof UndeterminedInvalidValue) {
      this.formControl.reset(UNDETERMINED_INVALID_LABEL, { emitEvent: false });
    } else if (value !== null) {
      this.formControl.reset(value.toFixed(this.anzahlNachkommastellen).replace('.', ','), { emitEvent: false });
    } else {
      this.formControl.reset('', { emitEvent: false });
    }
    this.formControl.markAsTouched();
    this.changeDetector.markForCheck();
  }

  public validate(): ValidationErrors | null {
    return this.formControl.errors;
  }
}
