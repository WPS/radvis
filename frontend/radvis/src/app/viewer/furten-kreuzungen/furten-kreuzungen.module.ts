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
import { FURTEN_KREUZUNGEN } from 'src/app/viewer/furten-kreuzungen/models/furten-kreuzungen.infrastruktur';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { FurtenKreuzungenEditorComponent } from './components/furten-kreuzungen-editor/furten-kreuzungen-editor.component';
import { FurtenKreuzungenLayerComponent } from './components/furten-kreuzungen-layer/furten-kreuzungen-layer.component';
import { FurtenKreuzungenTabelleComponent } from './components/furten-kreuzungen-tabelle/furten-kreuzungen-tabelle.component';

@NgModule({
  declarations: [FurtenKreuzungenEditorComponent, FurtenKreuzungenTabelleComponent, FurtenKreuzungenLayerComponent],
  imports: [CommonModule, SharedModule, ViewerSharedModule],
  providers: [{ provide: InfrastrukturToken, useValue: FURTEN_KREUZUNGEN, multi: true }],
  exports: [FurtenKreuzungenTabelleComponent, FurtenKreuzungenLayerComponent],
})
export class FurtenKreuzungenModule {}
