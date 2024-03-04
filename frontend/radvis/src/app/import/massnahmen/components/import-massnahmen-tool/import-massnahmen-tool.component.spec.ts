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
import { Subject } from 'rxjs';
import { ImportModule } from 'src/app/import/import.module';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { instance, mock, verify, when } from 'ts-mockito';
import { ImportMassnahmenToolComponent } from './import-massnahmen-tool.component';

describe(ImportMassnahmenToolComponent.name, () => {
  let component: ImportMassnahmenToolComponent;
  let fixture: MockedComponentFixture<ImportMassnahmenToolComponent>;

  let massnahmenImportService: MassnahmenImportService;
  let massnahmenImportRoutingService: MassnahmenImportRoutingService;

  let importSessionSubject: Subject<MassnahmenImportSessionView | null>;

  beforeEach(() => {
    massnahmenImportService = mock(MassnahmenImportService);
    massnahmenImportRoutingService = mock(MassnahmenImportRoutingService);

    return MockBuilder(ImportMassnahmenToolComponent, ImportModule)
      .provide({ provide: MassnahmenImportService, useValue: instance(massnahmenImportService) })
      .provide({ provide: MassnahmenImportRoutingService, useValue: instance(massnahmenImportRoutingService) });
  });

  beforeEach(() => {
    importSessionSubject = new Subject();

    when(massnahmenImportService.getImportSession()).thenReturn(importSessionSubject);

    fixture = MockRender(ImportMassnahmenToolComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('without session', () => {
    beforeEach(done => {
      importSessionSubject.subscribe(() => done());
      importSessionSubject.next(null);
    });

    it('should navigate to first step', () => {
      verify(massnahmenImportRoutingService.navigateToFirst()).once();
      expect().nothing();
    });
  });

  describe('with session', () => {
    let session: MassnahmenImportSessionView;
    beforeEach(done => {
      importSessionSubject.subscribe(() => done());
      session = {
        sollStandard: SollStandard.BASISSTANDARD,
        gebietskoerperschaften: [1],
        konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT,
        log: [],
        schritt: 3,
        executing: true,
        attribute: [],
      };
      importSessionSubject.next(session);
    });

    it('should call routing service', () => {
      verify(massnahmenImportRoutingService.navigateToStep(session.schritt)).once();
      expect().nothing();
    });
  });
});
