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
import { EditorSharedModule } from 'src/app/editor/editor-shared/editor-shared.module';
import { AutocorrectingNumberInputControlComponent } from 'src/app/editor/kanten/components/autocorrecting-number-input-control/autocorrecting-number-input-control.component';
import { KanteGeometryControlComponent } from 'src/app/editor/kanten/components/kante-geometry-control/kante-geometry-control.component';
import { KantenAttributeEditorMitSubauswahlComponent } from 'src/app/editor/kanten/components/kanten-attribute-editor-mit-subauswahl/kanten-attribute-editor-mit-subauswahl.component';
import { KantenAttributeEditorComponent } from 'src/app/editor/kanten/components/kanten-attribute-editor/kanten-attribute-editor.component';
import { KantenCreatorKnotenSelektionComponent } from 'src/app/editor/kanten/components/kanten-creator-knoten-selektion/kanten-creator-knoten-selektion.component';
import { KantenCreatorComponent } from 'src/app/editor/kanten/components/kanten-creator/kanten-creator.component';
import { KantenFahrtrichtungEditorComponent } from 'src/app/editor/kanten/components/kanten-fahrtrichtung-editor/kanten-fahrtrichtung-editor.component';
import { KantenFuehrungsformEditorComponent } from 'src/app/editor/kanten/components/kanten-fuehrungsform-editor/kanten-fuehrungsform-editor.component';
import { KantenGeschwindigkeitEditorComponent } from 'src/app/editor/kanten/components/kanten-geschwindigkeit-editor/kanten-geschwindigkeit-editor.component';
import { KantenSelektionTabelleComponent } from 'src/app/editor/kanten/components/kanten-selektion-tabelle/kanten-selektion-tabelle.component';
import { KantenToolComponent } from 'src/app/editor/kanten/components/kanten-tool/kanten-tool.component';
import { KantenVerlaufEditorComponent } from 'src/app/editor/kanten/components/kanten-verlauf-editor/kanten-verlauf-editor.component';
import { KantenZustaendigkeitEditorComponent } from 'src/app/editor/kanten/components/kanten-zustaendigkeit-editor/kanten-zustaendigkeit-editor.component';
import { LinearReferenzierterAbschnittControlComponent } from 'src/app/editor/kanten/components/lineare-referenz-control/linear-referenzierter-abschnitt-control.component';
import { ModifyGeometryLayerComponent } from 'src/app/editor/kanten/components/modify-geometry-layer/modify-geometry-layer.component';
import { UndeterminedCheckboxControlComponent } from 'src/app/editor/kanten/components/undetermined-checkbox-control/undetermined-checkbox-control.component';
import { SharedModule } from 'src/app/shared/shared.module';

@NgModule({
  declarations: [
    AutocorrectingNumberInputControlComponent,
    KanteGeometryControlComponent,
    KantenAttributeEditorComponent,
    KantenCreatorComponent,
    KantenCreatorKnotenSelektionComponent,
    KantenFahrtrichtungEditorComponent,
    KantenFuehrungsformEditorComponent,
    KantenGeschwindigkeitEditorComponent,
    KantenSelektionTabelleComponent,
    KantenVerlaufEditorComponent,
    KantenZustaendigkeitEditorComponent,
    LinearReferenzierterAbschnittControlComponent,
    ModifyGeometryLayerComponent,
    UndeterminedCheckboxControlComponent,
    KantenAttributeEditorMitSubauswahlComponent,
    KantenToolComponent,
  ],
  imports: [CommonModule, SharedModule, EditorSharedModule, RouterModule],
  exports: [KantenToolComponent],
})
export class KantenModule {}
