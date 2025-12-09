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
import Feature, { FeatureLike } from 'ol/Feature';
import { Geometry, LineString } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import { Subscription } from 'rxjs';
import { isArray } from 'rxjs/internal-compatibility';
import { filter } from 'rxjs/operators';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { NetzDetailSelektion } from 'src/app/shared/models/netzdetail-selektion';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { getRadvisNetzStyleFunction } from 'src/app/shared/models/radvis-netz-style';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { RADVIS_NETZ_LAYER_PREFIX } from 'src/app/viewer/viewer-shared/models/radvis-netz-layer-prefix';
import {
  highlightNetzklasseLayerZIndex,
  kanteHighlightLayerZIndex,
} from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-kanten-highlight-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KantenHighlightLayerComponent implements OnChanges, OnDestroy {
  public static LAYER_ID = `${RADVIS_NETZ_LAYER_PREFIX}_KantenDetailHighlightLayer`;

  @Input()
  selektion!: NetzDetailSelektion;

  @Input()
  verlauf: boolean | null = false;

  vectorSource = new VectorSource();
  olLayer: VectorLayer;

  private subscriptions: Subscription[] = [];

  private readonly HIGHLIGHTED = 'highlighted';
  private readonly HOVER_HIGHLIGHT = 'slightlyHighlighted';

  constructor(
    private olMapService: OlMapService,
    private netzAusblendenService: NetzAusblendenService,
    private featureHighlightService: FeatureHighlightService
  ) {
    this.olLayer = new VectorLayer({
      source: this.vectorSource,
      style: getRadvisNetzStyleFunction(this.styleFn),
      zIndex: kanteHighlightLayerZIndex,
    });
    this.olLayer.set(OlMapService.LAYER_ID, KantenHighlightLayerComponent.LAYER_ID);
    this.olMapService.addLayer(this.olLayer);

    // Dieses Highlight bezieht sich auf die leichte hellrosane Einfaerbung, wenn in dem Mehrfachauswahlmenu
    // ueber einer Kante gehovert wird
    this.subscriptions.push(
      this.featureHighlightService.highlightedFeature$
        .pipe(filter(hf => hf.layer === this.olLayer.get(OlMapService.LAYER_ID)))
        .subscribe(hf => {
          this.setFeatureHoverHighlighted(hf, true);
        }),
      this.featureHighlightService.unhighlightedFeature$
        .pipe(filter(hf => hf.layer === this.olLayer.get(OlMapService.LAYER_ID)))
        .subscribe(hf => {
          this.setFeatureHoverHighlighted(hf, false);
        })
    );
  }

  private static setHoverColorToStroke(styles: Style | Style[]): Style[] {
    return (isArray(styles) ? styles : [styles]).map(style => {
      const clonedStyle = style.clone();
      clonedStyle.getStroke()?.setColor(MapStyles.FEATURE_HOVER_COLOR);
      clonedStyle.setZIndex(highlightNetzklasseLayerZIndex);
      return clonedStyle;
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(this.selektion);

    if (changes.selektion) {
      const previousId = (changes.selektion.previousValue as NetzDetailSelektion)?.id;
      const currentId = (changes.selektion.currentValue as NetzDetailSelektion)?.id;
      if (previousId) {
        this.netzAusblendenService.kanteEinblenden(previousId);
      }
      if (currentId) {
        this.netzAusblendenService.kanteAusblenden(currentId);
      }
    }

    this.vectorSource.clear(true);
    const features = this.createFeatures(this.selektion, this.verlauf);
    this.vectorSource.addFeatures(features);
    this.vectorSource.changed();
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.olLayer);
    this.netzAusblendenService.kanteEinblenden(this.selektion.id);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  private styleFn = (feature: FeatureLike, resolution: number): Style | Style[] => {
    if (feature.get(this.HIGHLIGHTED)) {
      return MapStyles.getDefaultHighlightStyle(
        MapStyles.FEATURE_SELECT_COLOR,
        true,
        (feature.getGeometry() as LineString).getCoordinates()
      );
    } else if (feature.get(this.HOVER_HIGHLIGHT)) {
      const styles = MapStyles.getDefaultNetzStyleFunction()(feature, resolution);
      return KantenHighlightLayerComponent.setHoverColorToStroke(styles);
    } else {
      return MapStyles.getDefaultNetzStyleFunction()(feature, resolution);
    }
  };

  private createFeatures(selektion: NetzDetailSelektion, verlauf: boolean | null): Feature<Geometry>[] {
    const result: Feature<Geometry>[] = [];
    let geometryLinks = new LineString(selektion.hauptGeometry.coordinates);
    let geometryRechts = new LineString(selektion.hauptGeometry.coordinates);
    if (verlauf && selektion.verlaufLinks) {
      geometryLinks = new LineString(selektion.verlaufLinks.coordinates);
    }
    if (verlauf && selektion.verlaufRechts) {
      geometryRechts = new LineString(selektion.verlaufRechts.coordinates);
    }

    const isZweiseitig = selektion.seite !== null;

    const featureLinks = new Feature(geometryLinks);
    featureLinks.set(FeatureProperties.VERLAUF_PROPERTY_NAME, verlauf && selektion.verlaufLinks, true);
    featureLinks.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, selektion.id);
    featureLinks.set(this.HIGHLIGHTED, !isZweiseitig || selektion.seite === KantenSeite.LINKS);
    featureLinks.set(FeatureProperties.ZWEISEITIG_PROPERTY_NAME, isZweiseitig);
    if (isZweiseitig) {
      featureLinks.set(FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.LINKS);
    }
    result.push(featureLinks);

    if (isZweiseitig) {
      const featureRechts = new Feature(geometryRechts);
      featureRechts.set(FeatureProperties.VERLAUF_PROPERTY_NAME, verlauf && selektion.verlaufRechts, true);
      featureRechts.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, selektion.id);
      featureRechts.set(this.HIGHLIGHTED, selektion.seite === KantenSeite.RECHTS);
      featureRechts.set(FeatureProperties.ZWEISEITIG_PROPERTY_NAME, true);
      featureRechts.set(FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.RECHTS);
      result.push(featureRechts);
    }

    return result;
  }

  private setFeatureHoverHighlighted(highlightedFeature: RadVisFeature, hoverHighlighted: boolean): void {
    const selectedKanteId = highlightedFeature.attributes.get(FeatureProperties.KANTE_ID_PROPERTY_NAME);
    const selectedSeitenbezug = highlightedFeature.attributes.get(FeatureProperties.SEITE_PROPERTY_NAME);
    const selectedFeatures = this.getFeaturesByIdsAndSeitenbezug(selectedKanteId, selectedSeitenbezug);
    selectedFeatures.forEach(f => {
      f.set(this.HOVER_HIGHLIGHT, hoverHighlighted);
      f.changed();
    });
  }

  private getFeaturesByIdsAndSeitenbezug(kanteId: number | string, kantenSeite?: KantenSeite): Feature<Geometry>[] {
    return this.vectorSource
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
}
