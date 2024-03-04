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
import { AttributeImportFormat } from 'src/app/import/attribute/models/attribute-import-format';
import { AttributeImportSessionView } from 'src/app/import/attribute/models/attribute-import-session-view';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { AttributeRoutingService } from 'src/app/import/attribute/services/attribute-routing.service';
import { ImportModule } from 'src/app/import/import.module';
import { AutomatischerImportSchritt } from 'src/app/import/models/automatischer-import-schritt';
import { instance, mock, verify, when } from 'ts-mockito';
import { ImportAttributeToolComponent } from './import-attribute-tool.component';

describe(ImportAttributeToolComponent.name, () => {
  let component: ImportAttributeToolComponent;
  let fixture: MockedComponentFixture<ImportAttributeToolComponent>;

  let attributeImportService: AttributeImportService;
  let attributeImportRoutingService: AttributeRoutingService;

  let importSessionSubject: Subject<AttributeImportSessionView | null>;

  beforeEach(() => {
    attributeImportService = mock(AttributeImportService);
    attributeImportRoutingService = mock(AttributeRoutingService);

    return MockBuilder(ImportAttributeToolComponent, ImportModule)
      .provide({ provide: AttributeImportService, useValue: instance(attributeImportService) })
      .provide({ provide: AttributeRoutingService, useValue: instance(attributeImportRoutingService) });
  });

  beforeEach(() => {
    importSessionSubject = new Subject();

    when(attributeImportService.getImportSession()).thenReturn(importSessionSubject);

    fixture = MockRender(ImportAttributeToolComponent);
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
      verify(attributeImportRoutingService.navigateToFirst()).once();
      expect().nothing();
    });
  });

  describe('with session', () => {
    let session: AttributeImportSessionView;
    beforeEach(done => {
      importSessionSubject.subscribe(() => done());
      session = {
        aktuellerImportSchritt: AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
        log: [],
        organisationsID: 0,
        schritt: 3,
        executing: true,
        anzahlFeaturesOhneMatch: null,
        anzahlKantenMitUneindeutigerAttributzuordnung: null,
        attribute: ['attributex'],
        attributeImportFormat: AttributeImportFormat.LUBW,
      };
      importSessionSubject.next(session);
    });

    it('should call routing service', () => {
      verify(attributeImportRoutingService.navigateToStep(session.schritt)).once();
      expect().nothing();
    });
  });
});
