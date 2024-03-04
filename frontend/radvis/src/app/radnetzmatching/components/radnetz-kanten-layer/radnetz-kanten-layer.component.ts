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
import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import GeoJSON, { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import VectorLayer from 'ol/layer/Vector';
import { bbox } from 'ol/loadingstrategy';
import VectorSource from 'ol/source/Vector';
import { Subscription } from 'rxjs';
import { RadnetzKantenLayer } from 'src/app/radnetzmatching/components/radnetz-kanten-layer/radnetz-kanten-layer';
import { PrimarySelectionService } from 'src/app/radnetzmatching/services/primary-selection.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-radnetz-kanten-layer',
  templateUrl: './radnetz-kanten-layer.component.html',
  styleUrls: ['./radnetz-kanten-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RadnetzKantenLayerComponent implements OnDestroy, OnChanges {
  @Input()
  zugeordneteRadnetzKanteIds: number[] = [];

  private readonly layer = new RadnetzKantenLayer();
  private readonly vectorSource!: VectorSource;
  private readonly olLayer!: VectorLayer;
  private selectedKanteId: number | null = null;
  private subscription: Subscription;

  constructor(
    private olMapService: OlMapService,
    private http: HttpClient,
    private errorHandlingService: ErrorHandlingService,
    private primarySelectionService: PrimarySelectionService
  ) {
    this.vectorSource = this.createSource();
    this.olLayer = this.createLayer();
    this.olLayer.set(OlMapService.LAYER_ID, this.layer.id);
    this.olMapService.addLayer(this.olLayer);
    this.subscription = this.primarySelectionService.primarySelection$.subscribe(primarySelectedFeatureReference => {
      this.changeHighlightingByFeatureId(false, this.selectedKanteId);
      this.selectedKanteId = null;
      if (primarySelectedFeatureReference && primarySelectedFeatureReference.layerId === this.layer.id) {
        this.changeHighlightingByFeatureId(true, primarySelectedFeatureReference.featureId);
        this.selectedKanteId = primarySelectedFeatureReference.featureId;
      }
    });
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.subscription.unsubscribe();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.zugeordneteRadnetzKanteIds) {
      const previousZugeordneteFeatureIds = changes.zugeordneteRadnetzKanteIds.previousValue as number[];
      const currentZugeordneteFeatureIds = changes.zugeordneteRadnetzKanteIds.currentValue as number[];
      previousZugeordneteFeatureIds?.forEach(id => this.markAsZugeordnetById(false, id));
      currentZugeordneteFeatureIds?.forEach(id => this.markAsZugeordnetById(true, id));
    }
  }

  private changeHighlightingByFeatureId(highlight: boolean, id: number | null): void {
    if (id) {
      const feature = this.vectorSource.getFeatureById(id);
      feature?.set('highlighted', highlight);
      feature?.changed();
    }
  }

  private markAsZugeordnetById(zugeordnet: boolean, id: number | null): void {
    if (id) {
      const feature = this.vectorSource.getFeatureById(id);
      feature?.set('zugeordnet', zugeordnet);
      feature?.changed();
    }
  }

  private createSource(): VectorSource {
    return new VectorSource({
      format: new GeoJSON(),
      useSpatialIndex: false,
      loader: (extent): void => {
        const url = this.layer.url + '?view=' + extent;
        this.http.get<GeoJSONFeatureCollection>(url).subscribe(
          featureCollection => {
            this.vectorSource.clear(true);
            this.vectorSource.addFeatures(new GeoJSON().readFeatures(featureCollection));
            this.changeHighlightingByFeatureId(true, this.selectedKanteId);
            this.zugeordneteRadnetzKanteIds.forEach(id => this.markAsZugeordnetById(true, id));
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
      style: this.layer.style,
      minZoom: this.layer.minZoom,
      zIndex: 11,
    });
  }
}
