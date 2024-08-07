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
import { LEIHSTATIONEN } from 'src/app/viewer/leihstation/models/leihstation.infrastruktur';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { LeihstationEditorComponent } from './components/leihstation-editor/leihstation-editor.component';
import { LeihstationLayerComponent } from './components/leihstation-layer/leihstation-layer.component';
import { LeihstationTabelleComponent } from './components/leihstation-tabelle/leihstation-tabelle.component';

@NgModule({
  declarations: [LeihstationTabelleComponent, LeihstationEditorComponent, LeihstationLayerComponent],
  imports: [CommonModule, SharedModule, ViewerSharedModule],
  exports: [LeihstationTabelleComponent, LeihstationLayerComponent],
  providers: [
    {
      provide: InfrastrukturToken,
      useValue: LEIHSTATIONEN,
      multi: true,
    },
  ],
})
export class LeihstationModule {}
