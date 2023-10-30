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
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { take } from 'rxjs/operators';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';

describe(FilterQueryParamsService.name, () => {
  let filterQueryParamsService: FilterQueryParamsService;
  let activatedRoute: ActivatedRoute;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [],
      imports: [RouterTestingModule],
      providers: [
        FilterQueryParamsService,
        {
          provide: InfrastrukturToken,
          useValue: [MASSNAHMEN, FAHRRADROUTE],
        },
      ],
    }).compileComponents();
  });

  beforeEach(fakeAsync(() => {
    filterQueryParamsService = TestBed.inject(FilterQueryParamsService);
    activatedRoute = TestBed.inject(ActivatedRoute);
  }));

  describe('update', () => {
    it('should hold active Filters', done => {
      filterQueryParamsService.update([new FieldFilter('bezeichnung', 'whatitscalled')], MASSNAHMEN);
      setTimeout(() =>
        filterQueryParamsService.update([new FieldFilter('massnahmenkategorien', 'anycategory')], MASSNAHMEN)
      );

      let i = 0;
      const expected = [
        undefined,
        [new FieldFilter('bezeichnung', 'whatitscalled')],
        [new FieldFilter('bezeichnung', 'whatitscalled'), new FieldFilter('massnahmenkategorien', 'anycategory')],
      ];
      filterQueryParamsService.filter$.pipe(take(3)).subscribe(
        filterMap => {
          expect(filterMap).toHaveSize(i === 0 ? 0 : 1);
          expect(filterMap.get(MASSNAHMEN)).toEqual(expected[i]);
          expect(activatedRoute.snapshot.queryParams.filter_massnahmen).toEqual(
            expected[i] ? expected[i]?.map(fieldFilter => fieldFilter.stringify()).join(',') : undefined
          );
          i++;
        },
        () => {},
        () => done()
      );
    });

    it('should hold active Filters from multiple Infrastrukturen', done => {
      filterQueryParamsService.update([new FieldFilter('bezeichnung', 'whatitscalled')], MASSNAHMEN);
      setTimeout(() => filterQueryParamsService.update([new FieldFilter('name', 'whatsmyname')], FAHRRADROUTE));

      let i = 0;
      const expectedMassnahmenFilters = [
        undefined,
        [new FieldFilter('bezeichnung', 'whatitscalled')],
        [new FieldFilter('bezeichnung', 'whatitscalled')],
      ];
      const expectedFahrradrouteFilters = [undefined, undefined, [new FieldFilter('name', 'whatsmyname')]];
      const expectedInfrastructures = [0, 1, 2];
      filterQueryParamsService.filter$.pipe(take(3)).subscribe(
        filterMap => {
          expect(filterMap).toHaveSize(expectedInfrastructures[i]);
          expect(filterMap.get(MASSNAHMEN)).toEqual(expectedMassnahmenFilters[i]);
          expect(filterMap.get(FAHRRADROUTE)).toEqual(expectedFahrradrouteFilters[i]);
          i++;
        },
        () => {},
        () => done()
      );
    });

    it('should add Filters for equally named field of different Infrastrukturen ', done => {
      filterQueryParamsService.update([new FieldFilter('name', 'onlyForTesting')], MASSNAHMEN);
      setTimeout(() => filterQueryParamsService.update([new FieldFilter('name', 'whatsmyname')], FAHRRADROUTE));

      let i = 0;
      const expectedMassnahmenFilters = [
        undefined,
        [new FieldFilter('name', 'onlyForTesting')],
        [new FieldFilter('name', 'onlyForTesting')],
      ];
      const expectedFahrradrouteFilters = [undefined, undefined, [new FieldFilter('name', 'whatsmyname')]];
      const expectedSizes = [0, 1, 2];
      filterQueryParamsService.filter$.pipe(take(3)).subscribe(
        filterMap => {
          expect(filterMap).toHaveSize(expectedSizes[i]);
          expect(filterMap.get(MASSNAHMEN)).toEqual(expectedMassnahmenFilters[i]);
          expect(filterMap.get(FAHRRADROUTE)).toEqual(expectedFahrradrouteFilters[i]);
          i++;
        },
        () => {},
        () => done()
      );
    });

    it('should replace Filter for same field of same Infrastruktur ', done => {
      filterQueryParamsService.update([new FieldFilter('bezeichnung', 'oldValue')], MASSNAHMEN);
      setTimeout(() => filterQueryParamsService.update([new FieldFilter('bezeichnung', 'newValue')], MASSNAHMEN));

      let i = 0;
      const expectedMassnahmenFilters = [
        undefined,
        [new FieldFilter('bezeichnung', 'oldValue')],
        [new FieldFilter('bezeichnung', 'newValue')],
      ];
      const expectedInfrastructures = [0, 1, 1];
      filterQueryParamsService.filter$.pipe(take(3)).subscribe(
        filterMap => {
          expect(filterMap).toHaveSize(expectedInfrastructures[i]);
          expect(filterMap.get(MASSNAHMEN)).toEqual(expectedMassnahmenFilters[i]);
          i++;
        },
        () => {},
        () => done()
      );
    });

    it('should remove existing Filter if value is empty ', done => {
      filterQueryParamsService.update([new FieldFilter('bezeichnung', 'oldValue')], MASSNAHMEN);
      setTimeout(() => filterQueryParamsService.update([new FieldFilter('netzklassen', 'egal')], MASSNAHMEN));
      setTimeout(() => filterQueryParamsService.update([new FieldFilter('bezeichnung', '')], MASSNAHMEN));

      let i = 0;
      const expectedMassnahmenFilters = [
        undefined,
        [new FieldFilter('bezeichnung', 'oldValue')],
        [new FieldFilter('bezeichnung', 'oldValue'), new FieldFilter('netzklassen', 'egal')],
        [new FieldFilter('netzklassen', 'egal')],
      ];
      const expectedInfrastructures = [0, 1, 1, 1];
      filterQueryParamsService.filter$.pipe(take(4)).subscribe(
        filterMap => {
          expect(filterMap).toHaveSize(expectedInfrastructures[i]);
          expect(filterMap.get(MASSNAHMEN)).toEqual(expectedMassnahmenFilters[i]);
          i++;
        },
        () => {},
        () => done()
      );
    });

    it('should remove param for filtergroup of infrastructure if last filter is removed', done => {
      filterQueryParamsService.update([new FieldFilter('bezeichnung', 'oldValue')], MASSNAHMEN);
      setTimeout(() => filterQueryParamsService.update([new FieldFilter('bezeichnung', '')], MASSNAHMEN), 20);

      let i = 0;
      const expectedMassnahmenFilters = [undefined, [new FieldFilter('bezeichnung', 'oldValue')], undefined];
      const expectedInfrastructures = [0, 1, 0];
      filterQueryParamsService.filter$.pipe(take(3)).subscribe(
        filterMap => {
          expect(filterMap).toHaveSize(expectedInfrastructures[i]);
          expect(filterMap.get(MASSNAHMEN)).toEqual(expectedMassnahmenFilters[i]);
          i++;
        },
        () => {},
        () => done()
      );
    });

    it('should remove param for filtergroup on reset', done => {
      filterQueryParamsService.update([new FieldFilter('bezeichnung', 'oldValue')], MASSNAHMEN);
      setTimeout(() => filterQueryParamsService.reset(MASSNAHMEN), 20);

      let i = 0;
      const expectedMassnahmenFilters = [undefined, [new FieldFilter('bezeichnung', 'oldValue')], undefined];
      const expectedInfrastructures = [0, 1, 0];
      filterQueryParamsService.filter$.pipe(take(3)).subscribe(
        filterMap => {
          expect(filterMap).toHaveSize(expectedInfrastructures[i]);
          expect(filterMap.get(MASSNAHMEN)).toEqual(expectedMassnahmenFilters[i]);
          i++;
        },
        () => {},
        () => done()
      );
    });

    describe('filter$', () => {
      let router: Router;
      beforeEach(() => {
        router = TestBed.inject(Router);
      });

      it('should set filters from route', done => {
        const urlTree = router.parseUrl(router.url);
        urlTree.queryParams = { filter_massnahmen: 'field:' + encodeURIComponent('%&/hallo') };
        router.navigateByUrl(urlTree);

        const expectedMassnahmenFilter = [undefined, [new FieldFilter('field', '%&/hallo')]];
        let i = 0;
        filterQueryParamsService.filter$.pipe(take(2)).subscribe(
          filterMap => {
            expect(filterMap.get(MASSNAHMEN)).toEqual(expectedMassnahmenFilter[i++]);
          },
          () => {},
          () => done()
        );
      });

      it('should ignore malformed filterEntries in route', done => {
        const urlTree = router.parseUrl(router.url);
        urlTree.queryParams = { filter_massnahmen: 'this:is:wrong' };
        router.navigateByUrl(urlTree);

        // sollte nur einmal initial ausgeloest werden, da der falsche Eintrag ignoriert wird und sich
        // dadurch der filter_massnahmen FilterQueryParam nicht aendert ('' -> ''), wobei das zweite
        // triggern des observables dann durch das distinctUntilChanged verhindert wird
        filterQueryParamsService.filter$.pipe(take(1)).subscribe(
          filterMap => {
            expect(filterMap.get(MASSNAHMEN)).toBeUndefined();
          },
          () => {},
          () => done()
        );
      });

      it('should only trigger when filter_massnahmen changed', fakeAsync(() => {
        const expectedMassnahmenFilter = [
          undefined,
          [new FieldFilter('field', '%&/hallo')],
          [new FieldFilter('field', '%&/duda')],
        ];

        let i = 0;
        filterQueryParamsService.filter$.pipe().subscribe(filterMap => {
          expect(filterMap.get(MASSNAHMEN)).toEqual(expectedMassnahmenFilter[i++]);
        });
        tick();

        let urlTree = router.parseUrl(router.url);
        urlTree.queryParams = { filter_massnahmen: 'field:' + encodeURIComponent('%&/hallo') };
        router.navigateByUrl(urlTree);
        tick();

        // Dieses hier sollte durch das distinctUntilChanged rausgefiltert werden, weil sich der Inhalt vom
        // filter_massnahmen Feld nicht geaendert hat
        urlTree = router.parseUrl(router.url);
        urlTree.queryParams = {
          filter_massnahmen: 'field:' + encodeURIComponent('%&/hallo'),
          pizza: 'field:' + encodeURIComponent('%&/hawaii'),
        };
        router.navigateByUrl(urlTree);
        tick();

        urlTree = router.parseUrl(router.url);
        urlTree.queryParams = { filter_massnahmen: 'field:' + encodeURIComponent('%&/duda') };
        router.navigateByUrl(urlTree);
        tick();

        expect(i).toEqual(3);
      }));
    });
  });
});
