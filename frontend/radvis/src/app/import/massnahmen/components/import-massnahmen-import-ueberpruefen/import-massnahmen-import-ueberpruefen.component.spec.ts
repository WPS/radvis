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
import { ImportModule } from 'src/app/import/import.module';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { instance, mock } from 'ts-mockito';
import { ImportMassnahmenImportUeberpruefenComponent } from './import-massnahmen-import-ueberpruefen.component';

describe(ImportMassnahmenImportUeberpruefenComponent.name, () => {
  let component: ImportMassnahmenImportUeberpruefenComponent;
  let fixture: MockedComponentFixture<ImportMassnahmenImportUeberpruefenComponent>;

  let massnahmenImportService: MassnahmenImportService;
  let massnahmenImportRoutingService: MassnahmenImportRoutingService;

  beforeEach(() => {
    massnahmenImportService = mock(MassnahmenImportService);
    massnahmenImportRoutingService = mock(MassnahmenImportRoutingService);

    fixture = MockRender(ImportMassnahmenImportUeberpruefenComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  beforeEach(() => {
    return MockBuilder(ImportMassnahmenImportUeberpruefenComponent, ImportModule)
      .provide({ provide: MassnahmenImportService, useValue: instance(massnahmenImportService) })
      .provide({ provide: MassnahmenImportRoutingService, useValue: instance(massnahmenImportRoutingService) });
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
