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

import { ComponentFixture } from '@angular/core/testing';
import { MockBuilder, MockRender } from 'ng-mocks';
import { Observable } from 'rxjs';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { RadnetzMatchingComponent } from 'src/app/radnetzmatching/components/radnetz-matching/radnetz-matching.component';
import { RadnetzMatchingModule } from 'src/app/radnetzmatching/radnetz-matching.module';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { instance, mock, when } from 'ts-mockito';

describe('RadnetzMatchingComponent', () => {
  const mapQueryParamsSnapshot: MapQueryParams = new MapQueryParams([], [], null, null);
  let component: RadnetzMatchingComponent;
  let fixture: ComponentFixture<RadnetzMatchingComponent>;

  let mapQueryParamsService: MapQueryParamsService;
  let olMapService: OlMapService;

  beforeEach(() => {
    mapQueryParamsService = mock(MapQueryParamsService);
    when(mapQueryParamsService.mapQueryParamsSnapshot).thenReturn(mapQueryParamsSnapshot);
    when(mapQueryParamsService.layers$).thenReturn(new Observable<string[]>());

    olMapService = mock(OlMapService);

    return MockBuilder(RadnetzMatchingComponent, RadnetzMatchingModule)
      .provide({
        provide: MapQueryParamsService,
        useValue: instance(mapQueryParamsService),
      })
      .provide({
        provide: OlMapService,
        useValue: instance(olMapService),
      });
  });

  beforeEach(() => {
    fixture = MockRender(RadnetzMatchingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
