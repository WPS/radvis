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

import { GeoJSON } from 'ol/format';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import VectorLayer from 'ol/layer/Vector';
import { all } from 'ol/loadingstrategy';
import VectorSource from 'ol/source/Vector';
import { Observable } from 'rxjs';
import { getRadvisNetzStyleFunction } from 'src/app/shared/models/radvis-netz-style';

export class StreckenNetzVectorlayer extends VectorLayer {
  constructor(streckenProvider: () => Observable<GeoJSONFeatureCollection>, zIndex: number) {
    const vectorSource = new VectorSource({
      format: new GeoJSON(),
      useSpatialIndex: false,
      loader: (): void => {
        streckenProvider().subscribe(featureCollection => {
          const features = new GeoJSON().readFeatures(featureCollection);
          vectorSource.clear(true);
          vectorSource.addFeatures(features);
        });
      },
      strategy: all,
    });
    super({
      source: vectorSource,
      // @ts-ignore
      renderOrder: null,
      style: getRadvisNetzStyleFunction(),
      zIndex,
    });
  }
}
