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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, ActivatedRouteSnapshot, UrlSegment } from '@angular/router';
import { MockBuilder } from 'ng-mocks';
import { Feature, MapBrowserEvent } from 'ol';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Point } from 'ol/geom';
import VectorLayer from 'ol/layer/Vector';
import Projection from 'ol/proj/Projection';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { EditorModule } from 'src/app/editor/editor.module';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import invariant from 'tiny-invariant';
import { anything, instance, mock, verify, when } from 'ts-mockito';
import { KnotenSelektionComponent } from './knoten-selektion.component';

describe('KnotenSelektionComponent', () => {
  let component: KnotenSelektionComponent;
  let fixture: ComponentFixture<KnotenSelektionComponent>;

  let olMapService: OlMapService;
  let errorHandlingService: ErrorHandlingService;
  let featureService: NetzausschnittService;
  let editorRoutingService: EditorRoutingService;
  let activatedRoute: ActivatedRoute;
  const urlSubject = new BehaviorSubject<UrlSegment[]>([new UrlSegment('knoten', {})]);
  let knotenLayerNichtklassifiziert: VectorLayer;
  let netzklassenAuswahlSubject$: Subject<Netzklassefilter[]>;
  let netzklassenAuswahlService: NetzklassenAuswahlService;

  beforeEach(() => {
    olMapService = mock(OlMapComponent);
    errorHandlingService = mock(ErrorHandlingService);
    featureService = mock(NetzausschnittService);
    editorRoutingService = mock(EditorRoutingService);
    activatedRoute = mock(ActivatedRoute);
    netzklassenAuswahlSubject$ = new Subject<Netzklassefilter[]>();
    netzklassenAuswahlService = mock(NetzklassenAuswahlService);

    when(olMapService.click$()).thenReturn(of());
    when(featureService.getKnotenForView(anything(), anything())).thenReturn(of(createDummyFeatureCollection()));
    when(activatedRoute.snapshot).thenReturn(createDummySnapshotRoute());
    when(activatedRoute.url).thenReturn(urlSubject.asObservable());
    when(netzklassenAuswahlService.currentAuswahl$).thenReturn(netzklassenAuswahlSubject$);
    return MockBuilder(KnotenSelektionComponent, EditorModule)
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      })
      .provide({ provide: ErrorHandlingService, useValue: instance(errorHandlingService) })
      .provide({ provide: NetzausschnittService, useValue: instance(featureService) })
      .provide({ provide: EditorRoutingService, useValue: instance(editorRoutingService) })
      .provide({ provide: ActivatedRoute, useValue: instance(activatedRoute) })
      .provide({ provide: NetzklassenAuswahlService, useValue: instance(netzklassenAuswahlService) });
  });

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(KnotenSelektionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    component['netzklassen'] = [Netzklassefilter.NICHT_KLASSIFIZIERT];
    const knotenLayerNichtklassifiziertSearch = component.knotenLayers.find(
      kl => kl.getProperties().netzklasse === Netzklassefilter.NICHT_KLASSIFIZIERT
    );
    invariant(knotenLayerNichtklassifiziertSearch);
    knotenLayerNichtklassifiziert = knotenLayerNichtklassifiziertSearch;
    knotenLayerNichtklassifiziert.getSource().loadFeatures([0, 0, 5, 5], 20, new Projection({ code: 'EPSG:25832' }));
    tick();
  }));

  describe('olLayer', () => {
    it('should fill layer with features', fakeAsync(() => {
      expect(knotenLayerNichtklassifiziert.getSource().getFeatures().length).toEqual(3);
    }));
  });

  describe('onMapClick', () => {
    it('should do nothing when no features are under cursor', () => {
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([]);

      component['onMapClick']({ pixel: [0, 0] } as MapBrowserEvent<UIEvent>);

      verify(editorRoutingService.toKnotenAttributeEditor(anything())).never();
      expect().nothing();
    });

    it('should do nothing when clicked feature is not on correct layer', () => {
      const someFeature = new Feature(new Point([23, 77]));
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([someFeature]);

      component['onMapClick']({ pixel: [0, 0] } as MapBrowserEvent<UIEvent>);

      verify(editorRoutingService.toKnotenAttributeEditor(anything())).never();
      expect().nothing();
    });

    it('should adjust route when nearest clicked feature is on knoten layer', () => {
      const firstFeatureOnLayer = knotenLayerNichtklassifiziert.getSource().getFeatures()[0];
      const secondFeatureOnLayer = knotenLayerNichtklassifiziert.getSource().getFeatures()[1];
      when(olMapService.getFeaturesAtPixel(anything(), anything())).thenReturn([
        firstFeatureOnLayer,
        secondFeatureOnLayer,
      ]);

      component['onMapClick']({ pixel: [0, 0] } as MapBrowserEvent<UIEvent>);

      verify(editorRoutingService.toKnotenAttributeEditor(Number(firstFeatureOnLayer.getId()))).called();
      expect().nothing();
    });
  });

  describe('refreshSelektionLayer', () => {
    it('should call refresh when url changes', fakeAsync(() => {
      const refreshSpy = spyOn<any>(component, 'refreshSelektionLayer');

      urlSubject.next([new UrlSegment('knoten', {})]);
      tick();

      expect(refreshSpy).toHaveBeenCalled();
    }));
  });

  describe('netzklassen-zoomstufen', () => {
    it('should set correct min-zoom on layers', fakeAsync(() => {
      component['netzklassen'] = Netzklassefilter.getAll();

      component['knotenLayers'].forEach(kl => {
        let expectedMinZoom;
        switch (kl.getProperties().netzklasse) {
          case Netzklassefilter.RADNETZ:
            expectedMinZoom = MapStyles.DEFAULT_MIN_ZOOM_VALUE;
            break;
          case Netzklassefilter.KREISNETZ:
            expectedMinZoom = MapStyles.DEFAULT_MIN_ZOOM_VALUE;
            break;
          case Netzklassefilter.KOMMUNALNETZ:
            expectedMinZoom = MapStyles.DEFAULT_MIN_ZOOM_VALUE;
            break;
          case Netzklassefilter.NICHT_KLASSIFIZIERT:
            expectedMinZoom = 15.25;
            break;
          case Netzklassefilter.RADSCHNELLVERBINDUNG:
            expectedMinZoom = 14.72;
            break;
          case Netzklassefilter.RADVORRANGROUTEN:
            expectedMinZoom = 14.72;
            break;
          default:
            expectedMinZoom = 0;
        }
        expect(kl.getMinZoom()).toEqual(expectedMinZoom);
      });
    }));

    describe('onChanges', () => {
      it('should show layer when corresponding netzklasse is added', fakeAsync(() => {
        netzklassenAuswahlSubject$.next([]);
        tick();
        expect(knotenLayerNichtklassifiziert.getVisible()).toBeFalse();

        netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        tick();
        expect(knotenLayerNichtklassifiziert.getVisible()).toBeTrue();
      }));

      it('should hide layer when corresponding netzklasse is removed', fakeAsync(() => {
        netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        tick();
        expect(knotenLayerNichtklassifiziert.getVisible()).toBeTrue();

        netzklassenAuswahlSubject$.next([]);
        tick();
        expect(knotenLayerNichtklassifiziert.getVisible()).toBeFalse();
      }));

      it('should not refresh source when layer is neither selected nor deselected', fakeAsync(() => {
        const knotenLayerRadnetz = component['knotenLayers'].find(
          kl => kl.getProperties().netzklasse === Netzklassefilter.RADNETZ
        );
        invariant(knotenLayerRadnetz);
        const refreshSpy = spyOn(knotenLayerRadnetz.getSource(), 'refresh');
        component['netzklassen'] = [];

        netzklassenAuswahlSubject$.next([Netzklassefilter.NICHT_KLASSIFIZIERT]);
        tick();

        expect(refreshSpy).not.toHaveBeenCalled();
      }));
    });
  });
});

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function createDummySnapshotRoute(): ActivatedRouteSnapshot {
  return ({ firstChild: { params: { id: 2 } } } as unknown) as ActivatedRouteSnapshot;
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function createDummyFeatureCollection(): GeoJSONFeatureCollection {
  return {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'Point',
          coordinates: [1, 1],
        },
        id: '1',
      },
      {
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'Point',
          coordinates: [2, 2],
        },
        id: '2',
      },
      {
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'Point',
          coordinates: [3, 3],
        },
        id: '3',
      },
    ],
  } as GeoJSONFeatureCollection;
}
