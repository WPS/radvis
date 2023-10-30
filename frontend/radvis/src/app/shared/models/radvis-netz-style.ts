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

import { FeatureLike } from 'ol/Feature';
import { LineString } from 'ol/geom';
import GeometryType from 'ol/geom/GeometryType';
import { Style } from 'ol/style';
import { StyleFunction } from 'ol/style/Style';
import { isArray } from 'rxjs/internal-compatibility';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import { LineStringShifter } from 'src/app/shared/services/line-string-shifter';

const STROKE_WIDTH_IN_PIXEL = 4;
export const GAP_WIDTH_IN_PIXEL = 2;

const defaultPointLargeStyle = new Style({
  image: MapStyles.circleWithFill(5),
});

export const shiftFeature = (feature: FeatureLike, resolution: number, gapWidth: number): LineString => {
  const isLeft: boolean = feature.get(FeatureProperties.SEITE_PROPERTY_NAME) === Seitenbezug.LINKS;
  let shiftDistanceInPixel = STROKE_WIDTH_IN_PIXEL / 2 + gapWidth / 2;
  shiftDistanceInPixel = isLeft ? -shiftDistanceInPixel : shiftDistanceInPixel;
  return LineStringShifter.shiftLineStringByPixel(
    feature.getGeometry() as LineString,
    shiftDistanceInPixel,
    resolution
  );
};

export const getRadvisNetzStyleFunction = (kantenStyle?: StyleFunction, pointStyle?: StyleFunction) => {
  return (feature: FeatureLike, resolution: any): Style | Style[] => {
    if (feature.getGeometry()?.getType() === GeometryType.POINT && resolution < MapStyles.RESOLUTION_SMALL) {
      return pointStyle ? pointStyle(feature, resolution) : defaultPointLargeStyle;
    }

    const style = kantenStyle
      ? kantenStyle(feature, resolution)
      : MapStyles.getDefaultNetzStyleFunction()(feature, resolution);

    if (
      feature.getProperties()[FeatureProperties.ZWEISEITIG_PROPERTY_NAME] &&
      !feature.getProperties()[FeatureProperties.VERLAUF_PROPERTY_NAME]
    ) {
      // Um bei einer zweiseitigen Kante die verschobene Linie korrekt darzustellen (und nicht etwa die Originalgeometrie
      // zu nutzen), müssen wir diese hier setzen. Hat aber der Style bereits eine Geometrie (wie vor allem bei dem Pfeil
      // der Fall), machen wir nichts.
      if (isArray(style)) {
        style
          .filter(s => s.getGeometry() == null)
          .forEach(s => s.setGeometry(shiftFeature(feature, resolution, GAP_WIDTH_IN_PIXEL)));
      } else if (style.getGeometry() == null) {
        style.setGeometry(shiftFeature(feature, resolution, GAP_WIDTH_IN_PIXEL));
      }
    }

    return style;
  };
};
