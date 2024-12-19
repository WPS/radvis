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
import { ActivatedRoute, RouterOutlet } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MockBuilder, MockRender, NG_MOCKS_GUARDS, ngMocks } from 'ng-mocks';
import { Coordinate } from 'ol/coordinate';
import { NEVER, of } from 'rxjs';
import { take } from 'rxjs/operators';
import { Richtung } from 'src/app/editor/kanten/models/richtung';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { KanteDetailView } from 'src/app/shared/models/kante-detail-view';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { KantenDetailViewComponent } from 'src/app/viewer/netz-details/components/kanten-detail-view/kanten-detail-view.component';
import { kanteDetailsResolver } from 'src/app/viewer/netz-details/services/kante-details.resolver';
import { NetzdetailRoutingService } from 'src/app/viewer/netz-details/services/netzdetail-routing.service';
import { NetzdetailService } from 'src/app/viewer/netz-details/services/netzdetail.service';
import { ViewerRoutingModule } from 'src/app/viewer/viewer-routing.module';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, instance, mock, when } from 'ts-mockito';

describe(kanteDetailsResolver.name, () => {
  let netzService: NetzdetailService;
  let mapQueryParamsService: MapQueryParamsService;

  beforeEach(() => {
    netzService = mock(NetzdetailService);
    mapQueryParamsService = mock(MapQueryParamsService);
    when(mapQueryParamsService.signatur$).thenReturn(of(null));
  });

  beforeEach(() => {
    const infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(NEVER);
    return MockBuilder([ViewerRoutingModule, RouterTestingModule.withRoutes([])], ViewerModule)
      .keep(ViewerComponent)
      .keep(NetzdetailRoutingService)
      .exclude(NG_MOCKS_GUARDS)
      .provide({
        provide: NetzdetailService,
        useValue: instance(netzService),
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
    const routingService: NetzdetailRoutingService = fixture.point.injector.get(NetzdetailRoutingService);
    const location: Location = fixture.point.injector.get(Location);

    when(netzService.getKanteForView(anything(), anything(), anything())).thenCall((id, position, seite) =>
      Promise.resolve(detailViewFromRoute(id, seite, position))
    );

    // Now we can initialize navigation.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => routingService.toKanteDetails(1, [0, 0], KantenSeite.LINKS));
      tick(); // is needed for rendering of the current route.
    }

    fixture.detectChanges();

    // Checking that we are on the right page.
    expect(location.path().startsWith('/viewer/kante/1')).toBeTrue();

    // Let's extract ActivatedRoute of the current component.
    const el = ngMocks.find(KantenDetailViewComponent);
    const route: ActivatedRoute = el.injector.get(ActivatedRoute);

    let counter = 0;
    const expected: KanteDetailView[] = [
      detailViewFromRoute(1, KantenSeite.LINKS, NetzdetailRoutingService.buildParams([0, 0]).position),
      detailViewFromRoute(1, KantenSeite.RECHTS, NetzdetailRoutingService.buildParams([0, 0]).position),
      detailViewFromRoute(1, KantenSeite.RECHTS, NetzdetailRoutingService.buildParams([1, 1]).position),
    ];

    route.data.pipe(take(3)).subscribe(data => {
      expect(data.kante).toEqual(expected[counter]);
      counter++;
    });

    // Let's switch to the route with the resolver.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => routingService.toKanteDetails(1, [0, 0], KantenSeite.LINKS));
      tick(); // is needed for rendering of the current route.
      fixture.ngZone.run(() => routingService.toKanteDetails(1, [0, 0], KantenSeite.RECHTS));
      tick(); // is needed for rendering of the current route.
      fixture.ngZone.run(() => routingService.toKanteDetails(1, [1, 1], KantenSeite.RECHTS));
      tick();
    }

    expect(counter).toBe(3);
  }));

  const detailViewFromRoute = (id: number, seite: KantenSeite, position: Coordinate): KanteDetailView => {
    return {
      id,
      geometrie: {
        coordinates: [
          [0, 0],
          [1, 1],
        ],
        type: 'LineString',
      },
      seite,
      attributeAnPosition: { Test: JSON.stringify(position) },
      attributeAufGanzerLaenge: {},
      trennstreifenAttribute: {},
      trennstreifenEinseitig: true,
      trennstreifenRichtungLinks: Richtung.UNBEKANNT,
      trennstreifenRichtungRechts: Richtung.UNBEKANNT,
    };
  };
});
