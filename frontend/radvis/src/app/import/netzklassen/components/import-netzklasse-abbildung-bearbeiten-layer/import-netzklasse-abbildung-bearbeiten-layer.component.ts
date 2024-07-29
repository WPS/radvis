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
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
} from '@angular/core';
import { FeatureLike } from 'ol/Feature';
import { GeoJSON } from 'ol/format';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Layer } from 'ol/layer';
import VectorImageLayer from 'ol/layer/VectorImage';
import { Source } from 'ol/source';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import { Subscription } from 'rxjs';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-import-abbildung-bearbeiten-layer',
  template: '',
  styles: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportNetzklasseAbbildungBearbeitenLayerComponent implements OnDestroy, OnChanges, AfterViewInit {
  private static readonly COLOR_NETZKLASSE_ALREADYPRESENT = MapStyles.FEATURE_COLOR_LIGHTER;
  private static readonly COLOR_NETZKLASSE_INSERTED = MapStyles.FREMDNETZ_COLOR;
  private static readonly COLOR_NETZKLASSE_DELETED = [171, 21, 76, 1];

  @Input()
  alleFeatures!: GeoJSONFeatureCollection;

  @Input()
  kanteIdsMitNetzklasse: number[] | null = null;

  @Output()
  toggleNetzklasse: EventEmitter<number> = new EventEmitter<number>();

  @Output()
  featuresWithUnchangedNetzklasseVisible: EventEmitter<boolean> = new EventEmitter<boolean>();

  private featureSource: VectorSource = new VectorSource({ overlaps: false });

  private olLayer: VectorImageLayer;

  private subscriptions: Subscription[] = [];

  constructor(private olMapService: OlMapService) {
    this.olLayer = new VectorImageLayer({
      source: this.featureSource,
      style: (feature: FeatureLike, resolution: number): Style | Style[] =>
        ImportNetzklasseAbbildungBearbeitenLayerComponent.selectStyle(
          feature.get('hasNetzklasse'),
          this.hasNetzklasseNachher(feature),
          feature,
          resolution,
          this.olMapService.getZoomForResolution(resolution) as number
        ),
      declutter: true,
      minZoom: 0,
    });
    this.olLayer.setZIndex(EditorLayerZindexConfig.MANUELLER_IMPORT_NETZKLASSE_BEARBEITEN_LAYER);
    this.olMapService.addLayer(this.olLayer);

    this.subscriptions.push(
      this.olMapService.getResolution$().subscribe(event => {
        const zoomForResolution = this.olMapService.getZoomForResolution(event);
        if (zoomForResolution) {
          this.featuresWithUnchangedNetzklasseVisible.emit(
            ImportNetzklasseAbbildungBearbeitenLayerComponent.isFeatureWithUnchangedNetzklasseVisible(zoomForResolution)
          );
        }
      })
    );

    this.subscriptions.push(
      this.olMapService.click$().subscribe(event => {
        const featuresAtPixel = this.olMapService.getFeaturesAtPixel(
          event.pixel,
          (layer: Layer<Source>) => layer === this.olLayer
        );
        const clickedFeature = featuresAtPixel ? featuresAtPixel[0] : null;
        if (clickedFeature) {
          const id = clickedFeature.getId();
          invariant(id);
          this.toggleNetzklasse.emit(+id);
        }
      })
    );
  }

  private static selectStyle(
    hatNetzklasseVorher: boolean,
    hatNetzklasseNachher: boolean,
    feature: FeatureLike,
    resolution: number,
    zoomForResolution: number
  ): Style | Style[] {
    if (
      ImportNetzklasseAbbildungBearbeitenLayerComponent.isFeatureHidden(
        hatNetzklasseVorher,
        hatNetzklasseNachher,
        zoomForResolution
      )
    ) {
      return new Style();
    }

    return MapStyles.getDefaultNetzStyleFunction(
      ImportNetzklasseAbbildungBearbeitenLayerComponent.selectFeatureColor(hatNetzklasseVorher, hatNetzklasseNachher)
    )(feature, resolution);
  }

  private static isFeatureHidden(
    hatNetzklasseVorher: boolean,
    hatNetzklasseNachher: boolean,
    zoomForResolution: number
  ): boolean {
    return (
      ((!hatNetzklasseVorher && !hatNetzklasseNachher) || (hatNetzklasseVorher && hatNetzklasseNachher)) &&
      !this.isFeatureWithUnchangedNetzklasseVisible(zoomForResolution)
    );
  }

  private static isFeatureWithUnchangedNetzklasseVisible(zoomForResolution: number): boolean {
    return Netzklassefilter.NICHT_KLASSIFIZIERT.isVisibleOnZoomlevel(zoomForResolution);
  }

  private static selectFeatureColor(hatNetzklasseVorher: boolean, hatNetzklasseNachher: boolean): number[] | undefined {
    return !hatNetzklasseVorher && hatNetzklasseNachher
      ? ImportNetzklasseAbbildungBearbeitenLayerComponent.COLOR_NETZKLASSE_INSERTED
      : hatNetzklasseVorher && !hatNetzklasseNachher
        ? ImportNetzklasseAbbildungBearbeitenLayerComponent.COLOR_NETZKLASSE_DELETED
        : hatNetzklasseVorher && hatNetzklasseNachher
          ? ImportNetzklasseAbbildungBearbeitenLayerComponent.COLOR_NETZKLASSE_ALREADYPRESENT
          : undefined;
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(this.alleFeatures);

    if (changes.alleFeatures) {
      this.featureSource.clear(true);
      this.featureSource.addFeatures(new GeoJSON().readFeatures(this.alleFeatures));
    }

    if (this.kanteIdsMitNetzklasse) {
      this.olLayer.changed();
    }
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  ngAfterViewInit(): void {
    const res = this.olMapService.getCurrentResolution();
    if (res) {
      const zoomForRes = this.olMapService.getZoomForResolution(res);
      if (zoomForRes) {
        this.featuresWithUnchangedNetzklasseVisible.emit(
          ImportNetzklasseAbbildungBearbeitenLayerComponent.isFeatureWithUnchangedNetzklasseVisible(zoomForRes)
        );
      }
    }
  }

  private hasNetzklasseNachher(feature: FeatureLike): boolean {
    const id: number | string | undefined = feature.getId();
    if (id) {
      return this.kanteIdsMitNetzklasse?.includes(+id) || false;
    } else {
      return false;
    }
  }
}
