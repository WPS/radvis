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
import { ABSTELLANLAGEN } from 'src/app/viewer/abstellanlage/models/abstellanlage.infrastruktur';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { AbstellanlageEditorComponent } from './components/abstellanlage-editor/abstellanlage-editor.component';
import { AbstellanlageLayerComponent } from './components/abstellanlage-layer/abstellanlage-layer.component';
import { AbstellanlageTabelleComponent } from './components/abstellanlage-tabelle/abstellanlage-tabelle.component';
import { AbstellanlageToolComponent } from 'src/app/viewer/abstellanlage/components/abstellanlage-tool/abstellanlage-tool.component';

@NgModule({
  declarations: [
    AbstellanlageTabelleComponent,
    AbstellanlageEditorComponent,
    AbstellanlageLayerComponent,
    AbstellanlageToolComponent,
  ],
  imports: [CommonModule, SharedModule, ViewerSharedModule],
  exports: [AbstellanlageTabelleComponent, AbstellanlageLayerComponent, AbstellanlageToolComponent],
  providers: [
    {
      provide: InfrastrukturToken,
      useValue: ABSTELLANLAGEN,
      multi: true,
    },
  ],
})
export class AbstellanlageModule {}
