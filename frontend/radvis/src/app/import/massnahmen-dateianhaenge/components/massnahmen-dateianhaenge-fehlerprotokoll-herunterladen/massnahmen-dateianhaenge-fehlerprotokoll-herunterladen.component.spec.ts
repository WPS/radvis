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
import { MassnahmenDateianhaengeImportProtokollStats } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-protokoll-stats';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { anything, deepEqual, instance, mock, verify, when } from 'ts-mockito';
import { MassnahmenDateianhaengeFehlerprotokollHerunterladenComponent } from './massnahmen-dateianhaenge-fehlerprotokoll-herunterladen.component';

describe(MassnahmenDateianhaengeFehlerprotokollHerunterladenComponent.name, () => {
  let component: MassnahmenDateianhaengeFehlerprotokollHerunterladenComponent;
  let fixture: MockedComponentFixture<MassnahmenDateianhaengeFehlerprotokollHerunterladenComponent>;

  let service: MassnahmenDateianhaengeService;
  let routingService: MassnahmenDateianhaengeRoutingService;
  let fileHandlingService: FileHandlingService;

  const stats: MassnahmenDateianhaengeImportProtokollStats = {
    anzahlErfolgreichImportierterDateien: 1,
    anzahlNichtErfolgreichImportierterDateien: 0,
    anzahlFehlerhafterDateien: 2,
    anzahlIgnorierterDateien: 2,
  };

  ngMocks.faster();

  beforeAll(() => {
    service = mock(MassnahmenDateianhaengeService);
    routingService = mock(MassnahmenDateianhaengeRoutingService);
    fileHandlingService = mock(FileHandlingService);

    return MockBuilder(MassnahmenDateianhaengeFehlerprotokollHerunterladenComponent, ImportModule)
      .provide({ provide: MassnahmenDateianhaengeService, useValue: instance(service) })
      .provide({ provide: MassnahmenDateianhaengeRoutingService, useValue: instance(routingService) })
      .provide({ provide: FileHandlingService, useValue: instance(fileHandlingService) });
  });

  beforeEach(() => {
    when(service.getProtokollStats()).thenReturn(of(stats));

    fixture = MockRender(MassnahmenDateianhaengeFehlerprotokollHerunterladenComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(component.protokollStats).toEqual(stats);
  });

  it('should initiate download onDownloadFehlerprotokoll', () => {
    const blob = new Blob(['test'], { type: 'text/plain' });
    when(service.downloadFehlerprotokoll()).thenReturn(of(blob));

    component.onDownloadFehlerProtokoll();

    verify(fileHandlingService.downloadInBrowser(deepEqual(blob), anything())).once();
    expect().nothing();
  });

  it('should navigate to first onDone', () => {
    when(service.deleteImportSession()).thenReturn(of(undefined));

    component.onDone();

    verify(routingService.navigateToFirst()).once();
    expect().nothing();
  });
});
