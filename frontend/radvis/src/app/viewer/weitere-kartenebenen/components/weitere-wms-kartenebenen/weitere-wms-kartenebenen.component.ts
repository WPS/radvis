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
import { ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Feature } from 'ol';
import GeoJSON from 'ol/format/GeoJSON';
import Geometry from 'ol/geom/Geometry';
import TileLayer from 'ol/layer/Tile';
import VectorLayer from 'ol/layer/Vector';
import * as olProj from 'ol/proj';
import { TileWMS } from 'ol/source';
import { TileSourceEvent } from 'ol/source/Tile';
import VectorSource from 'ol/source/Vector';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, filter } from 'rxjs/operators';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { WMSLegende } from 'src/app/shared/models/wms-legende';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import invariant from 'tiny-invariant';
import { ListenerFunction } from 'ol/events';

@Component({
  selector: 'rad-weitere-wms-kartenebenen',
  templateUrl: './weitere-wms-kartenebenen.component.html',
  styleUrls: ['./weitere-wms-kartenebenen.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class WeitereWmsKartenebenenComponent implements OnDestroy, OnInit {
  @Input()
  public url = '';
  @Input()
  public deckkraft!: number;
  @Input()
  public zoomstufe!: number;
  @Input()
  public zindex!: number;
  @Input()
  public name = '';
  @Input()
  public quelle!: string;
  @Input()
  public layerId!: number;

  layer: TileLayer;
  source: TileWMS;

  highlightLayer: VectorLayer;
  highlightLayerSource: VectorSource = new VectorSource();

  errorOccurred = new Subject<TileSourceEvent>();

  private subscriptions: Subscription[] = [];

  constructor(
    private olMapService: OlMapService,
    private notifyUserService: NotifyUserService,
    private httpClient: HttpClient,
    featureHighlightService: FeatureHighlightService
  ) {
    const featureFilter = (f: RadVisFeature): boolean =>
      f.layer === WeitereKartenebene.LAYER_NAME && f.attributes.get(WeitereKartenebene.LAYER_ID_KEY) === this.layerId;

    this.source = new TileWMS({
      url: this.url,
      params: { projection: olProj.get('EPSG:25832')!.getCode() },
      transition: 0,
    });
    this.layer = new TileLayer({
      source: this.source,
    });

    this.highlightLayer = new VectorLayer({
      style: MapStyles.getDefaultHighlightStyle(MapStyles.FEATURE_HOVER_COLOR),
      source: this.highlightLayerSource,
      opacity: 1,
    });

    this.errorOccurred
      // debounceTime, weil viele Images gleichzeitig geholt werden und sich sonst die Fehlermeldungen stapeln
      .pipe(debounceTime(2000))
      .subscribe(event => {
        if (event) {
          this.notifyUserService.warn(
            `Beim Laden von weiteren WMS-Kartenebenen "${this.name}" ist ein Fehler aufgetreten. Bitte prüfen Sie die angegebene URL.`
          );
        }
      });

    this.source.addEventListener('tileloaderror', this.onTileLoadError);

    this.subscriptions.push(
      featureHighlightService.highlightedFeature$.pipe(filter(featureFilter)).subscribe(f => {
        this.highlightFeature(f);
      }),
      featureHighlightService.unhighlightedFeature$.pipe(filter(featureFilter)).subscribe(f => {
        this.unhighlightFeature(f);
      })
    );
  }

  // Arg, die OL-Typisierung lässt uns wieder mal im Stich, deshalb unknown statt BaseEvent :(
  private onTileLoadError: ListenerFunction = (event: unknown): void => {
    this.errorOccurred.next(event as TileSourceEvent);
  };

  ngOnInit(): void {
    invariant(this.deckkraft != null, 'Deckkraft muss gesetzt sein');
    invariant(this.deckkraft >= 0, 'Deckkraft muss positiv oder Null sein');
    invariant(this.quelle);

    invariant(this.zindex != null, 'zIndex muss gesetzt sein');
    invariant(this.zindex >= 0, 'zIndex muss >= 0 sein');

    invariant(this.zoomstufe != null, 'zoomstufe muss gesetzt sein');
    invariant(this.zoomstufe >= 0, 'zoomstufe muss >= 0 sein');

    this.layer.setZIndex(this.zindex);
    this.layer.setOpacity(this.deckkraft);
    this.layer.setMinZoom(this.zoomstufe);
    this.highlightLayer.setZIndex(this.zindex + 1);

    const legende: WMSLegende = new WMSLegende(this.name, this.url);

    this.layer.set(OlMapService.LAYER_ID, WeitereKartenebene.LAYER_NAME);

    this.olMapService.addWMSFeatureLayer(
      this.layer,
      this.getFeaturesCallback,
      {
        layerName: this.name,
        quellangabe: this.quelle,
      },
      legende
    );

    this.olMapService.addLayer(this.highlightLayer);
    this.source.setUrl(this.url);
  }

  ngOnDestroy(): void {
    if (this.layer) {
      this.olMapService.removeLayer(this.layer);
    }
    this.subscriptions.forEach(it => it.unsubscribe());
    this.source.removeEventListener('tileloaderror', this.onTileLoadError);
  }

  private highlightFeature(f: RadVisFeature): void {
    const externeWmsId = this.getExterneWmsId(f);
    if (externeWmsId) {
      const feature = new Feature<Geometry>(f.geometry);
      feature.setId(externeWmsId);
      this.highlightLayerSource.addFeature(feature);
      this.highlightLayerSource.changed();
    }
  }

  private unhighlightFeature(f: RadVisFeature): void {
    const externeWmsId = this.getExterneWmsId(f);
    if (externeWmsId) {
      const feature = this.highlightLayerSource.getFeatureById(externeWmsId);
      if (feature) {
        this.highlightLayerSource.removeFeature(feature);
        this.highlightLayer?.changed();
      }
    }
  }

  private getExterneWmsId(f: RadVisFeature): string {
    return f.attributes.get(WeitereKartenebene.EXTERNE_WMS_FEATURE_ID_PROPERTY_NAME);
  }

  private getFeaturesCallback = (coordinate: number[], resolution: number): Promise<Feature<Geometry>[]> => {
    const featureInfoUrl = this.source.getFeatureInfoUrl(coordinate, resolution, 'EPSG:25832', {
      // Explizit GeoJSON anfragen, um Fehler in encoding oder Feature-Typen zu vermeiden, da GeoJSON am flexibelsten ist.
      INFO_FORMAT: 'application/json',
      FEATURE_COUNT: 3,
    });
    if (featureInfoUrl) {
      const url = new URL(featureInfoUrl);
      url.searchParams.set('QUERY_LAYERS', url.searchParams.get('LAYERS') || '');

      return this.httpClient
        .get(url.toString(), { responseType: 'text' })
        .toPromise()
        .then(
          responseText => {
            const allFeatures = new GeoJSON().readFeatures(responseText);
            return allFeatures.map(feature => {
              feature.set(WeitereKartenebene.EXTERNE_WMS_FEATURE_ID_PROPERTY_NAME, feature.getId());
              feature.set(WeitereKartenebene.LAYER_ID_KEY, this.layerId);
              return feature;
            });
          },
          () => {
            this.notifyUserService.inform(
              `Der WMS-Dienst der weiteren Kartenebene "${this.name}" unterstütz den Abruf von Detailinformationen nicht.`
            );
            return Promise.resolve([]);
          }
        );
    }
    return Promise.resolve([]);
  };
}
