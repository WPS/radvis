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

import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { instance, mock, when } from 'ts-mockito';
import { FeatureHighlightService } from 'src/app/viewer/viewer-shared/services/feature-highlight.service';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { AbstractInfrastrukturLayerComponent } from 'src/app/viewer/viewer-shared/components/abstract-infrastruktur-layer.component';
import { Point } from 'ol/geom';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { FurtenKreuzungenLayerComponent } from 'src/app/viewer/furten-kreuzungen/components/furten-kreuzungen-layer/furten-kreuzungen-layer.component';
import { FurtenKreuzungenModule } from 'src/app/viewer/furten-kreuzungen/furten-kreuzungen.module';
import { FurtenKreuzungenRoutingService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen-routing.service';
import { FurtenKreuzungenFilterService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen-filter.service';
import { FurtKreuzungListenView } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-listen-view';
import { BehaviorSubject, Subject } from 'rxjs';

describe(FurtenKreuzungenLayerComponent.name, () => {
  let component: FurtenKreuzungenLayerComponent;
  let fixture: MockedComponentFixture<FurtenKreuzungenLayerComponent>;

  let olMapComponent: OlMapComponent;
  let featureHighlightService: FeatureHighlightService;
  let furtenKreuzungenRoutingService: FurtenKreuzungenRoutingService;
  let furtenKreuzungenFilterService: FurtenKreuzungenFilterService;

  const filteredListSubject: BehaviorSubject<FurtKreuzungListenView[]> = new BehaviorSubject<FurtKreuzungListenView[]>(
    []
  );
  const selectedFurtenKreuzungenIdTestSubject = new BehaviorSubject<number | null>(null);
  const highlightFeatureSubject = new Subject<RadVisFeature>();
  const unhighlightFeatureSubject = new Subject<RadVisFeature>();

  beforeEach(() => {
    olMapComponent = mock(OlMapComponent);
    featureHighlightService = mock(FeatureHighlightService);
    furtenKreuzungenRoutingService = mock(FurtenKreuzungenRoutingService);
    furtenKreuzungenFilterService = mock(FurtenKreuzungenFilterService);

    when(furtenKreuzungenFilterService.filteredList$).thenReturn(filteredListSubject);
    when(furtenKreuzungenRoutingService.selectedInfrastrukturId$).thenReturn(
      selectedFurtenKreuzungenIdTestSubject.asObservable()
    );

    when(featureHighlightService.highlightedFeature$).thenReturn(highlightFeatureSubject.asObservable());
    when(featureHighlightService.unhighlightedFeature$).thenReturn(unhighlightFeatureSubject.asObservable());

    return MockBuilder(FurtenKreuzungenLayerComponent, [FurtenKreuzungenModule])
      .provide({ provide: OlMapService, useValue: instance(olMapComponent) })
      .provide({ provide: FeatureHighlightService, useValue: instance(featureHighlightService) })
      .provide({ provide: FurtenKreuzungenRoutingService, useValue: instance(furtenKreuzungenRoutingService) })
      .provide({ provide: FurtenKreuzungenFilterService, useValue: instance(furtenKreuzungenFilterService) });
  });

  beforeEach(() => {
    fixture = MockRender(FurtenKreuzungenLayerComponent);
    component = fixture.point.componentInstance;
    component['vectorSource']['loader_']();
  });

  it('should create feature', () => {
    // Arrange
    const listView = {
      id: 123,
      iconPosition: { type: 'Point', coordinates: [1, 2] } as PointGeojson,
    } as FurtKreuzungListenView;

    // Act
    filteredListSubject.next([listView]);

    // Assert
    const features = component['vectorSource'].getFeatures();
    expect(features).toHaveSize(1);
    expect(features[0].getId()).toEqual(listView.id);
    expect((features[0].getGeometry() as Point).getCoordinates()).toEqual(listView.iconPosition!.coordinates);
    expect(features[0].get(AbstractInfrastrukturLayerComponent.BEZEICHNUNG_PROPERTY_NAME)).toEqual('Furt/Kreuzung');
  });

  it('should create no feature for list view without point', () => {
    // Arrange
    const listView = {
      id: 123,
      iconPosition: undefined,
    } as FurtKreuzungListenView;

    // Act
    filteredListSubject.next([listView]);

    // Assert
    const features = component['vectorSource'].getFeatures();
    expect(features).toHaveSize(0);
  });
});
