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
import { HttpClientModule } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { Feature } from 'ol';
import { LineString } from 'ol/geom';
import { Subject } from 'rxjs';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { WeitereKartenebenenModule } from 'src/app/viewer/weitere-kartenebenen/weitere-kartenebenen.module';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { instance, mock, when } from 'ts-mockito';
import { WeitereWfsKartenebenenComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-wfs-kartenebenen/weitere-wfs-kartenebenen.component';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';

describe(WeitereWfsKartenebenenComponent.name, () => {
  let component: WeitereWfsKartenebenenComponent;
  let fixture: MockedComponentFixture<WeitereWfsKartenebenenComponent>;
  let featureHighlightService: FeatureHighlightService;
  let highlight$$: Subject<RadVisFeature>;
  let unhighlight$$: Subject<RadVisFeature>;

  beforeEach(() => {
    highlight$$ = new Subject();
    unhighlight$$ = new Subject();

    featureHighlightService = mock(FeatureHighlightService);
    when(featureHighlightService.highlightedFeature$).thenReturn(highlight$$.asObservable());
    when(featureHighlightService.unhighlightedFeature$).thenReturn(unhighlight$$.asObservable());

    return MockBuilder(WeitereWfsKartenebenenComponent, WeitereKartenebenenModule)
      .replace(HttpClientModule, HttpClientTestingModule)
      .provide({
        provide: OlMapService,
        useValue: instance(mock(OlMapComponent)),
      })
      .provide({
        provide: FeatureHighlightService,
        useValue: instance(featureHighlightService),
      })
      .provide({
        provide: NotifyUserService,
        useValue: instance(mock(NotifyUserService)),
      });
  });

  const testUrl = 'https://testurl.de';
  beforeEach(() => {
    fixture = MockRender(WeitereWfsKartenebenenComponent, {
      url: testUrl,
      deckkraft: 1,
      minZoom: 8.7,
      zindex: 1000,
      layerId: 10,
      quelle: 'Testquelle',
    } as unknown as WeitereWfsKartenebenenComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('convert to feature', () => {
    let httpMock: HttpTestingController;

    beforeEach(() => {
      httpMock = fixture.point.injector.get(HttpTestingController);
    });

    it('should set layer id and reset feature id', fakeAsync(() => {
      component['load']([0, 0, 0, 0]);

      const req = httpMock.expectOne(
        `${testUrl}/?request=GetFeature&version=1.1.0&srsname=EPSG%3A25832&outputFormat=application%2Fjson&bbox=0%2C0%2C0%2C0%2CEPSG%3A25832`
      );
      const geojson: GeoJSON.FeatureCollection = {
        type: 'FeatureCollection',
        features: [
          {
            type: 'Feature',
            id: 'v_at_kreis.1071161986',
            geometry: {
              type: 'LineString',
              coordinates: [
                [0, 0],
                [10, 10],
              ],
            },
            properties: {
              testAttribut: 'TestValue1',
            },
          },
          {
            type: 'Feature',
            id: 'v_at_kreis.123',
            geometry: {
              type: 'LineString',
              coordinates: [
                [100, 100],
                [200, 200],
              ],
            },
            properties: {
              testAttribut: 'TestValue2',
            },
          },
        ],
      };
      req.flush(geojson);

      tick();

      const features = component['source'].getFeatures();
      expect(features.length).toBe(2);

      expect((features[0].getGeometry() as LineString).getCoordinates()).toEqual(
        (geojson.features[0].geometry as GeoJSON.LineString).coordinates
      );
      expect(features[0].getId()).toEqual(1);
      expect(features[0].get(component['FEATURE_ID_PROPERTY_NAME'])).toEqual(geojson.features[0].id);
      expect(features[0].get(WeitereKartenebene.LAYER_ID_KEY)).toEqual(component.layerId);
      expect(features[0].get(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME)).toBeFalse();
      expect(features[0].get('testAttribut')).toEqual(geojson.features[0].properties?.testAttribut);

      expect((features[1].getGeometry() as LineString).getCoordinates()).toEqual(
        (geojson.features[1].geometry as GeoJSON.LineString).coordinates
      );
      expect(features[1].getId()).toEqual(2);
      expect(features[1].get(component['FEATURE_ID_PROPERTY_NAME'])).toEqual(geojson.features[1].id);
      expect(features[1].get(WeitereKartenebene.LAYER_ID_KEY)).toEqual(component.layerId);
      expect(features[1].get(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME)).toBeFalse();
      expect(features[1].get('testAttribut')).toEqual(geojson.features[1].properties?.testAttribut);
    }));
  });

  describe('highlight feature', () => {
    let feature: Feature;

    beforeEach(() => {
      feature = new Feature(
        new LineString([
          [0, 0],
          [10, 10],
        ])
      );
      feature.set(WeitereKartenebene.LAYER_ID_KEY, component.layerId);
      feature.set(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME, false);
      feature.setId(1);
      component['source'].addFeature(feature);
    });

    it('should highlight', () => {
      highlight$$.next(
        RadVisFeature.ofAttributesMap(
          // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
          +feature.getId()!,
          feature.getProperties(),
          WeitereKartenebene.LAYER_NAME,
          // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
          feature.getGeometry()!
        )
      );

      expect(
        component['source'].getFeatures()[0].get(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME)
      ).toBeTrue();
    });

    it('should unhighlight', () => {
      const highlightedFeature = RadVisFeature.ofAttributesMap(
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        +feature.getId()!,
        feature.getProperties(),
        WeitereKartenebene.LAYER_NAME,
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        feature.getGeometry()!
      );
      highlight$$.next(highlightedFeature);

      unhighlight$$.next(highlightedFeature);
      expect(
        component['source'].getFeatures()[0].get(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME)
      ).toBeFalse();
    });

    it('should filter by layer name for highlighted', () => {
      const highlightedFeatureNichtExtern = RadVisFeature.ofAttributesMap(
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        +feature.getId()!,
        feature.getProperties(),
        'TestLayerName',
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        feature.getGeometry()!
      );
      highlight$$.next(highlightedFeatureNichtExtern);

      expect(
        component['source'].getFeatures()[0].get(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME)
      ).toBeFalse();
    });

    it('should filter by layer id for highlighted', () => {
      const properties = { ...feature.getProperties() };
      properties[WeitereKartenebene.LAYER_ID_KEY] = 65345;
      const highlightedFeatureNichtExtern = RadVisFeature.ofAttributesMap(
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        +feature.getId()!,
        properties,
        WeitereKartenebene.LAYER_NAME,
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        feature.getGeometry()!
      );
      highlight$$.next(highlightedFeatureNichtExtern);

      expect(
        component['source'].getFeatures()[0].get(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME)
      ).toBeFalse();
    });

    it('should filter by layer name for highlighted', () => {
      feature.set(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME, true);
      const highlightedFeatureNichtExtern = RadVisFeature.ofAttributesMap(
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        +feature.getId()!,
        feature.getProperties(),
        'TestLayerName',
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        feature.getGeometry()!
      );
      unhighlight$$.next(highlightedFeatureNichtExtern);

      expect(
        component['source'].getFeatures()[0].get(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME)
      ).toBeTrue();
    });

    it('should filter by layer id for highlighted', () => {
      feature.set(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME, true);
      const properties = { ...feature.getProperties() };
      properties[WeitereKartenebene.LAYER_ID_KEY] = 65345;
      const highlightedFeatureNichtExtern = RadVisFeature.ofAttributesMap(
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        +feature.getId()!,
        properties,
        WeitereKartenebene.LAYER_NAME,
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        feature.getGeometry()!
      );
      unhighlight$$.next(highlightedFeatureNichtExtern);

      expect(
        component['source'].getFeatures()[0].get(WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME)
      ).toBeTrue();
    });
  });
});
