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
import { Feature } from 'ol';
import { FeatureLike } from 'ol/Feature';
import GeoJSON, { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Geometry, LineString, SimpleGeometry } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { Style } from 'ol/style';
import { Subscription } from 'rxjs';
import { isArray } from 'rxjs/internal-compatibility';
import { filter } from 'rxjs/operators';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { MIN_STRECKEN_RESOLUTION } from 'src/app/shared/models/min-strecken-resolution';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { getRadvisNetzStyleFunction } from 'src/app/shared/models/radvis-netz-style';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { StreckenNetzVectorlayer } from 'src/app/shared/models/strecken-netz-vectorlayer';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { createVectorSource } from 'src/app/shared/services/vector-source.factory';
import {
  defaultNetzklasseLayerZIndex,
  highlightNetzklasseLayerZIndex,
} from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import invariant from 'tiny-invariant';

@Component({
  styleUrls: ['./netzklasse-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './netzklasse-layer.component.html',
  selector: 'rad-netzklasse-layer',
})
export class NetzklasseLayerComponent implements OnInit, OnDestroy, OnChanges {
  @Input()
  public netzklasse!: Netzklassefilter;
  @Input()
  public layerPrefix!: string;
  @Input()
  public mitVerlauf: boolean | null = false;

  private olLayer!: VectorLayer;
  private olStreckenLayer!: VectorLayer;

  private subscriptions: Subscription[] = [];

  constructor(
    private radVisNetzFeatureService: NetzausschnittService,
    private errorHandlingService: ErrorHandlingService,
    private olMapService: OlMapService,
    private netzAusblendenService: NetzAusblendenService,
    private featureHighlightService: FeatureHighlightService
  ) {}

  private static setHoverColorToStroke(styles: Style | Style[]): Style[] {
    return (isArray(styles) ? styles : [styles]).map(style => {
      const clonedStyle = style.clone();
      clonedStyle.getStroke().setColor(MapStyles.FEATURE_HOVER_COLOR);
      clonedStyle.setZIndex(highlightNetzklasseLayerZIndex);
      return clonedStyle;
    });
  }

  private static parseFeatures(featureCollection: GeoJSONFeatureCollection): Feature<Geometry>[] {
    return new GeoJSON().readFeatures(featureCollection).flatMap(feature => {
      feature.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, feature.getId());

      // Feature hat keine Seitenattribute
      if (!feature.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)) {
        return [feature];
      }

      // Bei Zweiseitigeit haben Kanten keine feature.ids !
      const featureLinks = feature.clone();
      featureLinks.set(FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.LINKS);

      const featureRechts = feature.clone();
      featureRechts.set(FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.RECHTS);

      // Feature hat sowohl Seitenattribute als auch Verlauf-Geometrien
      if (feature.getGeometry()?.getType() === 'MultiLineString') {
        const [coordinatesLinks, coordinatesRechts] = (feature.getGeometry() as SimpleGeometry).getCoordinates();
        featureLinks.setGeometry(new LineString(coordinatesLinks));
        featureLinks.set(FeatureProperties.VERLAUF_PROPERTY_NAME, true, true);
        featureRechts.setGeometry(new LineString(coordinatesRechts));
        featureRechts.set(FeatureProperties.VERLAUF_PROPERTY_NAME, true, true);
      }

      return [featureLinks, featureRechts];
    });
  }

  ngOnInit(): void {
    invariant(this.netzklasse);
    invariant(this.layerPrefix);
    const layerId = this.layerPrefix + this.netzklasse.toString();
    this.olLayer = this.createLayer(layerId, this.netzklasse.minZoom);
    if (this.netzklasse === Netzklassefilter.RADNETZ) {
      const streckenLayerId = this.layerPrefix + '_strecken_' + this.netzklasse.toString();
      this.olStreckenLayer = new StreckenNetzVectorlayer(
        () => this.radVisNetzFeatureService.getAlleRadNETZKantenForView(Boolean(this.mitVerlauf)),
        defaultNetzklasseLayerZIndex
      );
      this.olStreckenLayer.set(OlMapService.LAYER_ID, streckenLayerId);
      this.olStreckenLayer.setMinResolution(MIN_STRECKEN_RESOLUTION);
      this.olLayer.setMaxResolution(MIN_STRECKEN_RESOLUTION);
      this.olMapService.addLayer(this.olStreckenLayer);
    }
    this.olMapService.addLayer(this.olLayer);
    this.subscriptions.push(
      this.netzAusblendenService.kanteAusblenden$.subscribe(id => this.onChangeKanteVisibility(id, false)),
      this.netzAusblendenService.kanteEinblenden$.subscribe(id => this.onChangeKanteVisibility(id, true)),
      this.featureHighlightService.highlightedFeature$
        .pipe(filter(hf => hf.layer === this.olLayer.get(OlMapService.LAYER_ID)))
        .subscribe(hf => {
          this.setFeatureHighlighted(hf, true);
        }),
      this.featureHighlightService.unhighlightedFeature$
        .pipe(filter(hf => hf.layer === this.olLayer.get(OlMapService.LAYER_ID)))
        .subscribe(hf => {
          this.setFeatureHighlighted(hf, false);
        })
    );
  }

  setFeatureHighlighted(highlightedFeature: RadVisFeature, highlighted: boolean): void {
    const selectedKanteId = highlightedFeature.attributes.get(FeatureProperties.KANTE_ID_PROPERTY_NAME);
    const selectedSeitenbezug = highlightedFeature.attributes.get(FeatureProperties.SEITE_PROPERTY_NAME);
    const currentSelectedFeature = this.getFeaturesByIdsAndSeitenbezug(selectedKanteId, selectedSeitenbezug);
    currentSelectedFeature.forEach(f => {
      f.set('highlighted', highlighted);
      f.changed();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(!changes.netzklasse || changes.netzklasse.firstChange, 'Netzklasse darf sich nicht ändern!');
    if (this.olLayer) {
      this.olLayer.getSource().refresh();
    }
    if (this.olStreckenLayer) {
      this.olStreckenLayer.getSource().refresh();
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.olMapService.removeLayer(this.olLayer);
    if (this.olStreckenLayer) {
      this.olMapService.removeLayer(this.olStreckenLayer);
    }
  }

  private onChangeKanteVisibility(kanteId: number, visible: boolean): void {
    this.getFeaturesByIdsAndSeitenbezug(kanteId).forEach(feature => {
      if (visible) {
        feature.setStyle(undefined);
      } else {
        feature.setStyle(new Style());
      }
      feature.changed();
    });
  }

  private getFeaturesByIdsAndSeitenbezug(kanteId: number | string, kantenSeite?: KantenSeite): Feature<Geometry>[] {
    return this.getActiveLayer()
      .getSource()
      .getFeatures()
      .filter(feature => {
        return String(kanteId) === String(feature.get(FeatureProperties.KANTE_ID_PROPERTY_NAME));
      })
      .filter(feature => {
        if (kantenSeite) {
          return feature.get(FeatureProperties.SEITE_PROPERTY_NAME) === kantenSeite;
        }
        return true;
      });
  }

  private getActiveLayer(): VectorLayer {
    return this.olLayer.getVisible() ? this.olLayer : this.olStreckenLayer;
  }

  private createLayer(layerId: string, minZoom: number): VectorLayer {
    const vectorSource = createVectorSource({
      getFeaturesObservable: extent =>
        this.radVisNetzFeatureService.getKantenForView(extent, [this.netzklasse], Boolean(this.mitVerlauf)),
      parseFeatures: NetzklasseLayerComponent.parseFeatures,
      onFeaturesLoaded: () => {
        this.netzAusblendenService.ausgeblendeteKanten.forEach(id => this.onChangeKanteVisibility(id, false));
      },
      onError: error => this.errorHandlingService.handleError(error),
    });
    const olLayer = new VectorLayer({
      source: vectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: (feature: FeatureLike, resolution: number): Style | Style[] => {
        const styles: Style | Style[] = getRadvisNetzStyleFunction()(feature, resolution);
        return feature.get('highlighted') ? NetzklasseLayerComponent.setHoverColorToStroke(styles) : styles;
      },
      minZoom,
      zIndex: defaultNetzklasseLayerZIndex,
    });
    olLayer.set(OlMapService.LAYER_ID, layerId);
    olLayer.set(OlMapService.IS_BBOX_LAYER, true);
    return olLayer;
  }
}
