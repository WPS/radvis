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
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormControl } from '@angular/forms';
import { LayerId, RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { ColorToCssPipe } from 'src/app/shared/components/color-to-css.pipe';

@Component({
  selector: 'rad-layer-auswahl',
  templateUrl: './layer-auswahl.component.html',
  styleUrls: ['./layer-auswahl.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LayerAuswahlComponent implements OnChanges, OnInit {
  @Input()
  public layers: RadVisLayer[] = [];
  @Input()
  public visibleLayers: RadVisLayer[] | null = [];
  @Input()
  public zoom: number = Number.MAX_VALUE;
  @Output()
  public showLayer = new EventEmitter<LayerId>();
  @Output()
  public hideLayer = new EventEmitter<LayerId>();

  public formArray: UntypedFormArray;

  constructor(private formBuilder: UntypedFormBuilder) {
    this.formArray = formBuilder.array([]);
  }

  ngOnInit(): void {
    this.formArray = this.formBuilder.array(
      this.layers.map(layer => {
        const formGroup = this.formBuilder.group({
          name: layer.bezeichnung,
          selected: this.isVisible(layer),
          color: ColorToCssPipe.convertToCss(layer.baseColor),
          minZoom: layer.minZoom ? layer.minZoom : 0,
        });
        formGroup.get('selected')?.valueChanges.subscribe(value => {
          if (value) {
            this.showLayer.emit(layer.id);
          } else {
            this.hideLayer.emit(layer.id);
          }
        });
        return formGroup;
      })
    );
  }

  public ngOnChanges(changes: SimpleChanges): void {
    if (changes.visibleLayers && !changes.visibleLayers.firstChange) {
      this.applyLayerVisibility();
    }
  }

  public asFormControl(control: AbstractControl | null): UntypedFormControl {
    return control as UntypedFormControl;
  }

  private applyLayerVisibility(): void {
    this.formArray.setValue(
      this.layers.map(layer => {
        return {
          name: layer.bezeichnung,
          selected: this.isVisible(layer),
          color: ColorToCssPipe.convertToCss(layer.baseColor),
          minZoom: layer.minZoom ? layer.minZoom : 0,
        };
      }),
      { emitEvent: false }
    );
  }

  private isVisible(layer: RadVisLayer): boolean {
    return this.visibleLayers?.map(l => l.id).includes(layer.id) || false;
  }
}
