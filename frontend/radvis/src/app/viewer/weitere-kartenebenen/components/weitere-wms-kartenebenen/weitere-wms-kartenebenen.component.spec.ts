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
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { WeitereKartenebenenModule } from 'src/app/viewer/weitere-kartenebenen/weitere-kartenebenen.module';
import { instance, mock, when } from 'ts-mockito';
import { WeitereWmsKartenebenenComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-wms-kartenebenen/weitere-wms-kartenebenen.component';
import { Feature } from 'ol';
import Geometry from 'ol/geom/Geometry';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import { LineString, Point } from 'ol/geom';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { Subject } from 'rxjs';

describe(WeitereWmsKartenebenenComponent.name, () => {
  let component: WeitereWmsKartenebenenComponent;
  let fixture: MockedComponentFixture<WeitereWmsKartenebenenComponent>;
  let featureHighlightService: FeatureHighlightService;
  let highlight$$: Subject<RadVisFeature>;
  let unhighlight$$: Subject<RadVisFeature>;

  beforeEach(() => {
    highlight$$ = new Subject();
    unhighlight$$ = new Subject();

    featureHighlightService = mock(FeatureHighlightService);
    when(featureHighlightService.highlightedFeature$).thenReturn(highlight$$.asObservable());
    when(featureHighlightService.unhighlightedFeature$).thenReturn(unhighlight$$.asObservable());

    return MockBuilder(WeitereWmsKartenebenenComponent, WeitereKartenebenenModule)
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

  const testUrl = 'https://testurl.de?LAYERS=any_layer';
  const layerId = 10;

  beforeEach(() => {
    fixture = MockRender(WeitereWmsKartenebenenComponent, {
      url: testUrl,
      name: 'testName',
      deckkraft: 1,
      zoomstufe: 8.7,
      zindex: 1000,
      layerId,
      quelle: 'Testquelle',
    } as unknown as WeitereWmsKartenebenenComponent);
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
      const featuresSpy = jasmine.createSpy();
      component['getFeaturesCallback']([0, 0], 0).then(fs => {
        featuresSpy(fs);
      });

      const req = httpMock.expectOne(request => {
        return request.params.get('QUERY_LAYERS') === request.params.get('LAYERS');
      });
      const gml = `
<wfs:FeatureCollection xmlns:ms="http://mapserver.gis.umn.edu/mapserver" xmlns:wfs="http://www.opengis.net/wfs" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd                         http://mapserver.gis.umn.edu/mapserver http://aneto.oco/cgi-bin/worldwfs?SERVICE=WFS&amp;VERSION=1.0.0&amp;REQUEST=DescribeFeatureType&amp;TYPENAME=polygon&amp;OUTPUTFORMAT=XMLSCHEMA">
      <gml:boundedBy>
        <gml:Box srsName="EPSG:4326">
          <gml:coordinates>-0.768746,47.003018 3.002191,47.925567</gml:coordinates>
        </gml:Box>
      </gml:boundedBy>
  <gml:featureMember>
    <entity fid="id1">
      <attrib1>attrib1_value</attrib1>
      <attrib2container>
        <attrib2>attrib2_value</attrib2>
      </attrib2container>
      <location1>
          <gml:Point><gml:coordinates>3,50</gml:coordinates></gml:Point>
      </location1>
    </entity>
  </gml:featureMember>
  <gml:featureMember>
    <entity fid="id2">
      <attrib1>attrib1_value2</attrib1>
      <attrib2container>
        <attrib2>attrib2_value2</attrib2>
      </attrib2container>
      <location1>
          <gml:Point><gml:coordinates>2,20</gml:coordinates></gml:Point>
      </location1>
    </entity>
  </gml:featureMember>
</wfs:FeatureCollection>
`;
      req.flush(gml);

      tick();
      const features: Feature<Geometry>[] = featuresSpy.calls.mostRecent().args[0];
      expect(features).toHaveSize(2);
      expect(features[0].get(WeitereKartenebene.LAYER_ID_KEY)).toEqual(layerId);
      expect(features[0].get(WeitereKartenebene.EXTERNE_WMS_FEATURE_ID_PROPERTY_NAME)).toEqual('id1');
      expect((features[0].getGeometry() as Point).getCoordinates()).toEqual([3, 50, 0]);
      expect(features[0].get('attrib1')).toEqual('attrib1_value');
      expect(features[0].get('attrib2container')).toEqual({ attrib2: 'attrib2_value' });

      expect(features[1].get(WeitereKartenebene.LAYER_ID_KEY)).toEqual(layerId);
      expect(features[1].get(WeitereKartenebene.EXTERNE_WMS_FEATURE_ID_PROPERTY_NAME)).toEqual('id2');
      expect((features[1].getGeometry() as Point).getCoordinates()).toEqual([2, 20, 0]);
      expect(features[1].get('attrib1')).toEqual('attrib1_value2');
      expect(features[1].get('attrib2container')).toEqual({ attrib2: 'attrib2_value2' });
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
      feature.set(WeitereKartenebene.EXTERNE_WMS_FEATURE_ID_PROPERTY_NAME, 'A');
      feature.setId(1);
    });

    it('should highlight', () => {
      highlight$$.next(
        RadVisFeature.ofAttributesMap(
          // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
          null,
          feature.getProperties(),
          WeitereKartenebene.LAYER_NAME,
          // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
          feature.getGeometry()!
        )
      );

      expect(component['highlightLayer']?.getSource().getFeatures()).toHaveSize(1);

      expect(component['highlightLayer']?.getSource().getFeatures()[0].getId()).toEqual('A');
    });

    it('should unhighlight', () => {
      const highlightedFeature = RadVisFeature.ofAttributesMap(
        null,
        feature.getProperties(),
        WeitereKartenebene.LAYER_NAME,
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        feature.getGeometry()!
      );
      highlight$$.next(highlightedFeature);

      unhighlight$$.next(highlightedFeature);
      expect(component['highlightLayer']?.getSource().getFeatures()).toHaveSize(0);
    });

    it('should filter by layer name for highlighted', () => {
      const highlightedFeatureNichtExtern = RadVisFeature.ofAttributesMap(
        null,
        feature.getProperties(),
        'TestLayerName',
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        feature.getGeometry()!
      );
      highlight$$.next(highlightedFeatureNichtExtern);

      expect(component['highlightLayer']?.getSource().getFeatures()).toHaveSize(0);
    });

    it('should filter by layer id for highlighted', () => {
      const properties = { ...feature.getProperties() };
      properties[WeitereKartenebene.LAYER_ID_KEY] = 65345;
      const highlightedFeatureNichtExtern = RadVisFeature.ofAttributesMap(
        null,
        properties,
        WeitereKartenebene.LAYER_NAME,
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        feature.getGeometry()!
      );
      highlight$$.next(highlightedFeatureNichtExtern);

      expect(component['highlightLayer']?.getSource().getFeatures()).toHaveSize(0);
    });

    it('should filter by layer name for unhighlighted', () => {
      highlight$$.next(
        RadVisFeature.ofAttributesMap(
          null,
          feature.getProperties(),
          WeitereKartenebene.LAYER_NAME,
          // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
          feature.getGeometry()!
        )
      );

      const highlightedFeatureNichtExtern = RadVisFeature.ofAttributesMap(
        null,
        feature.getProperties(),
        'TestLayerName',
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        feature.getGeometry()!
      );
      unhighlight$$.next(highlightedFeatureNichtExtern);

      expect(component['highlightLayer']?.getSource().getFeatures()).toHaveSize(1);

      expect(component['highlightLayer']?.getSource().getFeatures()[0].getId()).toEqual('A');
    });

    it('should filter by layer id for unhighlighted', () => {
      highlight$$.next(
        RadVisFeature.ofAttributesMap(
          null,
          feature.getProperties(),
          WeitereKartenebene.LAYER_NAME,
          // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
          feature.getGeometry()!
        )
      );

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

      expect(component['highlightLayer']?.getSource().getFeatures()).toHaveSize(1);

      expect(component['highlightLayer']?.getSource().getFeatures()[0].getId()).toEqual('A');
    });
  });
});
