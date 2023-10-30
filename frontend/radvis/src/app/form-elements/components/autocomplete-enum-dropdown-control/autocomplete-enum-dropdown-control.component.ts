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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, Input, OnChanges } from '@angular/core';
import { FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import { EnumOption } from 'src/app/form-elements/models/enum-option';

@Component({
  selector: 'rad-autocomplete-enum-dropdown-control',
  templateUrl: './autocomplete-enum-dropdown-control.component.html',
  styleUrls: ['./autocomplete-enum-dropdown-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AutocompleteEnumDropdownControlComponent),
      multi: true,
    },
  ],
})
export class AutocompleteEnumDropdownControlComponent extends AbstractFormControl<string> implements OnChanges {
  @Input()
  options: EnumOption[] = [];

  formControl = new FormControl(null);
  filteredEnumOptions: EnumOption[] = this.options;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl.valueChanges.subscribe(value => {
      this.filteredEnumOptions = this.updateFilteredOptions(value);
      this.onChange(value);
    });
  }

  ngOnChanges(): void {
    this.filteredEnumOptions = this.updateFilteredOptions(this.formControl.value);
    // damit die displayFn neu ausgeführt wird
    this.formControl.setValue(this.formControl.value, { emitEvent: false });
  }

  displayFn = (name: string): string => {
    return this.options.find(opt => opt.name === name)?.displayText ?? '';
  };

  public writeValue(value: string | null): void {
    this.formControl.setValue(value, { emitEvent: false });
    this.filteredEnumOptions = this.updateFilteredOptions(value);
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

  private updateFilteredOptions = (value: string | null): EnumOption[] => {
    if (!value) {
      return this.options;
    }

    const result = this.options.filter(
      opt => opt.displayText.toLowerCase().includes(value.toLowerCase()) || opt.name.includes(value)
    );
    return result;
  };
}
