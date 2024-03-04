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
import { Subject } from 'rxjs';
import { ImportAttributeAutomatischeAbbildungTestAdapter } from 'src/app/import/attribute/components/import-attribute-automatische-abbildung/import-attribute-automatische-abbildung-test-adapter.spec';
import { ImportAttributeAutomatischeAbbildungComponent } from 'src/app/import/attribute/components/import-attribute-automatische-abbildung/import-attribute-automatische-abbildung.component';
import { AttributeImportFormat } from 'src/app/import/attribute/models/attribute-import-format';
import { AttributeImportSessionView } from 'src/app/import/attribute/models/attribute-import-session-view';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { AttributeRoutingService } from 'src/app/import/attribute/services/attribute-routing.service';
import { AutomatischerImportSchritt } from 'src/app/import/models/automatischer-import-schritt';
import { Severity } from 'src/app/import/models/import-session-view';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { instance, mock, when } from 'ts-mockito';

describe(ImportAttributeAutomatischeAbbildungComponent.name, () => {
  let component: ImportAttributeAutomatischeAbbildungComponent;
  let fixture: ComponentFixture<ImportAttributeAutomatischeAbbildungComponent>;
  let attributeImportService: AttributeImportService;
  let sessionSubject: Subject<AttributeImportSessionView | null>;

  const defaultAttributeImportSessionView = {
    attribute: ['test1', 'test2'],
    log: [],
    schritt: 3,
    executing: true,
    organisationsID: 1,
    anzahlFeaturesOhneMatch: null,
    anzahlKantenMitUneindeutigerAttributzuordnung: null,
    attributeImportFormat: AttributeImportFormat.RADVIS,
    aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
  } as AttributeImportSessionView;

  beforeEach(async () => {
    sessionSubject = new Subject();
    attributeImportService = mock(AttributeImportService);

    when(attributeImportService.getImportSession()).thenReturn(sessionSubject.asObservable());

    await TestBed.configureTestingModule({
      declarations: [ImportAttributeAutomatischeAbbildungComponent],
      imports: [MatIconModule, MatProgressSpinnerModule, MatFormFieldModule, MatExpansionModule, NoopAnimationsModule],
      providers: [
        { provide: NotifyUserService, useValue: instance(mock(NotifyUserService)) },
        { provide: AttributeRoutingService, useValue: instance(mock(AttributeRoutingService)) },
        { provide: AttributeImportService, useValue: instance(attributeImportService) },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ImportAttributeAutomatischeAbbildungComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    sessionSubject.next(defaultAttributeImportSessionView);
  });

  afterEach(() => {
    fixture.destroy();
  });

  describe('schritt', () => {
    let adapter: ImportAttributeAutomatischeAbbildungTestAdapter;
    beforeEach(() => {
      adapter = new ImportAttributeAutomatischeAbbildungTestAdapter(fixture.debugElement);
    });

    it('should disable Weiter-button if last step has not been reached', () => {
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultAttributeImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultAttributeImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();
    });

    it('should disable Weiter-button if errors are present', () => {
      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultAttributeImportSessionView,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        schritt: 3,
        executing: false,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultAttributeImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        schritt: 3,
        executing: false,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();

      sessionSubject.next({
        ...defaultAttributeImportSessionView,
        aktuellerImportSchritt: AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
        log: [{ fehlerBeschreibung: 'error!', severity: Severity.ERROR }],
        schritt: 3,
        executing: false,
      });
      component['changeDetectorRef'].detectChanges();

      expect(adapter.isWeiterButtonDisabled()).toBeTrue();
    });

    it('should enable Weiter-button if next step is reached', () => {
      sessionSubject.next({
        ...defaultAttributeImportSessionView,
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

      sessionSubject.next(defaultAttributeImportSessionView);
      component['changeDetectorRef'].detectChanges();

      expect(adapter.sindAlleSchritteOffen()).toBeTrue();

      sessionSubject.next({
        ...defaultAttributeImportSessionView,
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
        ...defaultAttributeImportSessionView,
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
        ...defaultAttributeImportSessionView,
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
        ...defaultAttributeImportSessionView,
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
        ...defaultAttributeImportSessionView,
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
        ...defaultAttributeImportSessionView,
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
        ...defaultAttributeImportSessionView,
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
        ...defaultAttributeImportSessionView,
        log: [],
      });

      expect(component.hasFehler).toBeFalse();
      expect(component.fehler.length).toBe(0);
    });

    it('should be false if warnungen', () => {
      sessionSubject.next({
        ...defaultAttributeImportSessionView,
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
        ...defaultAttributeImportSessionView,
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
        ...defaultAttributeImportSessionView,
        log: [],
      });

      expect(component.hasWarnung).toBeFalse();
      expect(component.warnungen.length).toBe(0);
    });

    it('should be false if errors', () => {
      sessionSubject.next({
        ...defaultAttributeImportSessionView,
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
