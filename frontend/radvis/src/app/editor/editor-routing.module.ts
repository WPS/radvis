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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { EditorComponent } from 'src/app/editor/components/editor/editor.component';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { KantenToolComponent } from 'src/app/editor/kanten/components/kanten-tool/kanten-tool.component';
import { KantenToolRoutingService } from 'src/app/editor/kanten/services/kanten-tool-routing.service';
import { KnotenToolComponent } from 'src/app/editor/knoten/components/knoten-tool/knoten-tool.component';
import { KnotenToolRoutingService } from 'src/app/editor/knoten/services/knoten-tool-routing.service';
import { benutzerAktivGuard } from 'src/app/shared/services/benutzer-aktiv.guard';
import { benutzerRegistriertGuard } from 'src/app/shared/services/benutzer-registriert.guard';

const routes: Routes = [
  {
    path: EditorRoutingService.EDITOR_ROUTE,
    canActivate: [benutzerRegistriertGuard, benutzerAktivGuard],
    component: EditorComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: EditorRoutingService.EDITOR_KANTEN_ROUTE,
      },
      {
        path: EditorRoutingService.EDITOR_KANTEN_ROUTE,
        component: KantenToolComponent,
        children: KantenToolRoutingService.getChildRoutes(),
      },
      {
        path: EditorRoutingService.EDITOR_KNOTEN_ROUTE,
        component: KnotenToolComponent,
        children: KnotenToolRoutingService.getChildRoutes(),
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class EditorRoutingModule {}
