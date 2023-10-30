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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  AbstractUndeterminedFormControl,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';

@Component({
  selector: 'rad-undetermined-checkbox-control',
  templateUrl: './undetermined-checkbox-control.component.html',
  styleUrls: ['./undetermined-checkbox-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => UndeterminedCheckboxControlComponent), multi: true },
  ],
})
export class UndeterminedCheckboxControlComponent extends AbstractUndeterminedFormControl<boolean> {
  isUndetermined = false;
  checked = false;
  disabled = false;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
  }

  onCheckboxChange(checked: boolean): void {
    // das ist notwendig, damit changes getriggert werden,
    // wenn über writeValue ein neues isUndetermined rein kommt
    this.checked = checked;
    this.isUndetermined = false;
    this.onChange(checked);
  }

  public writeValue(value: boolean | UndeterminedValue | null): void {
    if (value instanceof UndeterminedValue) {
      this.isUndetermined = true;
    } else {
      this.isUndetermined = false;
      this.checked = value || false;
    }
    this.changeDetector.markForCheck();
  }

  public setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.changeDetector.markForCheck();
  }
}
