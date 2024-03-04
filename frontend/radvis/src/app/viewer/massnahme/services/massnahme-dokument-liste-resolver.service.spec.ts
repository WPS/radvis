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
import { of } from 'rxjs';
import { take } from 'rxjs/operators';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { MassnahmenToolComponent } from 'src/app/viewer/massnahme/components/massnahmen-tool/massnahmen-tool.component';
import { defaultMassnahmeToolView } from 'src/app/viewer/massnahme/models/massnahme-tool-view-test-data-provider';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { ViewerRoutingModule } from 'src/app/viewer/viewer-routing.module';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, instance, mock, when } from 'ts-mockito';
import { DokumentListeComponent } from 'src/app/viewer/dokument/components/dokument-liste/dokument-liste.component';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { MassnahmeModule } from 'src/app/viewer/massnahme/massnahme.module';

describe('MassnahmeDokumentListeResolver', () => {
  let massnahmeService: MassnahmeService;
  let mapQueryParamsService: MapQueryParamsService;

  beforeEach(() => {
    massnahmeService = mock(MassnahmeService);
    mapQueryParamsService = mock(MapQueryParamsService);
    when(mapQueryParamsService.signatur$).thenReturn(of(null));
  });

  beforeEach(() =>
    MockBuilder([ViewerRoutingModule, RouterTestingModule.withRoutes([])], [ViewerModule, MassnahmeModule])
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
  it('should emit data if list remains empty', fakeAsync(() => {
    const fixture = MockRender(RouterOutlet, {});
    const router: Router = fixture.point.injector.get(Router);
    const location: Location = fixture.point.injector.get(Location);

    when(massnahmeService.getDokumentListe(1)).thenResolve({ canEdit: true, dokumente: [] });
    when(massnahmeService.getDokumentListe(2)).thenResolve({ canEdit: true, dokumente: [] });
    when(massnahmeService.getMassnahmeToolView(anything())).thenResolve(defaultMassnahmeToolView);
    when(massnahmeService.getBenachrichtigungsFunktion(anything())).thenResolve(false);

    // Let's switch to the route with the resolver.
    location.go('/viewer/massnahmen/1/dateien');

    // Now we can initialize navigation.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => router.initialNavigation());
      tick(); // is needed for rendering of the current route.
    }

    fixture.detectChanges();

    // Checking that we are on the right page.
    expect(location.path()).toEqual('/viewer/massnahmen/1/dateien');

    // Let's extract ActivatedRoute of the current component.
    const el = ngMocks.find(DokumentListeComponent);
    const route: ActivatedRoute = el.injector.get(ActivatedRoute);

    let counter = 0;

    route.data.pipe(take(2)).subscribe(() => {
      counter++;
    });

    // Let's switch to the route with the resolver.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => router.navigateByUrl('/viewer/massnahmen/2/dateien'));
      tick(); // is needed for rendering of the current route.
    }

    expect(location.path()).toEqual('/viewer/massnahmen/2/dateien');
    expect(counter).toBe(2);
  }));
});
