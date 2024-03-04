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
import { of, Subject } from 'rxjs';
import { ImportModule } from 'src/app/import/import.module';
import { ImportMassnahmenDateiHochladenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-datei-hochladen/import-massnahmen-datei-hochladen.component';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { defaultGemeinden } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';

describe(ImportMassnahmenDateiHochladenComponent.name, () => {
  let component: ImportMassnahmenDateiHochladenComponent;
  let fixture: MockedComponentFixture<ImportMassnahmenDateiHochladenComponent>;

  let massnahmenImportService: MassnahmenImportService;
  let massnahmenImportRoutingService: MassnahmenImportRoutingService;
  let organisationenService: OrganisationenService;
  let notifyUserService: NotifyUserService;

  let importExistsSubject: Subject<boolean>;
  let importSessionSubject: Subject<MassnahmenImportSessionView | null>;

  beforeEach(() => {
    massnahmenImportService = mock(MassnahmenImportService);
    massnahmenImportRoutingService = mock(MassnahmenImportRoutingService);
    organisationenService = mock(OrganisationenService);
    notifyUserService = mock(NotifyUserService);

    return MockBuilder(ImportMassnahmenDateiHochladenComponent, ImportModule)
      .provide({ provide: MassnahmenImportService, useValue: instance(massnahmenImportService) })
      .provide({ provide: MassnahmenImportRoutingService, useValue: instance(massnahmenImportRoutingService) })
      .provide({ provide: OrganisationenService, useValue: instance(organisationenService) })
      .provide({ provide: NotifyUserService, useValue: instance(notifyUserService) })
      .provide({ provide: ErrorHandlingService, useValue: instance(mock(ErrorHandlingService)) });
  });

  beforeEach(() => {
    importExistsSubject = new Subject();
    importSessionSubject = new Subject();

    when(organisationenService.getGebietskoerperschaften()).thenResolve(defaultGemeinden);
    when(massnahmenImportService.existsImportSession()).thenReturn(importExistsSubject);
    when(massnahmenImportService.getImportSession()).thenReturn(importSessionSubject);

    fixture = MockRender(ImportMassnahmenDateiHochladenComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('no session exists', () => {
    beforeEach(() => {
      importExistsSubject.next(false);
      importSessionSubject.next(null);
    });

    it('should create', () => {
      expect(component).toBeTruthy();
      expect(component.sessionExists).toBeFalse();
      expect(component.massnahmenSessionExists).toBeFalse();
    });

    it('should navigate to next onNext', () => {
      component.onNext();
      verify(massnahmenImportRoutingService.navigateToNext(1)).once();
      expect().nothing();
    });

    it('should build command correctly', fakeAsync(() => {
      when(massnahmenImportService.createSessionAndStartMassnahmenImport(anything(), anything())).thenReturn(
        of(undefined)
      );

      const expectedGebietskoerperschaften = [1, 2, 3];
      const expectedKonzeptionsquelle = Konzeptionsquelle.RADNETZ_MASSNAHME;
      const expectedSollStandard = SollStandard.BASISSTANDARD;

      component.formGroup.patchValue({
        gebietskoerperschaften: defaultGemeinden,
        konzeptionsquelle: expectedKonzeptionsquelle,
        sollStandard: expectedSollStandard,
        file: new File([], 'emptyFile'),
      });

      component.onStart();

      expect(capture(massnahmenImportService.createSessionAndStartMassnahmenImport).last()[0]).toEqual({
        gebietskoerperschaften: expectedGebietskoerperschaften,
        konzeptionsquelle: expectedKonzeptionsquelle,
        sollStandard: expectedSollStandard,
      });
      expect(component.sessionCreated).toBeTrue();

      tick();

      importSessionSubject.next({
        log: [],
        schritt: 2,
        executing: false,
        gebietskoerperschaften: expectedGebietskoerperschaften,
        konzeptionsquelle: expectedKonzeptionsquelle,
        sollStandard: expectedSollStandard,
        attribute: [],
      });

      tick();

      expect(component.sessionExists).toBeTrue();
      expect(component.massnahmenSessionExists).toBeTrue();
      verify(massnahmenImportRoutingService.navigateToNext(1)).once();
      component.pollingSubscription?.unsubscribe();
    }));

    describe('form', () => {
      beforeEach(() => {
        component.formGroup.patchValue({
          gebietskoerperschaften: defaultGemeinden,
          konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME,
          sollStandard: SollStandard.BASISSTANDARD,
          file: new File([], 'emptyFile'),
        });
      });

      it('should be valid', () => {
        expect(component.formGroup.valid).toBeTrue();
      });

      it('should be valid if no sollstandard', () => {
        component.formGroup.get('sollStandard')?.reset();
        expect(component.formGroup.valid).toBeTrue();
      });

      it('should be invalid if no gebietskoerperschaften', () => {
        component.formGroup.get('gebietskoerperschaften')?.reset();
        expect(component.formGroup.invalid).toBeTrue();
      });

      it('should be invalid if no konzeptionsquelle', () => {
        component.formGroup.get('konzeptionsquelle')?.reset();
        expect(component.formGroup.invalid).toBeTrue();
      });

      it('should be invalid if no file', () => {
        component.formGroup.get('file')?.reset();
        expect(component.formGroup.invalid).toBeTrue();
      });
    });
  });

  describe('session exists', () => {
    let session: MassnahmenImportSessionView;
    beforeEach(
      waitForAsync(() => {
        importExistsSubject.next(true);
        session = {
          log: [],
          schritt: 2,
          executing: false,
          gebietskoerperschaften: [1, 2, 3],
          konzeptionsquelle: Konzeptionsquelle.RADNETZ_MASSNAHME,
          sollStandard: SollStandard.BASISSTANDARD,
          attribute: [],
        };
        importSessionSubject.next(session);
      })
    );

    it('should fill form', () => {
      expect(component.sessionExists).toBeTrue();
      expect(component.massnahmenSessionExists).toBeTrue();
      expect(component.formGroup.disabled).toBeTrue();

      expect(component.formGroup.value).toEqual({
        konzeptionsquelle: session.konzeptionsquelle,
        sollStandard: session.sollStandard,
        gebietskoerperschaften: defaultGemeinden,
        file: null,
      });
    });

    it('should clear form on abort', () => {
      when(massnahmenImportService.deleteImportSession()).thenReturn(of(undefined));

      component.onAbort();

      expect(component.sessionExists).toBeFalse();
      expect(component.massnahmenSessionExists).toBeFalse();
      expect(component.sessionCreated).toBeFalse();
      expect(component.formGroup.disabled).toBeFalse();
      expect(component.formGroup.pristine).toBeTrue();
      expect(component.formGroup.value).toEqual({
        konzeptionsquelle: null,
        sollStandard: null,
        gebietskoerperschaften: null,
        file: null,
      });
    });
  });

  describe('different session exists', () => {
    beforeEach(() => {
      importExistsSubject.next(true);
      importSessionSubject.next(null);
    });

    it('should disable form', () => {
      expect(component.sessionExists).toBeTrue();
      expect(component.massnahmenSessionExists).toBeFalse();
      expect(component.formGroup.disabled).toBeTrue();
    });
  });
});
