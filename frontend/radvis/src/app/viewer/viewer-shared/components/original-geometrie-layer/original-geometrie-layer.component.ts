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

import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import GeoJSON from 'ol/format/GeoJSON';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import {
  isLineString,
  isMultiLineString,
  isPoint,
  LineStringGeojson,
  MultiLineStringGeojson,
  PointGeojson,
} from 'src/app/shared/models/geojson-geometrie';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { originalGeometrieLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-original-geometrie-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OriginalGeometrieLayerComponent implements OnDestroy, OnChanges, OnInit {
  @Input()
  public geometrie!: MultiLineStringGeojson | LineStringGeojson | PointGeojson;

  private vectorSource: VectorSource;
  private layer!: VectorLayer;

  constructor(private olMapService: OlMapService) {
    this.vectorSource = new VectorSource();
  }

  ngOnInit(): void {
    this.layer = new VectorLayer({
      source: this.vectorSource,
      style: MapStyles.getDefaultTransparentStyle(),
      zIndex: originalGeometrieLayerZIndex,
    });
    this.layer.set(OlMapService.LAYER_ID, FAHRRADROUTE.name);
    this.olMapService.addLayer(this.layer);
  }

  ngOnChanges(): void {
    invariant(this.geometrie);
    invariant(
      isLineString(this.geometrie) || isMultiLineString(this.geometrie) || isPoint(this.geometrie),
      'Originalgeometrie muss ein Linestring oder Multilinestring oder Point sein'
    );
    this.vectorSource.clear();
    this.vectorSource.addFeatures(new GeoJSON().readFeatures(this.geometrie));
    this.vectorSource.changed();
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.layer);
  }
}
