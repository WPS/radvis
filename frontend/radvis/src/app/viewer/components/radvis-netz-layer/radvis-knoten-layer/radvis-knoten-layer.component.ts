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
import { Circle, Style } from 'ol/style';
import CircleStyle from 'ol/style/Circle';
import { Subscription } from 'rxjs';
import { isArray } from 'rxjs/internal-compatibility';
import { filter } from 'rxjs/operators';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { getRadvisNetzStyleFunction } from 'src/app/shared/models/radvis-netz-style';
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
import { NetzAusblendenService } from 'src/app/viewer/viewer-shared/services/netz-ausblenden.service';

@Component({
  selector: 'rad-radvis-knoten-layer',
  templateUrl: './radvis-knoten-layer.component.html',
  styleUrls: ['./radvis-knoten-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
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

  private static setHoverColorToStrokeAndFill(styles: Style | Style[]): Style[] {
    return (isArray(styles) ? styles : [styles]).map(style => {
      const circle: CircleStyle = style.getImage() as Circle;
      circle?.getStroke().setColor(MapStyles.FEATURE_HOVER_COLOR);
      circle?.getFill().setColor(MapStyles.FEATURE_HOVER_COLOR);

      const clonedStyle = style.clone();
      clonedStyle.setZIndex(highlightNetzklasseLayerZIndex + 1);

      return clonedStyle;
    });
  }

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
          this.setFeatureHighlighted(hf.id as number, true);
        }),
      this.featureHighlightService.unhighlightedFeature$
        .pipe(filter(hf => hf.layer === this.olLayer.get(OlMapService.LAYER_ID)))
        .subscribe(hf => {
          this.setFeatureHighlighted(hf.id as number, false);
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
    if (this.olLayer) {
      this.olLayer.getSource().refresh();
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.olMapService.removeLayer(this.olLayer);
  }

  private getKnotenById(knotenId: number): Feature | null {
    return this.olLayer.getSource().getFeatureById(knotenId);
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

    const olLayer = new VectorLayer({
      source: vectorSource,
      // @ts-ignore
      renderOrder: null,
      style: (feature: FeatureLike, resolution: number): Style | Style[] => {
        const styles: Style | Style[] = getRadvisNetzStyleFunction()(feature, resolution);
        return feature.get('highlighted') ? RadvisKnotenLayerComponent.setHoverColorToStrokeAndFill(styles) : styles;
      },
      minZoom: 16,
      zIndex: defaultNetzklasseLayerZIndex + 1,
    });
    olLayer.set(OlMapService.LAYER_ID, RadvisKnotenLayerComponent.LAYER_NAME);
    return olLayer;
  }
}
