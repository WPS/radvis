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
  OnInit,
  SimpleChanges,
  forwardRef,
} from '@angular/core';
import { FormControl, NG_VALUE_ACCESSOR, ValidationErrors } from '@angular/forms';
import { MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { Observable, of } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import {
  AbstractUndeterminedFormControl,
  UNDETERMINED_LABEL,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';

export interface AutoCompleteOption {
  id?: number;
  name: string;
  displayText: string;
}

@Component({
  selector: 'rad-autocomplete-dropdown',
  templateUrl: './autocomplete-dropdown.component.html',
  styleUrl: './autocomplete-dropdown.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => AutocompleteDropdownComponent), multi: true },
  ],
})
export class AutocompleteDropdownComponent
  extends AbstractUndeterminedFormControl<AutoCompleteOption>
  implements OnChanges, OnInit {
  @Input()
  options: AutoCompleteOption[] = [];

  @Input()
  nullable = true;

  @Input()
  touchOnWrite = true;

  @Input()
  errors?: ValidationErrors | null = null;
  errorMessages: string[] = [];

  selectedOption: AutoCompleteOption | null = null;

  filteredOptions$: Observable<AutoCompleteOption[]> = of([]);
  formControl: FormControl<AutoCompleteOption | string | null>;

  public readonly NULL_LABEL = 'Keine Angabe';

  showUndeterminedOption = false;
  public readonly UNDETERMINED_OPTION: AutoCompleteOption = {
    name: 'UNDETERMINED',
    displayText: UNDETERMINED_LABEL,
  };

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl(null);
  }

  ngOnInit(): void {
    this.filteredOptions$ = this.formControl.valueChanges.pipe(
      startWith(this.formControl.value),
      map(value => {
        this.showUndeterminedOption = false;
        this.changeDetector.detectChanges();
        return this.filter(value);
      })
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.errors !== undefined) {
      this.formControl.setErrors(this.errors || null);
      this.errorMessages = this.errors ? Object.values<string>(this.errors) : [];
    }
  }

  public writeValue(value: AutoCompleteOption | null | UndeterminedValue): void {
    let formValue: AutoCompleteOption | null;
    if (value instanceof UndeterminedValue) {
      this.showUndeterminedOption = true;
      formValue = this.UNDETERMINED_OPTION;
      this.changeDetector.detectChanges();
    } else {
      formValue = value;
      this.selectedOption = value;
      this.showUndeterminedOption = false;
    }
    this.formControl.reset(formValue, { emitEvent: false });
    if (this.touchOnWrite) {
      this.formControl.markAsTouched();
    }
    this.changeDetector.markForCheck();
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value;
    this.onChange(value);
    this.selectedOption = value;
  }

  // Verlässt man das Formularfeld, ohne einen Wert auszuwählen, so wird die zuletzt ausgewählte Option wieder gesetzt
  onBlur(): void {
    if (this.selectedOption) {
      this.writeValue(this.selectedOption);
    }
  }

  // Das Standardverhalten von Autocomplete bei Klick wird hier überschrieben, weil sonst bei Auswahl eines Wertes und
  // erneutem Klick auf das Formularfeld das Options-Panel nicht mehr angezeigt wird
  onClick(event: Event, trigger: MatAutocompleteTrigger): void {
    this.formControl.reset('');
    event.stopPropagation();
    trigger.openPanel();
  }

  displayFn = (item: AutoCompleteOption | null): string => {
    return item ? item.displayText : '';
  };

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  private filter(value: AutoCompleteOption | string | null): AutoCompleteOption[] {
    if (value !== null) {
      if ((value as AutoCompleteOption).name) {
        return this.options.slice();
      }
      return this.options.filter(option => option.name.toLowerCase().includes((value as string).toLowerCase()));
    }
    return this.options.slice();
  }
}
