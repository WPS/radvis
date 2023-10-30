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
import { KanteGrundgeometrieLayerComponent } from 'src/app/editor/editor-shared/components/kante-grundgeometrie-layer/kante-grundgeometrie-layer.component';
import { KantenSelektionComponent } from 'src/app/editor/editor-shared/components/kanten-selektion/kanten-selektion.component';
import { KnotenSelektionComponent } from 'src/app/editor/editor-shared/components/knoten-selektion/knoten-selektion.component';
import { FehlerprotokollModule } from 'src/app/fehlerprotokoll/fehlerprotokoll.module';
import { KarteModule } from 'src/app/karte/karte.module';
import { SharedModule } from 'src/app/shared/shared.module';

const exported = [KanteGrundgeometrieLayerComponent, KnotenSelektionComponent, KantenSelektionComponent];

@NgModule({
  declarations: exported,
  imports: [CommonModule, SharedModule, KarteModule, FehlerprotokollModule],
  exports: [...exported, KarteModule, FehlerprotokollModule],
})
export class EditorSharedModule {}
