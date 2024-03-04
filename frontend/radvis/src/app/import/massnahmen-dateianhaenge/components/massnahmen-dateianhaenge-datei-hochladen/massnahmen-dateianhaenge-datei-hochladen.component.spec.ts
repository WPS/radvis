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
import { MockBuilder, MockRender, MockedComponentFixture } from 'ng-mocks';
import { Subject, of } from 'rxjs';
import { ImportModule } from 'src/app/import/import.module';
import { MassnahmenDateianhaengeImportSessionView } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-view';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';
import { defaultGemeinden } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { anything, capture, instance, mock, verify, when } from 'ts-mockito';
import { MassnahmenDateianhaengeDateiHochladenComponent } from './massnahmen-dateianhaenge-datei-hochladen.component';

describe(MassnahmenDateianhaengeDateiHochladenComponent.name, () => {
  let component: MassnahmenDateianhaengeDateiHochladenComponent;
  let fixture: MockedComponentFixture<MassnahmenDateianhaengeDateiHochladenComponent>;

  let service: MassnahmenDateianhaengeService;
  let routingService: MassnahmenDateianhaengeRoutingService;

  let organisationenService: OrganisationenService;
  let notifyUserService: NotifyUserService;

  let importExistsSubject: Subject<boolean>;
  let importSessionSubject: Subject<MassnahmenDateianhaengeImportSessionView | null>;

  beforeEach(() => {
    service = mock(MassnahmenDateianhaengeService);
    routingService = mock(MassnahmenDateianhaengeRoutingService);
    organisationenService = mock(OrganisationenService);
    notifyUserService = mock(NotifyUserService);

    return MockBuilder(MassnahmenDateianhaengeDateiHochladenComponent, ImportModule)
      .provide({ provide: MassnahmenDateianhaengeService, useValue: instance(service) })
      .provide({ provide: MassnahmenDateianhaengeRoutingService, useValue: instance(routingService) })
      .provide({ provide: OrganisationenService, useValue: instance(organisationenService) })
      .provide({ provide: NotifyUserService, useValue: instance(notifyUserService) })
      .provide({ provide: ErrorHandlingService, useValue: instance(mock(ErrorHandlingService)) });
  });

  beforeEach(() => {
    importExistsSubject = new Subject();
    importSessionSubject = new Subject();

    when(organisationenService.getGebietskoerperschaften()).thenResolve(defaultGemeinden);
    when(service.existsImportSession()).thenReturn(importExistsSubject);
    when(service.getImportSession()).thenReturn(importSessionSubject);

    fixture = MockRender(MassnahmenDateianhaengeDateiHochladenComponent);
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
      expect(component.massnahmenDateianhaengeSessionExists).toBeFalse();
    });

    it('should build command correctly', fakeAsync(() => {
      when(service.createSessionAndStartMassnahmenDateianhaengeImport(anything(), anything())).thenReturn(
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

      expect(capture(service.createSessionAndStartMassnahmenDateianhaengeImport).last()[0]).toEqual({
        gebietskoerperschaften: expectedGebietskoerperschaften,
        konzeptionsquelle: expectedKonzeptionsquelle,
        sollStandard: expectedSollStandard,
      });
      expect(component.sessionCreated).toBeTrue();
      verify(notifyUserService.inform(anything())).once();

      tick();

      importSessionSubject.next({
        log: [],
        schritt: 2,
        executing: false,
        gebietskoerperschaften: expectedGebietskoerperschaften,
        konzeptionsquelle: expectedKonzeptionsquelle,
        sollStandard: expectedSollStandard,
        zuordnungen: [],
      });

      tick();

      expect(component.sessionExists).toBeTrue();
      expect(component.massnahmenDateianhaengeSessionExists).toBeTrue();
      verify(routingService.navigateToNext(1)).once();
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
    let session: MassnahmenDateianhaengeImportSessionView;
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
          zuordnungen: [],
        };
        importSessionSubject.next(session);
      })
    );

    it('should fill form', () => {
      expect(component.sessionExists).toBeTrue();
      expect(component.massnahmenDateianhaengeSessionExists).toBeTrue();
      expect(component.formGroup.disabled).toBeTrue();

      expect(component.formGroup.value).toEqual({
        konzeptionsquelle: session.konzeptionsquelle,
        sollStandard: session.sollStandard,
        gebietskoerperschaften: defaultGemeinden,
        file: null,
      });
    });

    it('should clear form on abort', () => {
      when(service.deleteImportSession()).thenReturn(of(undefined));

      component.onAbort();

      expect(component.sessionExists).toBeFalse();
      expect(component.massnahmenDateianhaengeSessionExists).toBeFalse();
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

    it('should navigate to next onNext', () => {
      component.onNext();
      verify(routingService.navigateToNext(1)).once();
      expect().nothing();
    });
  });

  describe('different session exists', () => {
    beforeEach(() => {
      importExistsSubject.next(true);
      importSessionSubject.next(null);
    });

    it('should disable form', () => {
      expect(component.sessionExists).toBeTrue();
      expect(component.massnahmenDateianhaengeSessionExists).toBeFalse();
      expect(component.formGroup.disabled).toBeTrue();
    });
  });
});
