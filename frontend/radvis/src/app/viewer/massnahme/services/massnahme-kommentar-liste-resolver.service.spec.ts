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
import { MockBuilder, MockRender, NG_MOCKS_GUARDS, NG_MOCKS_ROOT_PROVIDERS, ngMocks } from 'ng-mocks';
import { of } from 'rxjs';
import { take } from 'rxjs/operators';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { MassnahmenToolComponent } from 'src/app/viewer/massnahme/components/massnahmen-tool/massnahmen-tool.component';
import { defaultMassnahmeToolView } from 'src/app/viewer/massnahme/models/massnahme-tool-view-test-data-provider';
import { massnahmeKommentarListeResolver } from 'src/app/viewer/massnahme/services/massnahme-kommentar-liste.resolver';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { ViewerRoutingModule } from 'src/app/viewer/viewer-routing.module';
import { anything, instance, mock, when } from 'ts-mockito';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { KommentarListeComponent } from 'src/app/viewer/kommentare/components/kommentar-liste/kommentar-liste.component';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';

describe(massnahmeKommentarListeResolver.name, () => {
  let massnahmeService: MassnahmeService;
  let mapQueryParamsService: MapQueryParamsService;

  beforeEach(() => {
    massnahmeService = mock(MassnahmeService);

    mapQueryParamsService = mock(MapQueryParamsService);
    when(mapQueryParamsService.signatur$).thenReturn(of(null));
  });

  beforeEach(() =>
    MockBuilder([ViewerRoutingModule, RouterTestingModule.withRoutes([]), NG_MOCKS_ROOT_PROVIDERS], [ViewerModule])
      .keep(InfrastrukturToken)
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
      .provide({
        provide: BenutzerDetailsService,
        useValue: instance(mock(BenutzerDetailsService)),
      })
  );

  // It is important to run routing tests in fakeAsync.
  it('should provide data onto the route', fakeAsync(() => {
    const fixture = MockRender(RouterOutlet, {});
    const router: Router = fixture.point.injector.get(Router);
    const location: Location = fixture.point.injector.get(Location);

    when(massnahmeService.getKommentarListe(1)).thenResolve([
      {
        kommentarText: 'text1',
        benutzer: 'nutzer1',
        datum: 'heute',
        fromLoggedInUser: false,
      },
    ]);
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

    fixture.detectChanges();

    // Checking that we are on the right page.
    expect(location.path()).toEqual('/viewer/massnahmen/1/kommentare');

    // Let's extract ActivatedRoute of the current component.
    const el = ngMocks.find(KommentarListeComponent);
    const route: ActivatedRoute = el.injector.get(ActivatedRoute);

    expect(route.snapshot.data).toEqual({
      kommentare: {
        massnahmeId: 1,
        liste: [
          {
            kommentarText: 'text1',
            benutzer: 'nutzer1',
            datum: 'heute',
            fromLoggedInUser: false,
          },
        ],
      },
    });

    // Let's switch to the route with the resolver.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => router.navigateByUrl('/viewer/massnahmen/2/kommentare'));
      tick(); // is needed for rendering of the current route.
    }

    expect(location.path()).toEqual('/viewer/massnahmen/2/kommentare');
    expect(route.snapshot.data).toEqual({
      kommentare: {
        massnahmeId: 2,
        liste: [],
      },
    });
  }));

  it('should emit data if list remains empty', fakeAsync(() => {
    const fixture = MockRender(RouterOutlet, {});
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

    fixture.detectChanges();

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
