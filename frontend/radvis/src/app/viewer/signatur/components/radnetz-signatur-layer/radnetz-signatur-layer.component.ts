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
import { Geometry } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { Style } from 'ol/style';
import { StyleFunction } from 'ol/style/Style';
import { Subscription } from 'rxjs';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { SignaturService } from 'src/app/viewer/signatur/services/signatur.service';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import TileLayer from 'ol/layer/Tile';
import { TileWMS } from 'ol/source';
import * as olProj from 'ol/proj';
import { WmsCapabilitiesService } from 'src/app/viewer/signatur/services/wms-capabilities.service';
import { StreckenNetzVectorlayer } from 'src/app/shared/models/strecken-netz-vectorlayer';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { SignaturNetzklasseLayerComponent } from '../signatur-netzklasse-layer/signatur-netzklasse-layer.component';
import { signaturNetzklasseLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-radnetz-signatur-layer',
  templateUrl: './radnetz-signatur-layer.component.html',
  styleUrls: ['./radnetz-signatur-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class RadnetzSignaturLayerComponent implements OnInit, OnDestroy, OnChanges {
  @Input()
  public streckenLayerPrefix!: string;
  @Input()
  public generatedStyleFunction: StyleFunction | null = null;
  @Input()
  public attributnamen!: string[];
  @Input()
  public signaturname: string | null = null;

  private wmsLayer: TileLayer | null = null;
  private olStreckenLayer: VectorLayer | null = null;

  private subscriptions: Subscription[] = [];

  constructor(
    private errorHandlingService: ErrorHandlingService,
    private olMapService: OlMapService,
    private wmsCapabilitiesService: WmsCapabilitiesService,
    private signaturService: SignaturService,
    private netzAusblendenService: NetzAusblendenService
  ) {}

  ngOnInit(): void {
    this.subscriptions.push(
      this.netzAusblendenService.kanteAusblenden$.subscribe(id => this.onChangeKanteVisibility(id, false)),
      this.netzAusblendenService.kanteEinblenden$.subscribe(id => this.onChangeKanteVisibility(id, true))
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.signaturname && changes.signaturname) {
      this.removeLayer();
      this.wmsCapabilitiesService
        .isStyleForLayerAvailable('radvisnetz_klassifiziert', this.replaceUmlaute(this.signaturname))
        .then(isStyleForLayerAvailable =>
          isStyleForLayerAvailable ? this.initWmsLayer() : this.initOlStreckenLayer()
        );
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.removeLayer();
  }

  private removeLayer(): void {
    if (this.olStreckenLayer) {
      this.olMapService.removeLayer(this.olStreckenLayer);
    }
    if (this.wmsLayer) {
      this.olMapService.removeLayer(this.wmsLayer);
    }
  }

  private initWmsLayer(): void {
    invariant(this.signaturname);
    this.wmsLayer = new TileLayer({
      source: new TileWMS({
        url: '/api/geoserver/saml/radvis/wms?layers=radvis%3Aradvisnetz_klassifiziert',
        params: {
          PROJECTION: olProj.get('EPSG:25832')!.getCode(),
          STYLES: 'radvis:' + this.replaceUmlaute(this.signaturname),
          CQL_FILTER: "netzklassen like 'RADNETZ_%'",
        },
        transition: 0,
      }),
      zIndex: signaturNetzklasseLayerZIndex,
    });
    this.wmsLayer.setMinResolution(SignaturNetzklasseLayerComponent.MIN_RESOLUTION_FOR_STRECKEN);
    this.olMapService.addLayer(this.wmsLayer);
  }

  private replaceUmlaute(s: string): string {
    return s
      .replace('ä', 'ae')
      .replace('Ä', 'AE')
      .replace('ö', 'oe')
      .replace('Ö', 'OE')
      .replace('ü', 'ue')
      .replace('Ü', 'UE');
  }

  private initOlStreckenLayer(): void {
    const streckenLayerId = this.streckenLayerPrefix + '_strecken_' + Netzklassefilter.RADNETZ.name;
    this.olStreckenLayer = new StreckenNetzVectorlayer(
      () => this.signaturService.getStreckenForNetzklasse(this.attributnamen, [0, 0, 0, 0], [Netzklassefilter.RADNETZ]),
      signaturNetzklasseLayerZIndex
    );
    this.olStreckenLayer.setStyle(this.styleFunction);
    this.olStreckenLayer.set(OlMapService.LAYER_ID, streckenLayerId);
    this.olStreckenLayer.setMinResolution(SignaturNetzklasseLayerComponent.MIN_RESOLUTION_FOR_STRECKEN);
    this.olMapService.addLayer(this.olStreckenLayer);
  }

  // Wenn man das auch für WMS-Layer braucht, müsste man mit ECQL-Filter die entsprechende Kante excluden
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
    return (
      this.olStreckenLayer
        ?.getSource()
        ?.getFeatures()
        .filter(feature => kanteId === +feature.get(FeatureProperties.KANTE_ID_PROPERTY_NAME)) ?? []
    );
  }

  private styleFunction = (feature: FeatureLike, resolution: any): Style | Style[] | void => {
    if (!this.generatedStyleFunction) {
      return new Style();
    } else {
      return this.generatedStyleFunction(feature, resolution);
    }
  };
}
