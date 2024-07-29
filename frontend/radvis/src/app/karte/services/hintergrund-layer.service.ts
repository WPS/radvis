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
import { Injectable } from '@angular/core';
import TileLayer from 'ol/layer/Tile';
import * as olProj from 'ol/proj';
import { TileWMS, XYZ } from 'ol/source';
import TileSource from 'ol/source/Tile';
import TileGrid from 'ol/tilegrid/TileGrid';
import { NEVER, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { HintergrundLayerConfiguration } from 'src/app/karte/models/hintergrund-layer-configuration';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { LayerAuswahl } from 'src/app/shared/models/layer-auswahl';
import { LayerQuelle } from 'src/app/shared/models/layer-quelle';
import { LayerTypes } from 'src/app/shared/models/layers/layer-types';
import { LayerId, RadVisLayer } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisLayerTyp } from 'src/app/shared/models/layers/rad-vis-layer-typ';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class HintergrundLayerService {
  private static readonly UTM32_STANDARD_RESOLUTIONS = [
    4891.969810251279, 2445.9849051256397, 1222.9924525628198, 611.4962262814099, 305.74811314070496,
    152.87405657035296, 76.43702828517628, 38.21851414258809, 19.109257071294095, 9.55462853564703, 4.777314267823519,
    2.3886571339117597, 1.1943285669558799, 0.5971642834779389,
  ];

  public allLayerAuswahl: LayerAuswahl[] = [];

  currentAuswahl$: Observable<LayerId> = NEVER;

  private allHintergrundLayers: RadVisLayer[] = [];

  constructor(
    private mapQueryParamService: MapQueryParamsService,
    private http: HttpClient
  ) {}

  public initLayers(): Promise<void> {
    return this.http
      .get<HintergrundLayerConfiguration[]>('/api/hintergrundkarte/layers')
      .toPromise()
      .then(layers => {
        this.allHintergrundLayers = layers
          .map(l => {
            const center: [number, number] | undefined = l.centerX ? [l.centerX, l.centerY!] : undefined;
            return new RadVisLayer(
              l.id,
              l.name,
              l.url,
              l.typ,
              undefined,
              undefined,
              LayerTypes.HINTERGRUND,
              undefined,
              l.maxZoom ?? undefined,
              center,
              l.quelle
            );
          })
          .sort((l1, l2) => l1.bezeichnung.localeCompare(l2.bezeichnung));
        this.allLayerAuswahl = this.allHintergrundLayers.map(l => {
          return {
            id: l.id,
            bezeichnung: l.bezeichnung,
          };
        });
        this.currentAuswahl$ = this.mapQueryParamService.hintergrund$.pipe(
          map(hintergrundLayer => {
            const defaultLayer = layers.find(l => l.defaultKarte)?.id ?? this.allHintergrundLayers[0].id;
            // Wir schauen erst, ob es den Layer wirklich gibt, falls die LayerConfig im Backend geändert wurde und der Link älter ist
            return this.allHintergrundLayers.find(l => l.id === hintergrundLayer)?.id ?? defaultLayer;
          })
        );
      });
  }

  public getQuelle(id: LayerId): LayerQuelle {
    const layer = this.allHintergrundLayers.find(l => l.id === id);
    invariant(layer);
    invariant(layer.quelle);

    return {
      layerName: `Hintergrundkarte (${layer.bezeichnung})`,
      quellangabe: layer.quelle,
    };
  }

  public getLayer(id: LayerId): TileLayer {
    let source: TileSource;
    const layer = this.allHintergrundLayers.find(l => l.id === id);
    invariant(layer);

    if (layer.typ === RadVisLayerTyp.TILE) {
      source = new XYZ({
        url: layer.url,
        maxZoom: layer.maxZoom,
      });
    } else if (layer.typ === RadVisLayerTyp.WMTS_UTM32_TILE) {
      source = new XYZ({
        url: layer.url,
        maxZoom: layer.maxZoom,
        tileGrid: new TileGrid({
          resolutions: HintergrundLayerService.UTM32_STANDARD_RESOLUTIONS,
          origin: layer.origin,
        }),
        projection: olProj.get('EPSG:25832'),
      });
    } else {
      source = new TileWMS({
        url: layer.url,
        params: { FORMAT: 'image/jpeg' },
      });
    }

    const result = new TileLayer({
      source,
      zIndex: 0,
    });
    result.set(OlMapService.LAYER_ID, layer.id);

    return result;
  }

  public setAuswahl(id: LayerId): void {
    this.mapQueryParamService.update({ hintergrund: id });
  }
}
