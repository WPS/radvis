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
import { Observable, of } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import {
  AbstractUndeterminedFormControl,
  UNDETERMINED_LABEL,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';

@Component({
  selector: 'rad-organisationen-dropdown-control',
  templateUrl: './organisationen-dropdown-control.component.html',
  styleUrls: ['./organisationen-dropdown-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => OrganisationenDropdownControlComponent), multi: true },
  ],
  standalone: false,
})
export class OrganisationenDropdownControlComponent
  extends AbstractUndeterminedFormControl<Verwaltungseinheit>
  implements OnChanges, OnInit
{
  @Input()
  options: Verwaltungseinheit[] = [];

  @Input()
  nullable = true;

  @Input()
  touchOnWrite = true;

  @Input()
  errors?: ValidationErrors | null = null;
  errorMessages: string[] = [];

  selectedOrganisation: Verwaltungseinheit | null = null;

  filteredOptions$: Observable<Verwaltungseinheit[]> = of([]);
  formControl: FormControl<Verwaltungseinheit | string | null>;

  public readonly NULL_LABEL = 'Keine Angabe';

  public readonly UNDETERMINED_LABEL = UNDETERMINED_LABEL;
  public readonly UNDETERMINED = 'UNDETERMINED';

  isUndetermined = false;
  showUndeterminedOption = false;

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

  public writeValue(value: Verwaltungseinheit | null | UndeterminedValue): void {
    let formValue: Verwaltungseinheit | string | null;
    if (value instanceof UndeterminedValue) {
      this.isUndetermined = true;
      this.showUndeterminedOption = true;
      formValue = this.UNDETERMINED;
      this.changeDetector.detectChanges();
    } else {
      formValue = value;
      this.selectedOrganisation = value;
      this.isUndetermined = false;
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
    this.selectedOrganisation = value;
    this.changeDetector.markForCheck();
  }

  // Verlässt man das Formularfeld, ohne einen Wert auszuwählen, so wird die zuletzt ausgewählte Option wieder gesetzt
  onBlur(): void {
    if (this.selectedOrganisation) {
      this.writeValue(this.selectedOrganisation);
    }
  }

  // Das Standardverhalten von Autocomplete bei Klick wird hier überschrieben, weil sonst bei Auswahl eines Wertes und
  // erneutem Klick auf das Formularfeld das Options-Panel nicht mehr angezeigt wird
  onClick(event: Event, trigger: MatAutocompleteTrigger): void {
    this.formControl.reset('');
    event.stopPropagation();
    trigger.openPanel();
  }

  displayFn = (item: Verwaltungseinheit | null | 'UNDETERMINED'): string => {
    if (item === this.UNDETERMINED) {
      return '';
    }

    return Verwaltungseinheit.getDisplayName(item);
  };

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  private filter(value: string | null | Verwaltungseinheit): Verwaltungseinheit[] {
    if (value != null) {
      if ((value as Verwaltungseinheit).name) {
        return this.options.slice();
      }
      return this.options.filter(option => option.name.toLowerCase().includes((value as string).toLowerCase()));
    }
    return this.options.slice();
  }
}
