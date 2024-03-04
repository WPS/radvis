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
import { UntypedFormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { Observable, of } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import {
  AbstractUndeterminedFormControl,
  UNDETERMINED_LABEL,
  UndeterminedValue,
} from 'src/app/form-elements/components/abstract-undetermined-form-control';
import { Wahlkreis } from 'src/app/shared/models/wahlkreis';

@Component({
  selector: 'rad-wahlkreis-dropdown-control',
  templateUrl: './wahlkreis-dropdown-control.component.html',
  styleUrls: ['./wahlkreis-dropdown-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => WahlkreisDropdownControlComponent), multi: true },
  ],
})
export class WahlkreisDropdownControlComponent extends AbstractUndeterminedFormControl<Wahlkreis> implements OnChanges {
  @Input()
  options: Wahlkreis[] = [];

  @Input()
  nullable = true;

  selectedWahlkreis: Wahlkreis | null = null;

  filteredOptions$: Observable<Wahlkreis[]> = of([]);
  formControl: UntypedFormControl;

  public readonly NULL_LABEL = 'Keine Angabe';

  public readonly UNDETERMINED_LABEL = UNDETERMINED_LABEL;
  public readonly UNDETERMINED = 'UNDETERMINED';
  showUndeterminedOption = false;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new UntypedFormControl(null);
  }

  ngOnChanges(): void {
    this.filteredOptions$ = this.formControl.valueChanges.pipe(
      startWith(this.formControl.value),
      map(value => {
        this.showUndeterminedOption = false;
        this.changeDetector.detectChanges();
        return this.filter(value);
      })
    );
  }

  public writeValue(value: Wahlkreis | null | UndeterminedValue): void {
    let formValue = value;
    if (value instanceof UndeterminedValue) {
      this.showUndeterminedOption = true;
      formValue = this.UNDETERMINED;
      this.changeDetector.detectChanges();
    } else {
      this.selectedWahlkreis = value;
      this.showUndeterminedOption = false;
    }
    this.formControl.reset(formValue, { emitEvent: false });
    this.changeDetector.markForCheck();
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value;
    this.onChange(value);
    this.selectedWahlkreis = value;
  }

  // Verlässt man das Formularfeld, ohne einen Wert auszuwählen, so wird die zuletzt ausgewählte Option wieder gesetzt
  onBlur(): void {
    this.onTouched();
    this.formControl.reset(this.selectedWahlkreis, { emitEvent: false });
  }

  // Das Standardverhalten von Autocomplete bei Klick wird hier überschrieben, weil sonst bei Auswahl eines Wertes und
  // erneutem Klick auf das Formularfeld das Options-Panel nicht mehr angezeigt wird
  onClick(event: Event, trigger: MatAutocompleteTrigger): void {
    this.formControl.reset('');
    event.stopPropagation();
    trigger.openPanel();
  }

  displayFn = (item: Wahlkreis | null | 'UNDETERMINED'): string => {
    if (item === this.UNDETERMINED) {
      return UNDETERMINED_LABEL;
    }

    return Wahlkreis.getDisplayName(item);
  };

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  private filter(value: string | null | Wahlkreis): Wahlkreis[] {
    if (value != null) {
      if ((value as Wahlkreis).name) {
        return this.options.slice();
      }
      return this.options.filter(option =>
        this.displayFn(option)
          .toLowerCase()
          .includes((value as string).toLowerCase())
      );
    }
    return this.options.slice();
  }
}
