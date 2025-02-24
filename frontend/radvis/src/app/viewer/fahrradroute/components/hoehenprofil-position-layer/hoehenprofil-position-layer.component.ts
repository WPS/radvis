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

import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, OnInit, Optional } from '@angular/core';
import { Feature } from 'ol';
import { Coordinate } from 'ol/coordinate';
import { Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { fahrradroutenMatchingFehlerGeometriesLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';

@Component({
  selector: 'rad-hoehenprofil-position-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class HoehenprofilPositionLayerComponent implements OnDestroy, OnChanges, OnInit {
  @Input()
  public position!: Coordinate | null;

  private vectorSource: VectorSource;
  private layer!: VectorLayer;

  constructor(@Optional() private olMapService: OlMapService) {
    this.vectorSource = new VectorSource();
  }

  ngOnInit(): void {
    this.layer = new VectorLayer({
      source: this.vectorSource,
      style: MapStyles.getPositionPointStyle(),
      zIndex: fahrradroutenMatchingFehlerGeometriesLayerZIndex,
    });
    this.olMapService?.addLayer(this.layer);
  }

  ngOnChanges(): void {
    this.vectorSource.clear();
    if (this.position) {
      this.vectorSource.addFeature(new Feature(new Point(this.position)));
      this.vectorSource.changed();
    }
  }

  ngOnDestroy(): void {
    this.olMapService?.removeLayer(this.layer);
  }
}
