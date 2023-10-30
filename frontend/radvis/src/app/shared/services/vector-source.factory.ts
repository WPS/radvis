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
import { Extent } from 'ol/extent';
import GeoJSON, { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Geometry } from 'ol/geom';
import VectorSource from 'ol/source/Vector';
import { Observable } from 'rxjs';
import { bbox } from 'ol/loadingstrategy';

const createLoadingFunction = (
  getFeaturesObservable: (extent: Extent) => Observable<GeoJSONFeatureCollection>,
  vectorSource: VectorSource,
  parseFeatures: (featureCollection: GeoJSONFeatureCollection) => Feature<Geometry>[],
  onFeaturesLoaded: () => void,
  onError: (error: Error) => void
): ((extent: Extent, resolution: any) => void) => {
  return (extent): void => {
    getFeaturesObservable(extent).subscribe(
      featureCollection => {
        const features = parseFeatures(featureCollection);
        vectorSource.clear(true);
        vectorSource.addFeatures(features);
        onFeaturesLoaded();
      },
      error => {
        onError(error);
      }
    );
  };
};

/**
 * doLoad - für den Fall, dass aufgrund einer Condition nicht geladen werden soll, wenn OL das machen will
 * getFeaturesObservable - "normales" Holen der Features, nicht Strecken
 * parseFeatures - create Features from GeoJSON, falls sonderbehandlung für z.B. Seitenbezug notwendig
 * onFeaturesLoaded - callback, der nach dem Laden der Features ausgeführt wird
 * onError - callback, wenn Fehler beim Laden der Features auftreten
 */
interface vectorSourceOptions {
  getFeaturesObservable: (extent: Extent) => Observable<GeoJSONFeatureCollection>;
  parseFeatures: (featureCollection: GeoJSONFeatureCollection) => Feature<Geometry>[];
  onFeaturesLoaded: () => void;
  onError: (error: Error) => void;
}

export const createVectorSource = (options: vectorSourceOptions): VectorSource => {
  const vectorSource: VectorSource = new VectorSource({
    format: new GeoJSON(),
    useSpatialIndex: false,
    strategy: bbox,
  });

  vectorSource.setLoader(
    createLoadingFunction(
      options.getFeaturesObservable,
      vectorSource,
      options.parseFeatures,
      options.onFeaturesLoaded,
      options.onError
    )
  );

  return vectorSource;
};
