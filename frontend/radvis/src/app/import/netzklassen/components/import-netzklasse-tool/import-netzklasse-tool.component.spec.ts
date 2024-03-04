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
import { AutomatischerImportSchritt } from 'src/app/import/models/automatischer-import-schritt';
import { ImportTyp } from 'src/app/import/models/import-typ';
import { NetzklassenImportSessionView } from 'src/app/import/netzklassen/models/netzklassen-import-session-view';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { instance, mock, verify, when } from 'ts-mockito';
import { ImportNetzklasseToolComponent } from './import-netzklasse-tool.component';

describe(ImportNetzklasseToolComponent.name, () => {
  let component: ImportNetzklasseToolComponent;
  let fixture: MockedComponentFixture<ImportNetzklasseToolComponent>;

  let netzklassenImportService: NetzklassenImportService;
  let netzklassenImportRoutingService: NetzklassenRoutingService;

  let importSessionSubject: Subject<NetzklassenImportSessionView | null>;

  beforeEach(() => {
    netzklassenImportService = mock(NetzklassenImportService);
    netzklassenImportRoutingService = mock(NetzklassenRoutingService);

    return MockBuilder(ImportNetzklasseToolComponent, ImportModule)
      .provide({ provide: NetzklassenImportService, useValue: instance(netzklassenImportService) })
      .provide({ provide: NetzklassenRoutingService, useValue: instance(netzklassenImportRoutingService) });
  });

  beforeEach(() => {
    importSessionSubject = new Subject();

    when(netzklassenImportService.getImportSession()).thenReturn(importSessionSubject);

    fixture = MockRender(ImportNetzklasseToolComponent);
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
      verify(netzklassenImportRoutingService.navigateToFirst()).once();
      expect().nothing();
    });
  });

  describe('with session', () => {
    let session: NetzklassenImportSessionView;
    beforeEach(done => {
      importSessionSubject.subscribe(() => done());
      session = {
        typ: ImportTyp.NETZKLASSE_ZUWEISEN,
        aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
        log: [],
        schritt: 3,
        executing: true,
        organisationsID: 0,
        anzahlFeaturesOhneMatch: null,
        netzklasse: Netzklasse.RADNETZ_ZIELNETZ,
      } as NetzklassenImportSessionView;
      importSessionSubject.next(session);
    });

    it('should call routing service', () => {
      verify(netzklassenImportRoutingService.navigateToStep(session.schritt)).once();
      expect().nothing();
    });
  });
});
