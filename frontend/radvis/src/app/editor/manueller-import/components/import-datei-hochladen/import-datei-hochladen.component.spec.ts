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
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, ActivatedRouteSnapshot, Router } from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { of } from 'rxjs';
import { ImportDateiHochladenTestAdapter } from 'src/app/editor/manueller-import/components/import-datei-hochladen/import-datei-hochladen-test-adapter.spec';
import { ImportDateiHochladenComponent } from 'src/app/editor/manueller-import/components/import-datei-hochladen/import-datei-hochladen.component';
import { ManuellerImportModule } from 'src/app/editor/manueller-import/manueller-import.module';
import { DateiUploadInfo } from 'src/app/editor/manueller-import/models/datei-upload-info';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { CreateSessionStateService } from 'src/app/editor/manueller-import/services/create-session.state.service';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import {
  defaultBundeslandOrganisation,
  defaultOrganisation,
  defaultUebergeordneteOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(ImportDateiHochladenComponent.name, () => {
  let component: ImportDateiHochladenComponent;
  let fixture: MockedComponentFixture<ImportDateiHochladenComponent>;
  let manuellerImportService: ManuellerImportService;
  let manuellerImportRoutingService: ManuellerImportRoutingService;
  let organisationenService: OrganisationenService;
  let errorHandlingService: ErrorHandlingService;
  let router: Router;
  let activatedRoute: ActivatedRoute;

  let createSessionStateService: CreateSessionStateService;

  const organisationsId = 65;
  const organisation = { ...defaultOrganisation, id: organisationsId };
  const importTyp: ImportTyp = ImportTyp.NETZKLASSE_ZUWEISEN;
  const fileName = 'emptyFile';
  const emptyTestFile: File = new File([], fileName);

  beforeEach(() => {
    manuellerImportService = mock(ManuellerImportService);
    manuellerImportRoutingService = mock(ManuellerImportRoutingService);
    organisationenService = mock(OrganisationenService);
    errorHandlingService = mock(ErrorHandlingService);
    createSessionStateService = mock(CreateSessionStateService);
    router = mock(Router);
    activatedRoute = mock(ActivatedRoute);

    when(organisationenService.getOrganisationen()).thenResolve([organisation]);
  });

  beforeEach(() => {
    return MockBuilder(ImportDateiHochladenComponent, ManuellerImportModule)
      .keep(MatDialog)
      .provide({
        provide: ManuellerImportService,
        useValue: instance(manuellerImportService),
      })
      .provide({ provide: ManuellerImportRoutingService, useValue: instance(manuellerImportRoutingService) })
      .provide({ provide: OrganisationenService, useValue: instance(organisationenService) })
      .provide({ provide: ErrorHandlingService, useValue: instance(errorHandlingService) })
      .provide({ provide: CreateSessionStateService, useValue: instance(createSessionStateService) })
      .provide({ provide: Router, useValue: instance(router) })
      .provide({ provide: ActivatedRoute, useValue: instance(activatedRoute) });
  });

  // Aufteilen in drei describe Bloecke, da schon im Konstruktor der Komponente die formGroup gefuellt wird
  // und das Verhalten abhängig ist, ob eine session existiert und ob der createSessionStateService schon eine
  // vollstaendige dateiUploadInfo hat oder es sich um einen komplett "cleanen" Zustand handelt

  // Von dem Anfangszustand unabhaengige Tests
  describe('NoSessionAndNoDateiUploadInfo', () => {
    beforeEach(() => {
      // Fall 0: wenn vorher noch nichts an der Komponente gemacht wurde
      when(manuellerImportService.existsImportSession()).thenResolve(false);
      when(createSessionStateService.dateiUploadInfo).thenReturn(null);
    });

    describe('with Baden Württemberg Organisation', () => {
      beforeEach(() => {
        when(organisationenService.getOrganisationen()).thenResolve([
          organisation,
          defaultUebergeordneteOrganisation,
          defaultBundeslandOrganisation,
        ]);

        fixture = MockRender(ImportDateiHochladenComponent);
        component = fixture.point.componentInstance;
        fixture.detectChanges();
      });

      it('should filter Kreise und Gemeiden', done => {
        component.organisationen$.then(organisationen => {
          expect(organisationen).toHaveSize(2);
          expect(organisationen[0]).toEqual(organisation);
          expect(organisationen[1]).toEqual(defaultUebergeordneteOrganisation);
          done();
        });
      });
    });

    describe('with defaults', () => {
      beforeEach(
        waitForAsync(() => {
          fixture = MockRender(ImportDateiHochladenComponent);
          component = fixture.point.componentInstance;
          fixture.detectChanges();
        })
      );

      describe('Weiter Button', () => {
        let adapter: ImportDateiHochladenTestAdapter;
        beforeEach(() => {
          adapter = new ImportDateiHochladenTestAdapter(fixture.debugElement);
        });

        it('should say Weiter and be enabled if session exists', () => {
          component.sessionExists = true;
          // eslint-disable-next-line @typescript-eslint/dot-notation
          component['changeDetectorRef'].markForCheck();
          fixture.detectChanges();
          expect(adapter.getWeiterButtonText()).toEqual('Weiter');
          expect(adapter.isWeiterButtonDisabled()).toBeFalse();
        });

        it('should be enabled if form valid and session not exists', () => {
          const validSpy = spyOnProperty(component.formGroup, 'valid');
          component.sessionExists = false;

          validSpy.and.returnValue(true);
          // eslint-disable-next-line @typescript-eslint/dot-notation
          component['changeDetectorRef'].markForCheck();
          fixture.detectChanges();
          expect(adapter.isWeiterButtonDisabled()).toBeFalse();
        });

        it('should be disabled if form invalid and session not exists', () => {
          const validSpy = spyOnProperty(component.formGroup, 'valid');
          component.sessionExists = false;

          validSpy.and.returnValue(true);
          // eslint-disable-next-line @typescript-eslint/dot-notation
          component['changeDetectorRef'].markForCheck();
          fixture.detectChanges();
          expect(adapter.isWeiterButtonDisabled()).toBeFalse();
        });
      });

      describe('Abbrechen Button', () => {
        let adapter: ImportDateiHochladenTestAdapter;
        beforeEach(() => {
          adapter = new ImportDateiHochladenTestAdapter(fixture.debugElement);
        });

        it('should say Abbrechen', () => {
          expect(adapter.getAbbrechenButtonText()).toEqual('Abbrechen');
        });

        describe('Session does not exist and upload data does not exist', () => {
          beforeEach(() => {
            component.sessionExists = false;
            fixture.detectChanges();
          });

          it('should be disabled', () => {
            expect(adapter.isAbbrechenButtonDisabled()).toBeTrue();
          });
        });

        describe('Session exists and upload data does not exist', () => {
          beforeEach(() => {
            component.sessionExists = true;
            fixture.detectChanges();
          });

          it('should be disabled if pristine', () => {
            component.formGroup.markAsPristine();
            expect(adapter.isAbbrechenButtonDisabled()).toBeFalse();
          });

          it('should be enabled if dirty', () => {
            component.formGroup.markAsDirty();
            expect(adapter.isAbbrechenButtonDisabled()).toBeFalse();
          });
        });

        describe('Session does not exist and upload data exists', () => {
          beforeEach(() => {
            component.sessionExists = false;
            when(createSessionStateService.dateiUploadInfo).thenReturn(
              DateiUploadInfo.of(ImportTyp.NETZKLASSE_ZUWEISEN, instance(mock(File)), 1)
            );
            fixture.detectChanges();
          });

          it('should not be disabled', () => {
            expect(adapter.isAbbrechenButtonDisabled()).toBeFalse();
          });
        });
      });

      describe('onNext', () => {
        beforeEach(() => {
          when(activatedRoute.snapshot).thenReturn(({
            data: {
              step: 1,
            },
          } as unknown) as ActivatedRouteSnapshot);
        });

        it('should just route if session exists', () => {
          component.sessionExists = true;
          // eslint-disable-next-line @typescript-eslint/dot-notation
          const spy = spyOn(component['router'], 'navigate');
          component.onNext();
          expect(spy).toHaveBeenCalled();
          verify(createSessionStateService.updateDateiUploadInfo(anything())).never();
        });

        it('should upload and route if session not exists', fakeAsync(() => {
          component.sessionExists = false;
          component.formGroup.patchValue({
            importTyp,
            organisation,
            file: emptyTestFile,
          });

          // eslint-disable-next-line @typescript-eslint/dot-notation
          const routerSpy = spyOn(component['router'], 'navigate');

          component.onNext();
          tick();

          verify(createSessionStateService.updateDateiUploadInfo(anything())).once();
          expect(capture(createSessionStateService.updateDateiUploadInfo).last()[0]).toEqual(
            DateiUploadInfo.of(importTyp, emptyTestFile, organisationsId)
          );

          tick();
          expect(routerSpy).toHaveBeenCalled();
        }));
      });

      describe('Form', () => {
        beforeEach(() => {
          component.formGroup.enable();
          component.formGroup.patchValue({
            organisation: defaultOrganisation,
            file: instance(mock(File)),
          });
        });

        it('should be valid', () => {
          expect(component.formGroup.valid).toBeTrue();
        });

        it('should be invalid if no organisation', () => {
          component.formGroup.get('organisation')?.reset();
          expect(component.formGroup.valid).toBeFalse();
        });

        it('should be invalid if no file', () => {
          component.formGroup.get('file')?.reset();
          expect(component.formGroup.valid).toBeFalse();
        });
      });
    });
  });

  // Fall 1: es existiert KEINE Session und der createSessionStateService enthaelt eine vollstaendige DateiUploadInfo
  describe('NoSessionExists_sessionStateService.dateiUploadInfo_vorhanden', () => {
    beforeEach(
      waitForAsync(() => {
        when(manuellerImportService.existsImportSession()).thenResolve(false);
        when(createSessionStateService.dateiUploadInfo).thenReturn(
          DateiUploadInfo.of(importTyp, emptyTestFile, organisationsId)
        );

        fixture = MockRender(ImportDateiHochladenComponent);
        component = fixture.point.componentInstance;
        fixture.detectChanges();
      })
    );

    it('should be filled and not disabled if dateiUploadInfo exists in createSessionStateService', () => {
      expect(component.formGroup.disabled).toBeFalse();
      expect(component.formGroup.getRawValue().organisation).toEqual(organisation);
      expect(component.formGroup.getRawValue().importTyp).toEqual(importTyp);
      expect(component.formGroup.getRawValue().file).toEqual(emptyTestFile);
    });

    describe('onAbort', () => {
      it('should clear form and reset createSessionStateService', fakeAsync(() => {
        when(manuellerImportService.deleteImportSession()).thenResolve();

        component.onAbort();
        tick();

        expect(component.sessionExists).toBeFalse();
        expect(component.formGroup.pristine);
        expect(component.formGroup.value.organisation).toBeNull();
        expect(component.formGroup.value.file).toBeNull();
        expect(component.formGroup.value.importTyp).toBe(ImportTyp.NETZKLASSE_ZUWEISEN);

        verify(createSessionStateService.reset()).once();
      }));
    });
  });

  // Fall 2: es existiert eine Session und der createSessionStateService ist leer
  describe('SessionExists', () => {
    beforeEach(
      waitForAsync(() => {
        when(manuellerImportService.existsImportSession()).thenResolve(true);
        when(manuellerImportService.getImportSession()).thenReturn(
          of({
            organisationsID: organisationsId,
            typ: importTyp,
          } as any)
        );
        when(createSessionStateService.dateiUploadInfo).thenReturn(null);

        fixture = MockRender(ImportDateiHochladenComponent);
        component = fixture.point.componentInstance;
        fixture.detectChanges();
      })
    );

    it('should be filled and disabled when session exists', () => {
      expect(component.formGroup.disabled).toBeTrue();
      expect(component.formGroup.getRawValue().organisation).toEqual(organisation);
      expect(component.formGroup.getRawValue().importTyp).toEqual(importTyp);
    });

    describe('onAbort', () => {
      it('should clear form and correct session exists', fakeAsync(() => {
        when(manuellerImportService.deleteImportSession()).thenResolve();

        component.onAbort();
        tick();

        expect(component.sessionExists).toBeFalse();
        expect(component.formGroup.value.organisation).toBeNull();
        expect(component.formGroup.value.file).toBeNull();
        expect(component.formGroup.value.importTyp).toBe(ImportTyp.NETZKLASSE_ZUWEISEN);
      }));
    });
  });
});
