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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatSliderChange } from '@angular/material/slider';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-deckkraft-slider-control',
  templateUrl: './deckkraft-slider-control.component.html',
  styleUrls: ['./deckkraft-slider-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeckkraftSliderControlComponent),
      multi: true,
    },
  ],
})
export class DeckkraftSliderControlComponent extends AbstractFormControl<number> {
  value!: number;
  disabled = false;

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.changeDetector.markForCheck();
  }

  writeValue(value: number): void {
    invariant(value != null, 'Ein Initialwert fuer die Deckkraft muss gesetzt sein');
    this.value = value;
    this.changeDetector.markForCheck();
  }

  onSliderMove(event: MatSliderChange): void {
    this.value = event.value as number;
    this.onChange(event.value);
  }
}