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
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { KantenNetzVectorlayer } from 'src/app/shared/components/netzbezug-selektion-control/kanten-netz-vectorlayer';
import { anything, deepEqual, instance, mock, strictEqual, verify, when } from 'ts-mockito';

describe('KantenNetzVectorlayer', () => {
  let kantenNetzVectorLayer: KantenNetzVectorlayer;
  let radvisNetzFeatureService: NetzausschnittService;
  let errorHandlingService: ErrorHandlingService;

  const kantenViewSubject = new Subject<GeoJSONFeatureCollection>();

  beforeEach(() => {
    radvisNetzFeatureService = mock(NetzausschnittService);
    errorHandlingService = mock(ErrorHandlingService);

    when(radvisNetzFeatureService.getKantenForView(anything(), anything(), anything())).thenReturn(kantenViewSubject);

    kantenNetzVectorLayer = new KantenNetzVectorlayer(
      instance(radvisNetzFeatureService),
      instance(errorHandlingService),
      false,
      undefined
    );
  });

  describe('toggleHighlightKante', () => {
    beforeEach(() => {
      kantenNetzVectorLayer.toggleHighlightKante(2);
    });
    it('unhighlight existing highlighted Kante', () => {
      kantenNetzVectorLayer.getSource().loadFeatures([0, 0, 20, 20], 10, new Projection({ code: '' }));
      kantenViewSubject.next(createDummyRadVisFeatureCollection());
      expect(
        kantenNetzVectorLayer.getSource().getFeatureById(2).get(KantenNetzVectorlayer['HIGHLIGHT_PROPERTY'])
      ).toBeTrue();
      kantenNetzVectorLayer.toggleHighlightKante(2);
      expect(
        kantenNetzVectorLayer.getSource().getFeatureById(2).get(KantenNetzVectorlayer['HIGHLIGHT_PROPERTY'])
      ).toBeFalsy();
    });
    it('highlight existing unhighlighted Kante', () => {
      kantenNetzVectorLayer.getSource().loadFeatures([0, 0, 20, 20], 10, new Projection({ code: '' }));
      kantenViewSubject.next(createDummyRadVisFeatureCollection());
      expect(
        kantenNetzVectorLayer.getSource().getFeatureById(1).get(KantenNetzVectorlayer['HIGHLIGHT_PROPERTY'])
      ).toBeFalsy();
      kantenNetzVectorLayer.toggleHighlightKante(1);
      expect(
        kantenNetzVectorLayer.getSource().getFeatureById(1).get(KantenNetzVectorlayer['HIGHLIGHT_PROPERTY'])
      ).toBeTrue();
    });
    it('should apply toggled highlighting to non-existing kanten when they are loaded', () => {
      kantenNetzVectorLayer.getSource().loadFeatures([0, 0, 20, 20], 10, new Projection({ code: '' }));
      kantenNetzVectorLayer.toggleHighlightKante(1);
      kantenNetzVectorLayer.toggleHighlightKante(2);
      kantenViewSubject.next(createDummyRadVisFeatureCollection());
      verify(
        radvisNetzFeatureService.getKantenForView(anything(), deepEqual(Netzklassefilter.getAll()), strictEqual(false))
      ).once();
      expect(
        kantenNetzVectorLayer.getSource().getFeatureById(2).get(KantenNetzVectorlayer['HIGHLIGHT_PROPERTY'])
      ).toBeFalsy();
      expect(
        kantenNetzVectorLayer.getSource().getFeatureById(1).get(KantenNetzVectorlayer['HIGHLIGHT_PROPERTY'])
      ).toBeTrue();
    });
  });

  describe('kante ein/ausblenden', () => {
    it('should ein und ausblenden', () => {
      kantenNetzVectorLayer.getSource().loadFeatures([0, 0, 20, 20], 10, new Projection({ code: '' }));
      kantenViewSubject.next(createDummyRadVisFeatureCollection());

      kantenNetzVectorLayer.kanteAusblenden(2);
      expect(kantenNetzVectorLayer.getSource().getFeatureById(2).getStyle()).toBeDefined();

      kantenNetzVectorLayer.kanteEinblenden(2);
      expect(kantenNetzVectorLayer.getSource().getFeatureById(2).getStyle()).toBeFalsy();
    });

    it('should consider ausgeblendete Kanten after initialization', () => {
      kantenNetzVectorLayer.getSource().loadFeatures([0, 0, 20, 20], 10, new Projection({ code: '' }));
      kantenNetzVectorLayer.kanteAusblenden(1, 2);
      kantenNetzVectorLayer.kanteEinblenden(1);

      kantenViewSubject.next(createDummyRadVisFeatureCollection());

      expect(kantenNetzVectorLayer.getSource().getFeatureById(2).getStyle()).toBeDefined();
      expect(kantenNetzVectorLayer.getSource().getFeatureById(1).getStyle()).toBeFalsy();
    });
  });

  describe('zweiseitigeNetzanzeige', () => {
    it('should load and update Features correctly', () => {
      kantenNetzVectorLayer.setZweiseitigeNetzanzeige(true);

      kantenNetzVectorLayer.getSource().loadFeatures([0, 0, 20, 20], 10, new Projection({ code: '' }));
      kantenViewSubject.next(createDummyRadVisFeatureCollection());

      expect(kantenNetzVectorLayer.getSource().getFeatures().length).toEqual(
        createDummyRadVisFeatureCollection().features.length * 2
      );

      kantenNetzVectorLayer.setZweiseitigeNetzanzeige(false);
      kantenViewSubject.next(createDummyRadVisFeatureCollection());

      expect(kantenNetzVectorLayer.getSource().getFeatures().length).toEqual(
        createDummyRadVisFeatureCollection().features.length
      );
    });

    it('should set zweiseitigkeit correctly on Features', () => {
      kantenNetzVectorLayer.setZweiseitigeNetzanzeige(true);

      kantenNetzVectorLayer.getSource().loadFeatures([0, 0, 20, 20], 10, new Projection({ code: '' }));
      kantenViewSubject.next(createDummyRadVisFeatureCollection());

      const features = kantenNetzVectorLayer.getSource().getFeatures();
      expect(features.map(f => f.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).every(b => b)).toBeTrue();
      expect(
        features.map(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME)).filter(n => n === KantenSeite.LINKS).length
      ).toEqual(2);
      expect(
        features.map(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME)).filter(n => n === KantenSeite.RECHTS).length
      ).toEqual(2);
      expect(new Set(features.map(f => f.get(FeatureProperties.KANTE_ID_PROPERTY_NAME)))).toEqual(new Set(['1', '2']));

      kantenNetzVectorLayer.setZweiseitigeNetzanzeige(false);
      kantenViewSubject.next(createDummyRadVisFeatureCollection());

      const features2 = kantenNetzVectorLayer.getSource().getFeatures();
      expect(features2.map(f => f.get(FeatureProperties.ZWEISEITIG_PROPERTY_NAME)).every(b => !b)).toBeTrue();
      expect(
        features.map(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME)).filter(n => n === undefined).length
      ).toEqual(2);
      expect(features.map(f => f.get(FeatureProperties.KANTE_ID_PROPERTY_NAME))).toEqual(['1', '2']);
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
          type: 'LineString',
          coordinates: [
            [1, 1],
            [2, 2],
          ],
        },
        id: '1',
      },
      {
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: [
            [3, 3],
            [4, 4],
          ],
        },
        id: '2',
      },
    ],
  } as GeoJSONFeatureCollection;
};
