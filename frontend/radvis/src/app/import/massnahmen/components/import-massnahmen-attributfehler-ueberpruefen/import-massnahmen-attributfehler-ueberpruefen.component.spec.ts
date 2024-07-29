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

import { BreakpointObserver } from '@angular/cdk/layout';
import { MatTableModule } from '@angular/material/table';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { of, Subject } from 'rxjs';
import { ImportModule } from 'src/app/import/import.module';
import { ImportMassnahmenAttributfehlerUeberpruefenTestAdapter } from 'src/app/import/massnahmen/components/import-massnahmen-attributfehler-ueberpruefen/import-massnahmen-attributfehler-ueberpruefen.test-adapter.spec';
import { MassnahmenImportAttribute } from 'src/app/import/massnahmen/models/massnahmen-import-attribute';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { MassnahmenImportZuordnungAttributfehler } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-attributfehler';
import { MassnahmenImportZuordnungStatus } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-status';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { instance, mock, verify, when } from 'ts-mockito';
import { ImportMassnahmenAttributfehlerUeberpruefenComponent } from './import-massnahmen-attributfehler-ueberpruefen.component';

describe(ImportMassnahmenAttributfehlerUeberpruefenComponent.name, () => {
  let component: ImportMassnahmenAttributfehlerUeberpruefenComponent;
  let fixture: MockedComponentFixture<ImportMassnahmenAttributfehlerUeberpruefenComponent>;

  let massnahmenImportService: MassnahmenImportService;
  let massnahmenImportRoutingService: MassnahmenImportRoutingService;

  let zuordnungenFehlerSubject: Subject<MassnahmenImportZuordnungAttributfehler[] | null>;
  let sessionSubject: Subject<MassnahmenImportSessionView | null>;

  let adapter: ImportMassnahmenAttributfehlerUeberpruefenTestAdapter;

  beforeEach(() => {
    zuordnungenFehlerSubject = new Subject<MassnahmenImportZuordnungAttributfehler[] | null>();

    massnahmenImportService = mock(MassnahmenImportService);
    when(massnahmenImportService.getZuordnungenAttributfehler()).thenReturn(zuordnungenFehlerSubject.asObservable());

    massnahmenImportRoutingService = mock(MassnahmenImportRoutingService);

    return MockBuilder(ImportMassnahmenAttributfehlerUeberpruefenComponent, ImportModule)
      .keep(MatTableModule)
      .keep(BreakpointObserver)
      .provide({ provide: MassnahmenImportService, useValue: instance(massnahmenImportService) })
      .provide({ provide: MassnahmenImportRoutingService, useValue: instance(massnahmenImportRoutingService) })
      .provide({ provide: ErrorHandlingService, useValue: instance(mock(ErrorHandlingService)) });
  });

  beforeEach(() => {
    sessionSubject = new Subject();
    when(massnahmenImportService.getImportSession()).thenReturn(sessionSubject.asObservable());
    when(massnahmenImportService.deleteImportSession()).thenReturn(of(undefined));

    fixture = MockRender(ImportMassnahmenAttributfehlerUeberpruefenComponent);
    component = fixture.point.componentInstance;
    adapter = new ImportMassnahmenAttributfehlerUeberpruefenTestAdapter(fixture.debugElement);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('hasValideMassnahmen', () => {
    it('should be false if keine massnahmen', () => {
      zuordnungenFehlerSubject.next([]);

      expect(component.hasValideMassnahme).toBe(false);
    });

    it('should be false if alle fehlerhaft', () => {
      zuordnungenFehlerSubject.next([
        {
          id: 1,
          massnahmeKonzeptId: 'Zuordnung1',
          status: MassnahmenImportZuordnungStatus.NEU,
          fehler: [{ attributName: 'TEST', text: 'fehler' }],
        },
        {
          id: 2,
          massnahmeKonzeptId: 'Zuordnung2',
          status: MassnahmenImportZuordnungStatus.NEU,
          fehler: [{ attributName: 'TEST', text: 'fehler' }],
        },
      ]);

      expect(component.hasValideMassnahme).toBe(false);
    });

    it('should be true', () => {
      zuordnungenFehlerSubject.next([
        {
          id: 1,
          massnahmeKonzeptId: 'Zuordnung1',
          status: MassnahmenImportZuordnungStatus.NEU,
          fehler: [
            { attributName: 'TEST', text: 'fehler' },
            { attributName: 'TEST2', text: 'fehler2' },
          ],
        },
        {
          id: 2,
          massnahmeKonzeptId: 'Zuordnung2',
          status: MassnahmenImportZuordnungStatus.NEU,
          fehler: [],
        },
      ]);

      expect(component.hasValideMassnahme).toBe(true);
    });
  });

  it('should subscribe to session and zuordnungen on init and handle data correctly', () => {
    const mockSession: MassnahmenImportSessionView = {
      gebietskoerperschaften: [1, 2],
      konzeptionsquelle: Konzeptionsquelle.KREISKONZEPT,
      attribute: [MassnahmenImportAttribute.BEZEICHNUNG, MassnahmenImportAttribute.PRIORITAET],
      log: [],
      schritt: 2,
      executing: false,
    };
    const mockZuordnungen: MassnahmenImportZuordnungAttributfehler[] = [
      {
        id: 1,
        massnahmeKonzeptId: 'Zuordnung1',
        status: MassnahmenImportZuordnungStatus.NEU,
        fehler: [
          {
            attributName: 'BEZEICHNUNG',
            text: 'Fehlende Bezeichnung',
          },
        ],
      },
    ];
    sessionSubject.next(mockSession);
    zuordnungenFehlerSubject.next(mockZuordnungen);

    expect(component.session).toEqual(mockSession);
    // Konvertiere die mockZuordnungen in das erwartete Format für die dataSource.data
    const erwarteteKonvertierteDaten = mockZuordnungen.map(zuordnung => ({
      status: zuordnung.status,
      id: zuordnung.massnahmeKonzeptId,
      attribut: zuordnung.fehler.map(fehler => fehler.attributName).join(', '),
      hinweis: zuordnung.fehler.map(fehler => fehler.text).join(', '),
      first: true,
      rowspan: 1,
    }));
    expect(component.dataSource.data).toEqual(erwarteteKonvertierteDaten);
  });

  describe('Navigation', () => {
    it('should navigate to first on abort', () => {
      component.onAbort();
      verify(massnahmenImportRoutingService.navigateToFirst()).once();
      expect().nothing();
    });

    it('should navigate to previous on previous button click', () => {
      component.onPrevious();
      verify(massnahmenImportRoutingService.navigateToPrevious(3)).once();
      expect().nothing();
    });

    it('should navigate to next on next button click', () => {
      component.onNext();
      verify(massnahmenImportRoutingService.navigateToNext(3)).once();
      expect().nothing();
    });
  });

  describe('with Zuordnungen', () => {
    it('should not show Zuordnung without Fehler', () => {
      zuordnungenFehlerSubject.next([
        {
          id: 1,
          status: MassnahmenImportZuordnungStatus.NEU,
          massnahmeKonzeptId: 'ValideNeueId',
          fehler: [],
        },
      ]);

      expect(adapter.numberOfRows).toEqual(0);
    });

    it('should determine correct number of massnahmen with errors', () => {
      zuordnungenFehlerSubject.next([
        {
          id: 1,
          status: MassnahmenImportZuordnungStatus.NEU,
          massnahmeKonzeptId: 'hat fehler',
          fehler: [
            { attributName: 'foo1', text: 'bar1' },
            { attributName: 'foo2', text: 'bar2' },
            { attributName: 'foo3', text: 'bar3' },
          ],
        },
        {
          id: 2,
          status: MassnahmenImportZuordnungStatus.NEU,
          massnahmeKonzeptId: 'hat keine fehler',
          fehler: [],
        },
      ]);

      expect(component.anzahlMassnahmen).toEqual(2);
      expect(component.anzahlFehlerhafterMassnahmen).toEqual(1);
    });

    [
      MassnahmenImportZuordnungStatus.NEU,
      MassnahmenImportZuordnungStatus.ZUGEORDNET,
      MassnahmenImportZuordnungStatus.FEHLERHAFT,
      MassnahmenImportZuordnungStatus.GELOESCHT,
    ].forEach(status => {
      it(`should show all Fehler for status ${status} correctly`, () => {
        zuordnungenFehlerSubject.next([
          {
            id: 1,
            status,
            massnahmeKonzeptId: `Id4Status_${status}`,
            fehler: [
              { attributName: 'attributA', text: 'Fehlertext1' },
              { attributName: 'attributB', text: 'Fehlertext2' },
            ],
          },
        ]);
        zuordnungenFehlerSubject.complete();

        expect(adapter.numberOfRows).toEqual(2);
        expect(adapter.attributes).toEqual(['attributA', 'attributB']);
        expect(adapter.fehler).toEqual(['Fehlertext1', 'Fehlertext2']);
        expect(adapter.ids).toEqual([`Id4Status_${status}`]);
      });
    });
  });
});
