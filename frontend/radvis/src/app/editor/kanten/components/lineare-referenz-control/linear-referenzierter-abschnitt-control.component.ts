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
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { FormArray, FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MAT_CHECKBOX_DEFAULT_OPTIONS, MatCheckboxDefaultOptions } from '@angular/material/checkbox';
import { LineString } from 'ol/geom';
import { Layer } from 'ol/layer';
import { Source } from 'ol/source';
import { Subscription } from 'rxjs';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { LinearReferenzierterAbschnittInMeter } from 'src/app/editor/kanten/models/linear-referenzierter-abschnitt-in-meter';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import { SelectElementEvent } from 'src/app/shared/components/lineare-referenzierung-layer/lineare-referenzierung-layer.component';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { LinearReferenzierterAbschnitt } from 'src/app/shared/models/linear-referenzierter-abschnitt';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import { IS_SELECTABLE_LAYER } from 'src/app/shared/models/selectable-layer-property';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-linear-referenzierter-abschnitt-control',
  templateUrl: './linear-referenzierter-abschnitt-control.component.html',
  styleUrls: ['./linear-referenzierter-abschnitt-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LinearReferenzierterAbschnittControlComponent),
      multi: true,
    },
    { provide: MAT_CHECKBOX_DEFAULT_OPTIONS, useValue: { clickAction: 'noop' } as MatCheckboxDefaultOptions },
  ],
})
export class LinearReferenzierterAbschnittControlComponent
  extends AbstractFormControl<LinearReferenzierterAbschnitt[]>
  implements OnChanges {
  @Input()
  seitenbezug: Seitenbezug | undefined;
  @Input()
  selectedIndices: number[] = [];
  @Input()
  geometrie!: LineStringGeojson;

  @Output()
  insertAtIndex = new EventEmitter<number>();
  @Output()
  deleteAtIndex = new EventEmitter<number>();
  @Output()
  selectElement = new EventEmitter<SelectElementEvent>();
  @Output()
  deselectElement = new EventEmitter<number>();

  hoveredSegmentIndex: number | null = null;
  isDisabled = false;
  lineareReferenzenForm: FormArray<FormControl<number>>;
  valueChangeSubscription: Subscription;
  previousControlValues: number[] = [];
  relativeSegmentPoints!: number[];
  lineString!: LineString;

  lineareReferenzierungZIndex = EditorLayerZindexConfig.LINEARE_REFERENZIERUNG_LAYER;

  private get gesamtLaenge(): number {
    return this.lineString.getLength();
  }

  constructor(private changeDetector: ChangeDetectorRef, private notifyUserService: NotifyUserService) {
    super();
    const initializerValue: FormControl<number>[] = [];
    this.lineareReferenzenForm = new FormArray(initializerValue);

    this.valueChangeSubscription = this.subscribeToValueChanges();
  }

  layerFilter: (l: Layer<Source>) => boolean = l => {
    return l.get(IS_SELECTABLE_LAYER);
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.geometrie) {
      if (changes.geometrie.firstChange) {
        this.lineString = new LineString(this.geometrie.coordinates);
      } else {
        throw new Error('Die Geometrie darf sich nicht ändern, während lineare Referenzen bearbeitet werden.');
      }
    }
  }

  onHoveredSegmentIndexChanged(index: number | null): void {
    this.hoveredSegmentIndex = index;
    this.changeDetector.detectChanges();
  }

  onHoverSegment(hovered: boolean, index: number): void {
    if (hovered) {
      this.hoveredSegmentIndex = index;
    } else {
      this.hoveredSegmentIndex = null;
    }
  }

  onCutSegment(index: number, event: Event): void {
    event.stopPropagation();
    const vonValue = this.getVonControl(index).value;
    const bisValue = this.getBisControl(index).value;
    const newValue = 0.5 * (bisValue - vonValue) + vonValue;

    this.insertAtIndex.emit(index + 1);
    this.lineareReferenzenForm.insert(index + 1, this.createControl(newValue));
  }

  onDeleteSegment(index: number, event: Event): void {
    event.stopPropagation();
    invariant(index > 0, 'Das erste Segment darf nicht gelöscht werden');
    invariant(index < this.lineareReferenzenForm.length - 1);
    this.deleteAtIndex.emit(index);
    this.lineareReferenzenForm.removeAt(index);
  }

  onSelectFromTabelle(segmentIndex: number): void {
    if (this.selectedIndices.includes(segmentIndex)) {
      this.deselectElement.emit(segmentIndex);
    } else {
      this.selectElement.emit({ index: segmentIndex, additiv: true });
    }
  }

  onSelectFromMap(event: SelectElementEvent): void {
    invariant(event.index < this.lineareReferenzenForm.length);
    this.selectElement.emit(event);
  }

  onDeselectFromMap(index: number): void {
    this.deselectElement.emit(index);
  }

  onSegmentierungAufKarteChanged(newSegmentierung: number[]): void {
    invariant(
      newSegmentierung.length === this.lineareReferenzenForm.controls.length,
      'über Karte kann Anzahl von Segmenten nicht verändert werden'
    );
    const newMetermarken = newSegmentierung.map(frac => frac * this.gesamtLaenge);
    this.lineareReferenzenForm.reset(newMetermarken);
  }

  public writeValue(value: LinearReferenzierterAbschnitt[] | null): void {
    this.valueChangeSubscription.unsubscribe();
    this.lineareReferenzenForm.clear();
    if (value != null && value.length > 0) {
      invariant(
        value.every(v => v.von <= v.bis),
        'von kleiner bis'
      );
      invariant(
        value.every(v => v.von >= 0 && v.bis <= 1),
        'Segmentierung ist relativ zwischen 0 und 1'
      );
      const sorted = value.sort(LinearReferenzierterAbschnitt.sort);
      invariant(sorted[0].von === 0, 'Segmentierung beginnt bei 0');
      invariant(sorted[sorted.length - 1].bis === 1, 'Segmentierung endet bei 1');

      const valuesInMeter: LinearReferenzierterAbschnittInMeter[] = sorted.map(linRef =>
        LinearReferenzierterAbschnittInMeter.of(linRef, this.gesamtLaenge)
      );
      this.lineareReferenzenForm.push(this.createControl(valuesInMeter[0].von, true));
      for (let index = 1; index < valuesInMeter.length; index++) {
        this.lineareReferenzenForm.push(this.createControl(valuesInMeter[index].von));
      }
      this.lineareReferenzenForm.push(this.createControl(valuesInMeter[valuesInMeter.length - 1].bis, true));
    }
    this.relativeSegmentPoints = this.lineareReferenzenForm.getRawValue().map(m => m / this.gesamtLaenge);
    this.changeDetector.markForCheck();
    this.valueChangeSubscription = this.subscribeToValueChanges();
    this.previousControlValues = this.lineareReferenzenForm.getRawValue();
  }

  public getVonControl(index: number): FormControl<number> {
    return this.lineareReferenzenForm.at(index) as FormControl<number>;
  }

  public getBisControl(index: number): FormControl<number> {
    return this.lineareReferenzenForm.at(index + 1) as FormControl<number>;
  }

  public setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    this.changeDetector.markForCheck();
  }

  private createControl(withValue: number, disabled = false): FormControl<number> {
    return new FormControl<number>({ value: withValue, disabled }, { nonNullable: true });
  }

  private subscribeToValueChanges(): Subscription {
    return this.lineareReferenzenForm.valueChanges.subscribe(() => {
      if (this.lineareReferenzenForm.length === this.previousControlValues.length) {
        this.nachMinUndMaxWertDerMetermarkenKorrigieren(this.lineareReferenzenForm);
      }
      const value = this.lineareReferenzenForm.getRawValue();
      const updatedLineareReferenzen: LinearReferenzierterAbschnitt[] = [];
      for (let i = 1; i < value.length; i++) {
        updatedLineareReferenzen.push(
          LinearReferenzierterAbschnittInMeter.ofMeter(
            value[i - 1],
            value[i]
          ).getLinearReferenzierterAbschnittRelativZuLaenge(this.gesamtLaenge)
        );
      }
      this.relativeSegmentPoints = value.map(m => m / this.gesamtLaenge);
      this.onChange(updatedLineareReferenzen);
      this.previousControlValues = value;
    });
  }

  private nachMinUndMaxWertDerMetermarkenKorrigieren(array: FormArray<FormControl<number>>): void {
    const controls = (array as FormArray<FormControl<number>>).controls;
    const i = this.getIndexOfFirstChangedValue(this.previousControlValues, array.getRawValue());
    if (i == null) {
      // kein Aenderungen -> keine Korrektur notwendig
      return;
    }
    const lowerLimit = controls[i - 1].value;
    const aktuelleMetermarke = controls[i].value;
    const upperLimit = controls[i + 1].value;
    const msg = 'Fehlerhafte Werte wurden automatisch korrigiert. Bitte prüfen!';
    if (aktuelleMetermarke < lowerLimit) {
      this.notifyUserService.warn(msg);
      controls[i].setValue(lowerLimit + LinearReferenzierterAbschnittInMeter.MINIMUM_ELEMENT_LENGTH);
    }
    if (aktuelleMetermarke > upperLimit) {
      this.notifyUserService.warn(msg);
      controls[i].setValue(upperLimit - LinearReferenzierterAbschnittInMeter.MINIMUM_ELEMENT_LENGTH);
    }
  }

  private getIndexOfFirstChangedValue(array1: number[], array2: number[]): number | null {
    for (let i = 1; i < array1.length - 1; i++) {
      if (array1[i] !== array2[i]) {
        return i;
      }
    }
    return null;
  }
}
