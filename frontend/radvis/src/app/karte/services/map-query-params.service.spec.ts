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

import { ActivatedRoute, ActivatedRouteSnapshot, Params, Router } from '@angular/router';
import { MockService } from 'ng-mocks';
import { Subject } from 'rxjs';
import { bufferCount, take } from 'rxjs/operators';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { instance, mock } from 'ts-mockito';

describe('MapQueryParamsService', () => {
  let mapQueryParamsService: MapQueryParamsService;
  let activatedRoute: ActivatedRoute;
  let params$: Subject<Params>;
  let params: Params;

  beforeEach(() => {
    params$ = new Subject();
    activatedRoute = MockService(ActivatedRoute, {
      queryParams: params$,
      snapshot: {
        queryParams: params,
      } as ActivatedRouteSnapshot,
    });
    mapQueryParamsService = new MapQueryParamsService(activatedRoute, instance(mock(Router)));
  });

  describe('mapQueryParams$', () => {
    it('should convert params - layers', (done: DoneFn) => {
      mapQueryParamsService.layers$.subscribe(layers => {
        expect(layers).toEqual(['TEST_LAYER_1', 'TEST_LAYER_2']);
        done();
      });
      params$.next({
        layers: 'TEST_LAYER_1;TEST_LAYER_2',
        view: '1;2;3;4',
      });
    });

    it('should convert params - extent', (done: DoneFn) => {
      mapQueryParamsService.view$.subscribe(view => {
        expect(view).toEqual([1, 2, 3, 4]);
        done();
      });
      params$.next({
        layers: 'TEST_LAYER_1;TEST_LAYER_2',
        view: '1;2;3;4',
      });
    });

    it('should work on undefined - layers', (done: DoneFn) => {
      mapQueryParamsService.layers$.subscribe(layers => {
        expect(layers).toEqual([]);
        done();
      });
      params$.next({
        layers: undefined,
        view: undefined,
      });
    });

    it('should work on undefined - view', (done: DoneFn) => {
      mapQueryParamsService.view$.subscribe(view => {
        expect(view).toBeNull();
        done();
      });
      params$.next({
        layers: undefined,
        view: undefined,
      });
    });

    it('should work on empty lists - layers', (done: DoneFn) => {
      mapQueryParamsService.layers$.subscribe(layers => {
        expect(layers).toEqual([]);
        done();
      });
      params$.next({
        layers: '',
        view: '',
      });
    });

    it('should work on empty lists - view', (done: DoneFn) => {
      mapQueryParamsService.view$.subscribe(view => {
        expect(view).toBeNull();
        done();
      });
      params$.next({
        layers: '',
        view: '',
      });
    });

    it('should work on one element lists - layers', (done: DoneFn) => {
      mapQueryParamsService.layers$.subscribe(layers => {
        expect(layers).toEqual(['TEST_LAYER_1']);
        done();
      });
      params$.next({
        layers: 'TEST_LAYER_1',
        view: '1',
      });
    });

    it('should work on one element lists - view', (done: DoneFn) => {
      mapQueryParamsService.view$.subscribe(view => {
        expect(view).toBeNull();
        done();
      });
      params$.next({
        layers: 'TEST_LAYER_1',
        view: '1',
      });
    });

    it('should only emit when distinct', (done: DoneFn) => {
      mapQueryParamsService.layers$.pipe(take(2), bufferCount(2)).subscribe(elements => {
        expect(elements[1]).toEqual(['TEST_LAYER_2']);
        done();
      });
      params$.next({
        layers: 'TEST_LAYER_1',
      });
      params$.next({
        layers: 'TEST_LAYER_1',
      });
      params$.next({
        layers: 'TEST_LAYER_2',
      });
    });
  });

  describe('mapQueryParamsSnapshot', () => {
    it('should convert params', () => {
      activatedRoute.snapshot.queryParams = {
        layers: 'TEST_LAYER_1;TEST_LAYER_2',
        view: '1;2;3;4',
      };
      expect(mapQueryParamsService.mapQueryParamsSnapshot.layers).toEqual(['TEST_LAYER_1', 'TEST_LAYER_2']);
      expect(mapQueryParamsService.mapQueryParamsSnapshot.view).toEqual([1, 2, 3, 4]);
    });

    it('should work on undefined', () => {
      activatedRoute.snapshot.queryParams = {
        layers: undefined,
        view: undefined,
      };
      expect(mapQueryParamsService.mapQueryParamsSnapshot.layers).toEqual([]);
      expect(mapQueryParamsService.mapQueryParamsSnapshot.view).toBeNull();
    });

    it('should work on empty lists', () => {
      activatedRoute.snapshot.queryParams = {
        layers: '',
        view: '',
      };
      expect(mapQueryParamsService.mapQueryParamsSnapshot.layers).toEqual([]);
      expect(mapQueryParamsService.mapQueryParamsSnapshot.view).toBeNull();
    });

    it('should work on one element lists', () => {
      activatedRoute.snapshot.queryParams = {
        layers: 'TEST_LAYER_1',
        view: '1',
      };
      expect(mapQueryParamsService.mapQueryParamsSnapshot.layers).toEqual(['TEST_LAYER_1']);
      expect(mapQueryParamsService.mapQueryParamsSnapshot.view).toBeNull();
    });
  });
});
