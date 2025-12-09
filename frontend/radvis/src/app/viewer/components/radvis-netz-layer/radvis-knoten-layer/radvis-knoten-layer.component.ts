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

import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { Feature } from 'ol';
import { FeatureLike } from 'ol/Feature';
import GeoJSON from 'ol/format/GeoJSON';
import VectorLayer from 'ol/layer/Vector';
import { Style } from 'ol/style';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { createVectorSource } from 'src/app/shared/services/vector-source.factory';
import { RADVIS_NETZ_LAYER_PREFIX } from 'src/app/viewer/viewer-shared/models/radvis-netz-layer-prefix';
import {
  defaultNetzklasseLayerZIndex,
  highlightNetzklasseLayerZIndex,
} from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';

@Component({
  selector: 'rad-radvis-knoten-layer',
  templateUrl: './radvis-knoten-layer.component.html',
  styleUrls: ['./radvis-knoten-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class RadvisKnotenLayerComponent implements OnInit, OnDestroy, OnChanges {
  public static LAYER_NAME = RADVIS_NETZ_LAYER_PREFIX + 'KnotenLayer';

  @Input()
  public netzklassen: Netzklassefilter[] = [];

  private olLayer!: VectorLayer;

  private subscriptions: Subscription[] = [];

  constructor(
    private olMapService: OlMapService,
    private radVisNetzFeatureService: NetzausschnittService,
    private netzAusblendenService: NetzAusblendenService,
    private errorHandlingService: ErrorHandlingService,
    private featureHighlightService: FeatureHighlightService
  ) {}

  ngOnInit(): void {
    this.olLayer = this.createKnotenLayer();
    this.olMapService.addLayer(this.olLayer);
    this.subscriptions.push(
      this.netzAusblendenService.knotenAusblenden$.subscribe(knotenId => {
        this.getKnotenById(knotenId)?.setStyle(new Style());
      }),
      this.netzAusblendenService.knotenEinblenden$.subscribe(knotenId => {
        const knoten = this.getKnotenById(knotenId);
        if (knoten) {
          knoten.setStyle(undefined);
          knoten.changed();
        }
      }),
      this.featureHighlightService.highlightedFeature$
        .pipe(filter(hf => hf.layer === this.olLayer.get(OlMapService.LAYER_ID)))
        .subscribe(hf => {
          this.setFeatureHighlighted(hf.id!, true);
        }),
      this.featureHighlightService.unhighlightedFeature$
        .pipe(filter(hf => hf.layer === this.olLayer.get(OlMapService.LAYER_ID)))
        .subscribe(hf => {
          this.setFeatureHighlighted(hf.id!, false);
        })
    );
  }

  setFeatureHighlighted(id: number, highlighted: boolean): void {
    const currentSelectedFeature = this.getKnotenById(id);
    if (currentSelectedFeature) {
      currentSelectedFeature.set('highlighted', highlighted);
      currentSelectedFeature.changed();
    }
  }

  ngOnChanges(): void {
    this.olLayer?.getSource()?.refresh();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.olMapService.removeLayer(this.olLayer);
  }

  private getKnotenById(knotenId: number): Feature | null {
    return this.olLayer?.getSource()?.getFeatureById(knotenId);
  }

  private createKnotenLayer(): VectorLayer {
    const vectorSource = createVectorSource({
      getFeaturesObservable: extent => this.radVisNetzFeatureService.getKnotenForView(extent, this.netzklassen),
      parseFeatures: featureCollection => new GeoJSON().readFeatures(featureCollection),
      onFeaturesLoaded: () => {
        this.netzAusblendenService.ausgeblendeteKnoten.forEach(id => this.getKnotenById(id)?.setStyle(new Style()));
      },
      onError: error => this.errorHandlingService.handleError(error),
    });

    const normalStyle = MapStyles.defaultPointStyleLarge(MapStyles.FEATURE_COLOR);
    normalStyle.setZIndex(highlightNetzklasseLayerZIndex + 1);

    const hoverStyle = MapStyles.defaultPointStyleLarge(MapStyles.FEATURE_HOVER_COLOR);
    hoverStyle.setZIndex(highlightNetzklasseLayerZIndex + 1);

    const olLayer = new VectorLayer({
      source: vectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: (feature: FeatureLike, resolution: number): Style | Style[] | void => {
        return feature.get('highlighted') ? hoverStyle : normalStyle;
      },
      minZoom: 16,
      zIndex: defaultNetzklasseLayerZIndex + 1,
    });
    olLayer.set(OlMapService.LAYER_ID, RadvisKnotenLayerComponent.LAYER_NAME);
    return olLayer;
  }
}
