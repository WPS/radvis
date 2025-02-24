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

@Component({
  selector: 'rad-text-input-control',
  templateUrl: './text-input-control.component.html',
  styleUrls: ['./text-input-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TextInputControlComponent),
      multi: true,
    },
  ],
  standalone: false,
})
export class TextInputControlComponent extends AbstractUndeterminedFormControl<string> implements OnChanges {
  @Input()
  asTextarea = false;

  @Input()
  rows = 2;

  @Input()
  inputType: 'text' | 'password' | 'email' | 'url' | 'search' = 'text';

  @Input()
  maxLength?: number;

  @Input()
  touchOnWrite = true;

  @Input()
  errors?: ValidationErrors | null = null;
  errorMessages: string[] = [];

  formControl: FormControl<string>;

  readonly UNDETERMINED_LABEL = UNDETERMINED_LABEL;
  isUndetermined = false;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl('', { nonNullable: true });
    this.formControl.valueChanges.subscribe(value => {
      this.onChange(value.trim());
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
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
  }

  public writeValue(value: string | UndeterminedValue | null): void {
    if (value instanceof UndeterminedValue) {
      this.isUndetermined = true;
      this.formControl.reset('', { emitEvent: false });
    } else if (value !== null) {
      this.isUndetermined = false;
      this.formControl.reset('' + value, { emitEvent: false });
    } else {
      this.isUndetermined = false;
      this.formControl.reset('', { emitEvent: false });
    }

    if (this.touchOnWrite) {
      this.formControl.markAsTouched();
    }
    this.changeDetector.markForCheck();
  }
}
