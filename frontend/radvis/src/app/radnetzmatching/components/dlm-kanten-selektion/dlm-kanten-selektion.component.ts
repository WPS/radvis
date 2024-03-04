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
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
} from '@angular/core';
import { Feature, MapBrowserEvent } from 'ol';
import { GeoJSON } from 'ol/format';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Geometry } from 'ol/geom';
import GeometryType from 'ol/geom/GeometryType';
import { Layer } from 'ol/layer';
import VectorLayer from 'ol/layer/Vector';
import { bbox } from 'ol/loadingstrategy';
import { Source } from 'ol/source';
import VectorSource from 'ol/source/Vector';
import { Subscription } from 'rxjs';
import { DlmKantenLayer } from 'src/app/radnetzmatching/components/dlm-kanten-selektion/dlm-kanten-layer';
import { PrimarySelectionService } from 'src/app/radnetzmatching/services/primary-selection.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';

@Component({
  selector: 'rad-dlm-kanten-selektion',
  templateUrl: './dlm-kanten-selektion.component.html',
  styleUrls: ['./dlm-kanten-selektion.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DlmKantenSelektionComponent implements OnDestroy, OnChanges {
  @Input()
  selectionActive = false;

  @Input()
  zugeordneteDlmKanteIds: number[] = [];

  @Output()
  dlmKantenSelect = new EventEmitter<number[]>();

  private readonly layer = new DlmKantenLayer();
  private readonly vectorSource!: VectorSource;
  private readonly olLayer!: VectorLayer;
  private subscriptions: Subscription[] = [];
  private selectedKanteIds: number[] = [];

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
    this.subscriptions.push(this.olMapService.click$().subscribe(clickEvent => this.onMapClick(clickEvent)));
    this.subscriptions.push(
      this.primarySelectionService.primarySelection$.subscribe(primarySelectedFeatureReference => {
        this.selectedKanteIds.forEach(id => this.changeHighlightingByFeatureId(false, id));
        this.selectedKanteIds = [];
        if (primarySelectedFeatureReference && primarySelectedFeatureReference.layerId === this.layer.id) {
          this.changeHighlightingByFeatureId(true, primarySelectedFeatureReference.featureId);
          this.selectedKanteIds = [primarySelectedFeatureReference.featureId];
        }
      })
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.zugeordneteDlmKanteIds) {
      const previousZugeordneteFeatureIds = changes.zugeordneteDlmKanteIds.previousValue as number[];
      const currentZugeordneteFeatureIds = changes.zugeordneteDlmKanteIds.currentValue as number[];
      previousZugeordneteFeatureIds?.forEach(id => this.markAsZugeordnetById(false, id));
      currentZugeordneteFeatureIds?.forEach(id => this.markAsZugeordnetById(true, id));
    }
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  private onMapClick(clickEvent: MapBrowserEvent<UIEvent>): void {
    const featuresAtPixel = this.olMapService.getFeaturesAtPixel(
      clickEvent.pixel,
      (layer: Layer<Source>) => this.selectionActive && layer === this.olLayer
    );
    const linestringFeaturesAtPixel = featuresAtPixel?.filter(
      feat => feat.getGeometry()?.getType() === GeometryType.LINE_STRING
    );
    if (!linestringFeaturesAtPixel || linestringFeaturesAtPixel.length === 0) {
      return;
    }
    // Das erste Feature im Array ist das am nähesten zur Click-Position liegende
    const clickedFeature = linestringFeaturesAtPixel[0] as Feature<Geometry>;
    const pointerEvent = clickEvent.originalEvent as PointerEvent;
    const toggle = pointerEvent.ctrlKey || pointerEvent.metaKey;
    const selectedKanteId = Number(clickedFeature.getId());
    if (!toggle) {
      // nothing changed
      if (this.selectedKanteIds.length === 1 && this.selectedKanteIds[0] === selectedKanteId) {
        return;
      }
      // select exclusive
      this.clearSelection();
      clickedFeature.set('highlighted', true);
      this.selectedKanteIds = [selectedKanteId];
    } else {
      if (this.selectedKanteIds.includes(selectedKanteId)) {
        // deselect
        clickedFeature.set('highlighted', false);
        const index = this.selectedKanteIds.findIndex(id => id === selectedKanteId);
        this.selectedKanteIds.splice(index, 1);
        clickedFeature.changed();
      } else {
        // select additiv
        clickedFeature.set('highlighted', true);
        this.selectedKanteIds.push(selectedKanteId);
      }
    }
    this.selectedKanteIds.forEach(id => {
      this.vectorSource.getFeatureById(id)?.changed();
    });
    this.dlmKantenSelect.emit(this.selectedKanteIds);
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

  private clearSelection(): void {
    this.selectedKanteIds.forEach(id => {
      const feature = this.vectorSource.getFeatureById(id);
      feature?.set('highlighted', false);
    });
    this.selectedKanteIds = [];
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
            this.selectedKanteIds.forEach(id => this.changeHighlightingByFeatureId(true, id));
            this.zugeordneteDlmKanteIds.forEach(id => this.markAsZugeordnetById(true, id));
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
      zIndex: 10,
    });
  }
}
