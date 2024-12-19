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

import { Injectable } from '@angular/core';
import { Route } from '@angular/router';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { KantenAttributeEditorComponent } from 'src/app/editor/kanten/components/kanten-attribute-editor/kanten-attribute-editor.component';
import { KantenCreatorComponent } from 'src/app/editor/kanten/components/kanten-creator/kanten-creator.component';
import { KantenFahrtrichtungEditorComponent } from 'src/app/editor/kanten/components/kanten-fahrtrichtung-editor/kanten-fahrtrichtung-editor.component';
import { KantenFuehrungsformEditorComponent } from 'src/app/editor/kanten/components/kanten-fuehrungsform-editor/kanten-fuehrungsform-editor.component';
import { KantenGeschwindigkeitEditorComponent } from 'src/app/editor/kanten/components/kanten-geschwindigkeit-editor/kanten-geschwindigkeit-editor.component';
import { KantenVerlaufEditorComponent } from 'src/app/editor/kanten/components/kanten-verlauf-editor/kanten-verlauf-editor.component';
import { KantenZustaendigkeitEditorComponent } from 'src/app/editor/kanten/components/kanten-zustaendigkeit-editor/kanten-zustaendigkeit-editor.component';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { kantenSelektionResetGuard } from 'src/app/editor/kanten/services/kanten-selektion-reset.guard';
import { netzklassenFilterResetGuard } from 'src/app/editor/kanten/services/netzklassen-filter-reset.guard';

@Injectable({
  providedIn: 'root',
})
export class KantenToolRoutingService {
  constructor() {}

  public static getChildRoutes(): Route[] {
    return [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: AttributGruppe.ALLGEMEIN,
      },
      {
        path: AttributGruppe.ALLGEMEIN,
        component: KantenAttributeEditorComponent,
        canDeactivate: [kantenSelektionResetGuard],
      },
      {
        path: AttributGruppe.VERLAUF,
        component: KantenVerlaufEditorComponent,
        canDeactivate: [kantenSelektionResetGuard],
      },
      {
        path: AttributGruppe.GESCHWINDIGKEIT,
        component: KantenGeschwindigkeitEditorComponent,
        canDeactivate: [kantenSelektionResetGuard],
      },
      {
        path: AttributGruppe.FUEHRUNGSFORM,
        component: KantenFuehrungsformEditorComponent,
        canDeactivate: [kantenSelektionResetGuard],
      },
      {
        path: AttributGruppe.ZUSTAENDIGKEIT,
        component: KantenZustaendigkeitEditorComponent,
        canDeactivate: [kantenSelektionResetGuard],
      },
      {
        path: AttributGruppe.FAHRTRICHTUNG,
        component: KantenFahrtrichtungEditorComponent,
        canDeactivate: [kantenSelektionResetGuard],
      },
      {
        path: EditorRoutingService.EDITOR_CREATE_SUBROUTE,
        component: KantenCreatorComponent,
        canDeactivate: [netzklassenFilterResetGuard],
      },
    ];
  }
}
