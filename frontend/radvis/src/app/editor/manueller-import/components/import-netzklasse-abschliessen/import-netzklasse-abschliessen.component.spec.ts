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

/* eslint-disable @typescript-eslint/dot-notation */
import { DebugElement } from '@angular/core';
import { ComponentFixture, discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, ActivatedRouteSnapshot, Router, RouterModule } from '@angular/router';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { of, Subscription } from 'rxjs';
import { ImportNetzklasseAbschliessenComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-abschliessen/import-netzklasse-abschliessen.component';
import { AutomatischerImportSchritt } from 'src/app/editor/manueller-import/models/automatischer-import-schritt';
import { ImportSessionStatus } from 'src/app/editor/manueller-import/models/import-session-status';
import { ImportLogEintrag, Severity } from 'src/app/editor/manueller-import/models/import-session-view';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { NetzklassenImportSessionView } from 'src/app/editor/manueller-import/models/netzklassen-import-session-view';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { FehlerprotokollService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll.service';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { anything, instance, mock, when } from 'ts-mockito';

describe('ImportNetzklasseAbschliessenComponent', () => {
  let component: ImportNetzklasseAbschliessenComponent;
  let fixture: ComponentFixture<ImportNetzklasseAbschliessenComponent>;
  let manuellerImportService: ManuellerImportService;
  let radVisNetzFeatureService: NetzausschnittService;
  let notifyUserService: NotifyUserService;
  let organisationenService;
  let activatedRoute: ActivatedRoute;

  const defaultNetzklassenImportSessionView: NetzklassenImportSessionView = {
    typ: ImportTyp.NETZKLASSE_ZUWEISEN,
    aktuellerImportSchritt: AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
    log: [],
    status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
    organisationsID: 1,
    netzklasse: Netzklasse.KREISNETZ_ALLTAG,
    anzahlFeaturesOhneMatch: null,
  };

  beforeEach(async () => {
    manuellerImportService = mock(ManuellerImportService);
    radVisNetzFeatureService = mock(NetzausschnittService);
    activatedRoute = mock(ActivatedRoute);
    notifyUserService = mock(NotifyUserService);
    organisationenService = mock(OrganisationenService);

    when(activatedRoute.snapshot).thenReturn(createDummySnapshotRoute());
    when(manuellerImportService.getImportSession()).thenReturn(of(defaultNetzklassenImportSessionView));
    when(radVisNetzFeatureService.getKantenFuerZustaendigkeitsbereich(anything(), anything())).thenResolve({
      type: 'FeatureCollection',
      features: [],
    } as GeoJSONFeatureCollection);
    when(manuellerImportService.getKanteIdsMitNetzklasse()).thenResolve([]);
    when(organisationenService.getOrganisation(anything())).thenResolve(defaultOrganisation);

    await TestBed.configureTestingModule({
      declarations: [ImportNetzklasseAbschliessenComponent],
      imports: [MatIconModule, RouterModule, MatProgressSpinnerModule, MatFormFieldModule],
      providers: [
        {
          provide: ManuellerImportService,
          useValue: instance(manuellerImportService),
        },
        {
          provide: NetzausschnittService,
          useValue: instance(radVisNetzFeatureService),
        },
        {
          provide: ActivatedRoute,
          useValue: instance(activatedRoute),
        },
        {
          provide: Router,
          useValue: instance(mock(Router)),
        },
        {
          provide: NotifyUserService,
          useValue: instance(notifyUserService),
        },
        {
          provide: OrganisationenService,
          useValue: instance(organisationenService),
        },
        {
          provide: FehlerprotokollService,
          useValue: instance(mock(FehlerprotokollService)),
        },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ImportNetzklasseAbschliessenComponent);
    component = fixture.componentInstance;
    component.fetchingFeatures = true;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  describe('session update is done on startup', () => {
    const updateDoneNetzklassenImportSessionView: NetzklassenImportSessionView = {
      ...defaultNetzklassenImportSessionView,
      status: ImportSessionStatus.UPDATE_DONE,
      anzahlFeaturesOhneMatch: 300,
    };

    beforeEach(fakeAsync(() => {
      fixture.destroy();

      when(manuellerImportService.getImportSession()).thenReturn(of(updateDoneNetzklassenImportSessionView));

      fixture = TestBed.createComponent(ImportNetzklasseAbschliessenComponent);
      component = fixture.componentInstance;
      component.fetchingFeatures = false;

      tick();
      discardPeriodicTasks();
    }));

    it('polling should not yet start', () => {
      expect(component['pollingSubscription']).toEqual(Subscription.EMPTY);
    });

    it('should have error statistics', () => {
      expect(component.session?.anzahlFeaturesOhneMatch).toBe(300);
    });
  });

  describe('session update is executing on startup', () => {
    const updateExecutingNetzklassenImportSessionView: NetzklassenImportSessionView = {
      ...defaultNetzklassenImportSessionView,
      status: ImportSessionStatus.UPDATE_EXECUTING,
      anzahlFeaturesOhneMatch: null,
    };

    beforeEach(fakeAsync(() => {
      fixture.destroy();

      when(manuellerImportService.getImportSession()).thenReturn(of(updateExecutingNetzklassenImportSessionView));

      fixture = TestBed.createComponent(ImportNetzklasseAbschliessenComponent);
      component = fixture.componentInstance;
      component.fetchingFeatures = false;

      tick();
      discardPeriodicTasks();
    }));

    it('polling should start', () => {
      expect(component['pollingSubscription']).not.toEqual(Subscription.EMPTY);
    });

    it('should have no error statistics', () => {
      expect(component.session?.anzahlFeaturesOhneMatch).toBeNull();
    });
  });

  describe('status', () => {
    it('should not display speichern button until result is loaded', () => {
      expect(doesSpeicherButtonExist(fixture.debugElement)).toBeFalse();
      expect(doesFertigButtonExist(fixture.debugElement)).toBeFalse();

      component.fetchingFeatures = false;
      component.session = defaultNetzklassenImportSessionView;
      component['changeDetectorRef'].detectChanges();

      expect(doesSpeicherButtonExist(fixture.debugElement)).toBeTrue();
      expect(isSpeicherButtonDisabled(fixture.debugElement)).toBeFalse();
      expect(doesFertigButtonExist(fixture.debugElement)).toBeFalse();
      expect(isSuccessMessageDisplayed(fixture.debugElement)).toBeFalse();
      expect(isAnyErrorDisplayed(fixture.debugElement)).toBeFalse();
    });

    it('should display disabled fertig button when executing', () => {
      expect(doesSpeicherButtonExist(fixture.debugElement)).toBeFalse();
      expect(doesFertigButtonExist(fixture.debugElement)).toBeFalse();

      component.fetchingFeatures = false;
      component.session = {
        ...defaultNetzklassenImportSessionView,
        status: ImportSessionStatus.UPDATE_EXECUTING,
      };
      component['changeDetectorRef'].detectChanges();

      expect(doesSpeicherButtonExist(fixture.debugElement)).toBeFalse();
      expect(doesFertigButtonExist(fixture.debugElement)).toBeTrue();
      expect(isFertigButtonDisabled(fixture.debugElement)).toBeTrue();
      expect(isSuccessMessageDisplayed(fixture.debugElement)).toBeFalse();
      expect(isAnyErrorDisplayed(fixture.debugElement)).toBeFalse();
    });

    it('should enable fertig button when done executing', () => {
      expect(doesSpeicherButtonExist(fixture.debugElement)).toBeFalse();
      expect(doesFertigButtonExist(fixture.debugElement)).toBeFalse();

      component.fetchingFeatures = false;
      component.session = {
        ...defaultNetzklassenImportSessionView,
        status: ImportSessionStatus.UPDATE_DONE,
      };
      component['changeDetectorRef'].detectChanges();

      expect(doesSpeicherButtonExist(fixture.debugElement)).toBeFalse();
      expect(doesFertigButtonExist(fixture.debugElement)).toBeTrue();
      expect(isFertigButtonDisabled(fixture.debugElement)).toBeFalse();
      expect(isSuccessMessageDisplayed(fixture.debugElement)).toBeTrue();
      expect(isAnyErrorDisplayed(fixture.debugElement)).toBeFalse();
    });

    it('should display error on error', () => {
      expect(doesSpeicherButtonExist(fixture.debugElement)).toBeFalse();
      expect(doesFertigButtonExist(fixture.debugElement)).toBeFalse();

      component.fetchingFeatures = false;
      component.session = {
        ...defaultNetzklassenImportSessionView,
        status: ImportSessionStatus.UPDATE_DONE,
        log: [
          {
            fehlerBeschreibung: 'Das ist ja gestern nicht so gut gelaufen. Aber lass dich davon nicht unterkriegen.',
            severity: Severity.ERROR,
          } as ImportLogEintrag,
        ],
      };
      component['changeDetectorRef'].detectChanges();

      expect(doesSpeicherButtonExist(fixture.debugElement)).toBeFalse();
      expect(doesFertigButtonExist(fixture.debugElement)).toBeTrue();
      expect(isFertigButtonDisabled(fixture.debugElement)).toBeFalse();
      expect(isAnyErrorDisplayed(fixture.debugElement)).toBeTrue();
      expect(
        isErrorDisplayed(
          fixture.debugElement,
          'Das ist ja gestern nicht so gut gelaufen. Aber lass dich davon nicht unterkriegen.'
        )
      ).toBeTrue();
      expect(isSuccessMessageDisplayed(fixture.debugElement)).toBeFalse();
    });
  });

  describe('handleSessionupdate', () => {
    it('should not trigger onChange when Updating', () => {
      const detectChangesSpy = spyOn(component['changeDetectorRef'], 'markForCheck');

      component['handleSessionupdate']({
        ...defaultNetzklassenImportSessionView,
        status: ImportSessionStatus.UPDATE_EXECUTING,
        anzahlFeaturesOhneMatch: 300,
      });

      expect(detectChangesSpy).not.toHaveBeenCalled();
    });

    it('should trigger onChange when Done', () => {
      const detectChangesSpy = spyOn(component['changeDetectorRef'], 'markForCheck');

      component['handleSessionupdate']({
        ...defaultNetzklassenImportSessionView,
        status: ImportSessionStatus.UPDATE_DONE,
        log: [
          {
            fehlerBeschreibung: 'Das ist ja gestern nicht so gut gelaufen. Aber lass dich davon nicht unterkriegen.',
            severity: Severity.ERROR,
          } as ImportLogEintrag,
        ],
      });

      expect(detectChangesSpy).toHaveBeenCalled();
    });
    it('should trigger onChange when returning to previous status', () => {
      component.session = {
        ...defaultNetzklassenImportSessionView,
        status: ImportSessionStatus.UPDATE_EXECUTING,
      };
      component['changeDetectorRef'].detectChanges();

      const detectChangesSpy = spyOn(component['changeDetectorRef'], 'markForCheck');
      const notifyUserSpy = spyOn(component['notifyUserService'], 'warn');
      component['handleSessionupdate']({
        ...defaultNetzklassenImportSessionView,
        status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
      });

      expect(detectChangesSpy).toHaveBeenCalled();
      expect(notifyUserSpy).toHaveBeenCalledWith(
        'Kanten in der Organisation wurden während des Speicherns durch einen anderen Prozess verändert. Bitte versuchen Sie es erneut.'
      );
    });
  });
});

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function createDummySnapshotRoute(): ActivatedRouteSnapshot {
  return ({ data: { step: 1 } } as unknown) as ActivatedRouteSnapshot;
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function doesSpeicherButtonExist(component: DebugElement): boolean {
  const speicherButton = component.query(By.css('.buttons'))?.children[2];
  return (
    speicherButton !== undefined && (speicherButton.nativeElement as HTMLElement).textContent?.trim() === 'Speichern'
  );
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function isSpeicherButtonDisabled(component: DebugElement): boolean {
  return component.query(By.css('.buttons'))?.children[2].nativeElement.disabled || false;
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function doesFertigButtonExist(component: DebugElement): boolean {
  const doneButton = component.query(By.css('.buttons'))?.children[0];
  return doneButton !== undefined && (doneButton.nativeElement as HTMLElement).textContent?.trim() === 'Fertig';
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function isFertigButtonDisabled(component: DebugElement): boolean {
  return component.query(By.css('.buttons'))?.children[0].nativeElement.disabled || false;
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function isAnyErrorDisplayed(component: DebugElement): boolean {
  return (
    (component.query(By.css('.error'))?.children[0]?.nativeElement as HTMLElement)?.textContent ===
      'Die Netzklassenübernahme ist fehlgeschlagen' || false
  );
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function isErrorDisplayed(component: DebugElement, expectedErrormessage: string): boolean {
  return (
    (component.query(By.css('.error'))?.children[1]?.nativeElement as HTMLElement)?.textContent === expectedErrormessage
  );
}

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function isSuccessMessageDisplayed(component: DebugElement): boolean {
  return (
    (component.query(By.css('span'))?.nativeElement as HTMLElement)?.textContent?.includes(
      'Die resultierende Netzklassenzugehörigkeit wurde gespeichert'
    ) || false
  );
}
