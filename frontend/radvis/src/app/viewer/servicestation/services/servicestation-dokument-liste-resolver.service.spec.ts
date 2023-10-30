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
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { ActivatedRoute, Router, RouterOutlet } from '@angular/router';
import { MockBuilder, MockRender, NG_MOCKS_GUARDS, ngMocks } from 'ng-mocks';
import { anything, instance, mock, when } from 'ts-mockito';
import { ViewerRoutingModule } from 'src/app/viewer/viewer-routing.module';
import { fakeAsync, tick } from '@angular/core/testing';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { take } from 'rxjs/operators';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { ServicestationService } from 'src/app/viewer/servicestation/services/servicestation.service';
import { ServicestationDokumentListeResolverService } from 'src/app/viewer/servicestation/services/servicestation-dokument-liste-resolver.service';
import { DokumentListeComponent } from 'src/app/viewer/dokument/components/dokument-liste/dokument-liste.component';
import { ServicestationToolComponent } from 'src/app/viewer/servicestation/components/servicestation-tool/servicestation-tool.component';
import { defaultServicestation } from 'src/app/viewer/servicestation/models/servicestation-testdata-provider.spec';

describe(ServicestationDokumentListeResolverService.name, () => {
  let servicestationService: ServicestationService;
  let mapQueryParamsService: MapQueryParamsService;

  beforeEach(() => {
    servicestationService = mock(ServicestationService);
    mapQueryParamsService = mock(MapQueryParamsService);
    when(mapQueryParamsService.signatur$).thenReturn(of(null));
  });

  beforeEach(() =>
    MockBuilder([ViewerRoutingModule, RouterTestingModule.withRoutes([])], ViewerModule)
      .keep(ViewerComponent)
      .keep(ServicestationToolComponent)
      .exclude(NG_MOCKS_GUARDS)
      .provide({
        provide: ServicestationService,
        useValue: instance(servicestationService),
      })
      .provide({
        provide: MapQueryParamsService,
        useValue: instance(mapQueryParamsService),
      })
  );

  // It is important to run routing tests in fakeAsync.
  it('should emit data if list remains empty', fakeAsync(() => {
    const fixture = MockRender(RouterOutlet);
    const router: Router = fixture.point.injector.get(Router);
    const location: Location = fixture.point.injector.get(Location);

    when(servicestationService.getDokumentListe(1)).thenResolve({ canEdit: true, dokumente: [] });
    when(servicestationService.getDokumentListe(2)).thenResolve({ canEdit: true, dokumente: [] });
    when(servicestationService.resolve(anything(), anything())).thenResolve(defaultServicestation);

    // Let's switch to the route with the resolver.
    location.go('/viewer/servicestation/1/dateien');

    // Now we can initialize navigation.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => router.initialNavigation());
      tick(); // is needed for rendering of the current route.
    }

    // Checking that we are on the right page.
    expect(location.path()).toEqual('/viewer/servicestation/1/dateien');

    // Let's extract ActivatedRoute of the current component.
    const el = ngMocks.find(DokumentListeComponent);
    const route: ActivatedRoute = el.injector.get(ActivatedRoute);

    let counter = 0;

    route.data.pipe(take(2)).subscribe(() => {
      counter++;
    });

    // Let's switch to the route with the resolver.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => router.navigateByUrl('/viewer/servicestation/2/dateien'));
      tick(); // is needed for rendering of the current route.
    }

    expect(location.path()).toEqual('/viewer/servicestation/2/dateien');
    expect(counter).toBe(2);
  }));
});
