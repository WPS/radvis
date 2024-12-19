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

import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, ParamMap, Router, UrlTree } from '@angular/router';
import { of, Subject } from 'rxjs';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { OrganisationenVerwaltungService } from 'src/app/administration/services/organisationen-verwaltung.service';
import { MaterialDesignModule } from 'src/app/material-design.module';
import {
  defaultBundeslandOrganisation,
  defaultOrganisation,
  defaultUebergeordneteOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { anything, deepEqual, instance, mock, strictEqual, verify, when } from 'ts-mockito';
import { OrganisationListComponent } from './organisation-list.component';

describe(OrganisationListComponent.name, () => {
  let component: OrganisationListComponent;
  let fixture: ComponentFixture<OrganisationListComponent>;

  let router: Router;
  let benutzerDetailsService: BenutzerDetailsService;
  let activatedRoute: ActivatedRoute;
  let changeDetectorRef: ChangeDetectorRef;
  let organisationenService: OrganisationenVerwaltungService;
  let administrationRoutingService: AdministrationRoutingService;

  let queryParamMap$: Subject<ParamMap>;

  beforeEach(async () => {
    queryParamMap$ = new Subject();

    router = mock(Router);
    benutzerDetailsService = mock(BenutzerDetailsService);
    activatedRoute = mock(ActivatedRoute);
    changeDetectorRef = mock(ChangeDetectorRef);
    organisationenService = mock(OrganisationenVerwaltungService);
    administrationRoutingService = mock(AdministrationRoutingService);

    when(activatedRoute.queryParamMap).thenReturn(queryParamMap$);
    when(activatedRoute.queryParams).thenReturn(of({}));

    const urlTree = new UrlTree();
    urlTree.queryParams = {};
    when(router.parseUrl(anything())).thenReturn(urlTree);

    when(organisationenService.getAlleFuerBearbeitung()).thenReturn(
      of([defaultOrganisation, defaultUebergeordneteOrganisation, defaultBundeslandOrganisation]).toPromise()
    );

    // Ohne dieses Setup, würde ein Fehler in AfterAll geworfen werden
    await TestBed.configureTestingModule({
      declarations: [OrganisationListComponent],
      imports: [FormsModule, ReactiveFormsModule, NoopAnimationsModule, MaterialDesignModule],
      providers: [
        { provide: ActivatedRoute, useValue: instance(activatedRoute) },
        { provide: Router, useValue: instance(router) },
        { provide: AdministrationRoutingService, useValue: instance(administrationRoutingService) },
        { provide: BenutzerDetailsService, useValue: instance(benutzerDetailsService) },
        { provide: ChangeDetectorRef, useValue: instance(changeDetectorRef) },
        { provide: OrganisationenVerwaltungService, useValue: instance(organisationenService) },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OrganisationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  describe(OrganisationListComponent.prototype.onClearSearch.name, () => {
    beforeEach(() => {
      component.formControl.setValue('WAMBO', { emitEvent: false });
      component.onClearSearch();
    });

    it('should clear form', () => {
      expect(component.formControl.value).toEqual('');
    });
  });

  describe('with Suchfeldeingabe', () => {
    let urlTree: UrlTree;
    let suchbegriff: string;

    beforeEach(fakeAsync(() => {
      urlTree = new UrlTree();
      urlTree.queryParams = {};
      suchbegriff = 'WAMBO';

      when(router.parseUrl(anything())).thenReturn(urlTree);
      component.formControl.setValue(suchbegriff);
      tick(100);
    }));

    it('should have invoked navigatedByUrl', () => {
      verify(router.navigateByUrl(strictEqual(urlTree), deepEqual({ replaceUrl: true }))).once();
      expect().nothing();
    });

    it('should set queryParams', () => {
      expect(urlTree.queryParams[AdministrationRoutingService.ORGANISATION_SUCHE_QUERY_PARAM]).toEqual(suchbegriff);
    });
  });

  describe('filter', () => {
    it('should filter status', () => {
      const inaktiveOrganisation = { ...defaultOrganisation, id: 1, aktiv: false };
      component.organisationDataSource.data = [inaktiveOrganisation, { ...defaultOrganisation, id: 2, aktiv: true }];
      component.organisationDataSource.filter = 'inaktiv';
      expect(component.organisationDataSource.filteredData).toEqual([inaktiveOrganisation]);
    });

    it('should filter partial matches', () => {
      component.organisationDataSource.data = [{ ...defaultOrganisation, name: 'test' }];
      component.organisationDataSource.filter = 'te';
      expect(component.organisationDataSource.filteredData.length).toBe(1);
    });

    it('should filter case insensitive', () => {
      component.organisationDataSource.data = [
        { ...defaultOrganisation, name: 'TEST', id: 1 },
        { ...defaultOrganisation, name: 'test', id: 2 },
      ];
      component.organisationDataSource.filter = 'test';
      expect(component.organisationDataSource.filteredData.length).toBe(2);

      component.organisationDataSource.filter = 'TEST';
      expect(component.organisationDataSource.filteredData.length).toBe(2);
    });
  });

  describe('with queryParamMap', () => {
    it('should update suche', fakeAsync(() => {
      const suchbegriff = 'Kartoffelpüree';
      component.organisationDataSource.filter = '';
      component.formControl.setValue('', { emitEvent: false });

      const paramMap: any = {};
      paramMap[AdministrationRoutingService.ORGANISATION_SUCHE_QUERY_PARAM] = suchbegriff;
      queryParamMap$.next(convertToParamMap(paramMap));

      tick();

      expect(component.organisationDataSource.filter).toEqual(suchbegriff);
      expect(component.formControl.value).toEqual(suchbegriff);
    }));

    it('should update index', (done: DoneFn) => {
      const index = 2;

      component.page$.subscribe(result => {
        expect(result).toBe(index);
        done();
      });

      const paramMap: any = {};
      paramMap[AdministrationRoutingService.ORGANISATION_SUCHE_PAGE_INDEX_QUERY_PARAM] = index;
      queryParamMap$.next(convertToParamMap(paramMap));
    });

    it('should update size', (done: DoneFn) => {
      const size = 20;

      component.pageSize$.subscribe(result => {
        expect(result).toBe(size);
        done();
      });

      const paramMap: any = {};
      paramMap[AdministrationRoutingService.ORGANISATION_SUCHE_PAGE_SIZE_QUERY_PARAM] = size;
      queryParamMap$.next(convertToParamMap(paramMap));
    });
  });

  describe('with pageSize and pageIndex', () => {
    beforeEach(() => {
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      component.paginator!.pageSize = 20;
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      component.paginator!.pageIndex = 1;
    });

    describe('onPage', () => {
      let urlTree: UrlTree;

      beforeEach(() => {
        urlTree = new UrlTree();
        urlTree.queryParams = {};
        when(router.parseUrl(anything())).thenReturn(urlTree);

        component.onPage();
      });

      it('should set pageIndex', () => {
        expect(urlTree.queryParams[AdministrationRoutingService.ORGANISATION_SUCHE_PAGE_INDEX_QUERY_PARAM]).toEqual(
          component.paginator?.pageIndex
        );
      });

      it('should set pageSize', () => {
        expect(urlTree.queryParams[AdministrationRoutingService.ORGANISATION_SUCHE_PAGE_SIZE_QUERY_PARAM]).toEqual(
          component.paginator?.pageSize
        );
      });

      it('should have invoked navigatedByUrl', () => {
        verify(router.navigateByUrl(strictEqual(urlTree), deepEqual({ replaceUrl: true }))).once();
        expect().nothing();
      });
    });
  });
});
