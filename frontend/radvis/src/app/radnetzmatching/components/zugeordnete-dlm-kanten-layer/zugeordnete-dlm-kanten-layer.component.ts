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

import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import GeoJSON, { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import VectorLayer from 'ol/layer/Vector';
import { bbox } from 'ol/loadingstrategy';
import VectorSource from 'ol/source/Vector';
import { ZugeordneteDlmKantenLayer } from 'src/app/radnetzmatching/components/zugeordnete-dlm-kanten-layer/zugeordnete-dlm-kanten-layer';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-zugeordnete-dlm-kanten-layer',
  templateUrl: './zugeordnete-dlm-kanten-layer.component.html',
  styleUrls: ['./zugeordnete-dlm-kanten-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ZugeordneteDlmKantenLayerComponent implements OnDestroy {
  private readonly vectorSource!: VectorSource;
  private readonly olLayer!: VectorLayer;
  private readonly zugeordneteDlmKantenLayer = new ZugeordneteDlmKantenLayer();

  constructor(
    private olMapService: OlMapService,
    private http: HttpClient,
    private errorHandlingService: ErrorHandlingService
  ) {
    this.vectorSource = this.createSource();
    this.olLayer = this.createLayer();
    this.olMapService.addLayer(this.olLayer);
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
  }

  private createSource(): VectorSource {
    return new VectorSource({
      format: new GeoJSON(),
      useSpatialIndex: false,
      loader: (extent): void => {
        const url = this.zugeordneteDlmKantenLayer.url + '?view=' + extent;
        this.http.get<GeoJSONFeatureCollection>(url).subscribe(
          featureCollection => {
            this.vectorSource.clear(true);
            const features = new GeoJSON().readFeatures(featureCollection);
            this.vectorSource.addFeatures(features);
          },
          error => this.errorHandlingService.handleError(error)
        );
      },
      strategy: bbox,
    });
  }

  private createLayer(): VectorLayer {
    return new VectorLayer({
      source: this.vectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: this.zugeordneteDlmKantenLayer.style,
      minZoom: this.zugeordneteDlmKantenLayer.minZoom,
      zIndex: 5,
    });
  }
}
