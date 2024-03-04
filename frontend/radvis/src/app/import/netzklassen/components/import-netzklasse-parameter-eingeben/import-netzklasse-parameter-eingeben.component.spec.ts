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
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { of } from 'rxjs';
import { ImportModule } from 'src/app/import/import.module';
import { DateiUploadInfo } from 'src/app/import/models/datei-upload-info';
import { ImportTyp } from 'src/app/import/models/import-typ';
import { ImportNetzklasseParameterEingebenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-parameter-eingeben/import-netzklasse-parameter-eingeben.component';
import { NetzklassenImportSessionView } from 'src/app/import/netzklassen/models/netzklassen-import-session-view';
import { NetzklassenParameter } from 'src/app/import/netzklassen/models/netzklassen-parameter';
import { StartNetzklassenImportSessionCommand } from 'src/app/import/netzklassen/models/start-netzklassen-import-session-command';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { CreateSessionStateService } from 'src/app/import/services/create-session.state.service';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(ImportNetzklasseParameterEingebenComponent.name, () => {
  let fixture: MockedComponentFixture<ImportNetzklasseParameterEingebenComponent>;
  let component: ImportNetzklasseParameterEingebenComponent;
  let netzklassenImportService: NetzklassenImportService;
  let netzklassenRoutingService: NetzklassenRoutingService;
  let createSessionStateService: CreateSessionStateService;

  const organisationsId = 65;
  const importTyp: ImportTyp = ImportTyp.NETZKLASSE_ZUWEISEN;
  const fileName = 'emptyFile';
  const emptyTestFile: File = new File([], fileName);
  const netzklasse = Netzklasse.RADVORRANGROUTEN;

  beforeEach(() => {
    netzklassenRoutingService = mock(NetzklassenRoutingService);
    netzklassenImportService = mock(NetzklassenImportService);
    createSessionStateService = mock(CreateSessionStateService);

    return MockBuilder(ImportNetzklasseParameterEingebenComponent, ImportModule)
      .provide({ provide: NetzklassenRoutingService, useValue: instance(netzklassenRoutingService) })
      .provide({ provide: NetzklassenImportService, useValue: instance(netzklassenImportService) })
      .provide({ provide: CreateSessionStateService, useValue: instance(createSessionStateService) });
  });

  // Aufteilen in drei describe Bloecke, da schon im Konstruktor der Komponente die formGroup gefuellt wird
  // und das Verhalten abhängig ist, ob eine session existiert und ob der createSessionStateService schon eine
  // vollstaendige dateiUploadInfo hat oder es sich um einen komplett "cleanen" Zustand handelt

  // Fall 0: wenn vorher noch nichts an der Komponente gemacht wurde
  describe('NoSessionAndNoDateiUploadInfo', () => {
    beforeEach(
      waitForAsync(() => {
        // Fall 0
        when(netzklassenImportService.getImportSession()).thenReturn(of(null));
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
        when(netzklassenImportService.getImportSession()).thenReturn(of(null));
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
        when(netzklassenImportService.deleteImportSession()).thenReturn(of(undefined));

        component.onAbort();
        tick();
      }));

      it('should invoke createSessionStateService.reset', () => {
        verify(createSessionStateService.reset()).once();
        expect().nothing();
      });

      it('should invoke navigate', () => {
        verify(netzklassenRoutingService.navigateToFirst()).once();
        expect().nothing();
      });
    });

    describe('onStart (form handling)', () => {
      beforeEach(fakeAsync(() => {
        when(netzklassenImportService.createSessionAndStartNetzklassenImport(anything(), anything())).thenResolve();
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
        when(netzklassenImportService.createSessionAndStartNetzklassenImport(anything(), anything())).thenResolve();

        component.onStart();
        tick();
      }));

      it('should invoke startNetzklassenImport', () => {
        verify(netzklassenImportService.createSessionAndStartNetzklassenImport(anything(), anything())).once();

        expect(capture(netzklassenImportService.createSessionAndStartNetzklassenImport).last()[0]).toEqual({
          organisation: organisationsId,
          netzklasse,
        } as StartNetzklassenImportSessionCommand);
      });

      it('should invoke navigate', () => {
        verify(netzklassenRoutingService.navigateToNext(2)).once();
        expect().nothing();
      });
    });
  });

  // Fall 2: es existiert eine Session und der createSessionStateService ist leer
  describe('SessionExists', () => {
    beforeEach(
      waitForAsync(() => {
        // Fall 2
        when(netzklassenImportService.getImportSession()).thenReturn(
          of({
            organisationsID: organisationsId,
            netzklasse,
          } as NetzklassenImportSessionView)
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
        when(netzklassenImportService.deleteImportSession()).thenReturn(of(undefined));

        component.onAbort();
        tick();
      }));

      it('should invoke deleteNetzklassenImportSession', () => {
        verify(netzklassenImportService.deleteImportSession()).once();
        expect().nothing();
      });

      it('should invoke navigate', () => {
        verify(netzklassenRoutingService.navigateToFirst()).once();
        expect().nothing();
      });
    });

    describe('onStart', () => {
      beforeEach(fakeAsync(() => {
        component.onStart();
        tick();
      }));

      it('should NOT invoke toFormData and startNetzklassenImport', () => {
        verify(netzklassenImportService.createSessionAndStartNetzklassenImport(anything(), anything())).never();
        expect().nothing();
      });

      it('should invoke navigate', () => {
        verify(netzklassenRoutingService.navigateToNext(2)).once();
        expect().nothing();
      });
    });
  });
});
