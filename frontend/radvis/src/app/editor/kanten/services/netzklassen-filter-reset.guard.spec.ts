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

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { KantenCreatorComponent } from 'src/app/editor/kanten/components/kanten-creator/kanten-creator.component';
import { netzklassenFilterResetGuard } from 'src/app/editor/kanten/services/netzklassen-filter-reset.guard';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe('netzklassenFilterResetGuard', () => {
  let mapQueryParamsService: MapQueryParamsService;
  beforeEach(() => {
    mapQueryParamsService = mock(MapQueryParamsService);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule.withRoutes([])],
      providers: [{ provide: MapQueryParamsService, useValue: instance(mapQueryParamsService) }],
    });
  });

  describe('with different netzklassen', () => {
    let previousNetzklassenfilter: Netzklassefilter[];
    let kanteCreatorComponentMock: KantenCreatorComponent;
    let currentRoute: ActivatedRouteSnapshot;
    let nextRouterStateMock: RouterStateSnapshot;

    beforeEach(() => {
      previousNetzklassenfilter = [Netzklassefilter.RADNETZ];

      when(mapQueryParamsService.updateQueryParams(anything(), anything(), anything())).thenCall(
        (opt, route, merge) => {
          return new MapQueryParams([], opt.netzklassen, null, null);
        }
      );

      kanteCreatorComponentMock = mock(KantenCreatorComponent);
      when(kanteCreatorComponentMock.previousNetzklassenfilter).thenReturn(previousNetzklassenfilter);

      currentRoute = new ActivatedRouteSnapshot();

      nextRouterStateMock = mock(RouterStateSnapshot);
      when(nextRouterStateMock.url).thenReturn(
        `/test?${new MapQueryParams([], Netzklassefilter.getAll(), null, null).toRoute()}`
      );
    });

    it('should revert to previous netzklassen', () => {
      let result: boolean | UrlTree;
      TestBed.runInInjectionContext(() => {
        result = netzklassenFilterResetGuard(
          instance(kanteCreatorComponentMock),
          currentRoute,
          instance(mock(RouterStateSnapshot)),
          instance(nextRouterStateMock)
        ) as boolean | UrlTree;
      });

      expect(MapQueryParams.fromRoute((result! as UrlTree).queryParams).netzklassen).toEqual(previousNetzklassenfilter);
      verify(mapQueryParamsService.updateQueryParams(anything(), anything(), anything())).once();
      expect(capture(mapQueryParamsService.updateQueryParams).last()[0]).toEqual({
        netzklassen: previousNetzklassenfilter,
      });
      expect(capture(mapQueryParamsService.updateQueryParams).last()[1]).toBe(currentRoute);
      expect(capture(mapQueryParamsService.updateQueryParams).last()[2]).toBeTrue();
    });

    it('should not build endless loop', () => {
      let result: boolean | UrlTree;
      TestBed.runInInjectionContext(() => {
        result = netzklassenFilterResetGuard(
          instance(kanteCreatorComponentMock),
          currentRoute,
          instance(mock(RouterStateSnapshot)),
          instance(nextRouterStateMock)
        ) as boolean | UrlTree;
      });

      when(nextRouterStateMock.url).thenReturn((result! as UrlTree).toString());
      TestBed.runInInjectionContext(() => {
        result = netzklassenFilterResetGuard(
          instance(kanteCreatorComponentMock),
          currentRoute,
          instance(mock(RouterStateSnapshot)),
          instance(nextRouterStateMock)
        ) as boolean | UrlTree;
      });

      expect(result!).toBeTrue();
    });
  });

  it('should not do anything if netzklassen set correctly', () => {
    const previousNetzklassenfilter = Netzklassefilter.getAll();

    when(mapQueryParamsService.updateQueryParams(anything(), anything(), anything())).thenCall((opt, route, merge) => {
      return new MapQueryParams([], opt.netzklassen, null, null);
    });

    const kanteCreatorComponent = mock(KantenCreatorComponent);
    when(kanteCreatorComponent.previousNetzklassenfilter).thenReturn(previousNetzklassenfilter);

    const currentRoute = new ActivatedRouteSnapshot();

    const nextRouterState = mock(RouterStateSnapshot);
    when(nextRouterState.url).thenReturn(
      `/test?${new MapQueryParams([], Netzklassefilter.getAll(), null, null).toRoute()}`
    );

    let result: boolean | UrlTree;
    TestBed.runInInjectionContext(() => {
      result = netzklassenFilterResetGuard(
        instance(kanteCreatorComponent),
        currentRoute,
        instance(mock(RouterStateSnapshot)),
        instance(nextRouterState)
      ) as boolean | UrlTree;
    });

    expect(result!).toBeTrue();
  });
});
