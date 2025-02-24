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
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import { FahrradrouteListenView } from 'src/app/viewer/viewer-shared/models/fahrradroute-listen-view';

@Component({
  selector: 'rad-fahrradrouten-dropdown-control',
  templateUrl: './fahrradrouten-dropdown-control.component.html',
  styleUrls: ['./fahrradrouten-dropdown-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => FahrradroutenDropdownControlComponent), multi: true },
  ],
  standalone: false,
})
export class FahrradroutenDropdownControlComponent
  extends AbstractFormControl<FahrradrouteListenView>
  implements OnChanges, OnInit
{
  @Input()
  options: FahrradrouteListenView[] = [];

  @Input()
  nullable = true;

  @Input()
  touchOnWrite = true;

  @Input()
  errors?: ValidationErrors | null = null;
  errorMessages: string[] = [];

  selectedFahrradroute: FahrradrouteListenView | null = null;

  filteredOptions$: Observable<FahrradrouteListenView[]> = of([]);
  formControl: FormControl<FahrradrouteListenView | string | null>;

  public readonly NULL_LABEL = 'Keine Angabe';

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl(null);
  }

  ngOnInit(): void {
    this.filteredOptions$ = this.formControl.valueChanges.pipe(
      startWith(this.formControl.value),
      map(value => {
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

  public writeValue(value: FahrradrouteListenView | null): void {
    this.selectedFahrradroute = value;
    this.formControl.reset(value, { emitEvent: false });
    if (this.touchOnWrite) {
      this.formControl.markAsTouched();
    }
    this.changeDetector.markForCheck();
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value;
    this.onChange(value);
    this.selectedFahrradroute = value;
  }

  // Verlässt man das Formularfeld, ohne einen Wert auszuwählen, so wird die zuletzt ausgewählte Option wieder gesetzt
  onBlur(): void {
    if (this.selectedFahrradroute) {
      this.writeValue(this.selectedFahrradroute);
    }
  }

  // Das Standardverhalten von Autocomplete bei Klick wird hier überschrieben, weil sonst bei Auswahl eines Wertes und
  // erneutem Klick auf das Formularfeld das Options-Panel nicht mehr angezeigt wird
  onClick(event: Event, trigger: MatAutocompleteTrigger): void {
    this.formControl.reset('');
    event.stopPropagation();
    trigger.openPanel();
  }

  displayFn = (item: FahrradrouteListenView | null): string => {
    if (item === null) {
      return '';
    }

    return FahrradrouteListenView.getDisplayName(item);
  };

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  private filter(value: string | null | FahrradrouteListenView): FahrradrouteListenView[] {
    if (value != null) {
      if ((value as FahrradrouteListenView).name) {
        return this.options.slice();
      }
      return this.options.filter(option => option.name.toLowerCase().includes((value as string).toLowerCase()));
    }
    return this.options.slice();
  }
}
