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
import { NG_VALUE_ACCESSOR, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { DateRange } from '@angular/material/datepicker';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';

@Component({
  selector: 'rad-jahreszeitraum-control',
  templateUrl: './jahreszeitraum-control.component.html',
  styleUrls: ['./jahreszeitraum-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => JahreszeitraumControlComponent), multi: true },
  ],
  standalone: false,
})
export class JahreszeitraumControlComponent extends AbstractFormControl<DateRange<Date>> {
  @Input()
  anzahlJahre = 10;

  public formGroup = new UntypedFormGroup({
    start: new UntypedFormControl(null),
    end: new UntypedFormControl(null),
  });

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formGroup.valueChanges.subscribe(value => {
      this.onChange(value.start && value.end ? new DateRange<Date>(value.start, value.end) : null);
      this.changeDetector.detectChanges();
    });
  }

  public writeValue(value: DateRange<Date> | null): void {
    this.formGroup.reset({ start: value?.start, end: value?.end }, { emitEvent: false });
    this.changeDetector.markForCheck();
  }

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formGroup.disable({ emitEvent: false });
    } else {
      this.formGroup.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }
}
