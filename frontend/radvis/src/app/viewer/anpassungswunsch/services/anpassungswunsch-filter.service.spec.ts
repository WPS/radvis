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

import { anything, capture, instance, mock, resetCalls, verify, when } from 'ts-mockito';

import { NEVER, of } from 'rxjs';
import { skip } from 'rxjs/operators';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { AnpassungswunschService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch.service';
import { FahrradrouteFilterKategorie } from 'src/app/viewer/viewer-shared/models/fahrradroute-filter-kategorie';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { FilterQueryParams } from 'src/app/viewer/viewer-shared/models/filter-query-params';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { AnpassungswunschFilterService } from './anpassungswunsch-filter.service';

describe(AnpassungswunschFilterService.name, () => {
  let service: AnpassungswunschFilterService;

  let anpassungswunschService: AnpassungswunschService;

  beforeEach(() => {
    anpassungswunschService = mock(AnpassungswunschService);
    when(anpassungswunschService.getAlleAnpassungswuensche(anything())).thenResolve([]);
    when(anpassungswunschService.getAlleAnpassungswuensche(anything(), anything())).thenResolve([]);

    const filterQueryParamService = mock(FilterQueryParamsService);
    when(filterQueryParamService.filter$).thenReturn(NEVER);
    const filterQueryParams = mock(FilterQueryParams);
    when(filterQueryParams.filters).thenReturn(new Map<string, FieldFilter[]>());
    when(filterQueryParamService.filterQueryParamsSnapshot).thenReturn(instance(filterQueryParams));

    const infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(of([ANPASSUNGSWUNSCH]));

    service = new AnpassungswunschFilterService(
      instance(infrastrukturenSelektionService),
      instance(filterQueryParamService),
      instance(anpassungswunschService)
    );
  });

  describe('erweiterter Filter', () => {
    it('should have correct default Values', (done: DoneFn) => {
      expect(service.erweiterterFilter.abgeschlosseneAusblenden).toBeTrue();
      expect(service.erweiterterFilter.fahrradrouteFilter).toBeNull();
      verify(anpassungswunschService.getAlleAnpassungswuensche(anything(), anything())).once();
      expect(capture(anpassungswunschService.getAlleAnpassungswuensche).last()[0]).toBeTrue();
      expect(capture(anpassungswunschService.getAlleAnpassungswuensche).last()[1]).toBeFalsy();
      service.erweiterterFilterActive$.subscribe(v => {
        expect(v).toBeTrue();
        done();
      });
    });

    it('should not refetch data on update if filter unchanged', () => {
      resetCalls(anpassungswunschService);

      service.updateErweiterterFilter(service.erweiterterFilter);
      verify(anpassungswunschService.getAlleAnpassungswuensche(anything(), anything())).never();
      verify(anpassungswunschService.getAlleAnpassungswuensche(anything())).never();

      expect().nothing();
    });

    it('should refetch data on update', () => {
      resetCalls(anpassungswunschService);

      const expectedFahrradroutenIds = [1, 4, 7];
      service.updateErweiterterFilter({
        abgeschlosseneAusblenden: true,
        fahrradrouteFilter: {
          fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
          fahrradroute: null,
          fahrradroutenIds: expectedFahrradroutenIds,
        },
      });
      verify(anpassungswunschService.getAlleAnpassungswuensche(anything(), anything())).once();

      expect(capture(anpassungswunschService.getAlleAnpassungswuensche).last()[0]).toBeTrue();
      expect(capture(anpassungswunschService.getAlleAnpassungswuensche).last()[1]).toEqual(expectedFahrradroutenIds);
    });

    it('should trigger activeFilter on update', (done: DoneFn) => {
      service.updateErweiterterFilter({ abgeschlosseneAusblenden: false, fahrradrouteFilter: null });
      service.erweiterterFilterActive$.pipe(skip(1)).subscribe(v => {
        expect(v).toBeTrue();
        done();
      });
      service.updateErweiterterFilter({ abgeschlosseneAusblenden: true, fahrradrouteFilter: null });
    });
  });

  describe('reset', () => {
    it('should reset erweiterter filter to default values', () => {
      service.updateErweiterterFilter({
        abgeschlosseneAusblenden: false,
        fahrradrouteFilter: {
          fahrradrouteFilterKategorie: FahrradrouteFilterKategorie.ALLE_DROUTEN,
          fahrradroute: null,
          fahrradroutenIds: [1, 2, 3],
        },
      });

      resetCalls(anpassungswunschService);
      service.reset();

      expect(service.erweiterterFilter).toEqual({ abgeschlosseneAusblenden: true, fahrradrouteFilter: null });
      verify(anpassungswunschService.getAlleAnpassungswuensche(anything(), anything())).once();
      expect(capture(anpassungswunschService.getAlleAnpassungswuensche).last()[0]).toBeTrue();
      expect(capture(anpassungswunschService.getAlleAnpassungswuensche).last()[1]).toBeFalsy();
    });
  });
});
