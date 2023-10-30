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

import { SimpleChange, SimpleChanges } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { Feature, MapBrowserEvent } from 'ol';
import { LineString, Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import { Subject } from 'rxjs';
import { defaultKnoten } from 'src/app/editor/knoten/models/knoten-test-data-provider.spec';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { defaultKantenGeometrie } from 'src/app/shared/models/geometrie-test-data-provider.spec';
import { Seitenbezug } from 'src/app/shared/models/seitenbezug';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { KantenNetzVectorlayer } from 'src/app/viewer/viewer-shared/components/netzbezug-selektion-control/kanten-netz-vectorlayer';
import { KnotenNetzVectorLayer } from 'src/app/viewer/viewer-shared/components/netzbezug-selektion-control/knoten-netz-vectorlayer';
import { NetzbezugSelektionLayerComponent } from 'src/app/viewer/viewer-shared/components/netzbezug-selektion-control/netzbezug-selektion-layer/netzbezug-selektion-layer.component';
import { PunktuellerKantenBezuegeVectorLayer } from 'src/app/viewer/viewer-shared/components/netzbezug-selektion-control/punktueller-kanten-bezuege-vector-layer';
import { KnotenNetzbezug } from 'src/app/viewer/viewer-shared/models/knoten-netzbezug';
import {
  AbschnittsweiserKantenSeitenBezug,
  KantenSeitenbezug,
  Netzbezug,
} from 'src/app/viewer/viewer-shared/models/netzbezug';
import { defaultNetzbezug } from 'src/app/viewer/viewer-shared/models/netzbezug-test-data-provider.spec';
import { PunktuellerKantenNetzBezug } from 'src/app/viewer/viewer-shared/models/punktueller-kanten-netzbezug';
import { NetzbezugAuswahlModusService } from 'src/app/viewer/viewer-shared/services/netzbezug-auswahl-modus.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, instance, mock, verify, when } from 'ts-mockito';

describe(NetzbezugSelektionLayerComponent.name, () => {
  let fixture: ComponentFixture<NetzbezugSelektionLayerComponent>;
  let component: NetzbezugSelektionLayerComponent;

  let olMapService: OlMapService;
  let notifyUserService: NotifyUserService;
  let radVisNetzFeatureService: NetzausschnittService;
  let errorHandlingService: ErrorHandlingService;
  let layerDisableService: NetzbezugAuswahlModusService;

  let onMapClick$: Subject<MapBrowserEvent<UIEvent>>;

  beforeEach(() => {
    olMapService = mock(OlMapComponent);
    notifyUserService = mock(NotifyUserService);
    radVisNetzFeatureService = mock(NetzausschnittService);
    errorHandlingService = mock(ErrorHandlingService);
    layerDisableService = mock(ViewerComponent);

    onMapClick$ = new Subject();
    when(olMapService.click$()).thenReturn(onMapClick$);

    return MockBuilder(NetzbezugSelektionLayerComponent, ViewerModule)
      .provide({ provide: OlMapService, useValue: instance(olMapService) })
      .provide({ provide: NotifyUserService, useValue: instance(notifyUserService) })
      .provide({ provide: NetzausschnittService, useValue: instance(radVisNetzFeatureService) })
      .provide({ provide: ErrorHandlingService, useValue: instance(errorHandlingService) })
      .provide({ provide: NetzbezugAuswahlModusService, useValue: instance(layerDisableService) });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NetzbezugSelektionLayerComponent);
    component = fixture.componentInstance;
    component.netzbezug = initialNetzbezug;
    fixture.detectChanges();
  });

  describe('constructor', () => {
    it('should disableLayers in viewer', () => {
      verify(layerDisableService.startNetzbezugAuswahl()).once();
      expect().nothing();
    });
  });

  describe('onInit', () => {
    it('should put layers on map', () => {
      verify(olMapService.addLayer(component['kantenNetzLayer'] as VectorLayer)).once();
      verify(olMapService.addLayer(component['knotenNetzLayer'] as VectorLayer)).once();
      verify(olMapService.addLayer(component['punktuelleKantenBezuegeLayer'] as VectorLayer)).once();
      expect().nothing();
    });
    it('should set initial NetzbezugSelection', () => {
      const netzbezug = component['selectedNetzbezug'];
      expect(netzbezug.toNetzbezug().kantenBezug).toEqual(initialNetzbezug.kantenBezug);
      expect(netzbezug.knoten).toEqual(initialNetzbezug.knotenBezug);
      expect(netzbezug.punktuelleKantenSeitenBezuege).toEqual(initialNetzbezug.punktuellerKantenBezug);
    });
    it('should highlight initial netzbezug', () => {
      expect(component['kantenNetzLayer']?.['ausgeblendeteKantenIds']).toEqual(new Set([42, 43]));
      expect(component['knotenNetzLayer']?.['highlightedKnotenIDs']).toEqual(new Set([20, 21]));
      expect(component['punktuelleKantenBezuegeLayer']?.getSource().getFeatures().length).toEqual(1);
      expect(
        component['punktuelleKantenBezuegeLayer']
          ?.getSource()
          .getFeatures()[0]
          .get(PunktuellerKantenBezuegeVectorLayer.KANTE_ID_PROPERTY_KEY)
      ).toEqual(5);
    });
  });

  describe('onChanges', () => {
    it('should unhighlight previously selected features and highlight newly selected features', () => {
      expect(component['kantenNetzLayer']?.['ausgeblendeteKantenIds']).toEqual(new Set([42, 43]));
      expect(component['knotenNetzLayer']?.['highlightedKnotenIDs']).toEqual(new Set([20, 21]));

      const neuerNetzbezug: Netzbezug = {
        kantenBezug: [
          {
            ...defaultNetzbezug.kantenBezug[0],
            kanteId: 55,
            geometrie: {
              coordinates: [
                [23, 50],
                [50, 30],
              ],
              type: 'LineString',
            },
          },
          {
            ...defaultNetzbezug.kantenBezug[0],
            kanteId: 42,
            geometrie: {
              coordinates: [
                [23, 50],
                [50, 30],
              ],
              type: 'LineString',
            },
          },
        ],
        knotenBezug: [{ knotenId: 20 }, { knotenId: 40 }],
        punktuellerKantenBezug: defaultNetzbezug.punktuellerKantenBezug,
      } as Netzbezug;

      component.netzbezug = neuerNetzbezug;

      component.ngOnChanges(({
        netzbezug: new SimpleChange(initialNetzbezug, neuerNetzbezug, false),
      } as unknown) as SimpleChanges);

      expect((component['kantenNetzLayer'] as KantenNetzVectorlayer)['ausgeblendeteKantenIds']).toEqual(
        new Set([42, 55])
      );
      expect((component['knotenNetzLayer'] as KnotenNetzVectorLayer)['highlightedKnotenIDs']).toEqual(
        new Set([20, 40])
      );
      expect(component['punktuelleKantenBezuegeLayer']?.getSource().getFeatures().length).toEqual(1);
      const feature = component['punktuelleKantenBezuegeLayer']?.getSource().getFeatures()[0];
      expect(feature?.get(PunktuellerKantenBezuegeVectorLayer.KANTE_ID_PROPERTY_KEY)).toEqual(
        defaultNetzbezug.punktuellerKantenBezug[0].kanteId
      );
      expect(feature?.get(PunktuellerKantenBezuegeVectorLayer.LINEARE_REFERENZ_PROPERTY_KEY)).toEqual(
        defaultNetzbezug.punktuellerKantenBezug[0].lineareReferenz
      );
    });
  });

  describe('onDestroy', () => {
    it('should remove layers', () => {
      const kantenLayer = component['kantenNetzLayer'] as VectorLayer;
      const knotenLayer = component['knotenNetzLayer'] as VectorLayer;
      const punktuelleKantenNetzbezuegeLayer = component['punktuelleKantenBezuegeLayer'] as VectorLayer;
      component.ngOnDestroy();
      verify(olMapService.removeLayer(kantenLayer)).once();
      verify(olMapService.removeLayer(knotenLayer)).once();
      verify(olMapService.removeLayer(punktuelleKantenNetzbezuegeLayer)).once();
      expect().nothing();
    });
    it('should unsubscribe', () => {
      const mapClickSubscriptionUnsubscribeSpy = spyOn(component['subscriptions'][0], 'unsubscribe');
      component.ngOnDestroy();
      expect(mapClickSubscriptionUnsubscribeSpy).toHaveBeenCalled();
    });
    it('should enable layers', () => {
      verify(layerDisableService.stopNetzbezugAuswahl()).never();
      component.ngOnDestroy();
      verify(layerDisableService.stopNetzbezugAuswahl()).once();
      expect().nothing();
    });
  });

  describe('onMapClick', () => {
    const clickEvent = ({
      pixel: [1, 2],
      coordinate: [12, 17],
      originalEvent: {
        ctrlKey: false,
        metaKey: false,
      } as PointerEvent,
    } as unknown) as MapBrowserEvent<UIEvent>;

    describe('in normal selectionMode', () => {
      describe('with Knoten', () => {
        let emitSpy: jasmine.Spy;
        const knotenPoint = new Point([0, 1]);

        beforeEach(() => {
          const feature = new Feature(knotenPoint);
          feature.setId(1);
          when(olMapService.getFeaturesAtPixel(anything())).thenReturn([feature]);
          component['knotenNetzLayer']?.getSource().addFeature(feature);
          emitSpy = spyOn(component.netzbezugChange, 'emit');
          inputNetzbezug({ kantenBezug: [], knotenBezug: [], punktuellerKantenBezug: [] });
        });

        it('should add knotenBezug', fakeAsync(() => {
          onMapClick$.next(clickEvent);

          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            kantenBezug: [],
            knotenBezug: [{ knotenId: 1, geometrie: { coordinates: knotenPoint.getCoordinates(), type: 'Point' } }],
            punktuellerKantenBezug: [],
          } as Netzbezug);
        }));

        it('should merge with existing netzbezug', fakeAsync(() => {
          const existingKnotenBezug = { ...defaultNetzbezug.knotenBezug[0], knotenId: 12 };
          const existingNetzbezug = { ...defaultNetzbezug, knotenBezug: [existingKnotenBezug] };
          inputNetzbezug(existingNetzbezug);

          onMapClick$.next(clickEvent);
          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            ...existingNetzbezug,
            knotenBezug: [
              existingKnotenBezug,
              { knotenId: 1, geometrie: { coordinates: knotenPoint.getCoordinates(), type: 'Point' } },
            ],
          });
        }));

        it('should remove from existing Netzbezug', fakeAsync(() => {
          const existingKnotenBezug = { ...defaultNetzbezug.knotenBezug[0], knotenId: 1 };
          const existingNetzbezug = { ...defaultNetzbezug, knotenBezug: [existingKnotenBezug] };
          inputNetzbezug(existingNetzbezug);

          onMapClick$.next(clickEvent);
          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            ...existingNetzbezug,
            knotenBezug: [],
          });
        }));

        it('should remove Knoten on subsequent click', fakeAsync(() => {
          onMapClick$.next(clickEvent);
          tick();

          onMapClick$.next(clickEvent);
          tick();

          expect(emitSpy).toHaveBeenCalledTimes(2);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            kantenBezug: [],
            knotenBezug: [],
            punktuellerKantenBezug: [],
          } as Netzbezug);
        }));
      });

      describe('with Kanten', () => {
        let emitSpy: jasmine.Spy;
        const kanteLinestring = new LineString([
          [0, 1],
          [0, 10],
        ]);

        beforeEach(() => {
          const feature = new Feature(kanteLinestring);
          feature.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, 1);
          when(olMapService.getFeaturesAtPixel(anything())).thenReturn([feature]);
          component['kantenNetzLayer']?.getSource().addFeature(feature);
          emitSpy = spyOn(component.netzbezugChange, 'emit');
          inputNetzbezug({ kantenBezug: [], knotenBezug: [], punktuellerKantenBezug: [] });
        });

        it('should add Kante', fakeAsync(() => {
          onMapClick$.next(clickEvent);
          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            kantenBezug: [
              {
                kanteId: 1,
                geometrie: { coordinates: kanteLinestring.getCoordinates(), type: 'LineString' },
                linearReferenzierterAbschnitt: { von: 0, bis: 1 },
                seitenbezug: KantenSeitenbezug.BEIDSEITIG,
              },
            ],
            knotenBezug: [],
            punktuellerKantenBezug: [],
          } as Netzbezug);
        }));

        it('should merge with existing netzbezug', fakeAsync(() => {
          const existingKantenBezug: AbschnittsweiserKantenSeitenBezug = {
            ...defaultNetzbezug.kantenBezug[0],
            kanteId: 12,
          };
          const existingNetzbezug: Netzbezug = {
            ...defaultNetzbezug,
            kantenBezug: [existingKantenBezug],
          };
          inputNetzbezug(existingNetzbezug);

          onMapClick$.next(clickEvent);
          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            ...existingNetzbezug,
            kantenBezug: [
              existingKantenBezug,
              {
                ...defaultNetzbezug.kantenBezug[0],
                kanteId: 1,
                geometrie: { coordinates: kanteLinestring.getCoordinates(), type: 'LineString' },
              },
            ],
          } as Netzbezug);
        }));
      });
      describe('with Kanten und Feature zweiseitig', () => {
        let emitSpy: jasmine.Spy;
        const kanteLinestring = new LineString([
          [0, 1],
          [0, 10],
        ]);

        beforeEach(() => {
          const feature = new Feature(kanteLinestring);
          feature.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, 1);
          feature.set(FeatureProperties.ZWEISEITIG_PROPERTY_NAME, true);

          const featureLinks = feature.clone();
          featureLinks.set(FeatureProperties.SEITE_PROPERTY_NAME, Seitenbezug.LINKS);
          const featureRechts = feature.clone();
          featureRechts.set(FeatureProperties.SEITE_PROPERTY_NAME, Seitenbezug.RECHTS);
          when(olMapService.getFeaturesAtPixel(anything())).thenReturn([featureLinks, featureRechts]);
          component['kantenNetzLayer']?.getSource().addFeature(featureLinks);
          component['kantenNetzLayer']?.getSource().addFeature(featureRechts);
          emitSpy = spyOn(component.netzbezugChange, 'emit');
          inputNetzbezug({ kantenBezug: [], knotenBezug: [], punktuellerKantenBezug: [] });
        });

        it('should add Kante', fakeAsync(() => {
          onMapClick$.next(clickEvent);
          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            kantenBezug: [
              {
                kanteId: 1,
                geometrie: { coordinates: kanteLinestring.getCoordinates(), type: 'LineString' },
                linearReferenzierterAbschnitt: { von: 0, bis: 1 },
                seitenbezug: KantenSeitenbezug.LINKS,
              },
            ],
            knotenBezug: [],
            punktuellerKantenBezug: [],
          } as Netzbezug);
        }));

        it('should merge with existing netzbezug', fakeAsync(() => {
          const existingKantenBezug: AbschnittsweiserKantenSeitenBezug = {
            ...defaultNetzbezug.kantenBezug[0],
            kanteId: 12,
          };
          const existingNetzbezug: Netzbezug = {
            ...defaultNetzbezug,
            kantenBezug: [existingKantenBezug],
          };
          inputNetzbezug(existingNetzbezug);

          onMapClick$.next(clickEvent);
          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            ...existingNetzbezug,
            kantenBezug: [
              existingKantenBezug,
              {
                ...defaultNetzbezug.kantenBezug[0],
                kanteId: 1,
                geometrie: { coordinates: kanteLinestring.getCoordinates(), type: 'LineString' },
                seitenbezug: KantenSeitenbezug.LINKS,
              },
            ],
          } as Netzbezug);
        }));
      });
    });

    describe('in PointSelectionMode', () => {
      beforeEach(() => {
        component.pointSelectionMode = true;
      });

      describe('chooses Knoten if first (closest) Feature is Knoten', () => {
        let emitSpy: jasmine.Spy;
        const knotenPoint = new Point([0, 1]);

        beforeEach(() => {
          const knotenFeature = new Feature(knotenPoint);
          const featureKante = new Feature(defaultKantenGeometrie);
          knotenFeature.setId(1);
          featureKante.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, 2);
          when(olMapService.getFeaturesAtPixel(anything())).thenReturn([knotenFeature, featureKante]);
          component['kantenNetzLayer']?.getSource().addFeature(featureKante);
          component['knotenNetzLayer']?.getSource().addFeature(knotenFeature);
          emitSpy = spyOn(component.netzbezugChange, 'emit');
          inputNetzbezug({ kantenBezug: [], knotenBezug: [], punktuellerKantenBezug: [] });
        });

        it('should add knotenBezug in PointSelectionMode', fakeAsync(() => {
          onMapClick$.next(clickEvent);

          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            kantenBezug: [],
            knotenBezug: [{ knotenId: 1, geometrie: { coordinates: knotenPoint.getCoordinates(), type: 'Point' } }],
            punktuellerKantenBezug: [],
          } as Netzbezug);
        }));
      });
      describe('chooses closest Point on Kante if first (closest) Feature is Kante', () => {
        let emitSpy: jasmine.Spy;
        const linestring = new LineString([
          [10, 10],
          [10, 15],
          [10, 20],
        ]);

        beforeEach(() => {
          const knotenFeature = new Feature(defaultKnoten.geometry);
          const featureKante = new Feature(linestring);
          knotenFeature.setId(1);
          featureKante.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, 2);
          when(olMapService.getFeaturesAtPixel(anything())).thenReturn([featureKante, knotenFeature]);
          component['kantenNetzLayer']?.getSource().addFeature(featureKante);
          component['knotenNetzLayer']?.getSource().addFeature(knotenFeature);
          emitSpy = spyOn(component.netzbezugChange, 'emit');
          inputNetzbezug({ kantenBezug: [], knotenBezug: [], punktuellerKantenBezug: [] });
        });

        it('should add punktueller Kantenbezug', fakeAsync(() => {
          onMapClick$.next(clickEvent);

          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            kantenBezug: [],
            knotenBezug: [],
            punktuellerKantenBezug: [
              {
                lineareReferenz: 0.7,
                kanteId: 2,
                geometrie: { coordinates: [10, 17], type: 'Point' },
              },
            ],
          } as Netzbezug);
        }));
        it('should merge with existing Netzbezug', fakeAsync(() => {
          inputNetzbezug(defaultNetzbezug);
          onMapClick$.next(clickEvent);
          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            ...defaultNetzbezug,
            punktuellerKantenBezug: [
              {
                geometrie: {
                  coordinates: [10, 12],
                  type: 'Point',
                },
                lineareReferenz: 0.5,
                kanteId: 3,
              },
              {
                geometrie: { coordinates: [10, 17], type: 'Point' },
                lineareReferenz: 0.7,
                kanteId: 2,
              },
            ],
          });
        }));
      });

      describe('selecting punktueller Netzbezug', () => {
        const knotenPoint = new Point([0, 1]);
        const linestring = new LineString([
          [10, 10],
          [10, 15],
          [10, 20],
        ]);

        let featureKante: Feature;
        let knotenFeature: Feature;
        let punktuellerKantenBezugFeature: Feature;

        let emitSpy: jasmine.Spy;

        beforeEach(() => {
          knotenFeature = new Feature(knotenPoint);
          knotenFeature.setId(1);
          component['knotenNetzLayer']?.getSource().addFeature(knotenFeature);

          featureKante = new Feature(linestring);
          featureKante.set(FeatureProperties.KANTE_ID_PROPERTY_NAME, 2);
          component['kantenNetzLayer']?.getSource().addFeature(featureKante);

          emitSpy = spyOn(component.netzbezugChange, 'emit');
        });

        it('should not remove from existing Netzbezug', fakeAsync(() => {
          inputNetzbezug({
            kantenBezug: [],
            knotenBezug: [],
            punktuellerKantenBezug: [
              {
                lineareReferenz: 0.7,
                kanteId: 2,
                geometrie: { coordinates: [10, 17], type: 'Point' },
              },
            ],
          });

          punktuellerKantenBezugFeature = // feature wird erst oben im inputNetzbezug von Component erzeugt und kann deshalb erst hier gesetzt werden
            component['punktuelleKantenBezuegeLayer']?.getSource().getFeatures()[0] || ({} as Feature);

          when(olMapService.getFeaturesAtPixel(anything())).thenReturn([
            featureKante,
            punktuellerKantenBezugFeature,
            knotenFeature,
          ]);

          onMapClick$.next(clickEvent);
          tick();

          expect(emitSpy).toHaveBeenCalledTimes(1);
          expect(emitSpy.calls.mostRecent().args[0]).toEqual({
            kantenBezug: [],
            knotenBezug: [],
            punktuellerKantenBezug: [
              {
                lineareReferenz: 0.7,
                kanteId: 2,
                geometrie: { coordinates: [10, 17], type: 'Point' },
              },
            ],
          });
        }));

        describe('with delete key held', () => {
          const clickEventWithDeleteKey = ({
            pixel: [1, 2],
            coordinate: [12, 17],
            originalEvent: {
              ctrlKey: true,
              metaKey: false,
            } as PointerEvent,
          } as unknown) as MapBrowserEvent<UIEvent>;

          it('should remove from existing Netzbezug', fakeAsync(() => {
            inputNetzbezug({
              kantenBezug: [],
              knotenBezug: [],
              punktuellerKantenBezug: [
                {
                  lineareReferenz: 0.7,
                  kanteId: 2,
                  geometrie: { coordinates: [10, 17], type: 'Point' },
                },
              ],
            });

            punktuellerKantenBezugFeature = // feature wird erst oben im inputNetzbezug von Component erzeugt und kann deshalb erst hier gesetzt werden
              component['punktuelleKantenBezuegeLayer']?.getSource().getFeatures()[0] || ({} as Feature);

            when(olMapService.getFeaturesAtPixel(anything())).thenReturn([
              featureKante,
              punktuellerKantenBezugFeature,
              knotenFeature,
            ]);

            onMapClick$.next(clickEventWithDeleteKey);
            tick();

            expect(emitSpy).toHaveBeenCalledTimes(1);
            expect(emitSpy.calls.mostRecent().args[0]).toEqual({
              kantenBezug: [],
              knotenBezug: [],
              punktuellerKantenBezug: [],
            });
          }));

          it('should do nothing, if no feature is present', fakeAsync(() => {
            inputNetzbezug({
              kantenBezug: [],
              knotenBezug: [],
              punktuellerKantenBezug: [],
            });

            when(olMapService.getFeaturesAtPixel(anything())).thenReturn([featureKante]);

            onMapClick$.next(clickEventWithDeleteKey);
            tick();

            expect(emitSpy).not.toHaveBeenCalled();
          }));
        });
      });
    });
  });

  describe('onDeselectSegment', () => {
    it('should update netzbezug', () => {
      const changeNetzbezugSpy = spyOn(component.netzbezugChange, 'emit');
      inputNetzbezug({
        ...defaultNetzbezug,
        kantenBezug: [
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
        ],
      });

      component.onDeselectSegment(2, defaultNetzbezug.kantenBezug[0].kanteId);
      expect(changeNetzbezugSpy).toHaveBeenCalled();
      expect(changeNetzbezugSpy.calls.mostRecent().args[0]).toEqual({
        ...defaultNetzbezug,
        kantenBezug: [{ ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } }],
      });
    });
    it('should update selection one-sided if selected segment is one-sided', () => {
      const changeNetzbezugSpy = spyOn(component.netzbezugChange, 'emit');
      inputNetzbezug({
        ...defaultNetzbezug,
        kantenBezug: [
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
        ],
      });

      component.onDeselectSegment(2, defaultNetzbezug.kantenBezug[0].kanteId, Seitenbezug.LINKS);
      expect(changeNetzbezugSpy).toHaveBeenCalled();
      const netzbezug = changeNetzbezugSpy.calls.mostRecent().args[0];
      netzbezug?.kantenBezug.sort(
        (kBZ1, kBZ2) => kBZ1.linearReferenzierterAbschnitt.von - kBZ2.linearReferenzierterAbschnitt.von
      );
      expect(netzbezug).toEqual({
        ...defaultNetzbezug,
        kantenBezug: [
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
          {
            ...defaultNetzbezug.kantenBezug[0],
            linearReferenzierterAbschnitt: { von: 0.7, bis: 1 },
            seitenbezug: KantenSeitenbezug.RECHTS,
          },
        ],
      });
    });
  });

  describe('onChangeSegmentierung', () => {
    it('should update netzbezug for both sides if change on both sides', () => {
      const changeNetzbezugSpy = spyOn(component.netzbezugChange, 'emit');
      inputNetzbezug({
        ...defaultNetzbezug,
        kantenBezug: [{ ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } }],
      });

      component.onSegmentierungChanged([0, 0.2, 1], defaultNetzbezug.kantenBezug[0].kanteId);
      expect(changeNetzbezugSpy).toHaveBeenCalled();
      expect(changeNetzbezugSpy.calls.mostRecent().args[0]).toEqual({
        ...defaultNetzbezug,
        kantenBezug: [{ ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.2 } }],
      });
    });

    it('should update netzbezug on one side if change on one side', () => {
      const changeNetzbezugSpy = spyOn(component.netzbezugChange, 'emit');
      inputNetzbezug({
        ...defaultNetzbezug,
        kantenBezug: [{ ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } }],
      });

      component.onSegmentierungChanged([0, 0.2, 1], defaultNetzbezug.kantenBezug[0].kanteId, Seitenbezug.LINKS);
      expect(changeNetzbezugSpy).toHaveBeenCalled();
      expect(changeNetzbezugSpy.calls.mostRecent().args[0]).toEqual({
        ...defaultNetzbezug,
        kantenBezug: [
          {
            ...defaultNetzbezug.kantenBezug[0],
            linearReferenzierterAbschnitt: { von: 0, bis: 0.2 },
            seitenbezug: KantenSeitenbezug.LINKS,
          },
          {
            ...defaultNetzbezug.kantenBezug[0],
            linearReferenzierterAbschnitt: { von: 0, bis: 0.3 },
            seitenbezug: KantenSeitenbezug.RECHTS,
          },
        ],
      });
    });
  });

  describe('onSelectSegment', () => {
    it('should schneiden if in Schere mode', () => {
      const changeNetzbezugSpy = spyOn(component.netzbezugChange, 'emit');
      const geometrie: LineStringGeojson = {
        coordinates: [
          [0, 0],
          [0, 100],
        ],
        type: 'LineString',
      };
      inputNetzbezug({
        ...defaultNetzbezug,
        kantenBezug: [
          {
            ...defaultNetzbezug.kantenBezug[0],
            geometrie,
            linearReferenzierterAbschnitt: { von: 0, bis: 1 },
          },
        ],
      });
      component.schereMode = true;

      component.onSelectSegment(
        { additiv: true, index: 0, clickedCoordinate: [0, 50] },
        defaultNetzbezug.kantenBezug[0].kanteId
      );
      expect(changeNetzbezugSpy).toHaveBeenCalled();
      expect(changeNetzbezugSpy.calls.mostRecent().args[0]).toEqual({
        ...defaultNetzbezug,
        kantenBezug: [
          { ...defaultNetzbezug.kantenBezug[0], geometrie, linearReferenzierterAbschnitt: { von: 0, bis: 0.5 } },
          { ...defaultNetzbezug.kantenBezug[0], geometrie, linearReferenzierterAbschnitt: { von: 0.5, bis: 1 } },
        ],
      });
    });

    it('should point select if in point mode', () => {
      const changeNetzbezugSpy = spyOn(component.netzbezugChange, 'emit');
      const geometrie: LineStringGeojson = {
        coordinates: [
          [0, 0],
          [0, 100],
        ],
        type: 'LineString',
      };
      inputNetzbezug({
        ...defaultNetzbezug,
        punktuellerKantenBezug: [],
        kantenBezug: [
          {
            ...defaultNetzbezug.kantenBezug[0],
            geometrie,
            linearReferenzierterAbschnitt: { von: 0, bis: 1 },
          },
        ],
      });
      component.pointSelectionMode = true;
      component.onSelectSegment(
        { additiv: true, index: 0, clickedCoordinate: [0, 50] },
        defaultNetzbezug.kantenBezug[0].kanteId
      );
      expect(changeNetzbezugSpy).toHaveBeenCalled();
      expect(changeNetzbezugSpy.calls.mostRecent().args[0]).toEqual({
        ...defaultNetzbezug,
        punktuellerKantenBezug: [
          {
            kanteId: defaultNetzbezug.kantenBezug[0].kanteId,
            geometrie: { coordinates: [0, 50], type: 'Point' },
            lineareReferenz: 0.5,
          },
        ],
        kantenBezug: [
          { ...defaultNetzbezug.kantenBezug[0], geometrie, linearReferenzierterAbschnitt: { von: 0, bis: 1 } },
        ],
      });
    });

    it('should select both sides of segment if in standard mode', () => {
      const changeNetzbezugSpy = spyOn(component.netzbezugChange, 'emit');
      inputNetzbezug({
        ...defaultNetzbezug,
        kantenBezug: [
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
        ],
      });

      component.onSelectSegment(
        { additiv: true, index: 1, clickedCoordinate: [0, 0] },
        defaultNetzbezug.kantenBezug[0].kanteId
      );
      expect(changeNetzbezugSpy).toHaveBeenCalled();
      expect(changeNetzbezugSpy.calls.mostRecent().args[0]).toEqual({
        ...defaultNetzbezug,
        kantenBezug: [
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 } },
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
        ],
      });
    });

    it('should select only one side of segment if in standard mode and one side is clicked', () => {
      const changeNetzbezugSpy = spyOn(component.netzbezugChange, 'emit');
      inputNetzbezug({
        ...defaultNetzbezug,
        kantenBezug: [
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
        ],
      });

      component.netzZweiseitig = true;

      component.onSelectSegment(
        { additiv: true, index: 1, clickedCoordinate: [0, 0] },
        defaultNetzbezug.kantenBezug[0].kanteId,
        Seitenbezug.LINKS
      );
      expect(changeNetzbezugSpy).toHaveBeenCalled();
      const netzbezug = changeNetzbezugSpy.calls.mostRecent().args[0];
      netzbezug?.kantenBezug.sort(
        (kBZ1, kBZ2) => kBZ1.linearReferenzierterAbschnitt.von - kBZ2.linearReferenzierterAbschnitt.von
      );
      expect(netzbezug).toEqual({
        ...defaultNetzbezug,
        kantenBezug: [
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0, bis: 0.3 } },
          {
            ...defaultNetzbezug.kantenBezug[0],
            linearReferenzierterAbschnitt: { von: 0.3, bis: 0.7 },
            seitenbezug: KantenSeitenbezug.LINKS,
          },
          { ...defaultNetzbezug.kantenBezug[0], linearReferenzierterAbschnitt: { von: 0.7, bis: 1 } },
        ],
      });
    });
  });

  const inputNetzbezug = (next: Netzbezug, prev?: Netzbezug): void => {
    component.netzbezug = next;
    component.ngOnChanges({ netzbezug: new SimpleChange(prev || null, next, false) });
  };

  const initialNetzbezug: Netzbezug = {
    kantenBezug: [
      {
        kanteId: 42,
        geometrie: {
          coordinates: [
            [22, 22],
            [44, 44],
          ],
          type: 'LineString',
        },
        seitenbezug: KantenSeitenbezug.BEIDSEITIG,
        linearReferenzierterAbschnitt: { von: 0, bis: 1 },
      } as AbschnittsweiserKantenSeitenBezug,
      {
        kanteId: 43,
        geometrie: {
          coordinates: [
            [0, 0],
            [5, 5],
          ],
          type: 'LineString',
        },
        seitenbezug: KantenSeitenbezug.BEIDSEITIG,
        linearReferenzierterAbschnitt: { von: 0, bis: 1 },
      } as AbschnittsweiserKantenSeitenBezug,
    ],
    knotenBezug: [{ knotenId: 20 } as KnotenNetzbezug, { knotenId: 21 } as KnotenNetzbezug],
    punktuellerKantenBezug: [
      {
        kanteId: 5,
        lineareReferenz: 0.4,
        geometrie: {
          coordinates: [0, 0],
          type: 'Point',
        },
      } as PunktuellerKantenNetzBezug,
    ],
  } as Netzbezug;
});
