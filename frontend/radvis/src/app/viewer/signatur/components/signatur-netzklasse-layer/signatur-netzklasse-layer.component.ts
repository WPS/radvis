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
import { Extent } from 'ol/extent';
import { FeatureLike } from 'ol/Feature';
import GeoJSON, { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Geometry } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { Style } from 'ol/style';
import { StyleFunction } from 'ol/style/Style';
import { Observable, Subscription } from 'rxjs';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { SignaturLegende } from 'src/app/shared/models/signatur-legende';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { createVectorSource } from 'src/app/shared/services/vector-source.factory';
import { SignaturService } from 'src/app/viewer/signatur/services/signatur.service';
import { signaturNetzklasseLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import { NetzAusblendenService } from 'src/app/viewer/viewer-shared/services/netz-ausblenden.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-signatur-netzklasse-layer',
  templateUrl: './signatur-netzklasse-layer.component.html',
  styleUrls: ['./signatur-netzklasse-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SignaturNetzklasseLayerComponent implements OnInit, OnDestroy, OnChanges {
  public static MIN_RESOLUTION_FOR_STRECKEN = 20;

  @Input()
  public netzklasse!: Netzklassefilter;
  @Input()
  public layerPrefix!: string;
  @Input()
  public generatedStyleFunction: StyleFunction | null = null;
  @Input()
  public attributnamen!: string[];
  @Input()
  public legende: SignaturLegende | null = null;

  private olLayer!: VectorLayer;

  private subscriptions: Subscription[] = [];

  constructor(
    private errorHandlingService: ErrorHandlingService,
    private olMapService: OlMapService,
    private signaturService: SignaturService,
    private netzAusblendenService: NetzAusblendenService
  ) {}

  ngOnInit(): void {
    invariant(this.netzklasse);
    invariant(this.layerPrefix);
    invariant(this.attributnamen.length > 0);
    const layerId = this.layerPrefix + this.netzklasse.toString();
    this.olLayer = this.createLayer(layerId, this.netzklasse.minZoom);
    if (this.netzklasse === Netzklassefilter.RADNETZ) {
      this.olLayer.setMaxResolution(SignaturNetzklasseLayerComponent.MIN_RESOLUTION_FOR_STRECKEN);
    }
    this.olMapService.addLayer(this.olLayer, undefined, this.legende ?? undefined);
    this.subscriptions.push(
      this.netzAusblendenService.kanteAusblenden$.subscribe(id => this.onChangeKanteVisibility(id, false)),
      this.netzAusblendenService.kanteEinblenden$.subscribe(id => this.onChangeKanteVisibility(id, true))
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    invariant(!changes.netzklasse || changes.netzklasse.firstChange, 'Netzklasse darf sich nicht ändern!');
    if (this.olLayer) {
      this.olLayer.getSource().refresh();
    }

    if (changes.legende && this.olLayer) {
      this.olMapService.updateLegende(this.olLayer, this.legende);
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.olMapService.removeLayer(this.olLayer);
  }

  private onChangeKanteVisibility(id: number, visible: boolean): void {
    this.getFeaturesByIds(id).forEach(feature => {
      if (visible) {
        feature.setStyle(undefined);
      } else {
        feature.setStyle(new Style());
      }
      feature.changed();
    });
  }

  private getFeaturesByIds(kanteId: number): Feature<Geometry>[] {
    if (this.olLayer.getVisible()) {
      return this.olLayer
        .getSource()
        .getFeatures()
        .filter(feature => kanteId === +(feature.get(FeatureProperties.KANTE_ID_PROPERTY_NAME) as number));
    }
    return [];
  }

  private createLayer(layerId: string, minZoom: number): VectorLayer {
    const vectorSource = createVectorSource({
      getFeaturesObservable: (extent: Extent): Observable<GeoJSONFeatureCollection> =>
        this.signaturService.getFeaturesForView(this.attributnamen, extent, [this.netzklasse]),
      parseFeatures: featureCollection => new GeoJSON().readFeatures(featureCollection),
      onFeaturesLoaded: () => {
        this.netzAusblendenService.ausgeblendeteKanten.forEach(id => this.onChangeKanteVisibility(id, false));
      },
      onError: error => this.errorHandlingService.handleError(error),
    });

    const olLayer = new VectorLayer({
      source: vectorSource,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: this.styleFunction,
      minZoom,
    });
    olLayer.setZIndex(signaturNetzklasseLayerZIndex);
    olLayer.set(OlMapService.LAYER_ID, layerId);
    olLayer.set(OlMapService.IS_BBOX_LAYER, true);
    return olLayer;
  }

  private styleFunction = (feature: FeatureLike, resolution: any): Style | Style[] => {
    if (!this.generatedStyleFunction) {
      return new Style();
    } else {
      return this.generatedStyleFunction(feature, resolution);
    }
  };
}
