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

import { HttpClient, HttpEvent } from '@angular/common/http';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ImageTile } from 'ol';
import { Point } from 'ol/geom';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { FeatureProperties } from 'src/app/shared/models/feature-properties';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { FahrradrouteLayerComponent } from 'src/app/viewer/fahrradroute/components/fahrradroute-layer/fahrradroute-layer.component';
import { FahrradrouteListenView } from 'src/app/viewer/fahrradroute/models/fahrradroute-listen-view';
import { testFahrradrouteListenView } from 'src/app/viewer/fahrradroute/models/fahrradroute-listen-view-test-data-provider.spec';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { FahrradrouteFilterService } from 'src/app/viewer/fahrradroute/services/fahrradroute-filter.service';
import { FahrradrouteRoutingService } from 'src/app/viewer/fahrradroute/services/fahrradroute-routing.service';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { toRadVisFeatureAttributesFromMap } from 'src/app/shared/models/rad-vis-feature-attributes';
import stringMatching = jasmine.stringMatching;

describe(FahrradrouteLayerComponent.name, () => {
  let component: FahrradrouteLayerComponent;
  let fixture: ComponentFixture<FahrradrouteLayerComponent>;

  let fahrradrouteFilterService: FahrradrouteFilterService;
  let fahrradrouteRoutingService: FahrradrouteRoutingService;
  let featureHighlightService: FeatureHighlightService;
  let httpClient: HttpClient;

  let selectedFahrradrouteIdTestSubject: BehaviorSubject<number | null>;
  let fahrradroutenListenViewsSubject: BehaviorSubject<FahrradrouteListenView[]>;
  const highlightFeatureSubject = new Subject<RadVisFeature>();
  const unhighlightFeatureSubject = new Subject<RadVisFeature>();

  beforeEach(async () => {
    selectedFahrradrouteIdTestSubject = new BehaviorSubject<number | null>(null);
    fahrradrouteFilterService = mock(FahrradrouteFilterService);
    fahrradrouteRoutingService = mock(FahrradrouteRoutingService);
    httpClient = mock(HttpClient);
    when(httpClient.post<Blob>(anything(), anything(), anything())).thenReturn({
      toPromise: () => {
        return Promise.resolve(new Blob([], { type: 'image/png' }));
      },
    } as Observable<HttpEvent<Blob>>);

    featureHighlightService = mock(FeatureHighlightService);
    fahrradroutenListenViewsSubject = new BehaviorSubject<FahrradrouteListenView[]>(testFahrradrouteListenView);
    when(fahrradrouteFilterService.filteredList$).thenReturn(fahrradroutenListenViewsSubject);
    when(fahrradrouteFilterService.currentFilteredList).thenCall(() => fahrradroutenListenViewsSubject.value);
    when(fahrradrouteFilterService.getById(anything())).thenCall(id => {
      const listenView = fahrradroutenListenViewsSubject.value.find(flv => flv.id === id);
      return Promise.resolve({ ...listenView, geometrie: listenView?.geometry });
    });
    when(fahrradrouteRoutingService.selectedInfrastrukturId$).thenReturn(
      selectedFahrradrouteIdTestSubject.asObservable()
    );
    when(featureHighlightService.highlightedFeature$).thenReturn(highlightFeatureSubject.asObservable());

    when(featureHighlightService.unhighlightedFeature$).thenReturn(unhighlightFeatureSubject.asObservable());
    await TestBed.configureTestingModule({
      declarations: [FahrradrouteLayerComponent],
      providers: [
        { provide: OlMapService, useValue: instance(mock(OlMapComponent)) },
        { provide: FahrradrouteFilterService, useValue: instance(fahrradrouteFilterService) },
        { provide: FahrradrouteRoutingService, useValue: instance(fahrradrouteRoutingService) },
        { provide: FeatureHighlightService, useValue: instance(featureHighlightService) },
        { provide: HttpClient, useValue: instance(httpClient) },
      ],
    }).compileComponents();
  });

  const createComponent = (): void => {
    fixture = TestBed.createComponent(FahrradrouteLayerComponent);
    component = fixture.componentInstance;
    const mockedImageElement = mock(ImageTile);
    when(mockedImageElement.getImage()).thenReturn(mock(instance(HTMLImageElement)));
    // @ts-expect-error Migration von ts-ignore
    component['source'].getImageLoadFunction()(instance(mockedImageElement), '/testUrl/wms');

    selectedFahrradrouteIdTestSubject.next(null);
  };

  beforeEach(fakeAsync(() => {
    createComponent();
    tick();
  }));

  it('should create and make correct wms call', () => {
    expect(component).toBeTruthy();
    verify(httpClient.post(anything(), anything(), anything())).called();
    expect(capture(httpClient.post).last()[0]).toEqual(stringMatching('^/testUrl/wms'));
    expect(capture(httpClient.post).last()[1].get('CQL_FILTER')).toEqual(
      `variante_kategorie IS NULL AND id IN (${fahrradroutenListenViewsSubject.value.map(flv => flv.id)})`
    );
  });

  describe('change selection', () => {
    it('should set feature properties correctly on selection change', fakeAsync(() => {
      selectedFahrradrouteIdTestSubject.next(1);
      tick();

      expect(component.getFeatureByFahrradroutenId(1)[0].get('highlighted')).toBeTrue();
      expect(component.getFeatureByFahrradroutenId(2)).toHaveSize(0);

      selectedFahrradrouteIdTestSubject.next(2);
      tick();

      expect(component.getFeatureByFahrradroutenId(1)[0].get('highlighted')).toBeFalsy();
      expect(component.getFeatureByFahrradroutenId(2)[0].get('highlighted')).toBeTrue();
    }));
  });

  describe('highlight from highlightService', () => {
    it('should set feature properties correctly when highlightService triggers', fakeAsync(() => {
      // arrange
      const radVisFeature1 = new RadVisFeature(
        null,
        toRadVisFeatureAttributesFromMap([[FeatureProperties.FAHRRADROUTE_ID_PROPERTY_NAME, 1]]),
        FAHRRADROUTE.name,
        new Point([1, 2])
      );
      const radVisFeature2 = new RadVisFeature(
        null,
        toRadVisFeatureAttributesFromMap([[FeatureProperties.FAHRRADROUTE_ID_PROPERTY_NAME, 2]]),
        FAHRRADROUTE.name,
        new Point([1, 2])
      );

      expect(component.getFeatureByFahrradroutenId(1)).toHaveSize(0);
      expect(component.getFeatureByFahrradroutenId(2)).toHaveSize(0);

      highlightFeatureSubject.next(radVisFeature1);
      tick();

      expect(component.getFeatureByFahrradroutenId(1)[0].get('highlighted')).toBeTruthy();
      expect(component.getFeatureByFahrradroutenId(2)).toHaveSize(0);

      unhighlightFeatureSubject.next(radVisFeature1);
      tick();

      expect(component.getFeatureByFahrradroutenId(1)[0].get('highlighted')).toBeFalsy();
      expect(component.getFeatureByFahrradroutenId(2)).toHaveSize(0);

      highlightFeatureSubject.next(radVisFeature2);
      tick();

      expect(component.getFeatureByFahrradroutenId(1)[0].get('highlighted')).toBeFalsy();
      expect(component.getFeatureByFahrradroutenId(2)[0].get('highlighted')).toBeTruthy();
    }));
  });
});
