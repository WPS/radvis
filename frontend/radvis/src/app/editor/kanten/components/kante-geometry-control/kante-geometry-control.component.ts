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
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import { Color } from 'ol/color';
import Geometry from 'ol/geom/Geometry';
import LineString from 'ol/geom/LineString';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-kante-geometry-control',
  templateUrl: './kante-geometry-control.component.html',
  styleUrls: ['./kante-geometry-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => KanteGeometryControlComponent), multi: true },
  ],
})
export class KanteGeometryControlComponent extends AbstractFormControl<LineStringGeojson> {
  @Input()
  color!: Color;

  @Input()
  withDirectionalArrows!: boolean;

  public linestring: LineString = new LineString([]);
  public disabled = false;

  constructor(private changeDetectorRef: ChangeDetectorRef) {
    super();
  }

  public writeValue(value: LineStringGeojson | null): void {
    this.linestring = new LineString(value?.coordinates || []);
    this.changeDetectorRef.markForCheck();
  }

  public setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.changeDetectorRef.markForCheck();
  }

  onModify(geometry: Geometry): void {
    const linestring = geometry as LineString;
    invariant(linestring);
    this.onChange({ coordinates: linestring.getCoordinates(), type: 'LineString' });
  }
}
