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
import { ImportNetzklasseDateiHochladenTestAdapter } from 'src/app/import/netzklassen/components/import-netzklasse-datei-hochladen/import-netzklasse-datei-hochladen-test-adapter.spec';
import { ImportNetzklasseDateiHochladenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-datei-hochladen/import-netzklasse-datei-hochladen.component';
import { NetzklassenImportSessionView } from 'src/app/import/netzklassen/models/netzklassen-import-session-view';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { CreateSessionStateService } from 'src/app/import/services/create-session.state.service';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import {
  defaultBundeslandOrganisation,
  defaultOrganisation,
  defaultUebergeordneteOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(ImportNetzklasseDateiHochladenComponent.name, () => {
  let component: ImportNetzklasseDateiHochladenComponent;
  let fixture: MockedComponentFixture<ImportNetzklasseDateiHochladenComponent>;

  let netzklassenImportService: NetzklassenImportService;
  let netzklassenRoutingService: NetzklassenRoutingService;
  let organisationenService: OrganisationenService;
  let errorHandlingService: ErrorHandlingService;
  let olMapService: OlMapService;

  let createSessionStateService: CreateSessionStateService;

  const organisationsId = 65;
  const organisation = { ...defaultOrganisation, id: organisationsId };
  const importTyp: ImportTyp = ImportTyp.NETZKLASSE_ZUWEISEN;
  const fileName = 'emptyFile';
  const emptyTestFile: File = new File([], fileName);

  beforeEach(() => {
    netzklassenImportService = mock(NetzklassenImportService);
    netzklassenRoutingService = mock(NetzklassenRoutingService);
    organisationenService = mock(OrganisationenService);
    errorHandlingService = mock(ErrorHandlingService);
    createSessionStateService = mock(CreateSessionStateService);
    olMapService = mock(OlMapComponent);

    when(organisationenService.getOrganisationen()).thenResolve([organisation]);
    when(organisationenService.getBereichEnvelopeView(anything())).thenResolve({
      bereich: { coordinates: [[], []], type: 'Polygon' },
    });
  });

  beforeEach(() => {
    return MockBuilder(ImportNetzklasseDateiHochladenComponent, ImportModule)
      .provide({ provide: NetzklassenImportService, useValue: instance(netzklassenImportService) })
      .provide({ provide: NetzklassenRoutingService, useValue: instance(netzklassenRoutingService) })
      .provide({ provide: OrganisationenService, useValue: instance(organisationenService) })
      .provide({ provide: ErrorHandlingService, useValue: instance(errorHandlingService) })
      .provide({ provide: CreateSessionStateService, useValue: instance(createSessionStateService) })
      .provide({ provide: OlMapService, useValue: instance(olMapService) });
  });

  // Aufteilen in drei describe Bloecke, da schon im Konstruktor der Komponente die formGroup gefuellt wird
  // und das Verhalten abhängig ist, ob eine session existiert und ob der createSessionStateService schon eine
  // vollstaendige dateiUploadInfo hat oder es sich um einen komplett "cleanen" Zustand handelt

  // Von dem Anfangszustand unabhaengige Tests
  describe('NoSessionAndNoDateiUploadInfo', () => {
    beforeEach(() => {
      // Fall 0: wenn vorher noch nichts an der Komponente gemacht wurde
      when(netzklassenImportService.existsImportSession()).thenReturn(of(false));
      when(netzklassenImportService.getImportSession()).thenReturn(of(null));
      when(createSessionStateService.dateiUploadInfo).thenReturn(null);
    });

    describe('with Baden Württemberg Organisation', () => {
      beforeEach(() => {
        when(organisationenService.getOrganisationen()).thenResolve([
          organisation,
          defaultUebergeordneteOrganisation,
          defaultBundeslandOrganisation,
        ]);

        fixture = MockRender(ImportNetzklasseDateiHochladenComponent);
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
      beforeEach(fakeAsync(() => {
        fixture = MockRender(ImportNetzklasseDateiHochladenComponent);
        component = fixture.point.componentInstance;
        fixture.detectChanges();
      }));

      describe('Weiter Button', () => {
        let adapter: ImportNetzklasseDateiHochladenTestAdapter;
        beforeEach(() => {
          adapter = new ImportNetzklasseDateiHochladenTestAdapter(fixture.debugElement);
        });

        it('should say Weiter and be enabled if session exists', () => {
          component.sessionExists = true;
          component.netzklassenSessionExists = true;
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
        let adapter: ImportNetzklasseDateiHochladenTestAdapter;
        beforeEach(() => {
          adapter = new ImportNetzklasseDateiHochladenTestAdapter(fixture.debugElement);
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
            component.netzklassenSessionExists = true;
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
        it('should just route if session exists', () => {
          component.sessionExists = true;
          component.onNext();

          verify(netzklassenRoutingService.navigateToNext(1)).once();
          verify(createSessionStateService.updateDateiUploadInfo(anything())).never();
          expect().nothing();
        });

        it('should upload and route if session not exists', fakeAsync(() => {
          component.sessionExists = false;
          component.formGroup.patchValue({
            organisation,
            file: emptyTestFile,
          });

          component.onNext();
          tick();

          verify(createSessionStateService.updateDateiUploadInfo(anything())).once();
          expect(capture(createSessionStateService.updateDateiUploadInfo).last()[0]).toEqual(
            DateiUploadInfo.of(importTyp, emptyTestFile, organisationsId)
          );

          tick();
          verify(netzklassenRoutingService.navigateToNext(1)).once();
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

      describe('Automatic map zoom', () => {
        beforeEach(() => {
          component.formGroup.enable();
        });

        it('should zoom if a valid entry is selected', fakeAsync(() => {
          component.formGroup.get('organisation')?.patchValue(defaultOrganisation);
          tick();
          verify(olMapService.scrollIntoViewByGeometry(anything())).once();
          expect().nothing();
        }));

        it('should not zoom if the null label is selected', fakeAsync(() => {
          component.formGroup.get('organisation')?.patchValue(null);
          tick();
          verify(olMapService.scrollIntoViewByGeometry(anything())).never();
          expect().nothing();
        }));
      });
    });
  });

  // Fall 1: es existiert KEINE Session und der createSessionStateService enthaelt eine vollstaendige DateiUploadInfo
  describe('NoSessionExists_sessionStateService.dateiUploadInfo_vorhanden', () => {
    beforeEach(
      waitForAsync(() => {
        when(netzklassenImportService.existsImportSession()).thenReturn(of(false));
        when(netzklassenImportService.getImportSession()).thenReturn(of(null));
        when(createSessionStateService.dateiUploadInfo).thenReturn(
          DateiUploadInfo.of(importTyp, emptyTestFile, organisationsId)
        );

        fixture = MockRender(ImportNetzklasseDateiHochladenComponent);
        component = fixture.point.componentInstance;
        fixture.detectChanges();
      })
    );

    it('should be filled and not disabled if dateiUploadInfo exists in createSessionStateService', () => {
      expect(component.formGroup.disabled).toBeFalse();
      expect(component.formGroup.getRawValue().organisation).toEqual(organisation);
      expect(component.formGroup.getRawValue().file).toEqual(emptyTestFile);
      expect(component.importTyp).toEqual(importTyp);
    });

    describe('onAbort', () => {
      it('should clear form and reset createSessionStateService', fakeAsync(() => {
        when(netzklassenImportService.deleteImportSession()).thenReturn(of(undefined));

        component.onAbort();
        tick();

        expect(component.sessionExists).toBeFalse();
        expect(component.formGroup.pristine);
        expect(component.formGroup.value.organisation).toBeNull();
        expect(component.formGroup.value.file).toBeNull();
        expect(component.importTyp).toEqual(importTyp);

        verify(createSessionStateService.reset()).once();
      }));
    });
  });

  // Fall 2: es existiert eine Session und der createSessionStateService ist leer
  describe('SessionExists', () => {
    beforeEach(
      waitForAsync(() => {
        when(netzklassenImportService.existsImportSession()).thenReturn(of(true));
        when(netzklassenImportService.getImportSession()).thenReturn(
          of({
            organisationsID: organisationsId,
          } as NetzklassenImportSessionView)
        );
        when(createSessionStateService.dateiUploadInfo).thenReturn(null);

        fixture = MockRender(ImportNetzklasseDateiHochladenComponent);
        component = fixture.point.componentInstance;
        fixture.detectChanges();
      })
    );

    it('should be filled and disabled when session exists', () => {
      expect(component.formGroup.disabled).toBeTrue();
      expect(component.formGroup.getRawValue().organisation).toEqual(organisation);
      expect(component.importTyp).toEqual(importTyp);
    });

    describe('onAbort', () => {
      it('should clear form and correct session exists', fakeAsync(() => {
        when(netzklassenImportService.deleteImportSession()).thenReturn(of(undefined));

        component.onAbort();
        tick();

        expect(component.sessionExists).toBeFalse();
        expect(component.formGroup.value.organisation).toBeNull();
        expect(component.formGroup.value.file).toBeNull();
        expect(component.importTyp).toEqual(importTyp);
      }));
    });
  });
});
