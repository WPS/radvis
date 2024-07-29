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

import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import Feature from 'ol/Feature';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { isPoint, PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { RADVIS_NETZ_LAYER_PREFIX } from 'src/app/viewer/viewer-shared/models/radvis-netz-layer-prefix';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-knoten-highlight-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KnotenHighlightLayerComponent implements OnChanges, OnDestroy {
  public static LAYER_ID = `${RADVIS_NETZ_LAYER_PREFIX}_KnotenDetailHighlightLayer`;

  @Input()
  geometrie!: PointGeojson;
  @Input()
  id!: number;

  vectorSource = new VectorSource();
  olLayer: VectorLayer;

  constructor(
    private olMapService: OlMapService,
    private netzAusblendenService: NetzAusblendenService
  ) {
    this.olLayer = new VectorLayer({
      source: this.vectorSource,
      style: MapStyles.getDefaultHighlightStyle(MapStyles.FEATURE_SELECT_COLOR),
      zIndex: 333,
    });
    this.olLayer.set(OlMapService.LAYER_ID, KnotenHighlightLayerComponent.LAYER_ID);
    this.olMapService.addLayer(this.olLayer);
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(this.geometrie);
    invariant(isPoint(this.geometrie), 'Knoten Geometrie muss ein Punkt sein');
    invariant(this.id);

    if (changes.id) {
      if (!changes.id.firstChange) {
        this.netzAusblendenService.knotenEinblenden(changes.id.previousValue);
      }
      this.netzAusblendenService.knotenAusblenden(changes.id.currentValue);
    }
    if (changes.geometrie) {
      this.vectorSource.clear();
      const feature = new Feature(new Point(this.geometrie.coordinates));
      feature.setId(this.id);
      this.vectorSource.addFeature(feature);
      this.vectorSource.changed();
    }
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.netzAusblendenService.knotenEinblenden(this.id);
  }
}
