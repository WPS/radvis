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

import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, ActivatedRouteSnapshot, convertToParamMap, ParamMap, Router, UrlTree } from '@angular/router';
import { of, Subject } from 'rxjs';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { MaterialDesignModule } from 'src/app/material-design.module';
import { anything, deepEqual, instance, mock, strictEqual, verify, when } from 'ts-mockito';
import { BenutzerListComponent } from './benutzer-list.component';

describe(BenutzerListComponent.name, () => {
  let component: BenutzerListComponent;
  let fixture: ComponentFixture<BenutzerListComponent>;

  let router: Router;
  let activatedRoute: ActivatedRoute;
  let administrationRoutingService: AdministrationRoutingService;

  let queryParamMap$: Subject<ParamMap>;

  beforeEach(async () => {
    queryParamMap$ = new Subject();

    router = mock(Router);
    activatedRoute = mock(ActivatedRoute);
    administrationRoutingService = mock(AdministrationRoutingService);

    when(activatedRoute.snapshot).thenReturn({
      data: {
        benutzer: [],
      },
    } as unknown as ActivatedRouteSnapshot);
    when(activatedRoute.queryParamMap).thenReturn(queryParamMap$);
    when(activatedRoute.queryParams).thenReturn(of({}));

    const urlTree = new UrlTree();
    urlTree.queryParams = {};
    when(router.parseUrl(anything())).thenReturn(urlTree);

    // Ohne dieses Setup, würde ein Fehler in AfterAll geworfen werden
    await TestBed.configureTestingModule({
      declarations: [BenutzerListComponent],
      imports: [FormsModule, ReactiveFormsModule, NoopAnimationsModule, MaterialDesignModule],
      providers: [
        { provide: Router, useValue: instance(router) },
        { provide: ActivatedRoute, useValue: instance(activatedRoute) },
        { provide: AdministrationRoutingService, useValue: instance(administrationRoutingService) },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BenutzerListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  describe(BenutzerListComponent.prototype.onClearSearch.name, () => {
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
      expect(urlTree.queryParams[AdministrationRoutingService.BENUTZER_SUCHE_QUERY_PARAM]).toEqual(suchbegriff);
    });
  });

  describe('with queryParamMap', () => {
    it('should update suche', fakeAsync(() => {
      const suchbegriff = 'Kartoffelpüree';
      component.benutzerDataSource.filter = '';
      component.formControl.setValue('', { emitEvent: false });

      const paramMap: any = {};
      paramMap[AdministrationRoutingService.BENUTZER_SUCHE_QUERY_PARAM] = suchbegriff;
      queryParamMap$.next(convertToParamMap(paramMap));

      tick();

      expect(component.benutzerDataSource.filter).toEqual(suchbegriff);
      expect(component.formControl.value).toEqual(suchbegriff);
    }));

    it('should update index', (done: DoneFn) => {
      const index = 2;

      component.page$.subscribe(result => {
        expect(result).toBe(index);
        done();
      });

      const paramMap: any = {};
      paramMap[AdministrationRoutingService.BENUTZER_SUCHE_PAGE_INDEX_QUERY_PARAM] = index;
      queryParamMap$.next(convertToParamMap(paramMap));
    });

    it('should update size', (done: DoneFn) => {
      const size = 20;

      component.pageSize$.subscribe(result => {
        expect(result).toBe(size);
        done();
      });

      const paramMap: any = {};
      paramMap[AdministrationRoutingService.BENUTZER_SUCHE_PAGE_SIZE_QUERY_PARAM] = size;
      queryParamMap$.next(convertToParamMap(paramMap));
    });
  });

  describe('with pageSize and pageIndex', () => {
    beforeEach(() => {
      component.paginator!.pageSize = 20;

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
        expect(urlTree.queryParams[AdministrationRoutingService.BENUTZER_SUCHE_PAGE_INDEX_QUERY_PARAM]).toEqual(
          component.paginator?.pageIndex
        );
      });

      it('should set pageSize', () => {
        expect(urlTree.queryParams[AdministrationRoutingService.BENUTZER_SUCHE_PAGE_SIZE_QUERY_PARAM]).toEqual(
          component.paginator?.pageSize
        );
      });

      it('should have invoked navigatedByUrl', () => {
        verify(router.navigateByUrl(strictEqual(urlTree), deepEqual({ replaceUrl: true }))).once();
        expect().nothing();
      });
    });
  });

  describe(BenutzerListComponent.prototype.onEdit.name, () => {
    let benutzerId: number;

    beforeEach(() => {
      benutzerId = 42;
      component.onEdit(benutzerId);
    });

    it('should invoke toAdministratorEditor', () => {
      verify(administrationRoutingService.toBenutzerEditor(benutzerId)).once();
      expect().nothing();
    });
  });
});
