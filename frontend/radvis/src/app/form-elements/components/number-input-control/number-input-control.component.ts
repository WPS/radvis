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
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import {
  UNDETERMINED_LABEL,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';

@Component({
  selector: 'rad-number-input-control',
  templateUrl: './number-input-control.component.html',
  styleUrls: ['./number-input-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => NumberInputControlComponent),
      multi: true,
    },
  ],
  standalone: false,
})
export class NumberInputControlComponent extends AbstractFormControl<number> implements OnChanges {
  @Input()
  value: number | null = null;

  // Bei direkter Zuweisung des Controls kommt kein neues "enabled" rein, daher binden wir das hier zusätzlich
  @Input()
  isDisabled = false;

  @Input()
  errors?: ValidationErrors | null = null;
  errorMessages: string[] = [];

  formControl: FormControl<number | null>;

  readonly UNDETERMINED_LABEL = UNDETERMINED_LABEL;
  isUndetermined = false;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl(null);
    this.formControl.valueChanges.subscribe(value => {
      if (value === undefined || value === null) {
        this.onChange(null);
      } else {
        this.onChange(value);
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

  public writeValue(value: number | null | UndeterminedValue): void {
    if (value instanceof UndeterminedValue) {
      this.isUndetermined = true;
      this.formControl.reset(null, { emitEvent: false });
    } else {
      this.isUndetermined = false;
      this.formControl.reset(value, { emitEvent: false });
    }
    this.formControl.markAsTouched();
    this.changeDetector.markForCheck();
  }
}
