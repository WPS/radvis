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

import { DebugElement } from '@angular/core';
import { discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { By } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { MockedComponentFixture, MockRender, ngMocks } from 'ng-mocks';
import { MatomoTracker } from 'ngx-matomo-client';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { of, Subscription } from 'rxjs';
import { FehlerprotokollService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll.service';
import { ImportSharedModule } from 'src/app/import/import-shared/import-shared.module';
import { AutomatischerImportSchritt } from 'src/app/import/models/automatischer-import-schritt';
import { ImportLogEintrag, Severity } from 'src/app/import/models/import-session-view';
import { ImportNetzklasseAbschliessenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-abschliessen/import-netzklasse-abschliessen.component';
import { NetzklassenImportSessionView } from 'src/app/import/netzklassen/models/netzklassen-import-session-view';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { defaultOrganisation } from 'src/app/shared/models/organisation-test-data-provider.spec';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { anything, instance, mock, when } from 'ts-mockito';

describe(ImportNetzklasseAbschliessenComponent.name, () => {
  let component: ImportNetzklasseAbschliessenComponent;
  let fixture: MockedComponentFixture<ImportNetzklasseAbschliessenComponent>;
  let netzklassenImportService: NetzklassenImportService;
  let radVisNetzFeatureService: NetzausschnittService;
  let notifyUserService: NotifyUserService;
  let organisationenService: OrganisationenService;
  let matomoTracker: MatomoTracker;

  const defaultNetzklassenImportSessionView: NetzklassenImportSessionView = {
    aktuellerImportSchritt: AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
    log: [],
    schritt: 5,
    executing: false,
    organisationsID: 1,
    netzklasse: Netzklasse.KREISNETZ_ALLTAG,
    anzahlFeaturesOhneMatch: null,
  };

  ngMocks.faster();

  beforeEach(() => {
    netzklassenImportService = mock(NetzklassenImportService);
    radVisNetzFeatureService = mock(NetzausschnittService);
    notifyUserService = mock(NotifyUserService);
    organisationenService = mock(OrganisationenService);
    matomoTracker = mock(MatomoTracker);

    when(netzklassenImportService.getImportSession()).thenReturn(of(defaultNetzklassenImportSessionView));
    when(radVisNetzFeatureService.getKantenFuerZustaendigkeitsbereich(anything(), anything())).thenResolve({
      type: 'FeatureCollection',
      features: [],
    } as GeoJSONFeatureCollection);
    when(netzklassenImportService.getKanteIdsMitNetzklasse()).thenResolve([]);
    when(organisationenService.getOrganisation(anything())).thenResolve(defaultOrganisation);

    return TestBed.configureTestingModule({
      declarations: [ImportNetzklasseAbschliessenComponent],
      imports: [MatIconModule, ImportSharedModule, RouterModule, MatProgressSpinnerModule, MatFormFieldModule],
      providers: [
        { provide: NetzklassenImportService, useValue: instance(netzklassenImportService) },
        { provide: NetzklassenRoutingService, useValue: instance(mock(NetzklassenRoutingService)) },
        { provide: NetzausschnittService, useValue: instance(radVisNetzFeatureService) },
        { provide: NotifyUserService, useValue: instance(notifyUserService) },
        { provide: OrganisationenService, useValue: instance(organisationenService) },
        { provide: FehlerprotokollService, useValue: instance(mock(FehlerprotokollService)) },
        { provide: MatomoTracker, useValue: instance(matomoTracker) },
      ],
    });
  });

  beforeEach(() => {
    fixture = MockRender(ImportNetzklasseAbschliessenComponent);
    component = fixture.point.componentInstance;
    component.fetchingFeatures = true;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  describe('session update is done on startup', () => {
    const updateDoneNetzklassenImportSessionView: NetzklassenImportSessionView = {
      ...defaultNetzklassenImportSessionView,
      schritt: 0,
      executing: false,
      anzahlFeaturesOhneMatch: 300,
    };

    beforeEach(fakeAsync(() => {
      fixture.destroy();

      when(netzklassenImportService.getImportSession()).thenReturn(of(updateDoneNetzklassenImportSessionView));

      fixture = MockRender(ImportNetzklasseAbschliessenComponent);
      component = fixture.point.componentInstance;
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
      schritt: 5,
      executing: true,
      anzahlFeaturesOhneMatch: null,
    };

    beforeEach(fakeAsync(() => {
      fixture.destroy();

      when(netzklassenImportService.getImportSession()).thenReturn(of(updateExecutingNetzklassenImportSessionView));

      fixture = MockRender(ImportNetzklasseAbschliessenComponent);
      component = fixture.point.componentInstance;
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

  describe('schritt', () => {
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
        schritt: 5,
        executing: true,
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
        schritt: 0,
        executing: false,
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
        schritt: 0,
        executing: false,
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
        schritt: 5,
        executing: true,
        anzahlFeaturesOhneMatch: 300,
      });

      expect(detectChangesSpy).not.toHaveBeenCalled();
    });

    it('should trigger onChange when Done', () => {
      const detectChangesSpy = spyOn(component['changeDetectorRef'], 'markForCheck');

      component['handleSessionupdate']({
        ...defaultNetzklassenImportSessionView,
        schritt: 0,
        executing: false,
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
        schritt: 5,
        executing: true,
      };
      component['changeDetectorRef'].detectChanges();

      const detectChangesSpy = spyOn(component['changeDetectorRef'], 'markForCheck');
      const notifyUserSpy = spyOn(component['notifyUserService'], 'warn');
      component['handleSessionupdate']({
        ...defaultNetzklassenImportSessionView,
        schritt: 5,
        executing: false,
      });

      expect(detectChangesSpy).toHaveBeenCalled();
      expect(notifyUserSpy).toHaveBeenCalledWith(
        'Kanten in der Organisation wurden während des Speicherns durch einen anderen Prozess verändert. Bitte versuchen Sie es erneut.'
      );
    });
  });
});

function doesSpeicherButtonExist(component: DebugElement): boolean {
  const speicherButton = buttons(component)?.[2];
  return (
    speicherButton !== undefined && (speicherButton.nativeElement as HTMLElement).textContent?.trim() === 'Speichern'
  );
}

function isSpeicherButtonDisabled(component: DebugElement): boolean {
  return buttons(component)?.[2].nativeElement.disabled || false;
}

function doesFertigButtonExist(component: DebugElement): boolean {
  const doneButton = buttons(component)?.[0];
  return doneButton !== undefined && (doneButton.nativeElement as HTMLElement).textContent?.trim() === 'Fertig';
}

function isFertigButtonDisabled(component: DebugElement): boolean {
  return buttons(component)?.[0].nativeElement.disabled || false;
}

function isAnyErrorDisplayed(component: DebugElement): boolean {
  return (
    (component.query(By.css('.error'))?.children[0]?.nativeElement as HTMLElement)?.textContent ===
      'Die Netzklassenübernahme ist fehlgeschlagen' || false
  );
}

function isErrorDisplayed(component: DebugElement, expectedErrormessage: string): boolean {
  return (
    (component.query(By.css('.error'))?.children[1]?.nativeElement as HTMLElement)?.textContent === expectedErrormessage
  );
}

function isSuccessMessageDisplayed(component: DebugElement): boolean {
  return (
    (component.query(By.css('span'))?.nativeElement as HTMLElement)?.textContent?.includes(
      'Die resultierende Netzklassenzugehörigkeit wurde gespeichert'
    ) || false
  );
}

const buttons = (component: DebugElement): DebugElement[] =>
  component.queryAll(By.css('.import-step-footer-buttons button'));
