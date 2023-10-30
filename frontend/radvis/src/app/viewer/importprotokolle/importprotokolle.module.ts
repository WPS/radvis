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

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { IMPORTPROTOKOLLE } from 'src/app/viewer/importprotokolle/models/importprotokoll.infrastruktur';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { FahrradrouteImportDetailViewComponent } from './components/fahrradroute-import-detail-view/fahrradroute-import-detail-view.component';
import { ImportprotokolleTabelleComponent } from './components/importprotokolle-tabelle/importprotokolle-tabelle.component';
import { WegweiserImportDetailViewComponent } from './components/wegweiser-import-detail-view/wegweiser-import-detail-view.component';
import { StatistikAnzeigeComponent } from './components/statistik-anzeige/statistik-anzeige.component';

@NgModule({
  declarations: [
    ImportprotokolleTabelleComponent,
    FahrradrouteImportDetailViewComponent,
    WegweiserImportDetailViewComponent,
    StatistikAnzeigeComponent,
  ],
  providers: [
    {
      provide: InfrastrukturToken,
      useValue: IMPORTPROTOKOLLE,
      multi: true,
    },
  ],
  imports: [CommonModule, SharedModule, ViewerSharedModule],
  exports: [ImportprotokolleTabelleComponent],
})
export class ImportprotokolleModule {}
