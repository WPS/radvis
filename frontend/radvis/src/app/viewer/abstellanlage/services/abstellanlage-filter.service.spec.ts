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

import { AbstellanlageFilterService } from 'src/app/viewer/abstellanlage/services/abstellanlage-filter.service';
import { instance, mock, when } from 'ts-mockito';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { AbstellanlageService } from 'src/app/viewer/abstellanlage/services/abstellanlage.service';
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { AbstellanlagenStatus } from 'src/app/viewer/abstellanlage/models/abstellanlagen-status';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { AbstellanlagenQuellSystem } from 'src/app/viewer/abstellanlage/models/abstellanlagen-quell-system';
import { Stellplatzart } from 'src/app/viewer/abstellanlage/models/stellplatzart';
import { BehaviorSubject } from 'rxjs';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';

describe(AbstellanlageFilterService.name, () => {
  let service: AbstellanlageFilterService;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let filterQueryParamsService: FilterQueryParamsService;
  let abstellanlagenService: AbstellanlageService;

  beforeEach(() => {
    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    filterQueryParamsService = mock(FilterQueryParamsService);
    abstellanlagenService = mock(AbstellanlageService);

    const filter$ = new BehaviorSubject(new Map<Infrastruktur, FieldFilter[]>());
    when(filterQueryParamsService.filter$).thenReturn(filter$.asObservable());

    const selektierteInfrastrukturen$ = new BehaviorSubject<Infrastruktur[]>([]);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(
      selektierteInfrastrukturen$.asObservable()
    );

    service = new AbstellanlageFilterService(
      instance(infrastrukturenSelektionService),
      instance(filterQueryParamsService),
      instance(abstellanlagenService)
    );
  });

  describe('With Abstellanlage', () => {
    let abstellanlage: Abstellanlage;

    beforeEach(() => {
      abstellanlage = {
        status: AbstellanlagenStatus.AKTIV,
        zustaendig: {
          id: 1,
          name: 'orga',
          idUebergeordneteOrganisation: null,
          organisationsArt: OrganisationsArt.GEMEINDE,
        } as Verwaltungseinheit,
        quellSystem: AbstellanlagenQuellSystem.MOBIDATABW,
        stellplatzart: Stellplatzart.DOPPELSTOECKIG,
        istBikeAndRide: true,
        anzahlStellplaetze: 123,
      } as Abstellanlage;
    });

    it('should get status via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(abstellanlage, 'status')).toEqual(
        AbstellanlagenStatus.getDisplayText(abstellanlage.status)
      );
    });

    it('should get zustaendig via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(abstellanlage, 'zustaendig')).toEqual(
        Verwaltungseinheit.getDisplayName(abstellanlage.zustaendig)
      );
    });

    it('should get quellSystem via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(abstellanlage, 'quellSystem')).toEqual(
        AbstellanlagenQuellSystem.getDisplayText(abstellanlage.quellSystem)
      );
    });

    it('should get groessenklasse via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(abstellanlage, 'groessenklasse')).toEqual('');
    });

    it('should get stellplatzart via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(abstellanlage, 'stellplatzart')).toEqual(
        Stellplatzart.getDisplayText(abstellanlage.stellplatzart)
      );
    });

    it('should get istBikeAndRide via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(abstellanlage, 'istBikeAndRide')).toEqual('ja');
    });

    it('should get anzahlStellplaetze via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(abstellanlage, 'anzahlStellplaetze')).toEqual('123');
    });

    it('should get ueberdacht via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(abstellanlage, 'ueberdacht')).toEqual('');
    });
  });
});
