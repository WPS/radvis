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
import GeoJSON from 'ol/format/GeoJSON';
import { Geometry } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { StyleFunction } from 'ol/style/Style';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { getRadvisNetzStyleFunction } from 'src/app/shared/models/radvis-netz-style';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { createVectorSource } from 'src/app/shared/services/vector-source.factory';
import { knotenNetzVectorlayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';

export class KnotenNetzVectorLayer extends VectorLayer {
  private static readonly HIGHLIGHT_PROPERTY = 'highlighted';

  private highlightedKnotenIDs: Set<number>;

  constructor(
    private radVisNetzFeatureService: NetzausschnittService,
    private errorHandlingService: ErrorHandlingService,
    private minZoom: number | undefined
  ) {
    super({
      source: undefined,
      // @ts-ignore
      renderOrder: null,
      style: getRadvisNetzStyleFunction(undefined, KnotenNetzVectorLayer.highlightStyle),
      minZoom,
      zIndex: knotenNetzVectorlayerZIndex,
    });
    this.highlightedKnotenIDs = new Set();

    this.setSource(this.createVectorSource());
  }

  private static highlightStyle: StyleFunction = (feature, resolution) => {
    if (feature.get(KnotenNetzVectorLayer.HIGHLIGHT_PROPERTY) === true) {
      return MapStyles.getDefaultHighlightStyle(MapStyles.FEATURE_SELECT_COLOR);
    } else {
      return MapStyles.getDefaultNetzStyleFunction()(feature, resolution);
    }
  };

  public toggleHighlightKnoten(id: number): void {
    const feature = this.getSource().getFeatureById(id);
    if (feature) {
      feature.set(
        KnotenNetzVectorLayer.HIGHLIGHT_PROPERTY,
        !feature.get(KnotenNetzVectorLayer.HIGHLIGHT_PROPERTY) ?? true
      );
      feature.changed();
    }
    if (this.highlightedKnotenIDs.has(id)) {
      this.highlightedKnotenIDs.delete(id);
    } else {
      this.highlightedKnotenIDs.add(id);
    }
  }

  public hasFeature(f: Feature<Geometry>): boolean {
    return this.getSource().hasFeature(f);
  }

  private createVectorSource(): VectorSource {
    return createVectorSource({
      getFeaturesObservable: extent =>
        this.radVisNetzFeatureService.getKnotenForView(extent, Netzklassefilter.getAll()),
      parseFeatures: featureCollection =>
        new GeoJSON().readFeatures(featureCollection).map(feature => {
          if (this.highlightedKnotenIDs.has(+(feature.getId() as number))) {
            feature.set(KnotenNetzVectorLayer.HIGHLIGHT_PROPERTY, true);
          }
          return feature;
        }),
      onFeaturesLoaded: () => {},
      onError: error => this.errorHandlingService.handleError(error),
    });
  }
}
