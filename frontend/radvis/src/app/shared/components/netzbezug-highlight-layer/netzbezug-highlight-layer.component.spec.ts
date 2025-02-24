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

import { SimpleChange } from '@angular/core';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { LineString } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import {
  defaultKantenGeometrie,
  defaultKnotenGeometrie,
} from 'src/app/shared/models/geometrie-test-data-provider.spec';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { NetzbezugHighlightLayerComponent } from 'src/app/shared/components/netzbezug-highlight-layer/netzbezug-highlight-layer.component';
import { KantenSeitenbezug, Netzbezug } from 'src/app/shared/models/netzbezug';
import { defaultNetzbezug } from 'src/app/shared/models/netzbezug-test-data-provider.spec';
import { NetzAusblendenService } from 'src/app/shared/services/netz-ausblenden.service';
import { anything, capture, instance, mock, resetCalls, verify } from 'ts-mockito';

describe(NetzbezugHighlightLayerComponent.name, () => {
  let fixture: MockedComponentFixture<NetzbezugHighlightLayerComponent, { layerId: string; netzbezug: Netzbezug }>;
  let component: NetzbezugHighlightLayerComponent;
  let netzausblendenService: NetzAusblendenService;
  let olMapService: OlMapService;

  let updatePuntuelleKantenNetzbezuegeSpy: jasmine.Spy;

  beforeEach(() => {
    netzausblendenService = mock(NetzAusblendenService);
    olMapService = mock(OlMapComponent);
    return MockBuilder(NetzbezugHighlightLayerComponent)
      .provide({
        provide: NetzAusblendenService,
        useValue: instance(netzausblendenService),
      })
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      });
  });

  beforeEach(() => {
    const inputs = {
      netzbezug: defaultNetzbezug,
      layerId: 'netzbezugHighlightLayer',
    };
    fixture = MockRender(NetzbezugHighlightLayerComponent, inputs);

    component = fixture.point.componentInstance;
    updatePuntuelleKantenNetzbezuegeSpy = spyOn(
      component['punktuellerKantenBezuegeVectorLayer'],
      'updatePuntuelleKantenNetzbezuege'
    );
    fixture.detectChanges();
  });

  describe('onInit', () => {
    it('should add layers', () => {
      verify(olMapService.addLayer(component['kantenUndKnotenHighlightLayer'])).once();
      verify(olMapService.addLayer(component['punktuellerKantenBezuegeVectorLayer'])).once();
      expect().nothing();
    });
  });

  describe('onDestroy', () => {
    let kantenUndKnotenHighlightLayer: VectorLayer;
    let punktuelleKantenNetzbezuegeLayer: VectorLayer;
    beforeEach(() => {
      kantenUndKnotenHighlightLayer = component['kantenUndKnotenHighlightLayer'];
      punktuelleKantenNetzbezuegeLayer = component['punktuellerKantenBezuegeVectorLayer'];

      component.ngOnDestroy();
    });

    it('should remove layers', () => {
      verify(olMapService.removeLayer(kantenUndKnotenHighlightLayer)).once();
      verify(olMapService.removeLayer(punktuelleKantenNetzbezuegeLayer)).once();
      expect().nothing();
    });
  });

  describe('ngOnChanges', () => {
    it('should current Netzbezug ausblenden, previous einblenden', () => {
      const nextKnotenId = 23;
      const nextKantenId = 10;
      const previousKnotenId = 563;
      const previousKantenId = 668;

      // Der Input wird schon einmal beim setUp gesetzt, worauf hin ngOnChanges aufgerufen wird,
      // was wiederum kantenEin- und Ausblenden des netzausblendenService aufruft.
      resetCalls(netzausblendenService);

      inputNetzbezug(
        {
          knotenBezug: [
            {
              geometrie: defaultKnotenGeometrie,
              knotenId: nextKnotenId,
            },
          ],
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              kanteId: nextKantenId,
              geometrie: defaultKantenGeometrie,
            },
          ],
          punktuellerKantenBezug: defaultNetzbezug.punktuellerKantenBezug,
        },
        {
          knotenBezug: [
            {
              geometrie: defaultKnotenGeometrie,
              knotenId: previousKnotenId,
            },
          ],
          kantenBezug: [
            {
              ...defaultNetzbezug.kantenBezug[0],
              kanteId: previousKantenId,
              geometrie: defaultKantenGeometrie,
            },
          ],
          punktuellerKantenBezug: [],
        }
      );

      verify(netzausblendenService.kanteEinblenden(anything())).once();
      expect(capture(netzausblendenService.kanteEinblenden).last()[0]).toBe(previousKantenId);

      verify(netzausblendenService.kanteAusblenden(anything())).once();
      expect(capture(netzausblendenService.kanteAusblenden).last()[0]).toBe(nextKantenId);

      verify(netzausblendenService.knotenEinblenden(anything())).once();
      expect(capture(netzausblendenService.knotenEinblenden).last()[0]).toBe(previousKnotenId);

      verify(netzausblendenService.knotenAusblenden(anything())).once();
      expect(capture(netzausblendenService.knotenAusblenden).last()[0]).toBe(nextKnotenId);
    });

    it('should not einblenden if first change', () => {
      // Der initiale Netzbezug beim Erstellen der Component wird als input im setup zu
      // defaultNetzbezug gesetzt

      verify(netzausblendenService.knotenAusblenden(anything())).once();
      expect(capture(netzausblendenService.knotenAusblenden).last()[0]).toBe(defaultNetzbezug.knotenBezug[0].knotenId);

      verify(netzausblendenService.kanteAusblenden(anything())).once();
      expect(capture(netzausblendenService.kanteAusblenden).last()[0]).toBe(defaultNetzbezug.kantenBezug[0].kanteId);

      verify(netzausblendenService.knotenEinblenden(anything())).never();
      verify(netzausblendenService.kanteEinblenden(anything())).never();
    });

    it('should update punktuellenKantenBezug', () => {
      // spy funktioniert nur nach initialisierung, daher hier nochmal onChanges aufruf
      component.ngOnChanges({ netzbezug: new SimpleChange(undefined, defaultNetzbezug, true) });
      expect(updatePuntuelleKantenNetzbezuegeSpy).toHaveBeenCalledWith(defaultNetzbezug.punktuellerKantenBezug);
    });

    it('should respect linear referenz', () => {
      inputNetzbezug({
        knotenBezug: [],
        punktuellerKantenBezug: [],
        kantenBezug: [
          {
            ...defaultNetzbezug.kantenBezug[0],
            geometrie: {
              coordinates: [
                [0, 0],
                [0, 100],
              ],
              type: 'LineString',
            },
            linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 },
          },
        ],
      });
      const features = component['kantenUndKnotenHighlightSource'].getFeatures();
      expect(features).toHaveSize(3);
      expect((features[0].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 0],
        [0, 30],
      ]);
      expect(features[0].get(NetzbezugHighlightLayerComponent['HIGHLIGHTED_PROPERTY'])).toBeFalse();
      expect((features[1].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 30],
        [0, 70],
      ]);
      expect(features[1].get(NetzbezugHighlightLayerComponent['HIGHLIGHTED_PROPERTY'])).toBeTrue();
      expect((features[2].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 70],
        [0, 100],
      ]);
      expect(features[2].get(NetzbezugHighlightLayerComponent['HIGHLIGHTED_PROPERTY'])).toBeFalse();
    });

    it('should respect linear referenz und seitenbezug', () => {
      inputNetzbezug({
        knotenBezug: [],
        punktuellerKantenBezug: [],
        kantenBezug: [
          {
            ...defaultNetzbezug.kantenBezug[0],
            geometrie: {
              coordinates: [
                [0, 0],
                [0, 100],
              ],
              type: 'LineString',
            },
            linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 },
            kantenSeite: KantenSeitenbezug.LINKS,
          },
          {
            ...defaultNetzbezug.kantenBezug[0],
            geometrie: {
              coordinates: [
                [0, 0],
                [0, 100],
              ],
              type: 'LineString',
            },
            linearReferenzierterAbschnitt: { von: 0.1, bis: 0.4 },
            kantenSeite: KantenSeitenbezug.RECHTS,
          },
          {
            ...defaultNetzbezug.kantenBezug[0],
            geometrie: {
              coordinates: [
                [0, 0],
                [0, 100],
              ],
              type: 'LineString',
            },
            linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
            kantenSeite: KantenSeitenbezug.BEIDSEITIG,
          },
        ],
      });
      const features = component['kantenUndKnotenHighlightSource'].getFeatures();
      expect(features).toHaveSize(7);

      const featuresLinks = features.filter(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME) === KantenSeite.LINKS);
      expect(featuresLinks).toHaveSize(3);
      expect((featuresLinks[0].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 0],
        [0, 30],
      ]);
      expect(featuresLinks[0].get(NetzbezugHighlightLayerComponent['HIGHLIGHTED_PROPERTY'])).toBeFalse();
      expect((featuresLinks[1].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 30],
        [0, 70],
      ]);
      expect(featuresLinks[1].get(NetzbezugHighlightLayerComponent['HIGHLIGHTED_PROPERTY'])).toBeTrue();
      expect((featuresLinks[2].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 70],
        [0, 100],
      ]);
      expect(featuresLinks[2].get(NetzbezugHighlightLayerComponent['HIGHLIGHTED_PROPERTY'])).toBeTrue();

      const featuresRechts = features.filter(f => f.get(FeatureProperties.SEITE_PROPERTY_NAME) === KantenSeite.RECHTS);
      expect(featuresRechts).toHaveSize(4);
      expect((featuresRechts[0].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 0],
        [0, 10],
      ]);
      expect(featuresRechts[0].get(NetzbezugHighlightLayerComponent['HIGHLIGHTED_PROPERTY'])).toBeFalse();
      expect((featuresRechts[1].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 10],
        [0, 40],
      ]);
      expect(featuresRechts[1].get(NetzbezugHighlightLayerComponent['HIGHLIGHTED_PROPERTY'])).toBeTrue();
      expect((featuresRechts[2].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 40],
        [0, 70],
      ]);
      expect(featuresRechts[2].get(NetzbezugHighlightLayerComponent['HIGHLIGHTED_PROPERTY'])).toBeFalse();
      expect((featuresRechts[3].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 70],
        [0, 100],
      ]);
      expect(featuresRechts[3].get(NetzbezugHighlightLayerComponent['HIGHLIGHTED_PROPERTY'])).toBeTrue();
    });
  });

  const inputNetzbezug = (next: Netzbezug, prev: Netzbezug | null = null): void => {
    component.netzbezug = next;
    component.ngOnChanges({ netzbezug: new SimpleChange(prev, next, prev === null) });
  };
});
