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

import { MockBuilder, MockedComponentFixture, MockRender, ngMocks } from 'ng-mocks';
import { of } from 'rxjs';
import { ImportModule } from 'src/app/import/import.module';
import { MassnahmenImportProtokollStats } from 'src/app/import/massnahmen/models/massnahmen-import-protokoll-stats';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { anything, deepEqual, instance, mock, verify, when } from 'ts-mockito';
import { ImportMassnahmenFehlerprotokollHerunterladenComponent } from './import-massnahmen-fehlerprotokoll-herunterladen.component';

describe(ImportMassnahmenFehlerprotokollHerunterladenComponent.name, () => {
  let component: ImportMassnahmenFehlerprotokollHerunterladenComponent;
  let fixture: MockedComponentFixture<ImportMassnahmenFehlerprotokollHerunterladenComponent>;

  let massnahmenImportService: MassnahmenImportService;
  let massnahmenImportRoutingService: MassnahmenImportRoutingService;
  let fileHandlingService: FileHandlingService;

  const stats: MassnahmenImportProtokollStats = {
    anzahlNeuAngelegt: 1,
    anzahlBearbeitet: 0,
    anzahlGeloescht: 2,
    anzahlImportNichtMoeglich: 2,
    anzahlNichtFuerImportSelektiert: 3,
  };

  ngMocks.faster();

  beforeAll(() => {
    massnahmenImportService = mock(MassnahmenImportService);
    massnahmenImportRoutingService = mock(MassnahmenImportRoutingService);
    fileHandlingService = mock(FileHandlingService);

    return MockBuilder(ImportMassnahmenFehlerprotokollHerunterladenComponent, ImportModule)
      .provide({ provide: MassnahmenImportService, useValue: instance(massnahmenImportService) })
      .provide({ provide: MassnahmenImportRoutingService, useValue: instance(massnahmenImportRoutingService) })
      .provide({ provide: FileHandlingService, useValue: instance(fileHandlingService) });
  });

  beforeEach(() => {
    when(massnahmenImportService.getProtokollStats()).thenReturn(of(stats));

    fixture = MockRender(ImportMassnahmenFehlerprotokollHerunterladenComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.protokollStats).toEqual(stats);
  });

  it('should initiate download onDownloadFehlerprotokoll', () => {
    const blob = new Blob(['test'], { type: 'text/plain' });
    when(massnahmenImportService.downloadFehlerprotokoll()).thenReturn(of(blob));

    component.onDownloadFehlerProtokoll();

    verify(fileHandlingService.downloadInBrowser(deepEqual(blob), anything())).once();
    expect().nothing();
  });

  describe('Navigation', () => {
    it('should navigate to first onDone', () => {
      when(massnahmenImportService.deleteImportSession()).thenReturn(of(undefined));

      component.onDone();

      verify(massnahmenImportRoutingService.navigateToFirst()).once();
      expect().nothing();
    });
  });
});
