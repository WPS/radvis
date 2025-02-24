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

import { fakeAsync, tick } from '@angular/core/testing';
import { ValidationErrors } from '@angular/forms';
import { MockBuilder, MockRender, MockedComponentFixture } from 'ng-mocks';
import { Collection, Feature, MapBrowserEvent } from 'ol';
import { Coordinate } from 'ol/coordinate';
import { LineString, Point } from 'ol/geom';
import { DrawEvent } from 'ol/interaction/Draw';
import { ModifyEvent } from 'ol/interaction/Modify';
import { Subject, of } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { LineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { BedienhinweisService } from 'src/app/shared/services/bedienhinweis.service';
import { NetzbezugAuswahlModusService } from 'src/app/shared/services/netzbezug-auswahl-modus.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { FahrradrouteNetzbezugControlComponent } from 'src/app/viewer/fahrradroute/components/fahrradroute-netzbezug-control/fahrradroute-netzbezug-control.component';
import { FahrradrouteModule } from 'src/app/viewer/fahrradroute/fahrradroute.module';
import { FahrradrouteNetzbezug } from 'src/app/viewer/fahrradroute/models/fahrradroute.netzbezug';
import { RoutingResult } from 'src/app/viewer/fahrradroute/models/routing-result';
import { FahrradrouteService } from 'src/app/viewer/fahrradroute/services/fahrradroute.service';
import { RoutingProfileService } from 'src/app/viewer/fahrradroute/services/routing-profile.service';
import { anything, capture, instance, mock, reset, verify, when } from 'ts-mockito';

describe(FahrradrouteNetzbezugControlComponent.name, () => {
  let fixture: MockedComponentFixture<FahrradrouteNetzbezugControlComponent>;
  let component: FahrradrouteNetzbezugControlComponent;
  let olMapService: OlMapService;
  let fahrradrouteService: FahrradrouteService;
  let mapClickSubject: Subject<MapBrowserEvent>;
  let bedienhinweisService: BedienhinweisService;
  let netzbezugAuswahlService: NetzbezugAuswahlModusService;
  let routingProfileService: RoutingProfileService;

  beforeEach(() => {
    olMapService = mock(OlMapComponent);
    mapClickSubject = new Subject<MapBrowserEvent>();
    when(olMapService.click$()).thenReturn(mapClickSubject);
    fahrradrouteService = mock(FahrradrouteService);
    bedienhinweisService = mock(BedienhinweisService);
    netzbezugAuswahlService = mock(ViewerComponent);
    routingProfileService = mock(RoutingProfileService);
    when(routingProfileService.profiles$).thenReturn(of([]));

    return MockBuilder(FahrradrouteNetzbezugControlComponent, FahrradrouteModule)
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({
        provide: FahrradrouteService,
        useValue: instance(fahrradrouteService),
      })
      .provide({
        provide: NetzbezugAuswahlModusService,
        useValue: instance(netzbezugAuswahlService),
      })
      .provide({
        provide: BedienhinweisService,
        useValue: instance(bedienhinweisService),
      })
      .provide({
        provide: NotifyUserService,
        useValue: instance(mock(NotifyUserService)),
      })
      .provide({
        provide: RoutingProfileService,
        useValue: instance(routingProfileService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(FahrradrouteNetzbezugControlComponent);
    component = fixture.point.componentInstance;
  });

  describe('with no initial value', () => {
    let onChangeSpy: jasmine.Spy;

    const startCoordinate = [0, 10];
    const endCoordinate = [10, 100];
    const routingResult = {
      kantenIDs: [0, 10],
      routenGeometrie: {
        coordinates: [
          [0, 0, 100],
          [0, 100, 200],
        ],
        type: 'LineString',
      },
      profilEigenschaften: [],
    } as RoutingResult;

    beforeEach(fakeAsync(() => {
      when(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).thenResolve(
        routingResult
      );
      onChangeSpy = spyOn(component, 'onChange');

      // irgendwie kompiliert das nicht, wenn man DrawEventType verwendet, um das DrawEvent zu erstellen
      component['drawStartEndInteraction'].dispatchEvent(
        // @ts-expect-error Migration von ts-ignore
        new DrawEvent('drawend', new Feature(new Point(startCoordinate)))
      );
      tick();

      component['drawStartEndInteraction'].dispatchEvent(
        // @ts-expect-error Migration von ts-ignore
        new DrawEvent('drawend', new Feature(new Point(endCoordinate)))
      );
      tick();
    }));

    it('should enable draw interaction', () => {
      component.writeValue({
        geometrie: {
          coordinates: [
            [0, 0],
            [1, 1],
          ],
          type: 'LineString',
        },
        kantenIDs: [0],
        stuetzpunkte: [
          [0, 0],
          [1, 1],
        ],
        profilEigenschaften: [],
        customProfileId: 123,
      });
      expect(component['drawStartEndInteraction'].getActive()).toBeFalse();

      component.writeValue(null);

      expect(component['drawStartEndInteraction'].getActive()).toBeTrue();
    });

    it('should set start and endpoint and route', () => {
      verify(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).once();
      expect(capture(fahrradrouteService.routeFahrradroutenVerlauf).last()[0]).toEqual([
        startCoordinate,
        endCoordinate,
      ]);
      expect(onChangeSpy).toHaveBeenCalled();
      expect(onChangeSpy.calls.mostRecent().args[0]).toEqual({
        stuetzpunkte: [startCoordinate, endCoordinate],
        kantenIDs: routingResult.kantenIDs,
        geometrie: routingResult.routenGeometrie,
        profilEigenschaften: routingResult.profilEigenschaften,
        customProfileId: -1,
      } as FahrradrouteNetzbezug);
      verify(olMapService.addInteraction(anything())).twice();
      expect(component['drawStartEndInteraction'].getActive()).toBeFalse();
    });

    describe('with stuetzpunkt added', () => {
      const eventMock = mock(MapBrowserEvent);
      const clickedPixel = [0, 1];
      const stuetzpunktCoordinate = [0, 50];

      const createStuetzpunkt = (coordinate: Coordinate): void => {
        when(eventMock.coordinate).thenReturn(coordinate);
        when(olMapService.getFeaturesAtPixel(clickedPixel, component['verlaufLayerFilter'])).thenReturn(
          component['verlaufVectorLayer'].getSource().getFeatures()
        );
        when(olMapService.getFeaturesAtPixel(clickedPixel, component['stuetzpunktLayerFilter'])).thenReturn([]);
        mapClickSubject.next(instance(eventMock));
      };

      beforeEach(fakeAsync(() => {
        when(eventMock.pixel).thenReturn(clickedPixel);
        createStuetzpunkt(stuetzpunktCoordinate);
        tick();
      }));

      it('should add stuetzpunkte in correct order', fakeAsync(() => {
        expect(onChangeSpy).toHaveBeenCalled();
        expect(onChangeSpy.calls.mostRecent().args[0]).toEqual({
          stuetzpunkte: [startCoordinate, stuetzpunktCoordinate, endCoordinate],
          kantenIDs: routingResult.kantenIDs,
          geometrie: routingResult.routenGeometrie,
          profilEigenschaften: routingResult.profilEigenschaften,
          customProfileId: -1,
        } as FahrradrouteNetzbezug);

        const clickedCoordinate2 = [0, 25];
        createStuetzpunkt(clickedCoordinate2);
        tick();

        expect(onChangeSpy).toHaveBeenCalled();
        expect(onChangeSpy.calls.mostRecent().args[0]).toEqual({
          stuetzpunkte: [startCoordinate, clickedCoordinate2, stuetzpunktCoordinate, endCoordinate],
          kantenIDs: routingResult.kantenIDs,
          geometrie: routingResult.routenGeometrie,
          profilEigenschaften: routingResult.profilEigenschaften,
          customProfileId: -1,
        } as FahrradrouteNetzbezug);

        const clickedCoordinate3 = [0, 75];
        createStuetzpunkt(clickedCoordinate3);
        tick();

        expect(onChangeSpy).toHaveBeenCalled();
        expect(onChangeSpy.calls.mostRecent().args[0]).toEqual({
          stuetzpunkte: [startCoordinate, clickedCoordinate2, stuetzpunktCoordinate, clickedCoordinate3, endCoordinate],
          kantenIDs: routingResult.kantenIDs,
          geometrie: routingResult.routenGeometrie,
          profilEigenschaften: routingResult.profilEigenschaften,
          customProfileId: -1,
        } as FahrradrouteNetzbezug);
      }));

      it('should move stutzpunkte and route', fakeAsync(() => {
        reset(fahrradrouteService);
        when(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).thenResolve(
          routingResult
        );

        const stuetzpunktFeature = component['stuetzpunktVectorSource'].getFeatures()[1];
        const movedCoordinate = [50, 50];
        stuetzpunktFeature.setGeometry(new Point(movedCoordinate));
        component['modifyStuetzpunktInteraction'].dispatchEvent(
          // @ts-expect-error Migration von ts-ignore
          new ModifyEvent('modifyend', new Collection([stuetzpunktFeature]))
        );
        tick();

        verify(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).once();
        expect(capture(fahrradrouteService.routeFahrradroutenVerlauf).last()[0]).toEqual([
          startCoordinate,
          movedCoordinate,
          endCoordinate,
        ]);
        expect(onChangeSpy).toHaveBeenCalled();
        expect(onChangeSpy.calls.mostRecent().args[0]).toEqual({
          stuetzpunkte: [startCoordinate, movedCoordinate, endCoordinate],
          kantenIDs: routingResult.kantenIDs,
          geometrie: routingResult.routenGeometrie,
          profilEigenschaften: routingResult.profilEigenschaften,
          customProfileId: -1,
        } as FahrradrouteNetzbezug);
      }));

      it('should delete stuetzpunkt and route', fakeAsync(() => {
        reset(fahrradrouteService);
        when(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).thenResolve(
          routingResult
        );

        expect(onChangeSpy.calls.mostRecent().args[0]).toEqual({
          stuetzpunkte: [startCoordinate, stuetzpunktCoordinate, endCoordinate],
          kantenIDs: routingResult.kantenIDs,
          geometrie: routingResult.routenGeometrie,
          profilEigenschaften: routingResult.profilEigenschaften,
          customProfileId: -1,
        } as FahrradrouteNetzbezug);

        const removeStuetzpunkt = (): void => {
          when(olMapService.getFeaturesAtPixel(clickedPixel, component['verlaufLayerFilter'])).thenReturn([]);
          const stuetzpunktToRemove = component['stuetzpunktVectorSource'].getFeatures()[1];
          when(
            olMapService.getFeaturesAtPixel(clickedPixel, component['stuetzpunktLayerFilter'], anything())
          ).thenReturn([stuetzpunktToRemove]);
          when(eventMock.originalEvent).thenReturn(new PointerEvent('click', { ctrlKey: true }));
          mapClickSubject.next(instance(eventMock));
        };

        removeStuetzpunkt();
        tick();

        verify(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).once();
        expect(capture(fahrradrouteService.routeFahrradroutenVerlauf).last()[0]).toEqual([
          startCoordinate,
          endCoordinate,
        ]);
        expect(onChangeSpy.calls.mostRecent().args[0]).toEqual({
          stuetzpunkte: [startCoordinate, endCoordinate],
          kantenIDs: routingResult.kantenIDs,
          geometrie: routingResult.routenGeometrie,
          profilEigenschaften: routingResult.profilEigenschaften,
          customProfileId: -1,
        } as FahrradrouteNetzbezug);
      }));

      it('should remove z-Coordinate from inserted Stuetzpunkt', fakeAsync(() => {
        const stuetzpunktCoordinate3D = [0, 25, 3];
        createStuetzpunkt(stuetzpunktCoordinate3D);
        tick();

        expect(onChangeSpy).toHaveBeenCalled();
        expect(onChangeSpy.calls.mostRecent().args[0]).toEqual({
          stuetzpunkte: [startCoordinate, stuetzpunktCoordinate3D.slice(0, 2), stuetzpunktCoordinate, endCoordinate],
          kantenIDs: routingResult.kantenIDs,
          geometrie: routingResult.routenGeometrie,
          profilEigenschaften: routingResult.profilEigenschaften,
          customProfileId: -1,
        } as FahrradrouteNetzbezug);
      }));

      it('should reset on writeValue(null)', () => {
        reset(bedienhinweisService);
        reset(olMapService);
        component.writeValue(null);

        expect(component['stuetzpunkte']).toEqual([]);
        expect(component['kanteIDs']).toEqual([]);
        expect(component['stuetzpunktVectorSource'].getFeatures()).toEqual([]);
        expect(component['verlaufVectorSource'].getFeatures()).toEqual([]);
        expect(component['drawStartEndInteraction'].getActive()).toBeTrue();
        verify(bedienhinweisService.showBedienhinweis(component['BEDIENHINWEIS_START'])).once();
        verify(olMapService.setCursor('point-selection-cursor')).once();
      });
    });
  });

  describe('with initial value', () => {
    it('should reset layers', () => {
      reset(bedienhinweisService);
      reset(olMapService);
      const stuetzpunkte = [
        [0, 0],
        [5, 5],
      ];
      const verlauf = [
        [0, 0],
        [0, 5],
        [5, 5],
      ];
      // Nötig durch das "writeValue" und damit verbundenen Changes an Form-Controls. Ergebnis des routing-Aufrufs ist
      // aber egal.
      when(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).thenResolve(null as any);
      component.writeValue({
        geometrie: {
          coordinates: verlauf,
          type: 'LineString',
        },
        kantenIDs: [0, 1],
        stuetzpunkte,
        profilEigenschaften: [],
        customProfileId: -1,
      });

      expect(component['drawStartEndInteraction'].getActive()).toBeFalse();
      const stuetzpunktFeatures = component['stuetzpunktVectorSource'].getFeatures();
      expect(stuetzpunktFeatures.length).toBe(2);
      expect(stuetzpunktFeatures.map(f => (f.getGeometry() as Point).getCoordinates())).toEqual(stuetzpunkte);
      expect((component['verlaufVectorSource'].getFeatures()[0].getGeometry() as LineString).getCoordinates()).toEqual(
        verlauf
      );
      verify(olMapService.resetCursor()).once();
      expect(component.createMode).toBeFalse();
    });

    it('should reemit correct values after stuetzpunkt added', fakeAsync(() => {
      const eventMock = mock(MapBrowserEvent);
      const clickedPixel = [0, 1];
      const newStuetzpunktCoordinate = [0, 2];

      const onChangeSpy = spyOn(component, 'onChange');
      const kantenIds = [0, 1];
      const verlauf = {
        coordinates: [
          [0, 0],
          [0, 5],
          [5, 5],
        ],
        type: 'LineString',
      } as LineStringGeojson;

      // Nötig durch das "writeValue" und damit verbundenen Changes an Form-Controls. Ergebnis des routing-Aufrufs ist
      // aber egal.
      when(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).thenResolve(null as any);
      component.writeValue({
        geometrie: verlauf,
        kantenIDs: kantenIds,
        stuetzpunkte: [
          [0, 0],
          [5, 5],
        ],
        profilEigenschaften: [],
        customProfileId: 123,
      });

      const createStuetzpunkt = (coordinate: Coordinate): void => {
        when(eventMock.coordinate).thenReturn(coordinate);
        when(olMapService.getFeaturesAtPixel(clickedPixel, component['verlaufLayerFilter'])).thenReturn(
          component['verlaufVectorLayer'].getSource().getFeatures()
        );
        when(olMapService.getFeaturesAtPixel(clickedPixel, component['stuetzpunktLayerFilter'])).thenReturn([]);
        mapClickSubject.next(instance(eventMock));
      };

      when(eventMock.pixel).thenReturn(clickedPixel);
      createStuetzpunkt(newStuetzpunktCoordinate);
      tick();

      expect(onChangeSpy.calls.mostRecent().args[0]).toEqual({
        geometrie: verlauf,
        kantenIDs: kantenIds,
        stuetzpunkte: [[0, 0], newStuetzpunktCoordinate, [5, 5]],
        profilEigenschaften: [],
        customProfileId: 123,
      } as FahrradrouteNetzbezug);
    }));

    it('should not work on reference', () => {
      const stuetzpunkte = [
        [0, 0],
        [5, 5],
      ];
      const verlauf = [
        [0, 0],
        [0, 5],
        [5, 5],
      ];
      const kanteIds = [0, 1];
      // Nötig durch das "writeValue" und damit verbundenen Changes an Form-Controls. Ergebnis des routing-Aufrufs ist
      // aber egal.
      when(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).thenResolve(null as any);
      component.writeValue({
        geometrie: {
          coordinates: verlauf,
          type: 'LineString',
        },
        kantenIDs: kanteIds,
        stuetzpunkte,
        profilEigenschaften: [],
        customProfileId: 123,
      });

      expect(component['stuetzpunkte']).not.toBe(stuetzpunkte);
      expect(component['kanteIDs']).not.toBe(kanteIds);
      expect(component['coordinates']).not.toBe(verlauf);
    });
  });

  describe('bedienhinweis', () => {
    const startCoordinate = [0, 10];
    const endCoordinate = [10, 100];
    const routingResult = {
      kantenIDs: [0, 10],
      routenGeometrie: {
        coordinates: [
          [0, 0],
          [0, 100],
        ],
        type: 'LineString',
      },
    } as RoutingResult;

    it('should change hinweis while setting start/end', fakeAsync(() => {
      when(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).thenResolve(
        routingResult
      );
      verify(bedienhinweisService.showBedienhinweis(component['BEDIENHINWEIS_START'])).once();

      // irgendwie kompiliert das nicht, wenn man DrawEventType verwendet, um das DrawEvent zu erstellen
      component['drawStartEndInteraction'].dispatchEvent(
        // @ts-expect-error Migration von ts-ignore
        new DrawEvent('drawend', new Feature(new Point(startCoordinate)))
      );
      tick();
      verify(bedienhinweisService.showBedienhinweis(component['BEDIENHINWEIS_END'])).once();

      component['drawStartEndInteraction'].dispatchEvent(
        // @ts-expect-error Migration von ts-ignore
        new DrawEvent('drawend', new Feature(new Point(endCoordinate)))
      );
      tick();
      verify(bedienhinweisService.showBedienhinweis(component['BEDIENHINWEIS_STUETZPUNKTE'])).once();
      expect().nothing();
    }));
  });

  describe('ngOnDestroy', () => {
    it('should reset everything', () => {
      component.ngOnDestroy();

      verify(olMapService.removeLayer(anything())).twice();
      verify(olMapService.removeInteraction(anything())).twice();
      verify(olMapService.resetCursor()).once();
      verify(bedienhinweisService.hideBedienhinweis()).once();
      verify(netzbezugAuswahlService.stopNetzbezugAuswahl()).once();

      expect().nothing();
    });
  });

  describe('validate', () => {
    it('should be valid', () => {
      component['stuetzpunkte'] = [
        [0, 0],
        [0, 10],
      ];
      component['kanteIDs'] = [0];
      component['coordinates'] = [
        [0, 0],
        [1, 2],
        [0, 10],
      ];
      expect(component.validate()).toBeNull();
    });

    it('should be invalid if no verlauf', () => {
      component['stuetzpunkte'] = [
        [0, 0],
        [0, 10],
      ];
      component['kanteIDs'] = [0];
      component['coordinates'] = [];
      expect(component.validate()).not.toBeNull();
    });

    it('should be invalid if no kanteIds', () => {
      component['stuetzpunkte'] = [
        [0, 0],
        [0, 10],
      ];
      component['kanteIDs'] = [];
      expect(component.validate()).not.toBeNull();
    });

    it('should be invalid if no start/end', () => {
      component['stuetzpunkte'] = [[0, 0]];
      component['kanteIDs'] = [0];
      expect(component.validate()).not.toBeNull();
    });

    it('should not stack error messages (only one initial error message)', () => {
      component['stuetzpunkte'] = [[0, 0]];
      component['kanteIDs'] = [];
      component['coordinates'] = [];
      const errors = component.validate();
      expect(Object.keys(errors!).length).toBe(1);
    });
  });

  describe('mitFahrtrichtungControl', () => {
    beforeEach(() => {
      when(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).thenResolve(
        null as unknown as RoutingResult
      );
    });

    it('should not reroute on Change if no start end', () => {
      component.writeValue(null);

      component.mitFahrtrichtungControl.setValue(false);

      verify(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), anything())).never();
      expect().nothing();
    });

    it('should reroute on Change', () => {
      const stuetzpunkte = [
        [0, 0],
        [5, 5],
      ];
      const verlauf = [
        [0, 0],
        [0, 5],
        [5, 5],
      ];
      component.writeValue({
        geometrie: {
          coordinates: verlauf,
          type: 'LineString',
        },
        kantenIDs: [0, 1],
        stuetzpunkte,
        profilEigenschaften: [],
        customProfileId: 123,
      });

      component.mitFahrtrichtungControl.setValue(false);

      verify(fahrradrouteService.routeFahrradroutenVerlauf(anything(), anything(), false)).once();
      expect().nothing();
    });
  });
});
