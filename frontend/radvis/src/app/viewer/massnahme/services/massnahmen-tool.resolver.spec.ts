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

import { Location } from '@angular/common';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router, RouterOutlet } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MockBuilder, MockRender, NG_MOCKS_GUARDS, ngMocks } from 'ng-mocks';
import { NEVER, of } from 'rxjs';
import { take } from 'rxjs/operators';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { MassnahmenToolComponent } from 'src/app/viewer/massnahme/components/massnahmen-tool/massnahmen-tool.component';
import { defaultMassnahmeToolView } from 'src/app/viewer/massnahme/models/massnahme-tool-view-test-data-provider';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { massnahmenToolResolver } from 'src/app/viewer/massnahme/services/massnahmen-tool.resolver';
import { ViewerRoutingModule } from 'src/app/viewer/viewer-routing.module';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { instance, mock, when } from 'ts-mockito';

describe(massnahmenToolResolver.name, () => {
  let massnahmeService: MassnahmeService;
  let mapQueryParamsService: MapQueryParamsService;

  beforeEach(() => {
    massnahmeService = mock(MassnahmeService);
    mapQueryParamsService = mock(MapQueryParamsService);
    when(mapQueryParamsService.signatur$).thenReturn(of(null));
  });

  beforeEach(() => {
    const infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(NEVER);
    return MockBuilder([ViewerRoutingModule, RouterTestingModule.withRoutes([])], ViewerModule)
      .keep(ViewerComponent)
      .exclude(NG_MOCKS_GUARDS)
      .provide({
        provide: MassnahmeService,
        useValue: instance(massnahmeService),
      })
      .provide({
        provide: MapQueryParamsService,
        useValue: instance(mapQueryParamsService),
      })
      .provide({
        provide: InfrastrukturenSelektionService,
        useValue: instance(infrastrukturenSelektionService),
      });
  });

  // It is important to run routing tests in fakeAsync.
  it('provides data to on the route', fakeAsync(() => {
    const fixture = MockRender(RouterOutlet, {});
    const router: Router = fixture.point.injector.get(Router);
    const location: Location = fixture.point.injector.get(Location);

    when(massnahmeService.getMassnahmeToolView(1)).thenResolve({ ...defaultMassnahmeToolView, id: 1 });
    when(massnahmeService.getMassnahmeToolView(2)).thenResolve({ ...defaultMassnahmeToolView, id: 2 });
    // Let's switch to the route with the resolver.
    location.go('/viewer/massnahmen/1');

    // Now we can initialize navigation.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => router.initialNavigation());
      tick(); // is needed for rendering of the current route.
    }

    fixture.detectChanges();
    // Checking that we are on the right page.
    expect(location.path()).toEqual('/viewer/massnahmen/1/eigenschaften');

    // Let's extract ActivatedRoute of the current component.
    const el = ngMocks.find(MassnahmenToolComponent);
    const route: ActivatedRoute = el.injector.get(ActivatedRoute);

    let counter = 0;
    const expectedIds = [1, 2];

    route.data.pipe(take(2)).subscribe(data => {
      expect(data.massnahme.id).toEqual(expectedIds[counter]);
      counter++;
    });

    // Let's switch to the route with the resolver.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => router.navigateByUrl('/viewer/massnahmen/2'));
      tick(); // is needed for rendering of the current route.
    }

    expect(location.path()).toEqual('/viewer/massnahmen/2/eigenschaften');
    expect(counter).toBe(2);
  }));
});
