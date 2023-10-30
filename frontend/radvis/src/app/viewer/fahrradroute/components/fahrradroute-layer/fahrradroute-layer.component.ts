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

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { Feature } from 'ol';
import { FeatureLike } from 'ol/Feature';
import { Geometry, LineString, MultiLineString } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { Style } from 'ol/style';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { isLineString, isMultiLineString } from 'src/app/shared/models/geojson-geometrie';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { FahrradrouteListenView } from 'src/app/viewer/fahrradroute/models/fahrradroute-listen-view';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { FahrradrouteFilterService } from 'src/app/viewer/fahrradroute/services/fahrradroute-filter.service';
import { FahrradrouteRoutingService } from 'src/app/viewer/fahrradroute/services/fahrradroute-routing.service';
import {
  AbstractInfrastrukturLayerComponent,
} from 'src/app/viewer/viewer-shared/components/abstract-infrastruktur-layer.component';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { ImageWMS } from 'ol/source';
import * as olProj from 'ol/proj';
import { WMSGetFeatureInfo } from 'ol/format';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { filter } from 'rxjs/operators';
import { FahrradrouteDetailView } from 'src/app/viewer/fahrradroute/models/fahrradroute-detail-view';
import { infrastrukturLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';
import ImageLayer from 'ol/layer/Image';

@Component({
  selector: 'rad-fahrradroute-layer',
  templateUrl: './fahrradroute-layer.component.html',
  styleUrls: ['./fahrradroute-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FahrradrouteLayerComponent
  extends AbstractInfrastrukturLayerComponent<FahrradrouteListenView>
  implements OnDestroy {
  private highlightLayer: VectorLayer;
  private layer: ImageLayer;
  private source: ImageWMS;

  constructor(
    private olMapService: OlMapService,
    private httpClient: HttpClient,
    fahrradrouteFilterService: FahrradrouteFilterService,
    fahrradrouteRoutingService: FahrradrouteRoutingService,
    featureHighlightService: FeatureHighlightService,
  ) {
    super(
      fahrradrouteRoutingService,
      fahrradrouteFilterService,
      featureHighlightService,
      FAHRRADROUTE,
    );

    this.source = new ImageWMS({
      url: `/api/geoserver/saml/radvis/wms`,
      params: {
        LAYERS: 'radvis:fahrradroute',
        PROJECTION: olProj.get('EPSG:25832').getCode(),
        STYLES: 'Fahrradrouten',
      },
      imageLoadFunction: (image, src): void => {
        const body = new URLSearchParams(src);

        // Bewusts nur Hauptstrecken anzeigen
        body.set('CQL_FILTER', `variante_kategorie IS NULL AND id IN (${this.filterService.currentFilteredList.map(flv => flv.id)})`);
        const headers = new HttpHeaders().set('Content-Type', 'application/x-www-form-urlencoded');

        this.httpClient
        .post<Blob>(src, body, {
          headers,
          responseType: 'blob' as 'json',
        })
        .toPromise()
        .then(blob => {
          if (blob) {
            // Wir setzen den blob, den wir erhalten (also die png, die vom WMS kommt), als source für das dem
            // gerenderten Bild zugeordnete ImageElement. Dafür muss der Blob unter einer lokalen URL verfügbar sein,
            // daher creatObjectURL(...).
            (image.getImage() as HTMLImageElement).src = URL.createObjectURL(blob);
          }
        });
      },
      serverType: 'geoserver',
    });
    this.layer = new ImageLayer({
      source: this.source,
      zIndex: infrastrukturLayerZIndex,
      opacity: 1,
      minZoom: 4,
    });

    this.olMapService.addWMSFeatureLayer(this.layer, this.getFeaturesCallback);

    // Wir nutzen den alten Fahrradrouten-Layer nur noch für das highlighting
    this.highlightLayer = this.createLayer();
    this.highlightLayer.setZIndex(infrastrukturLayerZIndex + 1);
    this.layer.set(OlMapService.LAYER_ID, this.layerId);
    this.olMapService.addLayer(this.highlightLayer);

    this.initServiceSubscriptions();
  }

  ngOnDestroy(): void {
    this.olMapService.removeLayer(this.layer);
    this.olMapService.removeLayer(this.highlightLayer);
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  getFeatureByFahrradroutenId(id: number): Feature<Geometry>[] {
    return this.vectorSource
    .getFeatures()
    .filter(feature => String(id) === String(feature.get(FeatureProperties.FAHRRADROUTE_ID_PROPERTY_NAME)));
  }

  // eslint-disable-next-line prettier/prettier
  protected override setFeatureHighlighted(id: number, highlighted: boolean): void {
    const currentSelectedFeatures = this.getFeatureByFahrradroutenId(id);
    if (currentSelectedFeatures && currentSelectedFeatures.length > 0) {
      // wenn die Features (bei Fahrradrouten immer nur eins) bereits in der VectorSource sind, setzen wir nur das highlight
      currentSelectedFeatures.forEach(feature => {
        feature.set('highlighted', highlighted);
        feature.changed();
      });
    } else {
      // Highlighting für eine Fahrradroute ohne KantenBezug und NetzBezugsLineString
      this.getFeatureWithGeometry(id).then(feature => {
        if (feature) {
          feature.set('highlighted', highlighted);
          this.vectorSource.addFeature(feature);
          feature.changed();
        }
      });
    }
  }

  // eslint-disable-next-line prettier/prettier
  protected override initServiceSubscriptions(): void {
    // Wir überschreiben nur die subscription auf dem FilterService
    this.subscriptions.push(
      this.filterService.filteredList$.subscribe(() => {
        // CQL-Filter ändert sich hierdurch, deswegen WMS-Dienst neu anfragen.
        this.source.changed();
      }),
    );

    this.subscriptions.push(
      this.featureHighlightService.highlightedFeature$.pipe(filter(hf => hf.layer === this.layerId)).subscribe(hf => {
        this.setFeatureHighlighted(this.extractIdFromFeature(hf), true);
      }),
      this.featureHighlightService.unhighlightedFeature$.pipe(filter(hf => hf.layer === this.layerId)).subscribe(hf => {
        this.setFeatureHighlighted(this.extractIdFromFeature(hf), false);
        if (this.selectedId) {
          this.setFeatureHighlighted(this.selectedId, true);
        }
      }),
    );
  }

  protected extractIdFromFeature(hf: RadVisFeature): number {
    return Number(hf.attribute.find(a => a.key === FeatureProperties.FAHRRADROUTE_ID_PROPERTY_NAME)?.value);
  }

  // Muss implementiert werden, wird aber nicht genutzt, da wir die filteredList$-Subscription überschreiben aus der
  // das hier aufgerufen wird.
  protected convertToFeature(): Feature<Geometry>[] {
    return [];
  }

  protected override styleFn = (feature: FeatureLike): Style | Style[] => {
    // Nur gehighlightete Features sollen gerendert werden!
    return feature.get('highlighted')
      ? MapStyles.getDefaultHighlightStyle()
      : new Style();
  };

  private convertDetailsToFeature(infrastruktur: FahrradrouteDetailView): Feature<Geometry> | undefined {
    let feature;
    const geometry = (infrastruktur.geometrie ?? infrastruktur.originalGeometrie);
    if (geometry) {
      if (isMultiLineString(geometry)) {
        feature = new Feature(new MultiLineString(geometry.coordinates));
      } else if (isLineString(geometry)) {
        feature = new Feature(new LineString(geometry.coordinates));
      } else {
        throw new Error('Fahrradrouten Geometrie ist weder LineString noch MultiLineString');
      }
      feature.set(FeatureProperties.FAHRRADROUTE_ID_PROPERTY_NAME, infrastruktur.id);
      feature.set(AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME, infrastruktur.name);
    }
    return feature;
  }

  private getFeaturesCallback = (coordinate: number[], resolution: number): Promise<Feature<Geometry>[]> => {
    const featureInfoUrl = this.source?.getFeatureInfoUrl(coordinate, resolution, 'EPSG:25832', {
      INFO_FORMAT: 'application/vnd.ogc.gml',
      FEATURE_COUNT: 5,
    });
    if (featureInfoUrl) {
      const url = new URL(featureInfoUrl, document.location.href);
      url.searchParams.set('QUERY_LAYERS', url.searchParams.get('LAYERS') || '');
      url.searchParams.set('propertyName', 'id,name,variante_kategorie');

      return this.httpClient
      .get(url.toString(), { responseType: 'text' })
      .toPromise()
      .then(
        gml => {
          const features = new WMSGetFeatureInfo()
          .readFeatures(gml)
          // Wir wollen keine Varianten
          .filter(f => !f.get('variante_kategorie'))
          // und nur Features, die dem Filter entsprechen
          .filter(f => this.filterService.currentFilteredList.map(flv => flv.id).includes(+f.get('id')));

          features.forEach(feature => {
            feature.set(FeatureProperties.FAHRRADROUTE_ID_PROPERTY_NAME, feature.get('id'));
            feature.set(AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME, feature.get('name'));
          });
          // Wir laden die angefragten Features in die vectorSource,
          // damit sie ggf. onHover gehighlighted werden können.
          Promise.all(features.map(f => this.getFeatureWithGeometry(f.get('id'))))
          .then(fs => {
            this.vectorSource.clear(true);
            // @ts-ignore filter undefined
            this.vectorSource.addFeatures(fs.filter(f => !!f));
          });
          return features;
        },
        () => {
          return Promise.resolve([]);
        },
      );
    }
    return Promise.resolve([]);
  };

  private getFeatureWithGeometry(id: number): Promise<Feature<Geometry> | undefined> {
    return (this.filterService as FahrradrouteFilterService).getById(id).then(this.convertDetailsToFeature);
  }
}
