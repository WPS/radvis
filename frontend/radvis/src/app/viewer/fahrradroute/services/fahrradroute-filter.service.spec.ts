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

import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { FahrradrouteListenView } from 'src/app/viewer/fahrradroute/models/fahrradroute-listen-view';
import { testFahrradrouteListenView } from 'src/app/viewer/fahrradroute/models/fahrradroute-listen-view-test-data-provider.spec';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { FahrradrouteService } from 'src/app/viewer/fahrradroute/services/fahrradroute.service';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { FilterQueryParams } from 'src/app/viewer/viewer-shared/models/filter-query-params';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, instance, mock, verify, when } from 'ts-mockito';
import { FahrradrouteFilterService } from './fahrradroute-filter.service';

describe(FahrradrouteFilterService.name, () => {
  let fahrradrouteFilterService: FahrradrouteFilterService;
  let fahrradrouteService: FahrradrouteService;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let selektierteInfrastrukturen$: BehaviorSubject<Infrastruktur[]>;
  let filterQueryParamsService: FilterQueryParamsService;
  let filter$: BehaviorSubject<Map<Infrastruktur, FieldFilter[]>>;
  const testFahrradrouten: FahrradrouteListenView[] = testFahrradrouteListenView;

  beforeEach(() => {
    fahrradrouteService = mock(FahrradrouteService);
    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    filterQueryParamsService = mock(FilterQueryParamsService);
    when(fahrradrouteService.getAll()).thenResolve(testFahrradrouten);
    selektierteInfrastrukturen$ = new BehaviorSubject<Infrastruktur[]>([]);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(
      selektierteInfrastrukturen$.asObservable()
    );
    filter$ = new BehaviorSubject(new Map<Infrastruktur, FieldFilter[]>());
    when(filterQueryParamsService.filter$).thenReturn(filter$.asObservable());
    when(filterQueryParamsService.filterQueryParamsSnapshot).thenReturn(
      new FilterQueryParams(new Map<string, FieldFilter[]>())
    );
    return MockBuilder(FahrradrouteFilterService, ViewerModule)
      .provide({
        provide: FahrradrouteService,
        useValue: instance(fahrradrouteService),
      })
      .provide({
        provide: InfrastrukturenSelektionService,
        useValue: instance(infrastrukturenSelektionService),
      })
      .provide({
        provide: FilterQueryParamsService,
        useValue: instance(filterQueryParamsService),
      });
  });

  beforeEach(() => {
    fahrradrouteFilterService = TestBed.inject(FahrradrouteFilterService);
  });

  describe('selektierteInfrastrukturen$', () => {
    it('should refetch if fahrradrouten got activated', fakeAsync(() => {
      selektierteInfrastrukturen$.next([FAHRRADROUTE]);

      tick();

      verify(fahrradrouteService.getAll()).once();
      verify(filterQueryParamsService.filterQueryParamsSnapshot).once();
      expect().nothing();
    }));

    it('should clear if fahrradrouten got deactivated', () => {
      let receivedFahrradrouten: FahrradrouteListenView[] = [];
      const subscription = fahrradrouteFilterService.filteredList$.subscribe(fahrradrouten => {
        receivedFahrradrouten = fahrradrouten;
      });

      selektierteInfrastrukturen$.next([]);

      subscription.unsubscribe();
      expect(receivedFahrradrouten).toEqual([]);
    });
  });

  describe('refetchFahrradrouten', () => {
    it('should assign fetched fahrradrouten to alleFahrradrouteListenViews', fakeAsync(() => {
      let index = 0;
      const subscription = fahrradrouteFilterService.filteredList$.subscribe(fahrradrouten => {
        expect(fahrradrouten).toEqual([[], testFahrradrouten][index]);
        index++;
      });

      tick();
      fahrradrouteFilterService.refetchData();

      tick();
      subscription.unsubscribe();
    }));
  });

  describe('filterField', () => {
    it('no filter should not filter the list', fakeAsync(() => {
      let index = 0;
      const subscription = fahrradrouteFilterService.filteredList$.subscribe(fahrradrouten => {
        expect(fahrradrouten).toEqual([[], testFahrradrouten, testFahrradrouten][index]);
        index++;
      });

      tick();
      fahrradrouteFilterService.refetchData();

      tick();
      fahrradrouteFilterService.filterField('name', '');

      tick();
      verify(filterQueryParamsService.update(anything(), anything())).once();
      subscription.unsubscribe();
    }));

    it('should filter out the missmatching fahrradroute', fakeAsync(() => {
      let index = 0;
      const subscription = fahrradrouteFilterService.filteredList$.subscribe(fahrradrouten => {
        expect(fahrradrouten).toEqual([[], testFahrradrouten, [testFahrradrouten[1]]][index]);
        index++;
      });

      tick();
      fahrradrouteFilterService.refetchData();

      tick();
      fahrradrouteFilterService.filterField('name', 'andere');

      tick();
      verify(filterQueryParamsService.update(anything(), anything())).once();
      subscription.unsubscribe();
    }));

    it('should filter out missmatching fahrradrouten', fakeAsync(() => {
      let index = 0;
      const subscription = fahrradrouteFilterService.filteredList$.subscribe(fahrradrouten => {
        expect(fahrradrouten).toEqual([[], testFahrradrouten, testFahrradrouten, [testFahrradrouten[0]]][index]);
        index++;
      });

      tick();
      fahrradrouteFilterService.refetchData();

      tick();
      fahrradrouteFilterService.filterField('name', 'test');

      tick();
      fahrradrouteFilterService.filterField('toubizId', 'toubiz-id-1');

      tick();
      verify(filterQueryParamsService.update(anything(), anything())).twice();
      subscription.unsubscribe();
    }));

    it('should not filter out anything, cause both values match', fakeAsync(() => {
      let index = 0;
      const subscription = fahrradrouteFilterService.filteredList$.subscribe(fahrradrouten => {
        expect(fahrradrouten).toEqual([[], testFahrradrouten, testFahrradrouten][index]);
        index++;
      });

      tick();
      fahrradrouteFilterService.refetchData();

      tick();
      fahrradrouteFilterService.filterField('name', 'test');

      tick();
      verify(filterQueryParamsService.update(anything(), anything())).once();
      subscription.unsubscribe();
    }));

    it('should filter out every fahrradroute', fakeAsync(() => {
      let index = 0;
      const subscription = fahrradrouteFilterService.filteredList$.subscribe(fahrradrouten => {
        expect(fahrradrouten).toEqual([[], testFahrradrouten, []][index]);
        index++;
      });

      tick();
      fahrradrouteFilterService.refetchData();

      tick();
      fahrradrouteFilterService.filterField('name', 'diesesWortKommtNichtInDenFahrradroutenVor');

      tick();
      verify(filterQueryParamsService.update(anything(), anything())).once();
      subscription.unsubscribe();
    }));
  });
});
