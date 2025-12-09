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

import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MockComponents } from 'ng-mocks';
import { Feature, Map, MapBrowserEvent } from 'ol';
import { buffer, getCenter } from 'ol/extent';
import { MultiLineString, Point } from 'ol/geom';
import TileLayer from 'ol/layer/Tile';
import VectorLayer from 'ol/layer/Vector';
import { TileWMS } from 'ol/source';
import VectorSource from 'ol/source/Vector';
import { FehlerprotokollLayerComponent } from 'src/app/fehlerprotokoll/components/fehlerprotokoll-layer/fehlerprotokoll-layer.component';
import { HintergrundAuswahlComponent } from 'src/app/karte/components/hintergrund-auswahl/hintergrund-auswahl.component';
import { HintergrundLayerComponent } from 'src/app/karte/components/hintergrund-layer/hintergrund-layer.component';
import { KarteButtonComponent } from 'src/app/karte/components/karte-button/karte-button.component';
import { KarteMenuItemComponent } from 'src/app/karte/components/karte-menu-item/karte-menu-item.component';
import { LegendeComponent } from 'src/app/karte/components/legende/legende.component';
import { NetzklassenAuswahlComponent } from 'src/app/karte/components/netzklassen-auswahl/netzklassen-auswahl.component';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { OrtsSucheComponent } from 'src/app/karte/components/orts-suche/orts-suche.component';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { instance, mock, when } from 'ts-mockito';

@Component({
  template: ` <rad-ol-map #component style="position: absolute; top: 0; left:0; width: 1000px; height: 500px">
    <div style="width:200px;" class="right-details"></div>
    <div style="height:150px;" class="unten"></div>
    <div style="width:250px" class="left-menu"></div>
  </rad-ol-map>`,
  standalone: false,
})
class TestWrapperComponent {
  @ViewChild('component')
  component!: OlMapComponent;
}

describe(OlMapComponent.name, () => {
  let hostComponent: TestWrapperComponent;
  let hostFixture: ComponentFixture<TestWrapperComponent>;
  let mapQueryParamsService: MapQueryParamsService;
  let map: Map;

  beforeEach(async () => {
    mapQueryParamsService = mock(MapQueryParamsService);

    when(mapQueryParamsService.mapQueryParamsSnapshot).thenReturn({
      view: [50000, 5000000, 1050000, 5500000],
    } as MapQueryParams);

    await TestBed.configureTestingModule({
      declarations: [
        TestWrapperComponent,
        OlMapComponent,
        ...MockComponents(
          HintergrundAuswahlComponent,
          HintergrundLayerComponent,
          KarteMenuItemComponent,
          KarteButtonComponent,
          NetzklassenAuswahlComponent,
          OrtsSucheComponent,
          LegendeComponent
        ),
      ],
      imports: [ReactiveFormsModule, MatIconModule],
      providers: [
        { provide: MapQueryParamsService, useValue: instance(mapQueryParamsService) },
        { provide: FeatureTogglzService, useValue: instance(mock(FeatureTogglzService)) },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    hostFixture = TestBed.createComponent(TestWrapperComponent);
    hostComponent = hostFixture.componentInstance;
    hostFixture.detectChanges();
    map = hostComponent.component['map']!;
  });

  describe(OlMapComponent.prototype.scrollIntoViewByCoordinate.name, () => {
    beforeEach(done => {
      map.once('postrender', () => {
        done();
      });
    });

    it('should not scroll if coordinate is visible', () => {
      const spy = spyOn(map.getView(), 'centerOn').and.callThrough();
      hostComponent.component.scrollIntoViewByCoordinate([500000, 5250000]);
      expect(spy).not.toHaveBeenCalled();
    });

    it('should scroll if coordinate is not visible because it is covered by elements', done => {
      const spy = spyOn(map.getView(), 'centerOn').and.callThrough();

      const coordinate = [500000, 5100000];
      const middleOfVisibleMap = [525, 175];
      map.once('postrender', () => {
        expect(spy).toHaveBeenCalled();
        expect(spy.calls.mostRecent().args[2]).toEqual(middleOfVisibleMap);

        const pixelFromCoordinate = map.getPixelFromCoordinate(coordinate);
        expect(pixelFromCoordinate).toEqual(middleOfVisibleMap);
        done();
      });
      hostComponent.component.scrollIntoViewByCoordinate(coordinate);
    });

    it('should scroll if coordinate is not visible because it is not inside the mapviewport', done => {
      const spy = spyOn(map.getView(), 'centerOn').and.callThrough();

      const coordinate = [590000, 5900000];
      const middleOfVisibleMap = [525, 175];
      map.once('postrender', () => {
        expect(spy).toHaveBeenCalled();
        expect(spy.calls.mostRecent().args[2]).toEqual(middleOfVisibleMap);

        const pixelFromCoordinate = map.getPixelFromCoordinate(coordinate);
        expect(pixelFromCoordinate).toEqual(middleOfVisibleMap);
        done();
      });

      hostComponent.component.scrollIntoViewByCoordinate(coordinate);
    });
  });

  describe(OlMapComponent.prototype.scrollIntoViewByGeometry.name, () => {
    beforeEach(done => {
      map.once('postrender', () => {
        done();
      });
    });

    it('should scroll to fahrradroute', done => {
      const spyCenterOn = spyOn(map.getView(), 'centerOn').and.callThrough();
      const spyFit = spyOn(map.getView(), 'fit').and.callThrough();

      const coordinate = [590000, 5900000];
      const coordinateVerschoben = [600000, 6000000];
      const geometry = new MultiLineString([[coordinate, coordinateVerschoben]]);
      const middleOfVisibleMap = [525, 175];
      map.once('postrender', () => {
        expect(spyFit).toHaveBeenCalled();
        expect(spyFit.calls.mostRecent().args[0]).toEqual(buffer(geometry.getExtent(), 50));
        expect(spyCenterOn).toHaveBeenCalled();
        expect(spyCenterOn.calls.mostRecent().args[2]).toEqual(middleOfVisibleMap);

        const pixelFromCoordinate = map.getPixelFromCoordinate(getCenter(geometry.getExtent()));
        expect(pixelFromCoordinate).toEqual(middleOfVisibleMap);
        done();
      });

      hostComponent.component.scrollIntoViewByGeometry(geometry);
    });
  });

  describe('with click', () => {
    let locationSelectedSpy: jasmine.Spy<jasmine.Func>;
    let locationSelectedOutputSpy: jasmine.Spy<jasmine.Func>;
    let wmsFeatureCallbackSpy: jasmine.Spy<jasmine.Func>;

    beforeEach(fakeAsync(() => {
      const feature = new Feature<Point>(new Point([0, 0]));
      feature.set(OlMapService.LAYER_ID, FehlerprotokollLayerComponent.LAYER_ID);
      const vectorSource = new VectorSource<Feature<Point>>();
      vectorSource.addFeature(feature);
      const vectorLayer = new VectorLayer({ source: vectorSource });
      map.addLayer(vectorLayer);
      const wmsLayer = new TileLayer({ source: new TileWMS() });

      wmsFeatureCallbackSpy = jasmine.createSpy();
      hostComponent.component.addWMSFeatureLayer(wmsLayer, () => {
        wmsFeatureCallbackSpy();
        return Promise.resolve([]);
      });

      locationSelectedSpy = jasmine.createSpy();
      hostComponent.component.locationSelected$().subscribe(locationSelectedSpy);

      locationSelectedOutputSpy = jasmine.createSpy();
      hostComponent.component.locationSelect.subscribe(locationSelectedOutputSpy);

      map.dispatchEvent({ type: 'click', coordinate: [0, 0], pixel: [0, 0] } as MapBrowserEvent<PointerEvent>);
      tick();
    }));

    it('should call selection once', () => {
      expect(locationSelectedSpy).toHaveBeenCalledTimes(1);
      expect(locationSelectedOutputSpy).toHaveBeenCalledTimes(1);
    });

    it('should call wmsFeatureCallback once', () => {
      expect(wmsFeatureCallbackSpy).toHaveBeenCalledTimes(1);
    });
  });
});
