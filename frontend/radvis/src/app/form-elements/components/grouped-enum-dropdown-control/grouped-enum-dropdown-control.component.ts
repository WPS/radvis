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
  OnInit,
  SimpleChanges,
} from '@angular/core';
import { FormControl, NG_VALUE_ACCESSOR, ValidationErrors } from '@angular/forms';
import { MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import {
  AbstractUndeterminedFormControl,
  UNDETERMINED_LABEL,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { GroupedEnumOptions } from 'src/app/form-elements/models/grouped-enum-options';

@Component({
  selector: 'rad-grouped-enum-dropdown-control',
  templateUrl: './grouped-enum-dropdown-control.component.html',
  styleUrls: ['./grouped-enum-dropdown-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => GroupedEnumDropdownControlComponent), multi: true },
  ],
  standalone: false,
})
export class GroupedEnumDropdownControlComponent
  extends AbstractUndeterminedFormControl<string>
  implements OnInit, OnChanges
{
  @Input()
  groupedOptions: GroupedEnumOptions = {};

  @Input()
  nullable = true;

  @Input()
  errors?: ValidationErrors | null = null;
  errorMessages: string[] = [];

  selectedOption: string | UndeterminedValue | null = null;

  formControl: FormControl<string | null>;
  filteredGroupedOptions: GroupedEnumOptions = {};

  public readonly UNDETERMINED = 'UNDETERMINED';

  public readonly UNDETERMINED_LABEL = UNDETERMINED_LABEL;

  showUndeterminedOption = false;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl(null);
    this.formControl.valueChanges.subscribe(searchTerm => {
      this.showUndeterminedOption = this.selectedOption instanceof UndeterminedValue;
      this.updateFilteredOptions(searchTerm);
      this.changeDetector.markForCheck();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.errors !== undefined) {
      this.formControl.setErrors(this.errors || null);
      this.errorMessages = this.errors ? Object.values<string>(this.errors) : [];
    }
  }

  ngOnInit(): void {
    this.filteredGroupedOptions = this.groupedOptions;
  }

  public writeValue(value: string | UndeterminedValue | null): void {
    let formValue: string | null;
    if (value instanceof UndeterminedValue) {
      formValue = this.UNDETERMINED;
      this.showUndeterminedOption = true;
      this.changeDetector.detectChanges();
    } else {
      formValue = value;
      this.showUndeterminedOption = false;
    }
    this.formControl.reset(formValue, { emitEvent: false });
    this.selectedOption = value;
    this.formControl.markAsTouched();
    this.changeDetector.markForCheck();
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value;
    this.onChange(value);
    this.selectedOption = value;
  }

  // Verlässt man das Formularfeld ohne einen Wert auszuwählen, so wird die zuletzt ausgewählte Option wieder gesetzt
  onBlur(): void {
    this.writeValue(this.selectedOption);
  }

  // Das Standardverhalten von Autocomplete bei Klick wird hier überschrieben, weil sonst bei Auswahl eines Wertes und
  // erneutem Klick auf das Formularfeld das Options-Panel nicht mehr angezeigt wird
  onClick(event: Event, trigger: MatAutocompleteTrigger): void {
    this.formControl.reset('');
    event.stopPropagation();
    trigger.openPanel();
  }

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
  }

  objectKeys(obj: GroupedEnumOptions): string[] {
    return Object.keys(obj);
  }

  displayFn = (key: string): string => {
    let result = '';
    if (key) {
      if (key === this.UNDETERMINED) {
        return UNDETERMINED_LABEL;
      }
      Object.keys(this.groupedOptions).forEach(group => {
        this.groupedOptions[group].options.forEach(enumOption => {
          if (enumOption.name === key) {
            result = enumOption.displayText;
          }
        });
      });
    }
    return result;
  };

  private updateFilteredOptions(searchTerm: string | null): void {
    const filteredGroupedOptionsUnderConstruction = {} as GroupedEnumOptions;
    if (searchTerm != null) {
      this.objectKeys(this.groupedOptions).forEach(group => {
        this.groupedOptions[group].options.forEach(enumOption => {
          if (enumOption.displayText.toLowerCase().includes(searchTerm.toLowerCase())) {
            if (!filteredGroupedOptionsUnderConstruction[group]) {
              filteredGroupedOptionsUnderConstruction[group] = {
                displayText: this.groupedOptions[group].displayText,
                options: [] as EnumOption[],
              };
            }
            filteredGroupedOptionsUnderConstruction[group].options.push(enumOption);
          }
        });
      });
      this.filteredGroupedOptions = filteredGroupedOptionsUnderConstruction;
    } else {
      this.filteredGroupedOptions = this.groupedOptions;
    }
  }
}
