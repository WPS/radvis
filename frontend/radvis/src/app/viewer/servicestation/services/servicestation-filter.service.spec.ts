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
import { ServicestationFilterService } from 'src/app/viewer/servicestation/services/servicestation-filter.service';
import { ServicestationService } from 'src/app/viewer/servicestation/services/servicestation.service';
import { Servicestation } from 'src/app/viewer/servicestation/models/servicestation';
import { ServicestationTyp } from 'src/app/viewer/servicestation/models/servicestation-typ';
import { ServicestationStatus } from 'src/app/viewer/servicestation/models/servicestation-status';
import { OrganisationsArt } from 'src/app/shared/models/organisations-art';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';

describe(ServicestationFilterService.name, () => {
  let service: ServicestationFilterService;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let filterQueryParamsService: FilterQueryParamsService;
  let servicestationService: ServicestationService;

  beforeEach(() => {
    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    filterQueryParamsService = mock(FilterQueryParamsService);
    servicestationService = mock(ServicestationService);

    const filter$ = new BehaviorSubject(new Map<Infrastruktur, FieldFilter[]>());
    when(filterQueryParamsService.filter$).thenReturn(filter$.asObservable());

    const selektierteInfrastrukturen$ = new BehaviorSubject<Infrastruktur[]>([]);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(
      selektierteInfrastrukturen$.asObservable()
    );

    service = new ServicestationFilterService(
      instance(infrastrukturenSelektionService),
      instance(filterQueryParamsService),
      instance(servicestationService)
    );
  });

  describe('With Servicestation', () => {
    let servicestation: Servicestation;

    beforeEach(() => {
      servicestation = {
        typ: ServicestationTyp.RADSERVICE_PUNKT_GROSS,
        status: ServicestationStatus.AKTIV,
        organisation: {
          id: 1,
          name: 'orga',
          idUebergeordneteOrganisation: null,
          organisationsArt: OrganisationsArt.GEMEINDE,
        } as Verwaltungseinheit,
        gebuehren: true,
        luftpumpe: true,
        kettenwerkzeug: false,
        werkzeug: false,
        fahrradhalterung: true,
        oeffnungszeiten: 'Immer',
      } as Servicestation;
    });

    it('should get typ via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(servicestation, 'typ')).toEqual(
        ServicestationTyp.getDisplayText(servicestation.typ)
      );
    });

    it('should get status via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(servicestation, 'status')).toEqual(
        ServicestationStatus.getDisplayText(servicestation.status)
      );
    });

    it('should get organisation via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(servicestation, 'organisation')).toEqual(
        Verwaltungseinheit.getDisplayName(servicestation.organisation)
      );
    });

    it('should get gebuehren via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(servicestation, 'gebuehren')).toEqual('ja');
    });

    it('should get luftpumpe via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(servicestation, 'luftpumpe')).toEqual('ja');
    });

    it('should get kettenwerkzeug via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(servicestation, 'kettenwerkzeug')).toEqual('nein');
    });

    it('should get werkzeug via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(servicestation, 'werkzeug')).toEqual('nein');
    });

    it('should get fahrradhalterung via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(servicestation, 'fahrradhalterung')).toEqual('ja');
    });

    it('should get oeffnungszeiten via key correctly', () => {
      expect(service.getInfrastrukturValueForKey(servicestation, 'oeffnungszeiten')).toEqual(
        servicestation.oeffnungszeiten
      );
    });
  });
});
