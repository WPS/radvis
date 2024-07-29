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
import Feature from 'ol/Feature';
import { Geometry, LineString, Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { StyleFunction } from 'ol/style/Style';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { LineStringOperations } from 'src/app/shared/models/line-string-operations';
import { getRadvisNetzStyleFunction } from 'src/app/shared/models/radvis-netz-style';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { PunktuellerKantenBezuegeVectorLayer } from 'src/app/shared/components/netzbezug-selektion-control/punktueller-kanten-bezuege-vector-layer';
import { KantenNetzbezug } from 'src/app/shared/models/kanten-netzbezug';
import { KnotenNetzbezug } from 'src/app/shared/models/knoten-netzbezug';
import { KantenSeitenbezug, Netzbezug, Segment } from 'src/app/shared/models/netzbezug';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import invariant from 'tiny-invariant';
import { infrastrukturHighlightLayerZIndex } from 'src/app/shared/models/shared-layer-zindex-config';

@Component({
  selector: 'rad-netzbezug-highlight-layer',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NetzbezugHighlightLayerComponent implements OnInit, OnChanges, OnDestroy {
  private static readonly HIGHLIGHTED_PROPERTY = 'highlighted';

  @Input()
  netzbezug!: Netzbezug;

  @Input()
  layerId!: string;

  private kantenUndKnotenHighlightLayer!: VectorLayer;
  private kantenUndKnotenHighlightSource: VectorSource = new VectorSource();

  // da wir auf dem Objekt und nicht der VectorSource im updateLayers arbeiten, muss das sofort initialisiert werden
  private punktuellerKantenBezuegeVectorLayer = new PunktuellerKantenBezuegeVectorLayer(undefined);

  constructor(
    private olMapService: OlMapService,
    private netzAusblendenService: NetzAusblendenService
  ) {}

  private static highlightStyle: StyleFunction = (feature, resolution) => {
    if (feature.get(NetzbezugHighlightLayerComponent.HIGHLIGHTED_PROPERTY)) {
      return MapStyles.getDefaultHighlightStyle(MapStyles.FEATURE_SELECT_COLOR);
    } else {
      return MapStyles.getDefaultNetzStyleFunction()(feature, resolution);
    }
  };

  ngOnInit(): void {
    invariant(this.layerId);
    this.kantenUndKnotenHighlightLayer = this.createKantenUndKnotenHighlightLayer(this.layerId);
    this.olMapService.addLayer(this.kantenUndKnotenHighlightLayer);
    this.olMapService.addLayer(this.punktuellerKantenBezuegeVectorLayer);
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(this.netzbezug);
    if (changes.netzbezug) {
      if (!changes.netzbezug.firstChange) {
        this.netzEinblenden(
          changes.netzbezug.previousValue?.kantenBezug.map((abschnitt: KantenNetzbezug) => abschnitt.kanteId),
          changes.netzbezug.previousValue?.knotenBezug.map((knoten: KnotenNetzbezug) => knoten.knotenId)
        );
      }

      this.updateLayers();

      this.netzAusblenden(
        this.netzbezug.kantenBezug.map(abschnitt => abschnitt.kanteId),
        this.netzbezug.knotenBezug.map(knoten => knoten.knotenId)
      );
    }
  }

  ngOnDestroy(): void {
    this.netzEinblenden(
      this.netzbezug.kantenBezug.map(abschnitt => abschnitt.kanteId),
      this.netzbezug.knotenBezug.map(knoten => knoten.knotenId)
    );
    this.olMapService.removeLayer(this.kantenUndKnotenHighlightLayer);
    this.olMapService.removeLayer(this.punktuellerKantenBezuegeVectorLayer);
  }

  private createKantenUndKnotenHighlightLayer(layerId: string): VectorLayer {
    const layer = new VectorLayer({
      source: this.kantenUndKnotenHighlightSource,
      zIndex: infrastrukturHighlightLayerZIndex,
    });

    layer.setRenderOrder(null);
    layer.setStyle(
      getRadvisNetzStyleFunction(
        NetzbezugHighlightLayerComponent.highlightStyle,
        NetzbezugHighlightLayerComponent.highlightStyle
      )
    );
    layer.set(OlMapService.LAYER_ID, layerId);

    return layer;
  }

  private updateLayers(): void {
    this.kantenUndKnotenHighlightSource.clear();

    Netzbezug.groupByKante(this.netzbezug.kantenBezug).forEach(bezuege => {
      const kantenBezuegeLinks = bezuege.filter(
        kBZ => kBZ.kantenSeite === KantenSeitenbezug.BEIDSEITIG || kBZ.kantenSeite === KantenSeitenbezug.LINKS
      );
      const kantenBezuegeRechts = bezuege.filter(
        kBZ => kBZ.kantenSeite === KantenSeitenbezug.BEIDSEITIG || kBZ.kantenSeite === KantenSeitenbezug.RECHTS
      );

      const zweiseitig = !Netzbezug.sindAbschnitteIdentisch(kantenBezuegeLinks, kantenBezuegeRechts);

      Netzbezug.extractKantenSelektion(kantenBezuegeLinks).forEach(segment => {
        const segmentFeature = this.createFeatureFromSegment(
          bezuege[0].geometrie,
          segment,
          zweiseitig,
          zweiseitig ? KantenSeite.LINKS : undefined
        );
        this.kantenUndKnotenHighlightSource.addFeature(segmentFeature);
      });
      if (zweiseitig) {
        Netzbezug.extractKantenSelektion(kantenBezuegeRechts).forEach(segment => {
          const segmentFeature = this.createFeatureFromSegment(bezuege[0].geometrie, segment, true, KantenSeite.RECHTS);
          this.kantenUndKnotenHighlightSource.addFeature(segmentFeature);
        });
      }
    });

    const knotenGeometrien = this.netzbezug.knotenBezug.map(knoten => {
      const knotenFeature = new Feature(new Point(knoten.geometrie.coordinates));
      knotenFeature.set(NetzbezugHighlightLayerComponent.HIGHLIGHTED_PROPERTY, true);
      return knotenFeature;
    });
    this.kantenUndKnotenHighlightSource.addFeatures(knotenGeometrien);

    this.kantenUndKnotenHighlightSource.changed();

    this.punktuellerKantenBezuegeVectorLayer.updatePuntuelleKantenNetzbezuege(this.netzbezug.punktuellerKantenBezug);
  }

  private createFeatureFromSegment(
    kantenGeometrie: LineStringGeojson,
    segment: Segment,
    zweiseitig: boolean,
    kantenSeite?: KantenSeite
  ): Feature<Geometry> {
    const subLineString = LineStringOperations.getSubLineString(new LineString(kantenGeometrie.coordinates), {
      von: segment.von,
      bis: segment.bis,
    });
    const segmentFeature = new Feature(subLineString);
    segmentFeature.set(NetzbezugHighlightLayerComponent.HIGHLIGHTED_PROPERTY, segment.selected);
    if (zweiseitig) {
      invariant(kantenSeite !== undefined);
      segmentFeature.set(FeatureProperties.ZWEISEITIG_PROPERTY_NAME, true);
      segmentFeature.set(FeatureProperties.SEITE_PROPERTY_NAME, kantenSeite);
    }
    return segmentFeature;
  }

  private netzEinblenden(kanten: number[], knoten: number[] = []): void {
    kanten.forEach(id => this.netzAusblendenService.kanteEinblenden(id));
    knoten.forEach(id => this.netzAusblendenService.knotenEinblenden(id));
  }

  private netzAusblenden(kanten: number[], knoten: number[] = []): void {
    kanten.forEach(id => this.netzAusblendenService.kanteAusblenden(id));
    knoten.forEach(id => this.netzAusblendenService.knotenAusblenden(id));
  }
}
