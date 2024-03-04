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

import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { FeatureLike } from 'ol/Feature';
import { GeoJSON } from 'ol/format';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Icon, Style } from 'ol/style';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-import-sackgassen-layer',
  template: '',
  styles: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportNetzklasseSackgassenLayerComponent implements OnInit, OnChanges, OnDestroy {
  @Input()
  alleFeatures!: GeoJSONFeatureCollection;

  private featureSource: VectorSource = new VectorSource();
  private olLayer: VectorLayer;

  constructor(private olMapService: OlMapService) {
    this.olLayer = new VectorLayer({
      source: this.featureSource,
      style: this.styleFunction,
      minZoom: 0,
    });
    this.olLayer.setZIndex(999);
    this.olMapService.addLayer(this.olLayer);
  }

  ngOnInit(): void {
    invariant(this.alleFeatures);
    const features = new GeoJSON().readFeatures(this.alleFeatures);

    this.featureSource.addFeatures(features);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.alleFeatures && !changes.alleFeatures.firstChange) {
      throw new Error('Die features dürfen sich nicht verändern');
    }
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
  }

  private styleFunction = (feature: FeatureLike): Style | Style[] => {
    return new Style({
      geometry: feature.getGeometry() as Point,
      image: new Icon({
        anchor: [0.5, 0.5],
        scale: 1.0,
        src: './assets/AchtungSackgasse.svg',
      }),
    });
  };
}
