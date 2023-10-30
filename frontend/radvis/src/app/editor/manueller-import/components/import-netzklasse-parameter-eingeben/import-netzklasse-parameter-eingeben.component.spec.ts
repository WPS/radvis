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

import { fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute, ActivatedRouteSnapshot, Router } from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { of } from 'rxjs';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { ImportNetzklasseParameterEingebenComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-parameter-eingeben/import-netzklasse-parameter-eingeben.component';
import { ManuellerImportModule } from 'src/app/editor/manueller-import/manueller-import.module';
import { DateiUploadInfo } from 'src/app/editor/manueller-import/models/datei-upload-info';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { NetzklassenParameter } from 'src/app/editor/manueller-import/models/netzklassen-parameter';
import { CreateSessionStateService } from 'src/app/editor/manueller-import/services/create-session.state.service';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { anything, capture, deepEqual, instance, mock, verify, when } from 'ts-mockito';
import { StartNetzklassenImportSessionCommand } from 'src/app/editor/manueller-import/models/start-netzklassen-import-session-command';

describe(ImportNetzklasseParameterEingebenComponent.name, () => {
  let fixture: MockedComponentFixture<ImportNetzklasseParameterEingebenComponent>;
  let component: ImportNetzklasseParameterEingebenComponent;
  let manuellerImportRoutingService: ManuellerImportRoutingService;
  let route: ActivatedRoute;
  let router: Router;
  let manuellerImportService: ManuellerImportService;
  let createSessionStateService: CreateSessionStateService;

  const organisationsId = 65;
  const importTyp: ImportTyp = ImportTyp.NETZKLASSE_ZUWEISEN;
  const fileName = 'emptyFile';
  const emptyTestFile: File = new File([], fileName);
  const netzklasse = Netzklasse.RADVORRANGROUTEN;
  let routeForStep: string;

  beforeEach(() => {
    manuellerImportRoutingService = mock(ManuellerImportRoutingService);
    route = mock(ActivatedRoute);
    router = mock(Router);
    manuellerImportService = mock(ManuellerImportService);
    createSessionStateService = mock(CreateSessionStateService);
    routeForStep = 'einString';

    when(route.snapshot).thenReturn(({ data: { step: 1 } } as unknown) as ActivatedRouteSnapshot);
    when(manuellerImportRoutingService.getRouteForStep(2, ImportTyp.NETZKLASSE_ZUWEISEN)).thenReturn(routeForStep);

    return MockBuilder(ImportNetzklasseParameterEingebenComponent, ManuellerImportModule)
      .provide({
        provide: ManuellerImportRoutingService,
        useFactory: () => instance(manuellerImportRoutingService),
      })
      .provide({
        provide: ActivatedRoute,
        useFactory: () => instance(route),
      })
      .provide({
        provide: Router,
        useFactory: () => instance(router),
      })
      .provide({
        provide: ManuellerImportService,
        useFactory: () => instance(manuellerImportService),
      })
      .provide({
        provide: CreateSessionStateService,
        useFactory: () => instance(createSessionStateService),
      });
  });

  // Aufteilen in drei describe Bloecke, da schon im Konstruktor der Komponente die formGroup gefuellt wird
  // und das Verhalten abhängig ist, ob eine session existiert und ob der createSessionStateService schon eine
  // vollstaendige dateiUploadInfo hat oder es sich um einen komplett "cleanen" Zustand handelt

  // Fall 0: wenn vorher noch nichts an der Komponente gemacht wurde
  describe('NoSessionAndNoDateiUploadInfo', () => {
    beforeEach(
      waitForAsync(() => {
        // Fall 0
        when(manuellerImportService.existsImportSession(ImportTyp.NETZKLASSE_ZUWEISEN)).thenResolve(false);
        when(createSessionStateService.dateiUploadInfo).thenReturn(null);
        when(createSessionStateService.parameterInfo).thenReturn(null);

        fixture = MockRender(ImportNetzklasseParameterEingebenComponent);
        component = fixture.point.componentInstance;
        fixture.detectChanges();
      })
    );

    it('should update parameterInfo if a Netzklasse is chosen', () => {
      component.formControl.setValue(netzklasse);

      verify(createSessionStateService.updateParameterInfo(anything())).once();
      expect(capture(createSessionStateService.updateParameterInfo).last()[0]).toEqual(
        NetzklassenParameter.of(netzklasse)
      );
    });
  });

  // Fall 1: es existiert KEINE Session und der createSessionStateService enthaelt eine vollstaendige DateiUploadInfo
  describe('NoSessionExists_sessionStateService.dateiUploadInfo_vorhanden', () => {
    beforeEach(
      waitForAsync(() => {
        // Fall 1
        when(manuellerImportService.existsImportSession(ImportTyp.NETZKLASSE_ZUWEISEN)).thenResolve(false);
        when(createSessionStateService.dateiUploadInfo).thenReturn(
          DateiUploadInfo.of(importTyp, emptyTestFile, organisationsId)
        );
        when(createSessionStateService.parameterInfo).thenReturn(NetzklassenParameter.of(netzklasse));

        fixture = MockRender(ImportNetzklasseParameterEingebenComponent);
        component = fixture.point.componentInstance;
        fixture.detectChanges();
      })
    );

    it('Should fill form correctly but enable modification', () => {
      expect(component.sessionExists).toBeFalse();
      expect(component.formControl.disabled).toBeFalse();
      expect(component.formControl.value).toEqual(netzklasse);
    });

    describe('onAbort', () => {
      beforeEach(fakeAsync(() => {
        when(manuellerImportService.deleteImportSession()).thenResolve();
        when(manuellerImportRoutingService.getStartStepRoute()).thenReturn(routeForStep);

        component.onAbort();
        tick();
      }));

      it('should invoke createSessionStateService.reset', () => {
        verify(createSessionStateService.reset()).once();
        expect().nothing();
      });

      it('should invoke navigate', () => {
        verify(router.navigate(deepEqual(['../' + routeForStep]), anything())).once();
        expect().nothing();
      });
    });

    describe('onStart (form handling)', () => {
      beforeEach(fakeAsync(() => {
        when(manuellerImportService.createSessionAndStartNetzklassenImport(anything(), anything())).thenResolve();
      }));

      it('should disable form', fakeAsync(() => {
        expect(component.formControl.disabled).toBeFalse();
        component.onStart();
        expect(component.formControl.disabled).toBeTrue();
      }));

      it('should reenable form if errors occured', fakeAsync(() => {
        when(createSessionStateService.parameterInfo).thenReturn(null);

        expect(component.formControl.disabled).toBeFalse();
        expect(() => {
          component.onStart();
        }).toThrow();

        expect(component.formControl.disabled).toBeFalse();
      }));
    });

    describe('onStart', () => {
      beforeEach(fakeAsync(() => {
        when(manuellerImportService.createSessionAndStartNetzklassenImport(anything(), anything())).thenResolve();

        component.onStart();
        tick();
      }));

      it('should invoke startNetzklassenImport', () => {
        verify(manuellerImportService.createSessionAndStartNetzklassenImport(anything(), anything())).once();

        expect(capture(manuellerImportService.createSessionAndStartNetzklassenImport).last()[0]).toEqual({
          organisation: organisationsId,
          netzklasse,
        } as StartNetzklassenImportSessionCommand);
      });

      it('should invoke navigate', () => {
        verify(router.navigate(deepEqual(['../' + routeForStep]), anything())).once();
        expect().nothing();
      });
    });
  });

  // Fall 2: es existiert eine Session und der createSessionStateService ist leer
  describe('SessionExists', () => {
    beforeEach(
      waitForAsync(() => {
        // Fall 2
        when(manuellerImportService.existsImportSession(ImportTyp.NETZKLASSE_ZUWEISEN)).thenResolve(true);
        when(manuellerImportService.getImportSession()).thenReturn(
          of({
            organisationsID: organisationsId,
            typ: importTyp,
            netzklasse,
          } as any)
        );
        when(createSessionStateService.dateiUploadInfo).thenReturn(null);
        when(createSessionStateService.parameterInfo).thenReturn(null);

        fixture = MockRender(ImportNetzklasseParameterEingebenComponent);
        component = fixture.point.componentInstance;
        fixture.detectChanges();
      })
    );

    it('Should fill form correctly but disable for modification', () => {
      expect(component.sessionExists).toBeTrue();
      expect(component.formControl.disabled).toBeTrue();
      expect(component.formControl.value).toEqual(netzklasse);
    });

    describe('onAbort', () => {
      beforeEach(fakeAsync(() => {
        when(manuellerImportService.deleteImportSession()).thenResolve();
        when(manuellerImportRoutingService.getStartStepRoute()).thenReturn(routeForStep);

        component.onAbort();
        tick();
      }));

      it('should invoke deleteNetzklassenImportSession', () => {
        verify(manuellerImportService.deleteImportSession()).once();
        expect().nothing();
      });

      it('should invoke navigate', () => {
        verify(router.navigate(deepEqual(['../' + routeForStep]), anything())).once();
        expect().nothing();
      });
    });

    describe('onStart', () => {
      beforeEach(fakeAsync(() => {
        when(manuellerImportRoutingService.getRouteForStep(2, ImportTyp.NETZKLASSE_ZUWEISEN)).thenReturn(routeForStep);

        component.onStart();
        tick();
      }));

      it('should NOT invoke toFormData and startNetzklassenImport', () => {
        verify(manuellerImportService.createSessionAndStartNetzklassenImport(anything(), anything())).never();
        expect().nothing();
      });

      it('should invoke navigate', () => {
        verify(router.navigate(deepEqual(['../' + routeForStep]), anything())).once();
        expect().nothing();
      });
    });
  });
});
