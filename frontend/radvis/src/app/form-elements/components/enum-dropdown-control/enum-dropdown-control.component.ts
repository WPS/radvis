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
import { EnumOption } from 'src/app/form-elements/models/enum-option';

@Component({
  selector: 'rad-enum-dropdown-control',
  templateUrl: './enum-dropdown-control.component.html',
  styleUrls: ['./enum-dropdown-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => EnumDropdownControlComponent), multi: true }],
  standalone: false,
})
export class EnumDropdownControlComponent
  extends AbstractUndeterminedFormControl<string | string[]>
  implements OnChanges
{
  @Input()
  options: EnumOption[] = [];
  @Input()
  nullable = true;
  @Input()
  showTooltip = false;
  @Input()
  multiple = false;

  @Input()
  touchOnWrite = true;

  @Input()
  errors?: ValidationErrors | null = null;
  errorMessages: string[] = [];

  public formControl: FormControl<string | string[] | null>;

  public readonly UNDETERMINED = 'UNDETERMINED';
  public readonly UNDETERMINED_LABEL = UNDETERMINED_LABEL;

  isUndetermined = false;
  showUndeterminedOption = false;

  get showUndeterminedHint(): boolean {
    if (!this.isUndetermined) {
      return false;
    }

    if (!this.formControl.value) {
      return false;
    }

    return this.formControl.value === this.UNDETERMINED || this.formControl.value.includes(this.UNDETERMINED);
  }

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl(null);
    this.formControl.valueChanges.subscribe(value => {
      this.showUndeterminedOption = false;
      this.onChange(value);
      this.changeDetector.detectChanges();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.errors !== undefined) {
      this.formControl.setErrors(this.errors || null);
      this.errorMessages = this.errors ? Object.values<string>(this.errors) : [];
    }
  }

  public writeValue(value: string | UndeterminedValue | null | string[]): void {
    let formValue: string | string[] | null;
    if (value instanceof UndeterminedValue) {
      formValue = this.multiple ? [this.UNDETERMINED] : this.UNDETERMINED;
      this.isUndetermined = true;
      this.showUndeterminedOption = !this.multiple;
      this.changeDetector.detectChanges();
    } else {
      formValue = value;
      this.isUndetermined = false;
      this.showUndeterminedOption = false;
    }
    this.formControl.reset(formValue, { emitEvent: false });
    if (this.touchOnWrite) {
      this.formControl.markAsTouched();
    }
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
