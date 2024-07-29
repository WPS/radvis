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

import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { Color } from 'ol/color';
import Feature from 'ol/Feature';
import { Geometry, LineString, MultiLineString } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { FahrradrouteNetzbezug } from 'src/app/viewer/fahrradroute/models/fahrradroute.netzbezug';
import { KantenNetzbezug } from 'src/app/shared/models/kanten-netzbezug';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import invariant from 'tiny-invariant';
import { infrastrukturHighlightLayerZIndex } from 'src/app/shared/models/shared-layer-zindex-config';

@Component({
  selector: 'rad-fahrradroute-netzbezug-highlight-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FahrradrouteNetzbezugHighlightLayerComponent implements OnInit, OnChanges, OnDestroy {
  @Input()
  kantenBezug?: KantenNetzbezug[];

  @Input()
  fahrradRouteNetzbezug?: FahrradrouteNetzbezug;

  @Input()
  color: Color = MapStyles.FEATURE_SELECT_COLOR;

  private olLayer!: VectorLayer;

  private vectorSource: VectorSource = new VectorSource();

  constructor(
    private olMapService: OlMapService,
    private netzAusblendenService: NetzAusblendenService
  ) {}

  ngOnInit(): void {
    this.olLayer = this.createLayer();
    this.olMapService.addLayer(this.olLayer);
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(this.color);
    if (changes.color) {
      this.vectorSource.changed();
    }

    if (changes.fahrradRouteNetzbezug || changes.kantenBezug) {
      // bevorzugt FahrradrouteNetzbezug
      if (this.fahrradRouteNetzbezug) {
        const feature = new Feature(new LineString(this.fahrradRouteNetzbezug.geometrie.coordinates));
        this.updateLayer(feature);
      } else {
        invariant(this.kantenBezug, 'Entweder Kantenbezug oder FahrradrouteNetzbezug muss gesetzt sein');
        const feature = new Feature(
          new MultiLineString(this.kantenBezug.map(k => new LineString(k.geometrie.coordinates)))
        );
        this.updateLayer(feature);
      }
    }
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
  }

  private createLayer(): VectorLayer {
    const layer = new VectorLayer({
      source: this.vectorSource,
      zIndex: infrastrukturHighlightLayerZIndex,
    });

    layer.setRenderOrder(null);
    layer.setStyle((): Style[] => MapStyles.getDefaultHighlightStyle(this.color));

    return layer;
  }

  private updateLayer(feature: Feature<Geometry>): void {
    this.vectorSource.clear();
    this.vectorSource.addFeature(feature);
    this.vectorSource.changed();
  }

  private kantenEinblenden(kanten: number[]): void {
    kanten.forEach(id => this.netzAusblendenService.kanteEinblenden(id));
  }

  private kantenAusblenden(kanten: number[]): void {
    kanten.forEach(id => this.netzAusblendenService.kanteAusblenden(id));
  }
}
