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

import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { instance, mock, when } from 'ts-mockito';
import { BehaviorSubject } from 'rxjs';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { LeihstationFilterService } from 'src/app/viewer/leihstation/services/leihstation-filter.service';
import { LeihstationService } from 'src/app/viewer/leihstation/services/leihstation.service';
import { LeihstationStatus } from 'src/app/viewer/leihstation/models/leihstation-status';
import { Leihstation } from 'src/app/viewer/leihstation/models/leihstation';

describe(LeihstationFilterService.name, () => {
  let service: LeihstationFilterService;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let filterQueryParamsService: FilterQueryParamsService;
  let leihstationenService: LeihstationService;

  beforeEach(() => {
    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    filterQueryParamsService = mock(FilterQueryParamsService);
    leihstationenService = mock(LeihstationService);

    const filter$ = new BehaviorSubject(new Map<Infrastruktur, FieldFilter[]>());
    when(filterQueryParamsService.filter$).thenReturn(filter$.asObservable());

    const selektierteInfrastrukturen$ = new BehaviorSubject<Infrastruktur[]>([]);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(
      selektierteInfrastrukturen$.asObservable()
    );

    service = new LeihstationFilterService(
      instance(infrastrukturenSelektionService),
      instance(filterQueryParamsService),
      instance(leihstationenService)
    );
  });

  describe('With Leihstationen', () => {
    let leihstation: Leihstation;

    beforeEach(() => {
      leihstation = {
        status: LeihstationStatus.AKTIV,
        freiesAbstellen: true,
        anzahlFahrraeder: 123,
      } as Leihstation;
    });

    it('should get status via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(leihstation, 'status')).toEqual(
        LeihstationStatus.getDisplayText(leihstation.status)
      );
    });

    it('should get freiesAbstellen via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(leihstation, 'freiesAbstellen')).toEqual('ja');
    });

    it('should get anzahlFahrraeder via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(leihstation, 'anzahlFahrraeder')).toEqual('123');
    });
  });
});
