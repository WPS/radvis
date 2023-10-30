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
  UndeterminedValue,
  UNDETERMINED_LABEL,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { EnumOption } from 'src/app/form-elements/models/enum-option';

@Component({
  selector: 'rad-enum-dropdown-control',
  templateUrl: './enum-dropdown-control.component.html',
  styleUrls: ['./enum-dropdown-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => EnumDropdownControlComponent), multi: true }],
})
export class EnumDropdownControlComponent extends AbstractUndeterminedFormControl<string> {
  @Input()
  options: EnumOption[] = [];
  @Input()
  nullable = true;
  @Input()
  showTooltip = false;

  public formControl: FormControl;

  public readonly UNDETERMINED = 'UNDETERMINED';
  public readonly UNDETERMINED_LABEL = UNDETERMINED_LABEL;

  showUndeterminedOption = false;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl(null);
    this.formControl.valueChanges.subscribe(value => {
      this.showUndeterminedOption = false;
      this.onChange(value);
      this.changeDetector.detectChanges();
    });
  }

  public writeValue(value: string | UndeterminedValue | null): void {
    let formValue = value;
    if (value instanceof UndeterminedValue) {
      formValue = this.UNDETERMINED;
      this.showUndeterminedOption = true;
      this.changeDetector.detectChanges();
    } else {
      this.showUndeterminedOption = false;
    }
    this.formControl.reset(formValue, { emitEvent: false });
    this.changeDetector.markForCheck();
  }

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }
}
