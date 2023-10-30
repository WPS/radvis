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
import { RouterModule } from '@angular/router';
import { AnpassungswunschAnlegenService } from 'src/app/shared/services/anpassungswunsch-anlegen.service';
import { SharedModule } from 'src/app/shared/shared.module';
import { AnpassungenEditorComponent } from 'src/app/viewer/anpassungswunsch/components/anpassungen-editor/anpassungen-editor.component';
import { AnpassungswuenscheLayerComponent } from 'src/app/viewer/anpassungswunsch/components/anpassungswuensche-layer/anpassungswuensche-layer.component';
import { AnpassungswunschToolComponent } from 'src/app/viewer/anpassungswunsch/components/anpassungswunsch-tool/anpassungswunsch-tool.component';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { AnpassungswunschAnlegenServiceImpl } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch-anlegen-impl.service';
import { KommentareModule } from 'src/app/viewer/kommentare/kommentare.module';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { AnpassungswunschTabelleComponent } from './components/anpassungswunsch-tabelle/anpassungswunsch-tabelle.component';

@NgModule({
  declarations: [
    AnpassungswuenscheLayerComponent,
    AnpassungenEditorComponent,
    AnpassungswunschToolComponent,
    AnpassungswunschTabelleComponent,
  ],
  imports: [CommonModule, SharedModule, ViewerSharedModule, RouterModule, KommentareModule],
  exports: [AnpassungswuenscheLayerComponent, AnpassungswunschTabelleComponent],
  providers: [
    { provide: InfrastrukturToken, useValue: ANPASSUNGSWUNSCH, multi: true },
    {
      provide: AnpassungswunschAnlegenService,
      useClass: AnpassungswunschAnlegenServiceImpl,
    },
  ],
})
export class AnpassungswunschModule {}
