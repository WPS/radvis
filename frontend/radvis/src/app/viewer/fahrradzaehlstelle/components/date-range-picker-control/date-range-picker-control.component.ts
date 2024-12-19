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
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  ViewChild,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import {
  DateRange,
  MAT_DATE_RANGE_SELECTION_STRATEGY,
  MatDatepickerIntl,
  MatDateRangeInput,
} from '@angular/material/datepicker';
import { DateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { ArtDerAuswertungSelectionStrategy } from './art-der-auswertung-selection-strategy';
import { RadvisMatDatepickerIntl } from 'src/app/shared/components/radvis-mat-datepicker-intl';
import { ArtDerAuswertung } from 'src/app/viewer/fahrradzaehlstelle/models/art-der-auswertung';
import { DeLocaleDateAdapter } from 'src/app/shared/components/de-locale-date-adapter';
import { debounceTime } from 'rxjs/operators';

@Component({
  selector: 'rad-date-range-picker-control',
  templateUrl: './date-range-picker-control.component.html',
  styleUrls: ['./date-range-picker-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: MAT_DATE_RANGE_SELECTION_STRATEGY,
      useClass: ArtDerAuswertungSelectionStrategy,
    },
    {
      provide: DateAdapter,
      useClass: DeLocaleDateAdapter,
    },
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DateRangePickerControlComponent),
      multi: true,
    },
    { provide: MAT_DATE_LOCALE, useValue: 'de-DE' },
    { provide: MatDatepickerIntl, useClass: RadvisMatDatepickerIntl },
  ],
})
export class DateRangePickerControlComponent extends AbstractFormControl<DateRange<Date>> implements OnChanges {
  @ViewChild('dateRangeInput', { static: true })
  dateRangeInput: ElementRef<MatDateRangeInput<DateRange<Date>>> | undefined;

  @Input()
  artDerAuswertung: ArtDerAuswertung = ArtDerAuswertung.DURCHSCHNITT_PRO_STUNDE;

  maxDate = new Date();

  public formGroup = new UntypedFormGroup({
    start: new UntypedFormControl(null),
    end: new UntypedFormControl(null),
  });
  manualDateInputDisabled = true;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formGroup.valueChanges.pipe(debounceTime(100)).subscribe(value => {
      if (value.start && value.end) {
        this.onChange(new DateRange<Date>(value.start, value.end));
        this.changeDetector.detectChanges();
      }
    });
  }

  public writeValue(value: DateRange<Date> | null): void {
    this.manualDateInputDisabled = false;
    this.formGroup.reset({ start: value?.start || null, end: value?.end || null });
    this.manualDateInputDisabled = true;
    this.changeDetector.detectChanges();
  }

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formGroup.disable({ emitEvent: false });
    } else {
      this.formGroup.enable({ emitEvent: false });
    }
    this.changeDetector.markForCheck();
  }

  ngOnChanges(): void {
    ArtDerAuswertungSelectionStrategy.setStrategy(this.artDerAuswertung);
  }
}
