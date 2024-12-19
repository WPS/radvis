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

import { BreakpointObserver } from '@angular/cdk/layout';
import { Location } from '@angular/common';
import { fakeAsync, tick } from '@angular/core/testing';
import { MatTableModule } from '@angular/material/table';
import { RouterOutlet } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MockBuilder, MockRender, NG_MOCKS_GUARDS, ngMocks } from 'ng-mocks';
import { LineString } from 'ol/geom';
import { NEVER, of } from 'rxjs';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { RadVisFeature } from 'src/app/shared/models/rad-vis-feature';
import { toRadVisFeatureAttributesFromMap } from 'src/app/shared/models/rad-vis-feature-attributes';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { ViewerRoutingModule } from 'src/app/viewer/viewer-routing.module';
import { DetailFeatureTableComponent } from 'src/app/viewer/viewer-shared/components/detail-feauture-table/detail-feature-table.component';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { WeitereKartenebenenDetailViewComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-kartenebenen-detail-view/weitere-kartenebenen-detail-view.component';
import { WeitereWfsKartenebenenComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-wfs-kartenebenen/weitere-wfs-kartenebenen.component';
import { WeitereKartenebene } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene';
import { WeitereKartenebeneTyp } from 'src/app/viewer/weitere-kartenebenen/models/weitere-kartenebene-typ';
import { WeitereKartenebenenRoutingService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen-routing.service';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { instance, mock, when } from 'ts-mockito';

describe(WeitereKartenebenenRoutingService.name, () => {
  let mapQueryParamsService: MapQueryParamsService;
  let weitereKartenebenenService: WeitereKartenebenenService;

  beforeEach(() => {
    mapQueryParamsService = mock(MapQueryParamsService);
    when(mapQueryParamsService.signatur$).thenReturn(of(null));
    weitereKartenebenenService = mock(WeitereKartenebenenService);
  });

  beforeEach(() => {
    const infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(NEVER);
    return MockBuilder([ViewerRoutingModule, RouterTestingModule.withRoutes([])], ViewerModule)
      .keep(ViewerComponent)
      .keep(WeitereKartenebenenDetailViewComponent)
      .keep(DetailFeatureTableComponent)
      .keep(WeitereKartenebenenRoutingService)
      .keep(MatTableModule)
      .keep(BreakpointObserver)
      .exclude(NG_MOCKS_GUARDS)
      .provide({
        provide: MapQueryParamsService,
        useValue: instance(mapQueryParamsService),
      })
      .provide({
        provide: InfrastrukturenSelektionService,
        useValue: instance(infrastrukturenSelektionService),
      })
      .provide({
        provide: WeitereKartenebenenService,
        useValue: instance(weitereKartenebenenService),
      });
  });

  // It is important to run routing tests in fakeAsync.
  it('should invoke detail view with data', fakeAsync(() => {
    const fixture = MockRender(RouterOutlet, {}, true);
    const routingService: WeitereKartenebenenRoutingService = fixture.point.injector.get(
      WeitereKartenebenenRoutingService
    );
    const location: Location = fixture.point.injector.get(Location);

    const geometry = new LineString([
      [0, 0],
      [10, 10],
    ]);
    const weitereKartenebenenId = 10;
    const feature: RadVisFeature = {
      attributes: toRadVisFeatureAttributesFromMap([
        ['test', 'TestValue'],
        [WeitereKartenebene.LAYER_ID_KEY, weitereKartenebenenId],
        ['geometry', geometry],
        [WeitereWfsKartenebenenComponent.HIGHLIGHTED_PROPERTY_NAME, false],
      ]),
      id: 1,
      geometry,
      isKnoten: false,
      istStrecke: false,
      kanteZweiseitig: false,
      layer: WeitereKartenebene.LAYER_NAME,
    };
    const layerName = 'Meine Weitere Kartenebene';
    when(weitereKartenebenenService.weitereKartenebenen).thenReturn([
      {
        deckkraft: 1,
        id: weitereKartenebenenId,
        name: layerName,
        weitereKartenebeneTyp: WeitereKartenebeneTyp.WFS,
        url: 'abc',
        zoomstufe: 8,
        quellangabe: 'Testquelle',
        zindex: 1,
      },
    ]);
    // Now we can initialize navigation.
    if (fixture.ngZone) {
      fixture.ngZone.run(() => routingService.routeToFeature(feature));
      tick(); // is needed for rendering of the current route.
    }

    // Checking that we are on the right page.
    expect(
      location
        .path()
        .startsWith(
          `/viewer/${WeitereKartenebenenRoutingService.ROUTE_LAYER}/10/${WeitereKartenebenenRoutingService.ROUTE_FEATURE}/1`
        )
    ).toBeTrue();

    fixture.detectChanges();
    tick();

    const el = ngMocks.find(WeitereKartenebenenDetailViewComponent);
    const tableRows = ngMocks.findAll(el, 'tr.mat-mdc-row');
    expect(tableRows.length).toBe(1);
    const tablecells = ngMocks.findAll(tableRows[0], 'td');
    expect(tablecells.length).toBe(2);
    expect(tablecells[0].nativeElement.innerText).toEqual('test');
    expect(tablecells[1].nativeElement.innerText).toEqual('TestValue');

    const header = ngMocks.find(el, 'h2');
    expect(header.nativeElement.innerText).toEqual(layerName);
  }));
});
