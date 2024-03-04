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
import { fakeAsync, tick } from '@angular/core/testing';
import { MockBuilder, MockRender, MockedComponentFixture } from 'ng-mocks';
import { Coordinate } from 'ol/coordinate';
import { LineString, MultiLineString, Point } from 'ol/geom';
import VectorSource from 'ol/source/Vector';
import { Style } from 'ol/style';
import { BehaviorSubject, Subject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { LineStringGeojson, MultiLineStringGeojson } from 'src/app/shared/models/geojson-geometrie';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { Umsetzungsstatus } from 'src/app/shared/models/umsetzungsstatus';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { MassnahmenLayerComponent } from 'src/app/viewer/massnahme/components/massnahmen-layer/massnahmen-layer.component';
import { MassnahmeModule } from 'src/app/viewer/massnahme/massnahme.module';
import { MassnahmeListenView } from 'src/app/viewer/massnahme/models/massnahme-listen-view';
import {
  defaultMassnahmeListView,
  getTestMassnahmeListenViews,
} from 'src/app/viewer/massnahme/models/massnahme-listen-view-test-data-provider.spec';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { SignaturStyleProviderService } from 'src/app/viewer/signatur/services/signatur-style-provider.service';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(MassnahmenLayerComponent.name, () => {
  let component: MassnahmenLayerComponent;
  let fixture: MockedComponentFixture<MassnahmenLayerComponent>;

  let olMapComponent: OlMapComponent;
  let massnahmeFilterService: MassnahmeFilterService;
  let massnahmenRoutingService: MassnahmenRoutingService;
  let featureHighlightService: FeatureHighlightService;
  let notifyUserService: NotifyUserService;

  const selectedMassnahmenIdTestSubject = new BehaviorSubject<number | null>(null);
  const highlightFeatureSubject = new Subject<RadVisFeature>();
  const unhighlightFeatureSubject = new Subject<RadVisFeature>();
  const resolutionChangedSubject = new Subject<number>();
  let filteredListSubject: BehaviorSubject<MassnahmeListenView[]>;
  let signaturStyleProviderService: SignaturStyleProviderService;
  let mapQueryParamsService: MapQueryParamsService;
  let signaturSubject: BehaviorSubject<Signatur | null>;

  beforeEach(() => {
    olMapComponent = mock(OlMapComponent);
    massnahmeFilterService = mock(MassnahmeFilterService);
    massnahmenRoutingService = mock(MassnahmenRoutingService);
    featureHighlightService = mock(FeatureHighlightService);
    notifyUserService = mock(NotifyUserService);
    filteredListSubject = new BehaviorSubject<MassnahmeListenView[]>(getTestMassnahmeListenViews());
    signaturStyleProviderService = mock(SignaturStyleProviderService);
    mapQueryParamsService = mock(MapQueryParamsService);
    signaturSubject = new BehaviorSubject<Signatur | null>(null);
    when(mapQueryParamsService.signatur$).thenReturn(signaturSubject);
    when(mapQueryParamsService.mapQueryParamsSnapshot).thenCall(
      () => new MapQueryParams([], [], null, null, null, signaturSubject.value)
    );

    when(olMapComponent.getResolution$()).thenReturn(resolutionChangedSubject.asObservable());

    when(massnahmeFilterService.filteredList$).thenReturn(filteredListSubject);
    when(massnahmeFilterService.currentFilteredList).thenCall(() => filteredListSubject.value);

    when(massnahmenRoutingService.selectedInfrastrukturId$).thenReturn(selectedMassnahmenIdTestSubject.asObservable());

    when(featureHighlightService.highlightedFeature$).thenReturn(highlightFeatureSubject.asObservable());
    when(featureHighlightService.unhighlightedFeature$).thenReturn(unhighlightFeatureSubject.asObservable());

    return MockBuilder(MassnahmenLayerComponent, [MassnahmeModule, ViewerModule])
      .provide({ provide: OlMapService, useValue: instance(olMapComponent) })
      .provide({ provide: MassnahmeFilterService, useValue: instance(massnahmeFilterService) })
      .provide({ provide: MassnahmenRoutingService, useValue: instance(massnahmenRoutingService) })
      .provide({ provide: FeatureHighlightService, useValue: instance(featureHighlightService) })
      .provide({ provide: MapQueryParamsService, useValue: instance(mapQueryParamsService) })
      .provide({ provide: SignaturStyleProviderService, useValue: instance(signaturStyleProviderService) })
      .provide({ provide: NotifyUserService, useValue: instance(notifyUserService) });
  });

  beforeEach(() => {
    fixture = MockRender(MassnahmenLayerComponent);
    component = fixture.point.componentInstance;
    // @ts-expect-error Migration von ts-ignore
    component['vectorSource']['loader_']();
    selectedMassnahmenIdTestSubject.next(null);
  });

  it('should create and fill vector Source', () => {
    expect(component).toBeTruthy();
    expect(component['vectorSource'].getFeatures()).toHaveSize(getTestMassnahmeListenViews().length);
  });

  describe('zoom-in notification', () => {
    describe('when resolution is initially undeterminded', () => {
      it('should not show zoom-in notification before resolution is determined', () => {
        verify(notifyUserService.inform(anything())).never();
        expect().nothing();
      });

      it('should show zoom-in notification on event with low zoom value', () => {
        when(olMapComponent.getZoomForResolution(123)).thenReturn(15);
        resolutionChangedSubject.next(123);
        verify(notifyUserService.inform(anything())).once();
        expect().nothing();
      });

      it('should not show zoom-in notification on event with too high zoom value', () => {
        when(olMapComponent.getZoomForResolution(123)).thenReturn(15.01);
        resolutionChangedSubject.next(123);
        verify(notifyUserService.inform(anything())).never();
        expect().nothing();
      });
    });
    describe('when resolution is initially determinded', () => {
      describe('and resulting zoom is high enough', () => {
        beforeEach(() => {
          when(olMapComponent.getCurrentResolution()).thenReturn(123);
          when(olMapComponent.getZoomForResolution(123)).thenReturn(15.01);
          fixture = MockRender(MassnahmenLayerComponent);
          component = fixture.point.componentInstance;
        });
        it('should not show zoom-in notification directly', () => {
          verify(notifyUserService.inform(anything())).never();
          expect().nothing();
        });
      });
      describe('and resulting zoom is too low', () => {
        beforeEach(() => {
          when(olMapComponent.getCurrentResolution()).thenReturn(123);
          when(olMapComponent.getZoomForResolution(123)).thenReturn(15);
          fixture = MockRender(MassnahmenLayerComponent);
          component = fixture.point.componentInstance;
        });
        it('should directly show zoom-in notification', () => {
          verify(notifyUserService.inform(anything())).once();
          expect().nothing();
        });
      });
    });
  });

  describe('change selection', () => {
    it('should set feature properties correctly on selection change', () => {
      selectedMassnahmenIdTestSubject.next(1);

      expect(component['vectorSource'].getFeatureById(1).get('highlighted')).toBeTrue();
      expect(component['vectorSource'].getFeatureById(2).get('highlighted')).toBeFalsy();

      selectedMassnahmenIdTestSubject.next(2);

      expect(component['vectorSource'].getFeatureById(1).get('highlighted')).toBeFalse();
      expect(component['vectorSource'].getFeatureById(2).get('highlighted')).toBeTrue();
    });
  });

  describe('highlight from highlightService', () => {
    it('should set feature properties correctly when highlightService triggers', () => {
      // arrange
      const radVisFeature1 = new RadVisFeature(1, [], MASSNAHMEN.name, new Point([1, 2]));
      const radVisFeature2 = new RadVisFeature(2, [], MASSNAHMEN.name, new Point([1, 2]));

      expect(component['vectorSource'].getFeatureById(1).get('highlighted')).toBeFalsy();
      expect(component['vectorSource'].getFeatureById(2).get('highlighted')).toBeFalsy();

      highlightFeatureSubject.next(radVisFeature1);

      expect(component['vectorSource'].getFeatureById(1).get('highlighted')).toBeTruthy();
      expect(component['vectorSource'].getFeatureById(2).get('highlighted')).toBeFalsy();

      unhighlightFeatureSubject.next(radVisFeature1);

      expect(component['vectorSource'].getFeatureById(1).get('highlighted')).toBeFalsy();
      expect(component['vectorSource'].getFeatureById(2).get('highlighted')).toBeFalsy();

      highlightFeatureSubject.next(radVisFeature2);

      expect(component['vectorSource'].getFeatureById(1).get('highlighted')).toBeFalsy();
      expect(component['vectorSource'].getFeatureById(2).get('highlighted')).toBeTruthy();
    });

    it('should not unhighlight selectedFeature', () => {
      // arrange
      selectedMassnahmenIdTestSubject.next(1);
      const radVisFeature1 = new RadVisFeature(1, [], MASSNAHMEN.name, new Point([1, 2]));
      const radVisFeature2 = new RadVisFeature(2, [], MASSNAHMEN.name, new Point([1, 2]));

      expect(component['vectorSource'].getFeatureById(1).get('highlighted')).toBeTruthy();
      expect(component['vectorSource'].getFeatureById(2).get('highlighted')).toBeFalsy();

      highlightFeatureSubject.next(radVisFeature1);

      expect(component['vectorSource'].getFeatureById(1).get('highlighted')).toBeTruthy();
      expect(component['vectorSource'].getFeatureById(2).get('highlighted')).toBeFalsy();

      unhighlightFeatureSubject.next(radVisFeature1);

      expect(component['vectorSource'].getFeatureById(1).get('highlighted')).toBeTruthy();
      expect(component['vectorSource'].getFeatureById(2).get('highlighted')).toBeFalsy();

      highlightFeatureSubject.next(radVisFeature2);

      expect(component['vectorSource'].getFeatureById(1).get('highlighted')).toBeTruthy();
      expect(component['vectorSource'].getFeatureById(2).get('highlighted')).toBeTruthy();
    });
  });

  describe('signature', () => {
    let vectorSource: VectorSource;
    const styleFn = (): Style | Style[] => {
      return new Style();
    };

    beforeEach(() => {
      vectorSource = component['signaturVectorSource'];
      when(signaturStyleProviderService.getStyleInformation(anything())).thenResolve({
        attributnamen: [],
        styleFunction: styleFn,
      });
      when(signaturStyleProviderService.getLegendeForSignatur(anything())).thenResolve({
        entries: [],
        name: 'Test',
      });
      filteredListSubject.next([
        {
          ...defaultMassnahmeListView,
          geometry: {
            coordinates: [
              [0, 0],
              [0, 100],
            ],
            type: 'LineString',
          } as LineStringGeojson,
          umsetzungsstatus: Umsetzungsstatus.IDEE,
          sollStandard: SollStandard.BASISSTANDARD,
          massnahmenkategorien: ['NEUBAU_BAULICHE_RADVERKEHRSANLAGE_AB_160CM', 'MARKIERUNG_SICHERHEITSTRENNSTREIFEN'],
        } as MassnahmeListenView,
        {
          ...defaultMassnahmeListView,
          id: 2,
          geometry: {
            coordinates: [
              [
                [100, 0],
                [100, 100],
              ],
              [
                [100, 100],
                [200, 100],
              ],
            ],
            type: 'MultiLineString',
          } as MultiLineStringGeojson,
          umsetzungsstatus: Umsetzungsstatus.PLANUNG,
          sollStandard: SollStandard.RADSCHNELLVERBINDUNG,
          massnahmenkategorien: ['OBERFLAECHE_ASPHALTIEREN', 'MARKIERUNG_SICHERHEITSTRENNSTREIFEN'],
        } as MassnahmeListenView,
      ]);
    });

    it('should set properties on feature and apply selected signatur', fakeAsync(() => {
      const signatur = { name: 'TestSignatur', typ: SignaturTyp.MASSNAHME };
      signaturSubject.next(signatur);
      tick();
      expect(vectorSource.getFeatures().length).toBe(2);
      expect(vectorSource.getFeatures()[0].get('Umsetzungsstatus')).toEqual('Idee');
      expect(vectorSource.getFeatures()[0].get('sollStandard')).toEqual('Basisstandard');
      expect(vectorSource.getFeatures()[0].get('Maßnahmenkategorie')).toEqual('Neubau');
      expect((vectorSource.getFeatures()[0].getGeometry() as LineString).getCoordinates()).toEqual([
        [0, 0],
        [0, 100],
      ] as Coordinate[]);
      expect(vectorSource.getFeatures()[1].get('Umsetzungsstatus')).toEqual('Planung');
      expect(vectorSource.getFeatures()[1].get('sollStandard')).toEqual('Radschnellverbindung');
      // Markierung hat Prioritaet ueber Belag
      expect(vectorSource.getFeatures()[1].get('Maßnahmenkategorie')).toEqual('Markierung');
      expect((vectorSource.getFeatures()[1].getGeometry() as MultiLineString).getCoordinates()).toEqual([
        [
          [100, 0],
          [100, 100],
        ],
        [
          [100, 100],
          [200, 100],
        ],
      ] as Coordinate[][]);
      verify(signaturStyleProviderService.getStyleInformation(anything())).once();
      expect(capture(signaturStyleProviderService.getStyleInformation).last()[0]).toEqual(signatur);
      expect(component['signaturLayer'].getStyle()).toBe(styleFn);
    }));

    it('should not display massnahme if no linestring present', fakeAsync(() => {
      filteredListSubject.next([
        {
          ...defaultMassnahmeListView,
          geometry: {
            coordinates: [
              [100, 0],
              [100, 100],
            ],
            type: 'Point',
          },
          umsetzungsstatus: Umsetzungsstatus.PLANUNG,
          sollStandard: SollStandard.RADSCHNELLVERBINDUNG,
        },
      ]);
      const signatur = { name: 'TestSignatur', typ: SignaturTyp.MASSNAHME };
      signaturSubject.next(signatur);
      tick();
      expect(vectorSource.getFeatures()).toHaveSize(0);
    }));

    it('should remove signatur features on deselect signatur (NETZ)', fakeAsync(() => {
      signaturSubject.next({ name: 'TestSignatur', typ: SignaturTyp.MASSNAHME });
      tick();
      expect(vectorSource.getFeatures()).toHaveSize(2);

      signaturSubject.next({ name: 'TestSignatur', typ: SignaturTyp.NETZ });
      tick();
      expect(vectorSource.getFeatures()).toHaveSize(0);
    }));

    it('should remove signatur features on deselect signatur (null)', fakeAsync(() => {
      signaturSubject.next({ name: 'TestSignatur', typ: SignaturTyp.MASSNAHME });
      tick();
      expect(vectorSource.getFeatures()).toHaveSize(2);

      signaturSubject.next(null);
      tick();
      expect(vectorSource.getFeatures()).toHaveSize(0);
    }));

    it('should update features if filteredList changes', fakeAsync(() => {
      signaturSubject.next({ name: 'TestSignatur', typ: SignaturTyp.MASSNAHME });
      tick();
      expect(vectorSource.getFeatures().length).toBe(2);
      filteredListSubject.next([
        {
          ...defaultMassnahmeListView,
          geometry: {
            coordinates: [
              [1000, 0],
              [1000, 100],
            ],
            type: 'LineString',
          },
          umsetzungsstatus: Umsetzungsstatus.UMGESETZT,
          sollStandard: SollStandard.KEIN_STANDARD_ERFUELLT,
          massnahmenkategorien: ['NEUMARKIERUNG_RADFAHRSTREIFEN', 'AUSBAU_BESTEHENDEN_WEGES_NACH_QUALITAETSSTANDARD'],
        },
      ]);
      tick();

      expect(vectorSource.getFeatures().length).toBe(1);
      expect((vectorSource.getFeatures()[0].getGeometry() as LineString).getCoordinates()).toEqual([
        [1000, 0],
        [1000, 100],
      ]);
      expect(vectorSource.getFeatures()[0].get('Umsetzungsstatus')).toEqual('Umgesetzt');
      expect(vectorSource.getFeatures()[0].get('sollStandard')).toEqual('Kein Standard erfüllt');
      // Ausbau Prioritaet ueber Markierung
      expect(vectorSource.getFeatures()[0].get('Maßnahmenkategorie')).toEqual('Ausbau');
    }));
  });
});
