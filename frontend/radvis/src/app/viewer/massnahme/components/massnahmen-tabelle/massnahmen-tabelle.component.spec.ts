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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MockBuilder } from 'ng-mocks';
import { BehaviorSubject, of } from 'rxjs';
import {
  defaultBundeslandOrganisation,
  defaultGemeinden,
  defaultOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { MassnahmenTabelleComponent } from 'src/app/viewer/massnahme/components/massnahmen-tabelle/massnahmen-tabelle.component';
import { MassnahmeListenView } from 'src/app/viewer/massnahme/models/massnahme-listen-view';
import { getTestMassnahmeListenViews } from 'src/app/viewer/massnahme/models/massnahme-listen-view-test-data-provider.spec';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { FieldFilter } from 'src/app/viewer/viewer-shared/models/field-filter';
import { FilterQueryParams } from 'src/app/viewer/viewer-shared/models/filter-query-params';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { FilterQueryParamsService } from 'src/app/viewer/viewer-shared/services/filter-query-params.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, instance, mock, verify, when } from 'ts-mockito';

describe(MassnahmenTabelleComponent.name, () => {
  let massnahmenTabelleComponent: MassnahmenTabelleComponent;
  let fixture: ComponentFixture<MassnahmenTabelleComponent>;

  let massnahmeFilterService: MassnahmeFilterService;
  let massnahmeService: MassnahmeService;
  let massnahmenRoutingService: MassnahmenRoutingService;
  let organisationService: OrganisationenService;
  let benutzerDetailService: BenutzerDetailsService;
  let infrastrukturenSelektionService: InfrastrukturenSelektionService;
  let filterQueryParamsService: FilterQueryParamsService;
  const testMassnahmenListenViews: MassnahmeListenView[] = getTestMassnahmeListenViews();

  const filteredMassnahmenSubject = new BehaviorSubject<MassnahmeListenView[]>([]);
  const selectedMassnahmenIdSubject = new BehaviorSubject<number | null>(null);

  beforeEach(() => {
    massnahmenRoutingService = mock(MassnahmenRoutingService);
    when(massnahmenRoutingService.selectedInfrastrukturId$).thenReturn(selectedMassnahmenIdSubject.asObservable());

    massnahmeService = mock(MassnahmeService);
    when(massnahmeService.getAll(anything())).thenResolve(testMassnahmenListenViews);

    organisationService = mock(OrganisationenService);
    when(organisationService.getOrganisationen()).thenResolve([defaultBundeslandOrganisation, ...defaultGemeinden]);

    benutzerDetailService = mock(BenutzerDetailsService);
    when(benutzerDetailService.aktuellerBenutzerOrganisation()).thenReturn(defaultOrganisation);

    infrastrukturenSelektionService = mock(InfrastrukturenSelektionService);
    when(infrastrukturenSelektionService.selektierteInfrastrukturen$).thenReturn(of([]));

    filterQueryParamsService = mock(FilterQueryParamsService);
    when(filterQueryParamsService.filter$).thenReturn(of(new Map<Infrastruktur, FieldFilter[]>()));
    when(filterQueryParamsService.filterQueryParamsSnapshot).thenReturn(
      new FilterQueryParams(new Map<string, FieldFilter[]>())
    );

    return MockBuilder(MassnahmenTabelleComponent, ViewerModule)
      .keep(MassnahmeFilterService)
      .provide({ provide: MassnahmenRoutingService, useValue: instance(massnahmenRoutingService) })
      .provide({ provide: OrganisationenService, useValue: instance(organisationService) })
      .provide({ provide: BenutzerDetailsService, useValue: instance(benutzerDetailService) })
      .provide({
        provide: MassnahmeService,
        useValue: instance(massnahmeService),
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

  describe('organisationsFilter', () => {
    describe('with initial Values for OrganisationenDropDown', () => {
      beforeEach(() => {
        when(organisationService.getOrganisationen()).thenResolve([defaultBundeslandOrganisation, ...defaultGemeinden]);
      });

      beforeEach(() => {
        fixture = TestBed.createComponent(MassnahmenTabelleComponent);
        massnahmenTabelleComponent = fixture.componentInstance;
        massnahmeFilterService = TestBed.inject(MassnahmeFilterService);
      });

      beforeEach(() => {
        filteredMassnahmenSubject.next([]);
      });

      it('should not have Bundesland-organisations as options', () => {
        expect(massnahmenTabelleComponent.alleNonBundeslandOrganisationenOptions).not.toContain(
          defaultBundeslandOrganisation
        );
      });
    });

    describe('with initial non-Bundesland-Value for BenutzerOrganisation', () => {
      beforeEach(() => {
        when(benutzerDetailService.aktuellerBenutzerOrganisation()).thenReturn(defaultOrganisation);
      });

      beforeEach(() => {
        fixture = TestBed.createComponent(MassnahmenTabelleComponent);
        massnahmenTabelleComponent = fixture.componentInstance;
        massnahmeFilterService = TestBed.inject(MassnahmeFilterService);
      });

      beforeEach(() => {
        filteredMassnahmenSubject.next([]);
      });

      it('should prefill Controls with correct Values from massnahmeFilterService', () => {
        expect(massnahmenTabelleComponent.organisationControl.value).toEqual(defaultOrganisation);
      });
    });

    describe('with initial bundesland-Value for BenutzerOrganisation', () => {
      beforeEach(() => {
        when(benutzerDetailService.aktuellerBenutzerOrganisation()).thenReturn(defaultBundeslandOrganisation);
      });

      beforeEach(() => {
        fixture = TestBed.createComponent(MassnahmenTabelleComponent);
        massnahmenTabelleComponent = fixture.componentInstance;
        massnahmeFilterService = TestBed.inject(MassnahmeFilterService);
      });

      beforeEach(() => {
        filteredMassnahmenSubject.next([]);
      });

      it('should prefill Controls with correct Values from massnahmeFilterService', () => {
        expect(massnahmenTabelleComponent.organisationControl.value).toBeNull();
      });
    });
  });

  describe('with default initial Values', () => {
    beforeEach(async () => {
      fixture = TestBed.createComponent(MassnahmenTabelleComponent);
      massnahmenTabelleComponent = fixture.componentInstance;
      massnahmeFilterService = TestBed.inject(MassnahmeFilterService);
    });

    beforeEach(() => {
      filteredMassnahmenSubject.next([]);
    });

    it('should set correct values', () => {
      massnahmenTabelleComponent.organisationControl.patchValue(defaultGemeinden[1]);
      expect(massnahmeFilterService.organisation).toEqual(defaultGemeinden[1]);

      massnahmenTabelleComponent.organisationControl.patchValue(defaultGemeinden[0]);
      expect(massnahmeFilterService.organisation).toEqual(defaultGemeinden[0]);
    });

    it('should trigger Data refetching with correct OrganisationsId, if filter is active and Organisation changes', () => {
      massnahmenTabelleComponent.organisationControl.patchValue(defaultGemeinden[0]);

      verify(massnahmeService.getAll(defaultGemeinden[0].id)).once();
      expect().nothing();
    });
  });
});
