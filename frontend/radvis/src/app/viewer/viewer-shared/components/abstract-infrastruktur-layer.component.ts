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

import Feature, { FeatureLike } from 'ol/Feature';
import { Geometry } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { infrastrukturLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';
import { AbstractInfrastrukturenRoutingService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-routing.service';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { Color } from 'ol/color';

export abstract class AbstractInfrastrukturLayerComponent<T> {
  public static BEZEICHNUNG_PROPERTY_NAME = 'bezeichnung';

  selectedId: number | null = null;

  protected vectorSource: VectorSource<Geometry>;

  protected subscriptions: Subscription[] = [];

  protected readonly HIGHLIGHTED_PROPERTY_NAME = 'highlighted';
  protected readonly ICON_COLOR_PROPERTY_NAME = 'icon-color';

  protected get layerId(): string {
    return this._layerId ?? this.infrastruktur.name;
  }

  constructor(
    protected routingService: AbstractInfrastrukturenRoutingService,
    protected filterService: AbstractInfrastrukturenFilterService<T>,
    protected featureHighlightService: FeatureHighlightService,
    private infrastruktur: Infrastruktur,
    private _layerId?: string
  ) {
    this.vectorSource = new VectorSource();

    this.subscriptions.push(
      this.routingService.selectedInfrastrukturId$.subscribe(id => {
        if (this.selectedId) {
          this.setFeatureHighlighted(this.selectedId, false);
        }
        if (id) {
          this.setFeatureHighlighted(id, true);
        }
        this.selectedId = id;
      })
    );
  }

  protected static infrastrukturIconStyle(highlighted: boolean, infrastruktur: Infrastruktur, color?: Color): Style[] {
    return MapStyles.getInfrastrukturIconStyle(infrastruktur.iconFileName, highlighted, color);
  }

  protected initServiceSubscriptions(): void {
    this.subscriptions.push(
      this.filterService.filteredList$.subscribe(list => {
        const features = ([] as Feature[]).concat(...list.map(infrastruktur => this.convertToFeature(infrastruktur)));
        this.vectorSource.clear(true);
        this.vectorSource.addFeatures(features);
        if (this.selectedId) {
          this.setFeatureHighlighted(this.selectedId, true);
        }
        this.vectorSource.changed();
      })
    );

    this.subscriptions.push(
      this.featureHighlightService.highlightedFeature$.pipe(filter(hf => hf.layer === this.layerId)).subscribe(hf => {
        this.setFeatureHighlighted(this.extractIdFromFeature(hf), true);
      }),
      this.featureHighlightService.unhighlightedFeature$.pipe(filter(hf => hf.layer === this.layerId)).subscribe(hf => {
        this.setFeatureHighlighted(this.extractIdFromFeature(hf), false);
        if (this.selectedId) {
          this.setFeatureHighlighted(this.selectedId, true);
        }
      })
    );
  }

  protected createLayer(minZoom: number = 0): VectorLayer {
    const olLayer = new VectorLayer({
      source: this.vectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: this.styleFn,
      zIndex: infrastrukturLayerZIndex,
      minZoom,
    });
    olLayer.set(OlMapService.LAYER_ID, this.layerId);
    return olLayer;
  }

  protected setFeatureHighlighted(id: number, highlighted: boolean): void {
    const currentSelectedFeature = this.vectorSource.getFeatureById(id);
    if (currentSelectedFeature) {
      currentSelectedFeature.set(this.HIGHLIGHTED_PROPERTY_NAME, highlighted);
      currentSelectedFeature.changed();
    }
  }

  // eslint-disable-next-line no-unused-vars
  protected styleFn = (feature: FeatureLike, resolution: number): Style | Style[] => {
    return AbstractInfrastrukturLayerComponent.infrastrukturIconStyle(
      feature.get(this.HIGHLIGHTED_PROPERTY_NAME),
      this.infrastruktur,
      feature.get(this.ICON_COLOR_PROPERTY_NAME)
    );
  };

  // eslint-disable-next-line no-unused-vars
  protected styleWithHighlightCircleFn = (feature: FeatureLike, resolution: number): Style | Style[] => {
    const styles = [];

    // Erst den highlight-Punkt hinzufügen, damit die Render-Reihenfolge stimmt.
    if (feature.get(this.HIGHLIGHTED_PROPERTY_NAME) && this.selectedId === feature.getId()) {
      styles.push(MapStyles.defaultPointStyleLarge(MapStyles.FEATURE_SELECT_COLOR));
    }

    styles.push(...(this.styleFn(feature, resolution) as Style[]));
    return styles;
  };

  // eslint-disable-next-line no-unused-vars
  protected addIconColorAttribute(infrastrukturen: T[]): void {}

  protected abstract convertToFeature(infrastruktur: T): Feature[];

  protected abstract extractIdFromFeature(hf: RadVisFeature): number;
}
