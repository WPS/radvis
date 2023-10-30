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
import { FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';

@Component({
  selector: 'rad-currency-input-control',
  templateUrl: './currency-input-control.component.html',
  styleUrls: ['./currency-input-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CurrencyInputControlComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: CurrencyInputControlComponent,
      multi: true,
    },
  ],
})
export class CurrencyInputControlComponent extends AbstractFormControl<number> implements OnChanges, Validator {
  // Bei direkter Zuweisung des Controls kommt kein neues "enabled" rein, daher binden wir das hier zusätzlich
  @Input()
  isDisabled = false;

  formControl: FormControl;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl('', RadvisValidators.isValidEuroString);
    this.formControl.valueChanges.subscribe(value => {
      if (value === undefined || value === null || value === '') {
        this.onChange(null);
      } else {
        const convertedValue: number | null = this.toCentAmount(value);
        this.onChange(convertedValue);
      }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.isDisabled) {
      this.setDisabledState(this.isDisabled);
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

  public writeValue(value: number | null): void {
    if (value !== null) {
      this.formControl.reset(this.toCurrencyString(value), { emitEvent: false });
    } else {
      this.formControl.reset('', { emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  public validate(): ValidationErrors | null {
    return this.formControl.errors;
  }

  private toCurrencyString(cents: number): string {
    return (cents / 100).toFixed(2).replace('.', ',');
  }

  private toCentAmount(currencyString: string): number | null {
    const split = currencyString.split(',');
    let result = split[0];
    if (split.length > 1) {
      result += split[1].padEnd(2, '0');
    } else {
      result += '00';
    }

    return +result;
  }
}
