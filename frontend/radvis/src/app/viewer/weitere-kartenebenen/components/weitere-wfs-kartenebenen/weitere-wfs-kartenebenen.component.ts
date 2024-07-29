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
import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { Feature } from 'ol';
import { fromString } from 'ol/color';
import { Extent } from 'ol/extent';
import { FeatureLike } from 'ol/Feature';
import GeoJSON from 'ol/format/GeoJSON';
import { Geometry } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { bbox } from 'ol/loadingstrategy';
import Projection from 'ol/proj/Projection';
import { default as VectorSource } from 'ol/source/Vector';
import Style, { StyleFunction } from 'ol/style/Style';
import { Subject, Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { weitereKartenebenenHighlightZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';

import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-weitere-wfs-kartenebenen',
  templateUrl: './weitere-wfs-kartenebenen.component.html',
  styleUrls: ['./weitere-wfs-kartenebenen.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeitereWfsKartenebenenComponent implements OnInit, OnDestroy, OnChanges {
  public static HIGHLIGHTED_PROPERTY_NAME = 'highlighted';
  private static readonly EPSG_25832_CODE = 'EPSG:25832';

  @Input()
  public url!: string;
  @Input()
  public color: string | undefined = undefined;
  @Input()
  public deckkraft!: number;
  @Input()
  public minZoom!: number;
  @Input()
  public zindex!: number;
  @Input()
  public name = '';
  @Input()
  public layerId!: number;
  @Input()
  public quelle!: string;

  private source: VectorSource = new VectorSource();
  private layer: VectorLayer | undefined = undefined;

  private errorOccurred = new Subject<boolean>();

  private subscriptions: Subscription[] = [];

  private readonly FEATURE_ID_PROPERTY_NAME = 'feature_id';

  constructor(
    private olMapService: OlMapService,
    private notifyUserService: NotifyUserService,
    private httpClient: HttpClient,
    featureHighlightService: FeatureHighlightService
  ) {
    this.subscriptions.push(
      featureHighlightService.highlightedFeature$
        .pipe(
          filter(
            f =>
              f.layer === WeitereKartenebene.LAYER_NAME &&
              f.attributes.get(WeitereKartenebene.LAYER_ID_KEY) === this.layerId
          )
        )
        .subscribe(f => {
          if (f.id) {
            const selectedFeature = this.source.getFeatureById(f.id.toString());
            selectedFeature.set(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME, true);
            selectedFeature.changed();
          }
        }),
      featureHighlightService.unhighlightedFeature$
        .pipe(
          filter(
            f =>
              f.layer === WeitereKartenebene.LAYER_NAME &&
              f.attributes.get(WeitereKartenebene.LAYER_ID_KEY) === this.layerId
          )
        )
        .subscribe(f => {
          if (f.id) {
            const selectedFeature = this.source.getFeatureById(f.id.toString());
            selectedFeature.set(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME, false);
            selectedFeature.changed();
          }
        })
    );
  }

  ngOnInit(): void {
    invariant(this.deckkraft != null, 'Deckkraft muss gesetzt sein');
    invariant(this.deckkraft >= 0, 'Deckkraft muss positiv oder Null sein');

    invariant(this.zindex != null, 'zIndex muss gesetzt sein');
    invariant(this.zindex >= 0, 'zIndex muss >= 0 sein');

    invariant(this.minZoom != null, 'minZoom muss gesetzt sein');
    invariant(this.minZoom >= 0, 'minZoom muss >= 0 sein');

    invariant(this.layerId);
    invariant(this.url);
    invariant(this.quelle);

    this.source = new VectorSource({
      format: new GeoJSON(),
      // eslint-disable-next-line no-unused-vars
      loader: (extent: Extent, _resolution: number, _projection: Projection): void => this.load(extent),
      strategy: bbox,
    });

    this.errorOccurred.subscribe(event => {
      if (event) {
        this.notifyUserService.warn(
          `Beim laden des externen WFS-Layers "${this.name}" ist ein Fehler aufgetreten. Bitte prüfen Sie die angegebene URL.`
        );
      }
    });
    this.layer = new VectorLayer({
      source: this.source,
      style: this.styleFn,
      zIndex: this.zindex,
      opacity: this.deckkraft,
      minZoom: this.minZoom,
    });
    this.layer.set(OlMapService.LAYER_ID, WeitereKartenebene.LAYER_NAME);

    this.olMapService.addLayer(this.layer, { layerName: this.name, quellangabe: this.quelle });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.url && !changes.url.firstChange) {
      throw new Error('Parameter für Externer Layer dürfen sich nicht ändern!');
    }
  }

  ngOnDestroy(): void {
    if (this.layer) {
      this.olMapService.removeLayer(this.layer);
    }
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  private buildURL(extent: any): string {
    const url = new URL(this.url);

    url.searchParams.set('request', 'GetFeature');
    url.searchParams.set('version', '1.1.0');
    url.searchParams.set('srsname', WeitereWfsKartenebenenComponent.EPSG_25832_CODE);
    url.searchParams.set('outputFormat', 'application/json');
    url.searchParams.set('bbox', [...extent, WeitereWfsKartenebenenComponent.EPSG_25832_CODE].join(','));

    return url.toString();
  }

  private load(extent: Extent): void {
    try {
      this.httpClient
        .get(this.buildURL(extent))
        .toPromise()
        .then(data => {
          // eslint-disable-next-line
          try {
            const features = this.source?.getFormat()?.readFeatures(data) as Feature<Geometry>[];
            if (features) {
              features.forEach((f, index) => {
                f.set(this.FEATURE_ID_PROPERTY_NAME, f.getId());
                f.set(WeitereKartenebene.LAYER_ID_KEY, this.layerId);
                f.setId(index + 1);
                f.set(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME, false);
              });
              this.source.clear(true);
              this.source.addFeatures(features);
            }
          } catch (error) {
            this.source.removeLoadedExtent(extent);
            this.errorOccurred.next(true);
          }
        })
        .catch((): void => {
          this.source.removeLoadedExtent(extent);
          this.errorOccurred.next(true);
        });
    } catch (exception) {
      // can for example be triggered if url is invalid
      this.source.removeLoadedExtent(extent);
      this.errorOccurred.next(true);
      throw exception;
    }
  }

  // eslint-disable-next-line no-unused-vars
  private styleFn: StyleFunction = (f: FeatureLike, r: number): Style[] | Style => {
    if (f.get(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME)) {
      const style = MapStyles.getDefaultHighlightStyle(MapStyles.FEATURE_HOVER_COLOR);
      style[0].setZIndex(weitereKartenebenenHighlightZIndex);
      return style;
    }

    if (this.color) {
      return MapStyles.getDefaultHighlightStyle(fromString(this.color));
    }

    return new Style();
  };
}
