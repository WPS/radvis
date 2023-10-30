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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, Input } from '@angular/core';
import { FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
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
})
export class TextInputControlComponent extends AbstractUndeterminedFormControl<string> {
  @Input()
  asTextarea = false;

  @Input()
  rows = 2;

  formControl: FormControl;

  isUndetermined = false;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl('');
    this.formControl.valueChanges.subscribe(value => {
      this.onChange(value.trim());
    });
  }

  public setDisabledState(disabled: boolean): void {
    if (disabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
  }

  public writeValue(value: string | UndeterminedValue | null): void {
    this.isUndetermined = false;
    if (value instanceof UndeterminedValue) {
      this.formControl.reset(UNDETERMINED_LABEL, { emitEvent: false });
      this.isUndetermined = true;
    } else if (value !== null) {
      this.formControl.reset('' + value, { emitEvent: false });
    } else {
      this.formControl.reset('', { emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }
}
