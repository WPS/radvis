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
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import Feature, { FeatureLike } from 'ol/Feature';
import Geometry from 'ol/geom/Geometry';
import LineString from 'ol/geom/LineString';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import Style, { StyleFunction } from 'ol/style/Style';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';
import { IS_SELECTABLE_LAYER } from 'src/app/shared/models/selectable-layer-property';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { MapBrowserEvent } from 'ol';
import { Layer } from 'ol/layer';
import { Source } from 'ol/source';
import { Subscription } from 'rxjs';

@Component({
  selector: 'rad-kante-grundgeometrie-layer',
  templateUrl: './kante-grundgeometrie-layer.component.html',
  styleUrls: ['./kante-grundgeometrie-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KanteGrundgeometrieLayerComponent implements OnChanges, OnDestroy, OnInit {
  @Input()
  geometry!: Geometry;
  @Input()
  kanteId!: number;

  @Output()
  deselected = new EventEmitter<void>();

  private vectorSource: VectorSource = new VectorSource();

  private olLayer: VectorLayer;
  private subscriptions: Subscription[] = [];

  constructor(private olMapService: OlMapService) {
    this.olLayer = new VectorLayer({
      source: this.vectorSource,
      zIndex: EditorLayerZindexConfig.KANTE_GRUNDGEOMETRIE_LAYER,
      style: this.styleFn,
    });
    this.olLayer.set(IS_SELECTABLE_LAYER, true);
  }

  ngOnInit(): void {
    this.olMapService.addLayer(this.olLayer);
    this.subscriptions.push(
      this.olMapService.click$().subscribe(clickEvent => {
        this.onMapClick(clickEvent);
      })
    );
  }

  ngOnChanges(): void {
    invariant(this.geometry);

    const feature = new Feature(this.geometry);
    feature.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, this.kanteId);

    this.vectorSource.clear();
    this.vectorSource.addFeature(feature);
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  private onMapClick(clickEvent: MapBrowserEvent<UIEvent>): void {
    const featuresAtPixel = this.olMapService.getFeaturesAtPixel(clickEvent.pixel, this.layerFilter);
    if (!featuresAtPixel || featuresAtPixel.length === 0) {
      return;
    }
    // Das erste Feature im Array ist das am nähesten zur Click-Position liegende
    const clickedFeature = featuresAtPixel[0] as Feature<Geometry>;
    if (this.olLayer.getSource().hasFeature(clickedFeature)) {
      // Pro Layer wird nur eine Geometrie angezeigt, wird diese also angeklickt, ist das definitiv ein deselect.
      this.deselected.emit();
    }
  }

  private layerFilter: (l: Layer<Source>) => boolean = l => {
    return l === this.olLayer;
  };

  private styleFn: StyleFunction = (feature: FeatureLike): Style[] => {
    const coords = (feature.getGeometry() as LineString)?.getCoordinates();
    return MapStyles.getDefaultHighlightStyle(MapStyles.FEATURE_SELECT_COLOR_TRANSPARENT, true, coords);
  };
}
