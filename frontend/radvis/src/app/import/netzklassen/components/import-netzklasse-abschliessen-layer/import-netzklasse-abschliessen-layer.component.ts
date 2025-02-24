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
import { Feature } from 'ol';
import { FeatureLike } from 'ol/Feature';
import { GeoJSON } from 'ol/format';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-import-netzklasse-abschliessen-layer',
  template: '',
  styles: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportNetzklasseAbschliessenLayerComponent implements OnInit, OnChanges, OnDestroy {
  @Input()
  alleFeatures!: GeoJSONFeatureCollection;

  @Input()
  kanteIdsMitNetzklasse!: number[];

  private featureSource: VectorSource = new VectorSource();
  private readonly olLayer: VectorLayer;

  constructor(private olMapService: OlMapService) {
    this.olLayer = new VectorLayer({
      source: this.featureSource,
      style: (feature: FeatureLike, resolution: number): Style | Style[] =>
        (feature.get('hasNetzklasse')
          ? MapStyles.getDefaultNetzStyleFunction(MapStyles.FREMDNETZ_COLOR)
          : (): Style => new Style())(feature, resolution),
      declutter: true,
      minZoom: 0,
    });
    this.olLayer.setZIndex(EditorLayerZindexConfig.MANUELLER_IMPORT_NETZKLASSE_ABSCHLIESSEN_LAYER);
    this.olMapService.addLayer(this.olLayer);
  }

  private static setHasNetzklasse(feature: Feature, kanteIdsMitNetzklasse: number[]): Feature {
    const id: number | string | undefined = feature.getId();
    if (id) {
      feature.set('hasNetzklasse', kanteIdsMitNetzklasse.includes(+id));
    } else {
      feature.set('hasNetzklasse', false);
    }
    return feature;
  }

  ngOnInit(): void {
    invariant(this.alleFeatures);
    invariant(this.kanteIdsMitNetzklasse);

    this.featureSource.addFeatures(
      new GeoJSON()
        .readFeatures(this.alleFeatures)
        .map(
          (feature: Feature): Feature =>
            ImportNetzklasseAbschliessenLayerComponent.setHasNetzklasse(feature, this.kanteIdsMitNetzklasse || [])
        )
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.alleFeatures && !changes.alleFeatures.firstChange) {
      throw new Error('Die features dürfen sich nicht verändern');
    }
    if (changes.kanteIdsMitNetzklasse && !changes.kanteIdsMitNetzklasse.firstChange) {
      throw new Error('Die KantenIDs mit Netzklasse dürfen sich nicht verändern');
    }
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
  }
}
