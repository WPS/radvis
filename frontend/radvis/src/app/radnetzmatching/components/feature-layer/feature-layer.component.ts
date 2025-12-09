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

import { HttpClient, HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import GeoJSON, { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import VectorLayer from 'ol/layer/Vector';
import { bbox } from 'ol/loadingstrategy';
import VectorSource from 'ol/source/Vector';
import { Subscription } from 'rxjs';
import { PrimarySelectionService } from 'src/app/radnetzmatching/services/primary-selection.service';
import { RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-feature-layer',
  templateUrl: './feature-layer.component.html',
  styleUrls: ['./feature-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class FeatureLayerComponent implements OnInit, OnDestroy, OnChanges {
  @Input()
  layer!: RadVisLayer;

  @Input()
  netzklassen: Netzklassefilter[] | null = [];

  @Input()
  featureWasRemoved: number | null = null;

  @Input()
  index = 0;

  olLayer!: VectorLayer;

  private subscriptions: Subscription[] = [];
  private selectedKanteId: number | null = null;

  constructor(
    private http: HttpClient,
    private olMapService: OlMapService,
    private errorHandlingService: ErrorHandlingService,
    private primarySelectionService: PrimarySelectionService
  ) {}

  ngOnInit(): void {
    invariant(this.layer);
    invariant(this.layer.typ === RadVisLayerTyp.GEO_JSON);
    this.createLayer();
    this.subscriptions.push(
      this.primarySelectionService.primarySelection$.subscribe(primarySelectedFeatureReference => {
        this.changeHighlightingByFeatureId(false, this.selectedKanteId);
        this.selectedKanteId = null;
        if (primarySelectedFeatureReference && primarySelectedFeatureReference.layerId === this.layer.id) {
          this.changeHighlightingByFeatureId(true, primarySelectedFeatureReference.featureId);
          this.selectedKanteId = primarySelectedFeatureReference.featureId;
        }
      })
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.layer && !changes.layer.firstChange) {
      throw new Error('layer Input for FeatureLayerComponent must not change');
    }
    if (changes.index && !changes.index.firstChange) {
      this.olLayer?.setZIndex(this.index + 1);
    }
    if (changes.netzklassen && !changes.netzklassen.firstChange) {
      this.olLayer?.getSource()?.refresh();
    }
    if (changes.featureWasRemoved && !changes.featureWasRemoved.firstChange) {
      if (this.featureWasRemoved) {
        const featureToRemove = this.olLayer?.getSource()?.getFeatureById(this.featureWasRemoved);
        if (featureToRemove) {
          this.olLayer?.getSource()?.removeFeature(featureToRemove);
        }
      }
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.olMapService.removeLayer(this.olLayer);
  }

  private createLayer(): void {
    const vectorSource = new VectorSource({
      format: new GeoJSON(),
      useSpatialIndex: false,
      loader: (extent): void => {
        const url = this.layer.url + '?view=' + extent;
        this.http
          .get<GeoJSONFeatureCollection>(url, {
            params: new HttpParams().set('netzklasseFilter', this.netzklassen?.map(x => x.toString()).join() || ''),
          })
          .subscribe(
            featureCollection => {
              vectorSource.clear(true);
              vectorSource.addFeatures(new GeoJSON().readFeatures(featureCollection));
              this.changeHighlightingByFeatureId(true, this.selectedKanteId);
            },
            error => this.errorHandlingService.handleError(error)
          );
      },
      strategy: bbox,
    });

    this.olLayer = new VectorLayer({
      source: vectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: this.layer.style,
      minZoom: this.layer.minZoom,
      // Feature-Layer z.B. Netzfehler liegen über Netzklassen-Layer
      zIndex: this.index,
    });
    this.olLayer.set(OlMapService.LAYER_ID, this.layer.id);
    this.olMapService.addLayer(this.olLayer);
  }

  private changeHighlightingByFeatureId(highlight: boolean, id: number | null): void {
    if (id) {
      const feature = this.olLayer?.getSource()?.getFeatureById(id);
      feature?.set('highlighted', highlight);
      feature?.changed();
    }
  }
}
