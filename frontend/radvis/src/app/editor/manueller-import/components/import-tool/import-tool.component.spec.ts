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

import { ActivatedRoute, Event, NavigationEnd, NavigationStart, Router } from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { Subject } from 'rxjs';
import { ManuellerImportModule } from 'src/app/editor/manueller-import/manueller-import.module';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { anything, deepEqual, instance, mock, verify, when } from 'ts-mockito';
import { ImportToolComponent } from './import-tool.component';
import { ImportSessionView, Severity } from 'src/app/editor/manueller-import/models/import-session-view';
import { ImportSessionStatus } from 'src/app/editor/manueller-import/models/import-session-status';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { AutomatischerImportSchritt } from 'src/app/editor/manueller-import/models/automatischer-import-schritt';
import { waitForAsync } from '@angular/core/testing';
import { AttributeImportSessionView } from 'src/app/editor/manueller-import/models/attribute-import-session-view';
import { AttributeImportFormat } from 'src/app/editor/manueller-import/models/attribute-import-format';

describe(ImportToolComponent.name, () => {
  let fixture: MockedComponentFixture<ImportToolComponent>;
  let component: ImportToolComponent;

  let router: Router;
  let manuellerImportService: ManuellerImportService;
  let activatedRoute: ActivatedRoute;

  let routerEvents: Subject<Event>;

  beforeEach(() => {
    router = mock(Router);
    manuellerImportService = mock(ManuellerImportService);
    activatedRoute = mock(ActivatedRoute);

    routerEvents = new Subject();
    when(router.events).thenReturn(routerEvents);

    return MockBuilder(ImportToolComponent, ManuellerImportModule)
      .provide({ provide: Router, useFactory: () => instance(router) })
      .provide({ provide: ManuellerImportService, useFactory: () => instance(manuellerImportService) })
      .provide({ provide: ActivatedRoute, useFactory: () => instance(activatedRoute) });
  });

  describe('without session', () => {
    let existsImportSession: Subject<boolean>;

    beforeEach(() => {
      existsImportSession = new Subject();

      when(manuellerImportService.existsImportSession()).thenReturn(existsImportSession.toPromise());
      when(manuellerImportService.getImportSession()).thenReturn(new Subject());

      fixture = MockRender(ImportToolComponent);
      component = fixture.point.componentInstance;
      fixture.detectChanges();
    });

    it('should create component', () => {
      expect(component).toBeTruthy();
    });

    describe('with routerEvents', () => {
      beforeEach(() => {
        component.activeStepIndex = -1;
      });

      [
        { url: ManuellerImportRoutingService.IMPORT_DATEI_UPLOAD_ROUTE, stepIndex: 0 },
        { url: ManuellerImportRoutingService.IMPORT_NETZKLASSE_PARAMETER_EINGEBEN_ROUTE, stepIndex: 1 },
        { url: ManuellerImportRoutingService.IMPORT_ATTRIBUTE_PARAMETER_EINGEBEN_ROUTE, stepIndex: 1 },
        { url: ManuellerImportRoutingService.IMPORT_AUTOMATISCHE_ABBILDUNG_ROUTE, stepIndex: 2 },
        { url: ManuellerImportRoutingService.IMPORT_NETZKLASSE_KORREKTUR_ROUTE, stepIndex: 3 },
        { url: ManuellerImportRoutingService.IMPORT_ATTRIBUTE_KORREKTUR_ROUTE, stepIndex: 3 },
        { url: ManuellerImportRoutingService.IMPORT_NETZKLASSE_ABSCHLUSS_ROUTE, stepIndex: 4 },
        { url: ManuellerImportRoutingService.IMPORT_ATTRIBUTE_ABSCHLUSS_ROUTE, stepIndex: 4 },
      ].forEach(({ url, stepIndex }) => {
        describe(`with NavigationEnd event and url ${url}`, () => {
          beforeEach(done => {
            routerEvents.subscribe(() => done());
            when(router.url).thenReturn(url);
            routerEvents.next(new NavigationEnd(1, 'egal', ''));
          });

          it(`should set activeStepIndex: ${stepIndex}`, () => {
            expect(component.activeStepIndex).toEqual(stepIndex);
          });
        });
      });

      describe('with invalid event', () => {
        beforeEach(done => {
          routerEvents.subscribe(() => done());
          when(router.url).thenReturn(ManuellerImportRoutingService.IMPORT_NETZKLASSE_ABSCHLUSS_ROUTE);
          routerEvents.next(new NavigationStart(1, 'egal'));
        });

        it('should not set activeStepIndex', () => {
          expect(component.activeStepIndex).toEqual(-1);
        });
      });
    });

    describe('without importSession', () => {
      beforeEach(done => {
        existsImportSession.subscribe(() => done());
        existsImportSession.next(false);
      });

      it(`should navigate to ${ManuellerImportRoutingService.IMPORT_DATEI_UPLOAD_ROUTE}`, () => {
        verify(router.navigate(deepEqual([ManuellerImportRoutingService.IMPORT_DATEI_UPLOAD_ROUTE]), anything()));
        expect().nothing();
      });
    });
  });

  describe('with session', () => {
    let importSessionSubject: Subject<ImportSessionView>;
    beforeEach(
      waitForAsync(() => {
        importSessionSubject = new Subject();
        when(manuellerImportService.existsImportSession()).thenReturn(Promise.resolve(true));
        when(manuellerImportService.getImportSession()).thenReturn(importSessionSubject);

        fixture = MockRender(ImportToolComponent);
        component = fixture.point.componentInstance;
        fixture.detectChanges();
      })
    );

    for (const { typ, url } of [
      {
        typ: ImportTyp.NETZKLASSE_ZUWEISEN,
        url: ManuellerImportRoutingService.IMPORT_NETZKLASSE_PARAMETER_EINGEBEN_ROUTE,
      },
      {
        typ: ImportTyp.ATTRIBUTE_UEBERNEHMEN,
        url: ManuellerImportRoutingService.IMPORT_ATTRIBUTE_PARAMETER_EINGEBEN_ROUTE,
      },
    ]) {
      describe(`Parameter eingeben mit typ: ${typ}`, () => {
        beforeEach(done => {
          importSessionSubject.subscribe(() => done());
          importSessionSubject.next({
            aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
            log: [],
            organisationsID: 0,
            typ,
            status: ImportSessionStatus.SESSION_CREATED,
            anzahlFeaturesOhneMatch: null,
            anzahlKantenMitUneindeutigerAttributzuordnung: null,
            attribute: ['attributex'],
            attributeImportFormat: AttributeImportFormat.LUBW,
          } as AttributeImportSessionView);
        });

        it(`should navigate to url: ${url}`, () => {
          verify(router.navigate(deepEqual([url]), anything())).once();
          expect().nothing();
        });
      });
    }

    describe('Automatische Abbildung', () => {
      describe('Running', () => {
        beforeEach(done => {
          importSessionSubject.subscribe(() => done());
          importSessionSubject.next({
            aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
            log: [],
            organisationsID: 0,
            typ: ImportTyp.ATTRIBUTE_UEBERNEHMEN,
            status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_RUNNING,
            anzahlFeaturesOhneMatch: null,
            anzahlKantenMitUneindeutigerAttributzuordnung: null,
            attribute: ['attributex'],
            attributeImportFormat: AttributeImportFormat.LUBW,
          } as AttributeImportSessionView);
        });

        it('should navigate to url', () => {
          verify(
            router.navigate(deepEqual([ManuellerImportRoutingService.IMPORT_AUTOMATISCHE_ABBILDUNG_ROUTE]), anything())
          ).once();
          expect().nothing();
        });
      });

      describe('Done with errors', () => {
        beforeEach(done => {
          importSessionSubject.subscribe(() => done());
          importSessionSubject.next({
            aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
            log: [{ severity: Severity.ERROR, fehlerBeschreibung: 'WAMBO!' }],
            organisationsID: 0,
            typ: ImportTyp.ATTRIBUTE_UEBERNEHMEN,
            status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
            anzahlFeaturesOhneMatch: null,
            anzahlKantenMitUneindeutigerAttributzuordnung: null,
            attribute: ['attributex'],
          } as AttributeImportSessionView);
        });

        it('should navigate to url', () => {
          verify(
            router.navigate(deepEqual([ManuellerImportRoutingService.IMPORT_AUTOMATISCHE_ABBILDUNG_ROUTE]), anything())
          ).once();
          expect().nothing();
        });
      });
    });

    for (const { typ, url } of [
      {
        typ: ImportTyp.NETZKLASSE_ZUWEISEN,
        url: ManuellerImportRoutingService.IMPORT_NETZKLASSE_KORREKTUR_ROUTE,
      },
      {
        typ: ImportTyp.ATTRIBUTE_UEBERNEHMEN,
        url: ManuellerImportRoutingService.IMPORT_ATTRIBUTE_KORREKTUR_ROUTE,
      },
    ]) {
      describe(`Abbildung bearbeiten mit typ: ${typ}`, () => {
        beforeEach(done => {
          importSessionSubject.subscribe(() => done());
          importSessionSubject.next({
            aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
            log: [],
            organisationsID: 0,
            typ,
            status: ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE,
            anzahlFeaturesOhneMatch: null,
            anzahlKantenMitUneindeutigerAttributzuordnung: null,
            attribute: ['attributex'],
            attributeImportFormat: AttributeImportFormat.LUBW,
          } as AttributeImportSessionView);
        });

        it(`should navigate to url: ${url}`, () => {
          verify(router.navigate(deepEqual([url]), anything())).once();
          expect().nothing();
        });
      });
    }

    for (const status of [ImportSessionStatus.UPDATE_DONE, ImportSessionStatus.UPDATE_EXECUTING]) {
      for (const { typ, url } of [
        {
          typ: ImportTyp.NETZKLASSE_ZUWEISEN,
          url: ManuellerImportRoutingService.IMPORT_NETZKLASSE_ABSCHLUSS_ROUTE,
        },
        {
          typ: ImportTyp.ATTRIBUTE_UEBERNEHMEN,
          url: ManuellerImportRoutingService.IMPORT_ATTRIBUTE_ABSCHLUSS_ROUTE,
        },
      ]) {
        describe(`Import abschließen mit typ: ${typ} and status: ${status}`, () => {
          beforeEach(done => {
            importSessionSubject.subscribe(() => done());
            importSessionSubject.next({
              aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
              log: [],
              organisationsID: 0,
              typ,
              status,
              anzahlFeaturesOhneMatch: null,
              anzahlKantenMitUneindeutigerAttributzuordnung: null,
              attribute: ['attributex'],
              attributeImportFormat: AttributeImportFormat.LUBW,
            } as AttributeImportSessionView);
          });

          it(`should navigate to url: ${url}`, () => {
            verify(router.navigate(deepEqual([url]), anything())).once();
            expect().nothing();
          });
        });
      }
    }
  });
});
