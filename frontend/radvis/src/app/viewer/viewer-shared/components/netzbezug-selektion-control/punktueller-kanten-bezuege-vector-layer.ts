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

import Feature from 'ol/Feature';
import { Geometry, Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { PunktuellerKantenNetzBezug } from 'src/app/viewer/viewer-shared/models/punktueller-kanten-netzbezug';
import { puntuelleKantenBezuegeVectorLayerZIndex } from 'src/app/viewer/viewer-shared/models/viewer-layer-zindex-config';

export class PunktuellerKantenBezuegeVectorLayer extends VectorLayer {
  public static KANTE_ID_PROPERTY_KEY = 'kanteId';
  public static LINEARE_REFERENZ_PROPERTY_KEY = 'lineareReferenz';

  constructor(minZoom: number | undefined) {
    super({
      source: new VectorSource(),
      // @ts-expect-error Migration von ts-ignore
      renderOrder: null,
      style: PunktuellerKantenBezuegeVectorLayer.getPointReferenceStyle(),
      minZoom,
      zIndex: puntuelleKantenBezuegeVectorLayerZIndex,
    });
  }

  private static getPointReferenceStyle(): Style {
    return new Style({
      image: MapStyles.circle(6, MapStyles.FEATURE_SELECT_COLOR),
    });
  }

  public hasFeature(f: Feature<Geometry>): boolean {
    return this.getSource().hasFeature(f);
  }

  public updatePuntuelleKantenNetzbezuege(punktuelleKantenNetzBezuege: PunktuellerKantenNetzBezug[]): void {
    const features = punktuelleKantenNetzBezuege.map(pKNB => {
      const feature = new Feature(new Point(pKNB.geometrie.coordinates));
      feature.set(PunktuellerKantenBezuegeVectorLayer.KANTE_ID_PROPERTY_KEY, pKNB.kanteId);
      feature.set(PunktuellerKantenBezuegeVectorLayer.LINEARE_REFERENZ_PROPERTY_KEY, pKNB.lineareReferenz);
      return feature;
    });
    this.getSource().clear();
    this.getSource().addFeatures(features);
  }
}
