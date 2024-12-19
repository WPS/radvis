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
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { MockComponent } from 'ng-mocks';
import { Subject } from 'rxjs';
import { InfoPanelComponent } from 'src/app/import/import-shared/components/info-panel/info-panel.component';
import { AutomatischerImportSchritt } from 'src/app/import/models/automatischer-import-schritt';
import { Severity } from 'src/app/import/models/import-session-view';
import { ImportNetzklasseAutomatischeAbbildungTestAdapter } from 'src/app/import/netzklassen/components/import-netzklasse-automatische-abbildung/import-netzklasse-automatische-abbildung-test-adapter.spec';
import { ImportNetzklasseAutomatischeAbbildungComponent } from 'src/app/import/netzklassen/components/import-netzklasse-automatische-abbildung/import-netzklasse-automatische-abbildung.component';
import { NetzklassenImportSessionView } from 'src/app/import/netzklassen/models/netzklassen-import-session-view';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { instance, mock, when } from 'ts-mockito';

describe(ImportNetzklasseAutomatischeAbbildungComponent.name, () => {
  let component: ImportNetzklasseAutomatischeAbbildungComponent;
  let fixture: ComponentFixture<ImportNetzklasseAutomatischeAbbildungComponent>;
  let netzklassenImportService: NetzklassenImportService;
  let sessionSubject: Subject<NetzklassenImportSessionView | null>;

  const defaultNetzklassenImportSessionView = {
    aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
    log: [],
    schritt: 3,
    executing: true,
    organisationsID: 1,
    netzklasse: Netzklasse.KREISNETZ_ALLTAG,
    kanteIdsMitNetzklasse: [],
    anzahlFeaturesOhneMatch: null,
    anzahlKantenMitUneindeutigerAttributzuordnung: null,
  } as NetzklassenImportSessionView;

  beforeEach(async () => {
    sessionSubject = new Subject();
    netzklassenImportService = mock(NetzklassenImportService);
    when(netzklassenImportService.getImportSession()).thenReturn(sessionSubject.asObservable());

    await TestBed.configureTestingModule({
      declarations: [ImportNetzklasseAutomatischeAbbildungComponent, MockComponent(InfoPanelComponent)],
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
          provide: NotifyUserService,
          useValue: instance(mock(NotifyUserService)),
        },
        {
          provide: NetzklassenRoutingService,
          useValue: instance(mock(NetzklassenRoutingService)),
        },
        {
          provide: NetzklassenImportService,
          useValue: instance(netzklassenImportService),
        },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ImportNetzklasseAutomatischeAbbildungComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    sessionSubject.next(defaultNetzklassenImportSessionView);
  });

  afterEach(() => {
    fixture.destroy();
  });

  describe('status', () => {
    let adapter: ImportNetzklasseAutomatischeAbbildungTestAdapter;
    beforeEach(() => {
      adapter = new ImportNetzklasseAutomatischeAbbildungTestAdapter(fixture.debugElement);
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
        schritt: 3,
        executing: false,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        schritt: 3,
        executing: false,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        schritt: 3,
        executing: false,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();
    });

    it('should enable Weiter-button if laststep is reached', () => {
      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
        log: [],
        schritt: 4,
        executing: false,
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
        schritt: 3,
        executing: false,
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
        schritt: 3,
        executing: false,
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
        schritt: 3,
        executing: false,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.getErledigteSchritte()).toEqual([AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN]);

      expect(adapter.getFehlerhafteSchritte()).toEqual([AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ]);

      expect(adapter.getOffeneSchritte()).toEqual([AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN]);

      sessionSubject.next({
        ...defaultNetzklassenImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
        log: [],
        schritt: 4,
        executing: false,
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
