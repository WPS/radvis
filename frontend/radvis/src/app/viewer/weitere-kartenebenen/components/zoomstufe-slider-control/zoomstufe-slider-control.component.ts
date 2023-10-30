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
import { View } from 'ol';
import { get } from 'ol/proj';
import { METERS_PER_UNIT } from 'ol/proj/Units';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-zoomstufe-slider-control',
  templateUrl: './zoomstufe-slider-control.component.html',
  styleUrls: ['./zoomstufe-slider-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ZoomstufeSliderControlComponent),
      multi: true,
    },
  ],
})
export class ZoomstufeSliderControlComponent extends AbstractFormControl<number> {
  value!: number;
  disabled = false;

  private viewEPSG25832 = new View({ projection: 'EPSG:25832' });

  constructor(private cd: ChangeDetectorRef) {
    super();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.cd.markForCheck();
  }

  writeValue(value: number): void {
    invariant(value != null, 'Ein Initialwert für die Zoomstufe muss gesetzt sein');
    this.value = value;
    this.cd.markForCheck();
  }

  onSliderMove(event: MatSliderChange): void {
    this.value = event.value as number;
    this.onChange(event.value);
  }

  getMassStab(): number {
    const resolution = this.viewEPSG25832.getResolutionForZoom(this.value);
    const unit = get('EPSG:25832').getUnits();
    const inchesPerMeter = 39.37;
    const mmPerInch = 25.4;
    const mmPerPixel = 0.28; // siehe https://openlayers.org/en/latest/apidoc/module-ol_control_ScaleLine-ScaleLine.html option dpi
    const dpi = mmPerInch / mmPerPixel;
    return resolution * METERS_PER_UNIT[unit] * inchesPerMeter * dpi;
  }

  getMassStabInfo(): string {
    if (this.value >= 8.5 && this.value <= 9) {
      return 'Baden-Württemberg';
    } else if (this.value < 7) {
      return 'Deutschland';
    }

    return '';
  }
}
