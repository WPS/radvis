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
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import GeoJSON from 'ol/format/GeoJSON';
import VectorLayer from 'ol/layer/Vector';
import { bbox } from 'ol/loadingstrategy';
import VectorSource from 'ol/source/Vector';
import { LandkreiseLayer } from 'src/app/radnetzmatching/components/landkreise-layer/landkreise-layer';
import { RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-landkreise-layer',
  templateUrl: './landkreise-layer.component.html',
  styleUrls: ['./landkreise-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LandkreiseLayerComponent implements OnInit, OnDestroy {
  layer: RadVisLayer = new LandkreiseLayer();

  olLayer!: VectorLayer;

  constructor(
    private http: HttpClient,
    private olMapService: OlMapService,
    private errorHandlingService: ErrorHandlingService,
    private organisationenService: OrganisationenService
  ) {
    this.createLayer();
  }

  ngOnInit(): void {
    invariant(this.layer);
    invariant(this.layer.typ === RadVisLayerTyp.GEO_JSON);
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
  }

  private createLayer(): void {
    const vectorSource = new VectorSource({
      format: new GeoJSON(),
      useSpatialIndex: false,
      loader: (): void => {
        this.organisationenService.getKreiseAlsFeatures().then(
          kreiseAlsFeatures => {
            vectorSource.clear(true);
            vectorSource.addFeatures(new GeoJSON().readFeatures(kreiseAlsFeatures));
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
      zIndex: 700,
    });
    this.olLayer.set(OlMapService.LAYER_ID, this.layer.id);
    this.olMapService.addLayer(this.olLayer);
  }
}
