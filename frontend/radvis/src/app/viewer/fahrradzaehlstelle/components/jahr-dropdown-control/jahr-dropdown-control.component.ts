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
import { FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import { EnumOption } from 'src/app/form-elements/models/enum-option';

@Component({
  selector: 'rad-jahr-dropdown-control',
  templateUrl: './jahr-dropdown-control.component.html',
  styleUrls: ['./jahr-dropdown-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => JahrDropdownControlComponent), multi: true }],
})
export class JahrDropdownControlComponent extends AbstractFormControl<Date> implements OnChanges {
  @Input()
  anzahlJahre = 10;
  @Input()
  nullable = true;
  @Input()
  jahresEnde = false;

  jahresOptions = this.getJahresOptions();

  public formControl: FormControl;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new FormControl(null);
    this.formControl.valueChanges.subscribe(value => {
      this.onChange(value ? new Date(value) : value);
      this.changeDetector.detectChanges();
    });
  }

  public writeValue(value: Date | null): void {
    this.formControl.reset(value?.toISOString(), { emitEvent: false });
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

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.jahresEnde && !changes.jahresEnde.firstChange) {
      throw new Error('jahresEnde darf nicht geändert werden');
    }
    this.jahresOptions = this.getJahresOptions();
  }

  getJahresOptions(): EnumOption[] {
    const currentYear = new Date().getFullYear();
    return [...Array(10).keys()].map(n => {
      return {
        displayText: (currentYear - n).toString(),
        name: this.jahresEnde
          ? new Date(Date.UTC(currentYear - n, 11, 31)).toISOString()
          : new Date(Date.UTC(currentYear - n, 0)).toISOString(),
      };
    });
  }
}
