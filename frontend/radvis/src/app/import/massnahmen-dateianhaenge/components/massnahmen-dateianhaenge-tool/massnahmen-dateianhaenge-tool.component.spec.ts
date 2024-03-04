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

import { MockBuilder, MockRender, MockedComponentFixture } from 'ng-mocks';
import { Subject } from 'rxjs';
import { ImportModule } from 'src/app/import/import.module';
import { MassnahmenDateianhaengeImportSessionView } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-view';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import { instance, mock, verify, when } from 'ts-mockito';
import { MassnahmenDateianhaengeToolComponent } from './massnahmen-dateianhaenge-tool.component';

describe(MassnahmenDateianhaengeToolComponent.name, () => {
  let component: MassnahmenDateianhaengeToolComponent;
  let fixture: MockedComponentFixture<MassnahmenDateianhaengeToolComponent>;

  let service: MassnahmenDateianhaengeService;
  let routingService: MassnahmenDateianhaengeRoutingService;

  let importSessionSubject: Subject<MassnahmenDateianhaengeImportSessionView | null>;

  beforeEach(() => {
    service = mock(MassnahmenDateianhaengeService);
    routingService = mock(MassnahmenDateianhaengeRoutingService);

    return MockBuilder(MassnahmenDateianhaengeToolComponent, ImportModule)
      .provide({ provide: MassnahmenDateianhaengeService, useValue: instance(service) })
      .provide({ provide: MassnahmenDateianhaengeRoutingService, useValue: instance(routingService) });
  });

  beforeEach(() => {
    importSessionSubject = new Subject();

    when(service.getImportSession()).thenReturn(importSessionSubject);

    fixture = MockRender(MassnahmenDateianhaengeToolComponent);
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
      verify(routingService.navigateToFirst()).once();
      expect().nothing();
    });
  });

  describe('with session', () => {
    let session: MassnahmenDateianhaengeImportSessionView;
    beforeEach(done => {
      importSessionSubject.subscribe(() => done());
      session = {
        sollStandard: SollStandard.BASISSTANDARD,
        gebietskoerperschaften: [1],
        konzeptionsquelle: Konzeptionsquelle.KOMMUNALES_KONZEPT,
        log: [],
        schritt: 3,
        executing: true,
        zuordnungen: [],
      };
      importSessionSubject.next(session);
    });

    it('should call routing service', () => {
      verify(routingService.navigateToStep(session.schritt)).once();
      expect().nothing();
    });
  });
});
