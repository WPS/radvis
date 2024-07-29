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

import { Feature } from 'ol';
import GeoJSON, { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Geometry } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import Style, { StyleFunction } from 'ol/style/Style';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { getRadvisNetzStyleFunction } from 'src/app/shared/models/radvis-netz-style';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { createVectorSource } from 'src/app/shared/services/vector-source.factory';
import { kantenNetzVectorlayerZIndex } from 'src/app/shared/models/shared-layer-zindex-config';

export class KantenNetzVectorlayer extends VectorLayer {
  private static HIGHLIGHT_PROPERTY = 'highlighted';

  private highlightedKanteIds: Set<number>;
  private ausgeblendeteKantenIds: Set<number>;
  private zweiseitigeNetzanzeige: boolean;

  constructor(
    private radVisNetzFeatureService: NetzausschnittService,
    private errorHandlingService: ErrorHandlingService,
    zweiseitigeNetzanzeige = false,
    minZoom: number | undefined
  ) {
    super({
      source: undefined,
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: getRadvisNetzStyleFunction(KantenNetzVectorlayer.highlightStyle),
      minZoom,
      zIndex: kantenNetzVectorlayerZIndex,
    });
    this.highlightedKanteIds = new Set();
    this.ausgeblendeteKantenIds = new Set();
    this.setSource(this.createVectorSource());
    this.zweiseitigeNetzanzeige = zweiseitigeNetzanzeige;
  }

  private static highlightStyle: StyleFunction = (feature, resolution) => {
    if (feature.getProperties().highlighted) {
      return MapStyles.getDefaultHighlightStyle(MapStyles.FEATURE_SELECT_COLOR);
    } else {
      return MapStyles.getDefaultNetzStyleFunction()(feature, resolution);
    }
  };

  public setZweiseitigeNetzanzeige(anzeige: boolean): void {
    const changed = anzeige !== this.zweiseitigeNetzanzeige;

    this.zweiseitigeNetzanzeige = anzeige;

    if (changed) {
      this.getSource().refresh();
    }
  }

  public kanteAusblenden(...ids: number[]): void {
    const features = this.getSource()
      .getFeatures()
      .filter(f => ids.includes(+(f.get(FeatureProperties.KANTE_ID_PROPERTY_NAME) as number)));

    features.forEach(f => {
      f.setStyle(new Style());
      f.changed();
    });
    ids.forEach(id => {
      this.ausgeblendeteKantenIds.add(id);
    });
  }

  public kanteEinblenden(...ids: number[]): void {
    const features = this.getSource()
      .getFeatures()
      .filter(f => ids.includes(+(f.get(FeatureProperties.KANTE_ID_PROPERTY_NAME) as number)));

    features.forEach(f => {
      f.setStyle(undefined);
      f.changed();
    });
    ids.forEach(id => {
      this.ausgeblendeteKantenIds.delete(id);
    });
  }

  public hasFeature(f: Feature<Geometry>): boolean {
    return this.getSource().hasFeature(f);
  }

  public toggleHighlightKante(id: number): void {
    const features = this.getSource()
      .getFeatures()
      .filter(f => +(f.get(FeatureProperties.KANTE_ID_PROPERTY_NAME) as number) === id);
    features.forEach(feature => {
      feature.set(
        KantenNetzVectorlayer.HIGHLIGHT_PROPERTY,
        !feature.get(KantenNetzVectorlayer.HIGHLIGHT_PROPERTY) ?? true
      );
      feature.changed();
    });
    if (this.highlightedKanteIds.has(id)) {
      this.highlightedKanteIds.delete(id);
    } else {
      this.highlightedKanteIds.add(id);
    }
  }

  private createVectorSource(): VectorSource {
    return createVectorSource({
      getFeaturesObservable: extent =>
        this.radVisNetzFeatureService.getKantenForView(extent, Netzklassefilter.getAll(), Boolean(false)),
      parseFeatures: featureCollection => this.parseFeatures(featureCollection),
      onFeaturesLoaded: () => {},
      onError: error => this.errorHandlingService.handleError(error),
    });
  }

  private parseFeatures(featureCollection: GeoJSONFeatureCollection): Feature<Geometry>[] {
    return new GeoJSON().readFeatures(featureCollection).flatMap(feature => {
      feature.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, feature.getId());

      if (this.highlightedKanteIds.has(+(feature.getId() as number))) {
        feature.set(KantenNetzVectorlayer.HIGHLIGHT_PROPERTY, true);
      }

      if (this.ausgeblendeteKantenIds.has(+(feature.getId() as number))) {
        feature.setStyle(new Style());
      }

      // Feature hat keine Seitenattribute
      if (!this.zweiseitigeNetzanzeige) {
        return [feature];
      }

      feature.set(FeatureProperties.ZWEISEITIG_PROPERTY_NAME, true);

      // Bei Zweiseitigeit haben Kanten keine feature.ids !
      const featureLinks = feature.clone();
      featureLinks.set(FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.LINKS);

      const featureRechts = feature.clone();
      featureRechts.set(FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.RECHTS);

      // Verlauf wird hier NICHT gesetzt
      return [featureLinks, featureRechts];
    });
  }
}
