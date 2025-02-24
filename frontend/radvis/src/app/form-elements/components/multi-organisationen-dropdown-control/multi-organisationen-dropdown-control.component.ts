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
import { NG_VALUE_ACCESSOR, UntypedFormControl, ValidationErrors } from '@angular/forms';
import { MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-multi-organisationen-dropdown-control',
  templateUrl: './multi-organisationen-dropdown-control.component.html',
  styleUrls: ['./multi-organisationen-dropdown-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MultiOrganisationenDropdownControlComponent),
      multi: true,
    },
  ],
  standalone: false,
})
export class MultiOrganisationenDropdownControlComponent
  extends AbstractFormControl<Verwaltungseinheit[]>
  implements OnChanges
{
  @Input()
  options: Verwaltungseinheit[] = [];

  @Input()
  errors?: ValidationErrors | null = null;
  errorMessages: string[] = [];

  selectedOrganisationen: Verwaltungseinheit[] = [];

  filteredOptions: Verwaltungseinheit[] = [];
  formControl: UntypedFormControl;

  organisationDisplayName = Verwaltungseinheit.getDisplayName;

  get unselectedOptions(): Verwaltungseinheit[] {
    return this.options.filter(o => !this.selectedOrganisationen.map(sel => sel.id).includes(o.id));
  }

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new UntypedFormControl('');
    this.formControl.valueChanges.subscribe(v => {
      this.filteredOptions = this.filter(v);
      this.changeDetector.markForCheck();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(this.options !== undefined);
    invariant(this.options !== null);
    this.filteredOptions = this.filter(this.formControl.value);

    if (changes.errors !== undefined) {
      this.formControl.setErrors(this.errors || null);
      this.errorMessages = this.errors ? Object.values<string>(this.errors) : [];
    }
  }

  canRemove(organisation: Verwaltungseinheit): boolean {
    return this.options.findIndex(o => organisation.id === o.id) > -1;
  }

  public writeValue(value: Verwaltungseinheit[] | null): void {
    this.selectedOrganisationen = value ?? [];
    this.formControl.reset('');
    this.changeDetector.markForCheck();
  }

  // Das Standardverhalten von Autocomplete bei Klick wird hier überschrieben, weil sonst bei Auswahl eines Wertes und
  // erneutem Klick auf das Formularfeld das Options-Panel nicht mehr angezeigt wird
  onClick(event: Event, trigger: MatAutocompleteTrigger): void {
    this.formControl.reset('');
    event.stopPropagation();
    trigger.openPanel();
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value;
    this.selectedOrganisationen.push(value);
    this.onChange(this.selectedOrganisationen);
    this.formControl.reset('');
  }

  onOrganisationRemoved(organisation: Verwaltungseinheit): void {
    this.selectedOrganisationen.splice(
      this.selectedOrganisationen.findIndex(o => o.id === organisation.id),
      1
    );
    this.onChange(this.selectedOrganisationen);
    this.filteredOptions = this.filter(this.formControl.value);
  }

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  private filter(value: string | null | Verwaltungseinheit): Verwaltungseinheit[] {
    if (value) {
      if ((value as Verwaltungseinheit).name) {
        return this.unselectedOptions;
      }
      return this.unselectedOptions.filter(option =>
        option.name.toLowerCase().includes((value as string).toLowerCase())
      );
    }
    return this.unselectedOptions;
  }
}
