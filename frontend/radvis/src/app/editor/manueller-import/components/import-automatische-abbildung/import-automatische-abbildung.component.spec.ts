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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, ActivatedRouteSnapshot, Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { ImportAutomatischeAbbildungTestAdapter } from 'src/app/editor/manueller-import/components/import-automatische-abbildung/import-automatische-abbildung-test-adapter.spec';
import { ImportAutomatischeAbbildungComponent } from 'src/app/editor/manueller-import/components/import-automatische-abbildung/import-automatische-abbildung.component';
import { AutomatischerImportSchritt } from 'src/app/editor/manueller-import/models/automatischer-import-schritt';
import { ImportSessionStatus } from 'src/app/editor/manueller-import/models/import-session-status';
import { ImportSessionView, Severity } from 'src/app/editor/manueller-import/models/import-session-view';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { NetzklassenImportSessionView } from 'src/app/editor/manueller-import/models/netzklassen-import-session-view';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { instance, mock, when } from 'ts-mockito';
import { MatExpansionModule } from '@angular/material/expansion';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('AutomatischeAbbildungComponent', () => {
  let component: ImportAutomatischeAbbildungComponent;
  let fixture: ComponentFixture<ImportAutomatischeAbbildungComponent>;
  let manuellerImportService: ManuellerImportService;
  let activatedRoute: ActivatedRoute;
  let sessionSubject: Subject<ImportSessionView>;

  const defaultNetzklassenImportSessionView = {
    typ: ImportTyp.NETZKLASSE_ZUWEISEN,
    aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
    log: [],
    status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_RUNNING,
    organisationsID: 1,
    netzklasse: Netzklasse.KREISNETZ_ALLTAG,
    kanteIdsMitNetzklasse: [],
    anzahlFeaturesOhneMatch: null,
    anzahlKantenMitUneindeutigerAttributzuordnung: null,
  } as NetzklassenImportSessionView;

  beforeEach(async () => {
    sessionSubject = new Subject();
    manuellerImportService = mock(ManuellerImportService);
    activatedRoute = mock(ActivatedRoute);

    when(activatedRoute.snapshot).thenReturn(createDummySnapshotRoute());
    when(manuellerImportService.getImportSession()).thenReturn(sessionSubject.asObservable());
    await TestBed.configureTestingModule({
      declarations: [ImportAutomatischeAbbildungComponent],
      imports: [
        MatIconModule,
        RouterModule,
        MatProgressSpinnerModule,
        MatFormFieldModule,
        MatExpansionModule,
        NoopAnimationsModule,
      ],
      providers: [
        {
          provide: ManuellerImportService,
          useValue: instance(manuellerImportService),
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
          useValue: instance(mock(NotifyUserService)),
        },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ImportAutomatischeAbbildungComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    sessionSubject.next(defaultNetzklassenImportSessionView);
  });
  afterEach(() => {
    fixture.destroy();
  });

  describe('status', () => {
    let adapter: ImportAutomatischeAbbildungTestAdapter;
    beforeEach(() => {
      adapter = new ImportAutomatischeAbbildungTestAdapter(fixture.debugElement);
    });

    it('should disable Weiter-button if last step has not been reached', () => {
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();
    });

    it('should disable Weiter-button if errors are present', () => {
      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();
    });

    it('should enable Weiter-button if laststep is reached', () => {
      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
        log: [],
        status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeFalse();
    });

    it('should show correct status-summary', () => {
      expect(adapter.sindAlleSchritteOffen()).toBeTrue();

      sessionSubject.next(defaultNetzklassenImportSessionView);
      component['changeDetectorRef'].detectChanges();

      expect(adapter.sindAlleSchritteOffen()).toBeTrue();

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.getErledigteSchritte()).toHaveSize(0);

      expect(adapter.getFehlerhafteSchritte()).toEqual([AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN]);

      expect(adapter.getOffeneSchritte()).toEqual([
        AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
        AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
      ]);

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.getErledigteSchritte()).toEqual([AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN]);

      expect(adapter.getFehlerhafteSchritte()).toHaveSize(0);

      expect(adapter.getOffeneSchritte()).toEqual([
        AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
        AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
      ]);

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.getErledigteSchritte()).toHaveSize(0);

      expect(adapter.getFehlerhafteSchritte()).toEqual([AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN]);

      expect(adapter.getOffeneSchritte()).toEqual([
        AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
        AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
      ]);

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.getErledigteSchritte()).toEqual([AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN]);

      expect(adapter.getFehlerhafteSchritte()).toHaveSize(0);

      expect(adapter.getOffeneSchritte()).toEqual([
        AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
        AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
      ]);

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.getErledigteSchritte()).toEqual([AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN]);

      expect(adapter.getFehlerhafteSchritte()).toEqual([AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ]);

      expect(adapter.getOffeneSchritte()).toEqual([AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN]);

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
        log: [],
        status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.sindAlleSchritteErledigt()).toBeTrue();
    });
  });

  describe('hasFehler', () => {
    it('should be true', () => {
      const fehlertext = 'Test';
      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        log: [
          {
            fehlerBeschreibung: fehlertext,
            severity: Severity.ERROR,
          },
        ],
      });

      expect(component.hasFehler).toBeTrue();
      expect(component.fehler).toEqual([fehlertext]);
    });

    it('should be false if empty', () => {
      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        log: [],
      });

      expect(component.hasFehler).toBeFalse();
      expect(component.fehler.length).toBe(0);
    });

    it('should be false if warnungen', () => {
      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        log: [
          {
            fehlerBeschreibung: 'Test',
            severity: Severity.WARN,
          },
        ],
      });

      expect(component.hasFehler).toBeFalse();
      expect(component.fehler.length).toBe(0);
    });
  });

  describe('hasWarnung', () => {
    it('should be true', () => {
      const fehlertext = 'Test';
      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        log: [
          {
            fehlerBeschreibung: fehlertext,
            severity: Severity.WARN,
          },
        ],
      });

      expect(component.hasWarnung).toBeTrue();
      expect(component.warnungen).toEqual([fehlertext]);
    });

    it('should be false if empty', () => {
      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        log: [],
      });

      expect(component.hasWarnung).toBeFalse();
      expect(component.warnungen.length).toBe(0);
    });

    it('should be false if errors', () => {
      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        log: [
          {
            fehlerBeschreibung: 'Test',
            severity: Severity.ERROR,
          },
        ],
      });

      expect(component.hasWarnung).toBeFalse();
      expect(component.warnungen.length).toBe(0);
    });
  });
});

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
function createDummySnapshotRoute(): ActivatedRouteSnapshot {
  return ({ data: { step: 1 } } as unknown) as ActivatedRouteSnapshot;
}
