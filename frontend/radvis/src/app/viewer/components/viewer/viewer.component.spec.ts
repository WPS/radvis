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
import { ComponentFixture } from '@angular/core/testing';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { MockBuilder, MockRender } from 'ng-mocks';
import { Point } from 'ol/geom';
import { Subject } from 'rxjs';
import { map, skip } from 'rxjs/operators';
import { FehlerprotokollLayerComponent } from 'src/app/fehlerprotokoll/components/fehlerprotokoll-layer/fehlerprotokoll-layer.component';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { SelectFeatureMenuComponent } from 'src/app/viewer/components/select-feature-menu/select-feature-menu.component';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { NetzdetailRoutingService } from 'src/app/viewer/netz-details/services/netzdetail-routing.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { instance, mock, when } from 'ts-mockito';

describe(ViewerComponent.name, () => {
  let mapQueryParams$: Subject<MapQueryParams>;
  let component: ViewerComponent;
  let fixture: ComponentFixture<ViewerComponent>;
  let viewerRoutingService: ViewerRoutingService;
  let mapQueryParamsService: MapQueryParamsService;
  let activatedRoute: ActivatedRoute;
  let netzdetailRoutingService: NetzdetailRoutingService;

  beforeEach(() => {
    mapQueryParams$ = new Subject();
    viewerRoutingService = mock(ViewerRoutingService);
    mapQueryParamsService = mock(MapQueryParamsService);
    activatedRoute = mock(ActivatedRoute);
    netzdetailRoutingService = mock(NetzdetailRoutingService);

    when(mapQueryParamsService.layers$).thenReturn(mapQueryParams$.pipe(map(params => params.layers)));
    when(mapQueryParamsService.netzklassen$).thenReturn(mapQueryParams$.pipe(map(params => params.netzklassen)));
    when(mapQueryParamsService.view$).thenReturn(mapQueryParams$.pipe(map(params => params.view)));
    when(mapQueryParamsService.mitVerlauf$).thenReturn(mapQueryParams$.pipe(map(params => !!params.mitVerlauf)));
    when(mapQueryParamsService.signatur$).thenReturn(mapQueryParams$.pipe(map(params => params.signatur)));
    const featureTogglzService = mock(FeatureTogglzService);
    when(featureTogglzService.fehlerprotokoll).thenReturn(true);
    return MockBuilder(ViewerComponent, ViewerModule)
      .provide({
        provide: ViewerRoutingService,
        useValue: instance(viewerRoutingService),
      })
      .provide({
        provide: NetzdetailRoutingService,
        useValue: instance(netzdetailRoutingService),
      })
      .provide({
        provide: DomSanitizer,
        useValue: {
          bypassSecurityTrustResourceUrl: (val: string) => val,
        },
      })
      .provide({
        provide: MapQueryParamsService,
        useValue: instance(mapQueryParamsService),
      })
      .provide({
        provide: ActivatedRoute,
        useValue: instance(activatedRoute),
      })
      .provide({
        provide: FeatureTogglzService,
        useValue: instance(featureTogglzService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(ViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('selected Signatur', () => {
    it('should select Netz Signatur', (done: DoneFn) => {
      const expectedSignatur = 'NETZSignatur';
      component.selectedNetzSignatur$.subscribe(signatur => {
        expect(signatur?.name).toEqual(expectedSignatur);
        done();
      });
      mapQueryParams$.next(
        new MapQueryParams([], [], null, null, null, { name: expectedSignatur, typ: SignaturTyp.NETZ })
      );
    });

    it('should deselect if null', (done: DoneFn) => {
      const expectedSignatur = 'NETZSignatur';
      component.selectedNetzSignatur$.pipe(skip(1)).subscribe(signatur => {
        expect(signatur).toBeNull();
        done();
      });
      mapQueryParams$.next(
        new MapQueryParams([], [], null, null, null, { name: expectedSignatur, typ: SignaturTyp.NETZ })
      );
      mapQueryParams$.next(new MapQueryParams([], [], null, null, null, null));
    });

    it('should deselect if Maßnahme Signatur', (done: DoneFn) => {
      component.selectedNetzSignatur$.pipe(skip(1)).subscribe(signatur => {
        expect(signatur).toBeNull();
        done();
      });
      mapQueryParams$.next(new MapQueryParams([], [], null, null, null, { name: 'TEST', typ: SignaturTyp.NETZ }));
      mapQueryParams$.next(new MapQueryParams([], [], null, null, null, { name: 'TEST', typ: SignaturTyp.MASSNAHME }));
    });
  });

  describe('onFeatureSelected', () => {
    it('should set selectedLocation and invoke component', () => {
      const coordinate = [99, 10];
      const menuSpy = spyOn(component.selectFeatureMenuComponent as SelectFeatureMenuComponent, 'onLocationSelect');
      const selectEvent = { coordinate, selectedFeatures: [] };
      component.onFeatureSelected(selectEvent);
      expect(component.selectedLocation).toEqual(coordinate);
      expect(menuSpy).toHaveBeenCalled();
      expect(menuSpy.calls.mostRecent().args[0]).toEqual(selectEvent);
    });

    it('should not do anything if in netzbezugAuswahl', () => {
      component.startNetzbezugAuswahl();
      const coordinate = [99, 10];
      const menuSpy = spyOn(component.selectFeatureMenuComponent as SelectFeatureMenuComponent, 'onLocationSelect');
      const selectEvent = { coordinate, selectedFeatures: [] };
      component.onFeatureSelected(selectEvent);
      expect(menuSpy).not.toHaveBeenCalled();
    });

    it('should not call select on menu when fehlerprotokoll feature selected', () => {
      component.startNetzbezugAuswahl();
      const coordinate = [99, 10];
      const menuSpy = spyOn(component.selectFeatureMenuComponent as SelectFeatureMenuComponent, 'onLocationSelect');
      const selectedFeature = new RadVisFeature(1, [], FehlerprotokollLayerComponent.LAYER_ID, new Point(coordinate));
      const selectEvent = {
        coordinate,
        selectedFeatures: [selectedFeature],
      };
      component.onFeatureSelected(selectEvent);

      expect(menuSpy).not.toHaveBeenCalled();
    });
  });
});
