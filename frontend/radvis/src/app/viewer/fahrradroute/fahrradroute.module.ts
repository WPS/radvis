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
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SharedModule } from 'src/app/shared/shared.module';
import { FahrradrouteAttributeEditorComponent } from 'src/app/viewer/fahrradroute/components/fahrradroute-attribute-editor/fahrradroute-attribute-editor.component';
import { FahrradrouteLayerComponent } from 'src/app/viewer/fahrradroute/components/fahrradroute-layer/fahrradroute-layer.component';
import { FahrradrouteNetzbezugHighlightLayerComponent } from 'src/app/viewer/fahrradroute/components/fahrradroute-netzbezug-highlight-layer/fahrradroute-netzbezug-highlight-layer.component';
import { FahrradrouteProfilComponent } from 'src/app/viewer/fahrradroute/components/fahrradroute-profil/fahrradroute-profil.component';
import { FahrradroutenCreatorComponent } from 'src/app/viewer/fahrradroute/components/fahrradrouten-creator/fahrradrouten-creator.component';
import { HoehenprofilPositionLayerComponent } from 'src/app/viewer/fahrradroute/components/hoehenprofil-position-layer/hoehenprofil-position-layer.component';
import { HoehenprofilComponent } from 'src/app/viewer/fahrradroute/components/hoehenprofil/hoehenprofil.component';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { AbweichendeSegmenteLayerComponent } from './components/abweichende-segmente-layer/abweichende-segmente-layer.component';
import { FahrradrouteNetzbezugControlComponent } from './components/fahrradroute-netzbezug-control/fahrradroute-netzbezug-control.component';
import { FahrradrouteTabelleComponent } from './components/fahrradroute-tabelle/fahrradroute-tabelle.component';
import { WurmfortsatzLayerComponent } from './components/wurmfortsatz-layer/wurmfortsatz-layer.component';
import { RoutingProfileVerwaltenDialogComponent } from 'src/app/viewer/fahrradroute/components/routing-profile-verwalten-dialog/routing-profile-verwalten-dialog.component';

@NgModule({
  declarations: [
    FahrradrouteLayerComponent,
    FahrradrouteTabelleComponent,
    FahrradrouteAttributeEditorComponent,
    AbweichendeSegmenteLayerComponent,
    WurmfortsatzLayerComponent,
    FahrradrouteNetzbezugHighlightLayerComponent,
    FahrradroutenCreatorComponent,
    FahrradrouteNetzbezugControlComponent,
    HoehenprofilComponent,
    FahrradrouteProfilComponent,
    HoehenprofilPositionLayerComponent,
    RoutingProfileVerwaltenDialogComponent,
  ],
  imports: [CommonModule, ViewerSharedModule, ReactiveFormsModule, RouterModule, SharedModule],
  exports: [FahrradrouteTabelleComponent, FahrradrouteLayerComponent],
  providers: [{ provide: InfrastrukturToken, useValue: FAHRRADROUTE, multi: true }],
})
export class FahrradrouteModule {}
