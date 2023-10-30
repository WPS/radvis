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

import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import Projection from 'ol/proj/Projection';
import { Subject } from 'rxjs';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { KnotenNetzVectorLayer } from 'src/app/viewer/viewer-shared/components/netzbezug-selektion-control/knoten-netz-vectorlayer';
import { anything, deepEqual, instance, mock, verify, when } from 'ts-mockito';

describe('KnotenNetzVectorLayer', () => {
  let knotenNetzVectorLayer: KnotenNetzVectorLayer;
  let radvisNetzFeatureService: NetzausschnittService;
  let errorHandlingService: ErrorHandlingService;

  const knotenViewSubject = new Subject<GeoJSONFeatureCollection>();

  beforeEach(() => {
    radvisNetzFeatureService = mock(NetzausschnittService);
    errorHandlingService = mock(ErrorHandlingService);

    when(radvisNetzFeatureService.getKnotenForView(anything(), anything())).thenReturn(knotenViewSubject);

    knotenNetzVectorLayer = new KnotenNetzVectorLayer(
      instance(radvisNetzFeatureService),
      instance(errorHandlingService),
      undefined
    );
  });

  describe('toggleHighlightKante', () => {
    beforeEach(() => {
      knotenNetzVectorLayer.toggleHighlightKnoten(2);
    });
    it('unhighlight existing highlighted Kante', () => {
      knotenNetzVectorLayer.getSource().loadFeatures([0, 0, 20, 20], 10, new Projection({ code: '' }));
      knotenViewSubject.next(createDummyRadVisFeatureCollection());
      expect(
        knotenNetzVectorLayer.getSource().getFeatureById(2).get(KnotenNetzVectorLayer['HIGHLIGHT_PROPERTY'])
      ).toBeTrue();
      knotenNetzVectorLayer.toggleHighlightKnoten(2);
      expect(
        knotenNetzVectorLayer.getSource().getFeatureById(2).get(KnotenNetzVectorLayer['HIGHLIGHT_PROPERTY'])
      ).toBeFalsy();
    });
    it('highlight existing unhighlighted Kante', () => {
      knotenNetzVectorLayer.getSource().loadFeatures([0, 0, 20, 20], 10, new Projection({ code: '' }));
      knotenViewSubject.next(createDummyRadVisFeatureCollection());
      expect(
        knotenNetzVectorLayer.getSource().getFeatureById(1).get(KnotenNetzVectorLayer['HIGHLIGHT_PROPERTY'])
      ).toBeFalsy();
      knotenNetzVectorLayer.toggleHighlightKnoten(1);
      expect(
        knotenNetzVectorLayer.getSource().getFeatureById(1).get(KnotenNetzVectorLayer['HIGHLIGHT_PROPERTY'])
      ).toBeTrue();
    });
    it('should apply toggled highlighting to non-existing knoten when they are loaded', () => {
      knotenNetzVectorLayer.getSource().loadFeatures([0, 0, 20, 20], 10, new Projection({ code: '' }));
      knotenNetzVectorLayer.toggleHighlightKnoten(1);
      knotenNetzVectorLayer.toggleHighlightKnoten(2);
      knotenViewSubject.next(createDummyRadVisFeatureCollection());
      verify(radvisNetzFeatureService.getKnotenForView(anything(), deepEqual(Netzklassefilter.getAll()))).once();
      expect(
        knotenNetzVectorLayer.getSource().getFeatureById(2).get(KnotenNetzVectorLayer['HIGHLIGHT_PROPERTY'])
      ).toBeFalsy();
      expect(
        knotenNetzVectorLayer.getSource().getFeatureById(1).get(KnotenNetzVectorLayer['HIGHLIGHT_PROPERTY'])
      ).toBeTrue();
    });
  });
});

const createDummyRadVisFeatureCollection = (): GeoJSONFeatureCollection => {
  return {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        geometry: {
          type: 'Point',
          coordinates: [1, 1],
        },
        id: '1',
      },
      {
        type: 'Feature',
        geometry: {
          type: 'Point',
          coordinates: [3, 3],
        },
        id: '2',
      },
    ],
  } as GeoJSONFeatureCollection;
};
