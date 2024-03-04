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

import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { ImportModule } from 'src/app/import/import.module';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { instance, mock, when } from 'ts-mockito';
import { ImportMassnahmenAttributfehlerUeberpruefenComponent } from './import-massnahmen-attributfehler-ueberpruefen.component';
import { Subject } from 'rxjs';
import {
  MassnahmenImportZuordnung,
  MassnahmenImportZuordnungStatus,
} from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung';
import { fakeAsync, tick } from '@angular/core/testing';
import { MatTableModule } from '@angular/material/table';
import { BreakpointObserver } from '@angular/cdk/layout';
import { ImportMassnahmenAttributfehlerUeberpruefenTestAdapter } from 'src/app/import/massnahmen/components/import-massnahmen-attributfehler-ueberpruefen/import-massnahmen-attributfehler-ueberpruefen.test-adapter.spec';

describe(ImportMassnahmenAttributfehlerUeberpruefenComponent.name, () => {
  let component: ImportMassnahmenAttributfehlerUeberpruefenComponent;
  let fixture: MockedComponentFixture<ImportMassnahmenAttributfehlerUeberpruefenComponent>;

  let massnahmenImportService: MassnahmenImportService;
  let massnahmenImportRoutingService: MassnahmenImportRoutingService;

  let zuordnungenSubject: Subject<MassnahmenImportZuordnung[] | null>;

  let adapter: ImportMassnahmenAttributfehlerUeberpruefenTestAdapter;

  beforeEach(() => {
    zuordnungenSubject = new Subject<MassnahmenImportZuordnung[] | null>();

    massnahmenImportService = mock(MassnahmenImportService);
    massnahmenImportRoutingService = mock(MassnahmenImportRoutingService);

    when(massnahmenImportService.getZuordnungen()).thenReturn(zuordnungenSubject.asObservable());

    return MockBuilder(ImportMassnahmenAttributfehlerUeberpruefenComponent, ImportModule)
      .keep(MatTableModule)
      .keep(BreakpointObserver)
      .provide({ provide: MassnahmenImportService, useValue: instance(massnahmenImportService) })
      .provide({ provide: MassnahmenImportRoutingService, useValue: instance(massnahmenImportRoutingService) });
  });

  beforeEach(() => {
    fixture = MockRender(ImportMassnahmenAttributfehlerUeberpruefenComponent);
    component = fixture.point.componentInstance;
    adapter = new ImportMassnahmenAttributfehlerUeberpruefenTestAdapter(fixture.debugElement);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('with Zuordnungen', () => {
    it('should not show Zuordnung without Fehler', fakeAsync(() => {
      zuordnungenSubject.next([
        {
          status: MassnahmenImportZuordnungStatus.NEU,
          id: 'ValideNeueId',
          fehler: [],
        },
      ]);
      zuordnungenSubject.complete();
      tick();

      expect(adapter.numberOfRows).toEqual(0);
    }));

    [
      MassnahmenImportZuordnungStatus.NEU,
      MassnahmenImportZuordnungStatus.GEMAPPT,
      MassnahmenImportZuordnungStatus.FEHLERHAFT,
      MassnahmenImportZuordnungStatus.GELOESCHT,
    ].forEach(status => {
      it(`should show all Fehler for status ${status} correctly`, fakeAsync(() => {
        zuordnungenSubject.next([
          {
            status,
            id: `Id4Status_${status}`,
            fehler: [
              { attributName: 'attributA', text: 'Fehlertext1' },
              { attributName: 'attributB', text: 'Fehlertext2' },
            ],
          },
        ]);
        zuordnungenSubject.complete();
        tick();

        expect(adapter.numberOfRows).toEqual(2);
        expect(adapter.attributes).toEqual(['attributA', 'attributB']);
        expect(adapter.fehler).toEqual(['Fehlertext1', 'Fehlertext2']);
        expect(adapter.ids).toEqual([`Id4Status_${status}`]);
      }));
    });
  });
});
