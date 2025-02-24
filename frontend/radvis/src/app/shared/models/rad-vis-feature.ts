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

import Geometry from 'ol/geom/Geometry';
import GeometryType from 'ol/geom/GeometryType';
import { LayerId } from 'src/app/shared/models/layers/rad-vis-layer';
import { RadVisFeatureAttributes } from 'src/app/shared/models/rad-vis-feature-attributes';

export class RadVisFeature {
  constructor(
    public id: number | null,
    public attributes: RadVisFeatureAttributes,
    public layer: LayerId,
    public geometry: Geometry,
    public istStrecke = false,
    public kanteZweiseitig = false
  ) {}

  public static ofAttributesMap(
    id: number | null,
    attributes: { [key: string]: any },
    layer: LayerId,
    geometry: Geometry
  ): RadVisFeature {
    return new RadVisFeature(
      id,
      new RadVisFeatureAttributes(attributes),
      layer,
      geometry,
      attributes.istStrecke || false,
      attributes.kanteZweiseitig || false
    );
  }

  public get isKnoten(): boolean {
    return this.geometry.getType() === GeometryType.POINT;
  }
}
