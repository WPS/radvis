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
import { MockBuilder, MockRender, ngMocks, NG_MOCKS_GUARDS } from 'ng-mocks';
import { of } from 'rxjs';
import { take } from 'rxjs/operators';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { KommentarListeComponent } from 'src/app/viewer/kommentare/components/kommentar-liste/kommentar-liste.component';
import { MassnahmenToolComponent } from 'src/app/viewer/massnahme/components/massnahmen-tool/massnahmen-tool.component';
import { defaultMassnahmeToolView } from 'src/app/viewer/massnahme/models/massnahme-tool-view-test-data-provider';
import { MassnahmeKommentarListeResolverService } from 'src/app/viewer/massnahme/services/massnahme-kommentar-liste-resolver.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { ViewerRoutingModule } from 'src/app/viewer/viewer-routing.module';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, instance, mock, when } from 'ts-mockito';

describe(MassnahmeKommentarListeResolverService.name, () => {
  let massnahmeService: MassnahmeService;
  let mapQueryParamsService: MapQueryParamsService;

  beforeEach(() => {
    massnahmeService = mock(MassnahmeService);

    mapQueryParamsService = mock(MapQueryParamsService);
    when(mapQueryParamsService.signatur$).thenReturn(of(null));
  });

  beforeEach(() =>
    MockBuilder([ViewerRoutingModule, RouterTestingModule.withRoutes([])], ViewerModule)
      .keep(ViewerComponent)
      .keep(MassnahmenToolComponent)
      .exclude(NG_MOCKS_GUARDS)
      .provide({
        provide: MassnahmeService,
        useValue: instance(massnahmeService),
      })
      .provide({
        provide: MapQueryParamsService,
        useValue: instance(mapQueryParamsService),
      })
  );

  // It is important to run routing tests in fakeAsync.
  it('provides data to on the route', fakeAsync(() => {
    const fixture = MockRender(RouterOutlet);
    const router: Router = fixture.point.injector.get(Router);
    const location: Location = fixture.point.injector.get(Location);

    when(massnahmeService.getKommentarListe(1)).thenResolve([]);
    when(massnahmeService.getKommentarListe(2)).thenResolve([]);
    when(massnahmeService.getMassnahmeToolView(anything())).thenResolve(defaultMassnahmeToolView);
    when(massnahmeService.getBenachrichtigungsFunktion(anything())).thenResolve(false);
    // Let's switch to the route with the resolver.
    location.go('/viewer/massnahmen/1/kommentare');

    // Now we can initialize navigation.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => router.initialNavigation());
      tick(); // is needed for rendering of the current route.
    }

    // Checking that we are on the right page.
    expect(location.path()).toEqual('/viewer/massnahmen/1/kommentare');

    // Let's extract ActivatedRoute of the current component.
    const el = ngMocks.find(KommentarListeComponent);
    const route: ActivatedRoute = el.injector.get(ActivatedRoute);

    let counter = 0;

    route.data.pipe(take(2)).subscribe(() => {
      counter++;
    });

    // Let's switch to the route with the resolver.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => router.navigateByUrl('/viewer/massnahmen/2/kommentare'));
      tick(); // is needed for rendering of the current route.
    }

    expect(location.path()).toEqual('/viewer/massnahmen/2/kommentare');
    expect(counter).toBe(2);
  }));
});
