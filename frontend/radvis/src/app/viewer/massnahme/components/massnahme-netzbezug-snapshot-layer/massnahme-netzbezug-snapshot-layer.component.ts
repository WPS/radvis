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
import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy } from '@angular/core';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { geojsonGeometryToFeature, GeometryCollectionGeojson } from 'src/app/shared/models/geojson-geometrie';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { kanteHighlightLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import invariant from 'tiny-invariant';
import Feature from 'ol/Feature';
import { Geometry } from 'ol/geom';

@Component({
  selector: 'rad-massnahme-netzbezug-snapshot-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class MassnahmeNetzbezugSnapshotLayerComponent implements OnDestroy, OnChanges {
  @Input()
  geometrie!: GeometryCollectionGeojson;

  private source = new VectorSource<Feature<Geometry>>();
  private layer = new VectorLayer<VectorSource<Feature<Geometry>>>({
    source: this.source,
    zIndex: kanteHighlightLayerZIndex,
    style: MapStyles.getDefaultHighlightStyle(MapStyles.FEATURE_SELECT_COLOR),
  });

  constructor(private mapService: OlMapService) {
    this.mapService.addLayer(this.layer);
  }

  ngOnChanges(): void {
    invariant(this.geometrie);

    this.source.clear(true);
    this.geometrie.geometries.forEach(g => {
      this.source.addFeature(geojsonGeometryToFeature(g)!);
    });
    this.source.changed();
  }

  ngOnDestroy(): void {
    this.mapService.removeLayer(this.layer);
  }
}
