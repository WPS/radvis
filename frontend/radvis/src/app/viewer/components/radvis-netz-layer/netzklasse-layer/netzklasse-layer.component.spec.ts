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

import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { LineString, MultiLineString } from 'ol/geom';
import { of, Subject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { toRadVisFeatureAttributesFromMap } from 'src/app/shared/models/rad-vis-feature-attributes';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { NetzklasseLayerComponent } from 'src/app/viewer/components/radvis-netz-layer/netzklasse-layer/netzklasse-layer.component';
import { RADVIS_NETZ_LAYER_PREFIX } from 'src/app/viewer/viewer-shared/models/radvis-netz-layer-prefix';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, instance, mock, when } from 'ts-mockito';

describe(NetzklasseLayerComponent.name, () => {
  let fixture: MockedComponentFixture<
    NetzklasseLayerComponent,
    {
      netzklasse: Netzklassefilter;
      layerPrefix: string;
      mitVerlauf: boolean | null;
    }
  >;
  let component: NetzklasseLayerComponent;

  let radVisNetzFeatureService: NetzausschnittService;
  let errorHandlingService: ErrorHandlingService;
  let netzAusblendenService: NetzAusblendenService;
  let featureHighlightService: FeatureHighlightService;
  let olMapService: OlMapService;

  const kanteAusblenden$ = new Subject<number>();
  const kanteEinblenden$ = new Subject<number>();
  const highlightFeatureSubject = new Subject<RadVisFeature>();
  const unhighlightFeatureSubject = new Subject<RadVisFeature>();

  const featureCollection = {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        id: '1',
        properties: {
          kanteZweiseitig: false,
        },
        geometry: {
          type: 'LineString',
          coordinates: [
            [1, 1],
            [2, 2],
            [3, 3],
          ],
        },
      },
      {
        type: 'Feature',
        id: '2',
        properties: {
          kanteZweiseitig: true,
        },
        geometry: {
          type: 'LineString',
          coordinates: [
            [4, 4],
            [5, 5],
          ],
        },
      },
      {
        type: 'Feature',
        id: '3',
        properties: {
          kanteZweiseitig: true,
        },
        geometry: {
          type: 'MultiLineString',
          coordinates: [
            [
              [5, 5],
              [6, 6],
            ],
            [
              [7, 7],
              [8, 8],
            ],
          ],
        },
      },
    ],
  } as GeoJSONFeatureCollection;

  beforeEach(() => {
    radVisNetzFeatureService = mock(NetzausschnittService);
    errorHandlingService = mock(ErrorHandlingService);
    netzAusblendenService = mock(NetzAusblendenService);
    featureHighlightService = mock(FeatureHighlightService);
    olMapService = mock(OlMapComponent);

    when(netzAusblendenService.kanteAusblenden$).thenReturn(kanteAusblenden$.asObservable());
    when(netzAusblendenService.kanteEinblenden$).thenReturn(kanteEinblenden$.asObservable());
    when(netzAusblendenService.ausgeblendeteKanten).thenReturn([]);
    when(featureHighlightService.highlightedFeature$).thenReturn(highlightFeatureSubject.asObservable());
    when(featureHighlightService.unhighlightedFeature$).thenReturn(unhighlightFeatureSubject.asObservable());
    when(radVisNetzFeatureService.getKantenForView(anything(), anything(), anything())).thenReturn(
      of(featureCollection)
    );

    return MockBuilder(NetzklasseLayerComponent, ViewerModule)
      .provide({
        provide: NetzausschnittService,
        useValue: instance(radVisNetzFeatureService),
      })
      .provide({
        provide: ErrorHandlingService,
        useValue: instance(errorHandlingService),
      })
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({
        provide: NetzAusblendenService,
        useValue: instance(netzAusblendenService),
      })
      .provide({
        provide: FeatureHighlightService,
        useValue: instance(featureHighlightService),
      });
  });

  beforeEach(() => {
    const inputs = {
      netzklasse: Netzklassefilter.RADNETZ,
      layerPrefix: 'RadVisNetz_',
      mitVerlauf: false,
    };
    fixture = MockRender(NetzklasseLayerComponent, inputs);

    component = fixture.point.componentInstance;

    // @ts-expect-error Migration von ts-ignore
    component['olLayer'].getSource()['loader_']();

    fixture.detectChanges();
  });

  describe('highlight from highlightService for RADNETZ Layer', () => {
    it('should set feature properties correctly when highlightService triggers', () => {
      const isFeatureHighlighted = (radVisFeature: RadVisFeature): boolean => {
        const id = radVisFeature.id || radVisFeature.attributes.get(FeatureProperties.KANTE_ID_PROPERTY_NAME);
        const seite = radVisFeature.attributes.get(FeatureProperties.SEITE_PROPERTY_NAME);

        return component['getFeaturesByIdsAndSeitenbezug'](id, seite)[0].get('highlighted') === true;
      };

      const feature1 = new RadVisFeature(
        1,
        toRadVisFeatureAttributesFromMap([[FeatureProperties.KANTE_ID_PROPERTY_NAME, 1]]),
        `${RADVIS_NETZ_LAYER_PREFIX}${Netzklassefilter.RADNETZ}`,
        new LineString([])
      );
      const feature2Links = new RadVisFeature(
        null,
        toRadVisFeatureAttributesFromMap([
          [FeatureProperties.KANTE_ID_PROPERTY_NAME, 2],
          [FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.LINKS],
        ]),
        `${RADVIS_NETZ_LAYER_PREFIX}${Netzklassefilter.RADNETZ}`,
        new LineString([])
      );
      const feature2Rechts = new RadVisFeature(
        null,
        toRadVisFeatureAttributesFromMap([
          [FeatureProperties.KANTE_ID_PROPERTY_NAME, 2],
          [FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.RECHTS],
        ]),
        `${RADVIS_NETZ_LAYER_PREFIX}${Netzklassefilter.RADNETZ}`,
        new LineString([])
      );
      const feature3Links = new RadVisFeature(
        null,
        toRadVisFeatureAttributesFromMap([
          [FeatureProperties.KANTE_ID_PROPERTY_NAME, 3],
          [FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.LINKS],
        ]),
        `${RADVIS_NETZ_LAYER_PREFIX}${Netzklassefilter.RADNETZ}`,
        new MultiLineString([])
      );
      const feature3Rechts = new RadVisFeature(
        null,
        toRadVisFeatureAttributesFromMap([
          [FeatureProperties.KANTE_ID_PROPERTY_NAME, 3],
          [FeatureProperties.SEITE_PROPERTY_NAME, KantenSeite.RECHTS],
        ]),
        `${RADVIS_NETZ_LAYER_PREFIX}${Netzklassefilter.RADNETZ}`,
        new MultiLineString([])
      );

      expect(isFeatureHighlighted(feature1)).toBeFalse();
      expect(isFeatureHighlighted(feature2Links)).toBeFalse();
      expect(isFeatureHighlighted(feature2Rechts)).toBeFalse();
      expect(isFeatureHighlighted(feature3Links)).toBeFalse();
      expect(isFeatureHighlighted(feature3Rechts)).toBeFalse();

      highlightFeatureSubject.next(feature1);
      expect(isFeatureHighlighted(feature1)).toBeTrue();
      expect(isFeatureHighlighted(feature2Links)).toBeFalse();
      expect(isFeatureHighlighted(feature2Rechts)).toBeFalse();
      expect(isFeatureHighlighted(feature3Links)).toBeFalse();
      expect(isFeatureHighlighted(feature3Rechts)).toBeFalse();

      unhighlightFeatureSubject.next(feature1);
      expect(isFeatureHighlighted(feature1)).toBeFalse();
      expect(isFeatureHighlighted(feature2Links)).toBeFalse();
      expect(isFeatureHighlighted(feature2Rechts)).toBeFalse();
      expect(isFeatureHighlighted(feature3Links)).toBeFalse();
      expect(isFeatureHighlighted(feature3Rechts)).toBeFalse();

      highlightFeatureSubject.next(feature2Links);
      expect(isFeatureHighlighted(feature1)).toBeFalse();
      expect(isFeatureHighlighted(feature2Links)).toBeTrue();
      expect(isFeatureHighlighted(feature2Rechts)).toBeFalse();
      expect(isFeatureHighlighted(feature3Links)).toBeFalse();
      expect(isFeatureHighlighted(feature3Rechts)).toBeFalse();

      unhighlightFeatureSubject.next(feature2Links);
      expect(isFeatureHighlighted(feature1)).toBeFalse();
      expect(isFeatureHighlighted(feature2Links)).toBeFalse();
      expect(isFeatureHighlighted(feature2Rechts)).toBeFalse();
      expect(isFeatureHighlighted(feature3Links)).toBeFalse();
      expect(isFeatureHighlighted(feature3Rechts)).toBeFalse();

      highlightFeatureSubject.next(feature2Rechts);
      expect(isFeatureHighlighted(feature1)).toBeFalse();
      expect(isFeatureHighlighted(feature2Links)).toBeFalse();
      expect(isFeatureHighlighted(feature2Rechts)).toBeTrue();
      expect(isFeatureHighlighted(feature3Links)).toBeFalse();
      expect(isFeatureHighlighted(feature3Rechts)).toBeFalse();

      unhighlightFeatureSubject.next(feature2Rechts);
      expect(isFeatureHighlighted(feature1)).toBeFalse();
      expect(isFeatureHighlighted(feature2Links)).toBeFalse();
      expect(isFeatureHighlighted(feature2Rechts)).toBeFalse();
      expect(isFeatureHighlighted(feature3Links)).toBeFalse();
      expect(isFeatureHighlighted(feature3Rechts)).toBeFalse();

      highlightFeatureSubject.next(feature3Links);
      expect(isFeatureHighlighted(feature1)).toBeFalse();
      expect(isFeatureHighlighted(feature2Links)).toBeFalse();
      expect(isFeatureHighlighted(feature2Rechts)).toBeFalse();
      expect(isFeatureHighlighted(feature3Links)).toBeTrue();
      expect(isFeatureHighlighted(feature3Rechts)).toBeFalse();

      unhighlightFeatureSubject.next(feature3Links);
      expect(isFeatureHighlighted(feature1)).toBeFalse();
      expect(isFeatureHighlighted(feature2Links)).toBeFalse();
      expect(isFeatureHighlighted(feature2Rechts)).toBeFalse();
      expect(isFeatureHighlighted(feature3Links)).toBeFalse();
      expect(isFeatureHighlighted(feature3Rechts)).toBeFalse();

      highlightFeatureSubject.next(feature3Rechts);
      expect(isFeatureHighlighted(feature1)).toBeFalse();
      expect(isFeatureHighlighted(feature2Links)).toBeFalse();
      expect(isFeatureHighlighted(feature2Rechts)).toBeFalse();
      expect(isFeatureHighlighted(feature3Links)).toBeFalse();
      expect(isFeatureHighlighted(feature3Rechts)).toBeTrue();

      unhighlightFeatureSubject.next(feature3Rechts);
      expect(isFeatureHighlighted(feature1)).toBeFalse();
      expect(isFeatureHighlighted(feature2Links)).toBeFalse();
      expect(isFeatureHighlighted(feature2Rechts)).toBeFalse();
      expect(isFeatureHighlighted(feature3Links)).toBeFalse();
      expect(isFeatureHighlighted(feature3Rechts)).toBeFalse();
    });
  });

  describe('Parsing of Features', () => {
    const features = NetzklasseLayerComponent['parseFeatures'](featureCollection);
    const [feature1, feature2Links, feature2Rechts, feature3Links, feature3Rechts] = features;

    it('should generate correct amount of features', () => {
      expect(features.length).toBe(5);
    });

    it('Should set kanteId', () => {
      expect(feature1.getId()).toEqual('1');

      // cloned features have no id
      expect(feature2Links.getId()).toEqual(undefined);
      expect(feature2Rechts.getId()).toEqual(undefined);
      expect(feature3Links.getId()).toEqual(undefined);
      expect(feature3Rechts.getId()).toEqual(undefined);

      // ids are moved into property
      expect(feature2Links.get(FeatureProperties.KANTE_ID_PROPERTY_NAME)).toEqual('2');
      expect(feature2Rechts.get(FeatureProperties.KANTE_ID_PROPERTY_NAME)).toEqual('2');
      expect(feature3Links.get(FeatureProperties.KANTE_ID_PROPERTY_NAME)).toEqual('3');
      expect(feature3Rechts.get(FeatureProperties.KANTE_ID_PROPERTY_NAME)).toEqual('3');
    });

    it('Should set Seitenbezug split left and right', () => {
      expect(feature1.get(FeatureProperties.SEITE_PROPERTY_NAME)).toEqual(undefined);
      expect(feature2Links.get(FeatureProperties.SEITE_PROPERTY_NAME)).toEqual(KantenSeite.LINKS);
      expect(feature2Rechts.get(FeatureProperties.SEITE_PROPERTY_NAME)).toEqual(KantenSeite.RECHTS);
      expect(feature3Links.get(FeatureProperties.SEITE_PROPERTY_NAME)).toEqual(KantenSeite.LINKS);
      expect(feature3Rechts.get(FeatureProperties.SEITE_PROPERTY_NAME)).toEqual(KantenSeite.RECHTS);
    });

    it('Should set geometries and split MultiLineString', () => {
      expect((feature1.getGeometry() as LineString).getCoordinates()).toEqual([
        [1, 1],
        [2, 2],
        [3, 3],
      ]);
      expect((feature2Links.getGeometry() as LineString).getCoordinates()).toEqual([
        [4, 4],
        [5, 5],
      ]);
      expect((feature2Rechts.getGeometry() as LineString).getCoordinates()).toEqual([
        [4, 4],
        [5, 5],
      ]);
      expect((feature3Links.getGeometry() as LineString).getCoordinates()).toEqual([
        [5, 5],
        [6, 6],
      ]);
      expect((feature3Rechts.getGeometry() as LineString).getCoordinates()).toEqual([
        [7, 7],
        [8, 8],
      ]);
    });
  });
});
