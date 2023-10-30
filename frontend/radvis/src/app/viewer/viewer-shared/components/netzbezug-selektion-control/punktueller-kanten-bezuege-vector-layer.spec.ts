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

/* eslint-disable @typescript-eslint/dot-notation */

import { Point } from 'ol/geom';
import { PunktuellerKantenBezuegeVectorLayer } from 'src/app/viewer/viewer-shared/components/netzbezug-selektion-control/punktueller-kanten-bezuege-vector-layer';
import { defaultNetzbezug } from 'src/app/viewer/viewer-shared/models/netzbezug-test-data-provider.spec';

describe(PunktuellerKantenBezuegeVectorLayer.name, () => {
  let punktuellerKantenBezuegeVectorLayer: PunktuellerKantenBezuegeVectorLayer;

  beforeEach(() => {
    punktuellerKantenBezuegeVectorLayer = new PunktuellerKantenBezuegeVectorLayer(undefined);
  });

  describe('updatePuntuelleKantenNetzbezuege', () => {
    beforeEach(() => {
      punktuellerKantenBezuegeVectorLayer.updatePuntuelleKantenNetzbezuege(defaultNetzbezug.punktuellerKantenBezug);
    });
    it('should add new features', () => {
      expect(punktuellerKantenBezuegeVectorLayer.getSource().getFeatures().length).toEqual(1);
      expect(
        (punktuellerKantenBezuegeVectorLayer
          .getSource()
          .getFeatures()
          .filter(feature => feature.get(PunktuellerKantenBezuegeVectorLayer.KANTE_ID_PROPERTY_KEY) === 3)[0]
          .getGeometry() as Point).getCoordinates()
      ).toEqual(defaultNetzbezug.punktuellerKantenBezug[0].geometrie.coordinates);
    });
    it('should remove old features', () => {
      punktuellerKantenBezuegeVectorLayer.updatePuntuelleKantenNetzbezuege([]);
      expect(punktuellerKantenBezuegeVectorLayer.getSource().getFeatures().length).toEqual(0);
    });
  });
});
