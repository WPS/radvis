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

import { BarriereLayerComponent } from 'src/app/viewer/barriere/components/barriere-layer/barriere-layer.component';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { instance, mock, when } from 'ts-mockito';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { BarriereModule } from 'src/app/viewer/barriere/barriere.module';
import { BarriereRoutingService } from 'src/app/viewer/barriere/services/barriere-routing.service';
import { BarriereFilterService } from 'src/app/viewer/barriere/services/barriere-filter.service';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { BarriereListenView } from 'src/app/viewer/barriere/models/barriere-listen-view';
import { PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import VectorSource from 'ol/source/Vector';
import { AbstractInfrastrukturLayerComponent } from 'src/app/viewer/viewer-shared/components/abstract-infrastruktur-layer.component';
import { Point } from 'ol/geom';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { BehaviorSubject, Subject } from 'rxjs';

describe(BarriereLayerComponent.name, () => {
  let component: BarriereLayerComponent;
  let fixture: MockedComponentFixture<BarriereLayerComponent>;

  let olMapComponent: OlMapComponent;
  let featureHighlightService: FeatureHighlightService;
  let barriereRoutingService: BarriereRoutingService;
  let barriereFilterService: BarriereFilterService;

  const filteredListSubject: BehaviorSubject<BarriereListenView[]> = new BehaviorSubject<BarriereListenView[]>([]);
  const selectedBarriereIdTestSubject = new BehaviorSubject<number | null>(null);
  const highlightFeatureSubject = new Subject<RadVisFeature>();
  const unhighlightFeatureSubject = new Subject<RadVisFeature>();

  beforeEach(() => {
    olMapComponent = mock(OlMapComponent);
    featureHighlightService = mock(FeatureHighlightService);
    barriereRoutingService = mock(BarriereRoutingService);
    barriereFilterService = mock(BarriereFilterService);

    when(barriereFilterService.filteredList$).thenReturn(filteredListSubject);
    when(barriereRoutingService.selectedInfrastrukturId$).thenReturn(selectedBarriereIdTestSubject.asObservable());

    when(featureHighlightService.highlightedFeature$).thenReturn(highlightFeatureSubject.asObservable());
    when(featureHighlightService.unhighlightedFeature$).thenReturn(unhighlightFeatureSubject.asObservable());

    return MockBuilder(BarriereLayerComponent, [BarriereModule])
      .provide({ provide: OlMapService, useValue: instance(olMapComponent) })
      .provide({ provide: FeatureHighlightService, useValue: instance(featureHighlightService) })
      .provide({ provide: BarriereRoutingService, useValue: instance(barriereRoutingService) })
      .provide({ provide: BarriereFilterService, useValue: instance(barriereFilterService) });
  });

  beforeEach(() => {
    fixture = MockRender(BarriereLayerComponent);
    component = fixture.point.componentInstance;
    // @ts-expect-error Migration von ts-ignore
    component['vectorSource']['loader_']();
  });

  it('should create feature', () => {
    // Arrange
    const listView = {
      id: 123,
      iconPosition: { type: 'Point', coordinates: [1, 2] } as PointGeojson,
    } as BarriereListenView;

    // Act
    filteredListSubject.next([listView]);

    // Assert
    const features = component['vectorSource'].getFeatures();
    expect(features).toHaveSize(1);
    expect(features[0].getId()).toEqual(listView.id);
    expect((features[0].getGeometry() as Point).getCoordinates()).toEqual(listView.iconPosition!.coordinates);
    expect(features[0].get(AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME)).toEqual('Barriere');
  });

  it('should create no feature for list view without point', () => {
    // Arrange
    const listView = {
      id: 123,
      iconPosition: undefined,
    } as BarriereListenView;

    // Act
    filteredListSubject.next([listView]);

    // Assert
    const features = component['vectorSource'].getFeatures();
    expect(features).toHaveSize(0);
  });
});
