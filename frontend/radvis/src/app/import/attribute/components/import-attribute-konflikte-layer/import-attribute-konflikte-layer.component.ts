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

import { ChangeDetectionStrategy, Component, EventEmitter, OnDestroy, Output } from '@angular/core';
import { MapBrowserEvent } from 'ol';
import Feature, { FeatureLike } from 'ol/Feature';
import { GeoJSON } from 'ol/format';
import { Geometry, LineString, Point } from 'ol/geom';
import { Layer } from 'ol/layer';
import VectorLayer from 'ol/layer/Vector';
import { Source } from 'ol/source';
import VectorSource from 'ol/source/Vector';
import { Icon, Style } from 'ol/style';
import { Subscription } from 'rxjs';
import { Property } from 'src/app/import/attribute/models/property';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-import-attribute-konflikte-layer',
  template: '',
  styles: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportAttributeKonflikteLayerComponent implements OnDestroy {
  private static readonly Z_INDEX_KONFLIKT_LAYER = 903;

  @Output()
  public selectKonflikt = new EventEmitter<Property[][]>();

  private konflikteVectorSource: VectorSource = new VectorSource();
  private readonly konflikteLayer: VectorLayer;

  private subscriptions: Subscription[] = [];

  constructor(
    private olMapService: OlMapService,
    private attributeImportService: AttributeImportService
  ) {
    // holt sich die Konflikte selber
    this.konflikteLayer = new VectorLayer({
      source: this.konflikteVectorSource,
      style: this.styleFunction,
    });

    this.attributeImportService.getKonfliktprotokolle().then(geojson => {
      this.konflikteVectorSource.addFeatures(new GeoJSON().readFeatures(geojson));
    });

    this.konflikteLayer.setZIndex(ImportAttributeKonflikteLayerComponent.Z_INDEX_KONFLIKT_LAYER);

    this.olMapService.addLayer(this.konflikteLayer);

    this.subscriptions.push(this.olMapService.click$().subscribe(clickEvent => this.onMapClick(clickEvent)));
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.konflikteLayer);
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private onMapClick(clickEvent: MapBrowserEvent<PointerEvent | KeyboardEvent | WheelEvent>): void {
    const featuresAtPixel = this.olMapService.getFeaturesAtPixel(
      clickEvent.pixel,
      (layer: Layer<Source>) => layer === this.konflikteLayer
    );
    if (!featuresAtPixel || featuresAtPixel.length === 0) {
      return;
    }
    // Das erste Feature im Array ist das am nähesten zur Click-Position liegende
    const clickedFeature = featuresAtPixel[0] as Feature<Geometry>;

    const konflikte: Property[][] = [];
    for (const konflikt of clickedFeature.getProperties().konflikte) {
      const entries = [];
      for (const key in konflikt) {
        entries.push({ key, value: konflikt[key] });
      }
      konflikte.push(entries);
    }

    this.selectKonflikt.emit(konflikte);
  }

  private styleFunction = (feature: FeatureLike): Style | Style[] => {
    return [
      new Style({
        geometry: new Point((feature.getGeometry() as LineString).getCoordinateAt(0.5)),
        image: new Icon({
          anchor: [0.5, 0.5],
          src: './assets/konflikt-background.svg',
          color: '#ce0071',
        }),
      }),
      new Style({
        geometry: new Point((feature.getGeometry() as LineString).getCoordinateAt(0.5)),
        image: new Icon({
          anchor: [0.5, 0.5],
          src: './assets/konflikt.svg',
        }),
      }),
    ];
  };
}
