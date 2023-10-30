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
import { ImportAttributeLayerComponent } from 'src/app/editor/manueller-import/components/import-attribute-layer/import-attribute-layer.component';
import { ImportAutomatischeAbbildungComponent } from 'src/app/editor/manueller-import/components/import-automatische-abbildung/import-automatische-abbildung.component';
import { ImportDateiHochladenComponent } from 'src/app/editor/manueller-import/components/import-datei-hochladen/import-datei-hochladen.component';
import { ImportNetzklasseAbbildungBearbeitenLayerComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-abbildung-bearbeiten-layer/import-netzklasse-abbildung-bearbeiten-layer.component';
import { ImportNetzklasseAbbildungBearbeitenComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-abbildung-bearbeiten/import-netzklasse-abbildung-bearbeiten.component';
import { ImportNetzklasseAbschliessenLayerComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-abschliessen-layer/import-netzklasse-abschliessen-layer.component';
import { ImportNetzklasseAbschliessenComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-abschliessen/import-netzklasse-abschliessen.component';
import { ImportNetzklasseParameterEingebenComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-parameter-eingeben/import-netzklasse-parameter-eingeben.component';
import { ImportNetzklasseSackgassenLayerComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-sackgassen-layer/import-netzklasse-sackgassen-layer.component';
import { NetzklassePipe } from 'src/app/editor/manueller-import/components/netzklasse.pipe';
import { TransformDialogComponent } from 'src/app/editor/manueller-import/components/transform-dialog/transform-dialog.component';
import { SharedModule } from 'src/app/shared/shared.module';
import { ImportAttributeAbbildungBearbeitenComponent } from './components/import-attribute-abbildung-bearbeiten/import-attribute-abbildung-bearbeiten.component';
import { ImportAttributeAbschliessenComponent } from './components/import-attribute-abschliessen/import-attribute-abschliessen.component';
import { ImportAttributeKonflikteLayerComponent } from './components/import-attribute-konflikte-layer/import-attribute-konflikte-layer.component';
import { ImportAttributeParameterEingebenComponent } from './components/import-attribute-parameter-eingeben/import-attribute-parameter-eingeben.component';
import { ImportNetzklasseSackgassenComponent } from './components/import-netzklasse-sackgassen/import-netzklasse-sackgassen.component';
import { ImportToolComponent } from './components/import-tool/import-tool.component';

@NgModule({
  declarations: [
    ImportToolComponent,
    ImportDateiHochladenComponent,
    ImportAutomatischeAbbildungComponent,
    ImportNetzklasseAbschliessenComponent,
    ImportNetzklasseAbschliessenLayerComponent,
    ImportNetzklasseAbbildungBearbeitenComponent,
    ImportNetzklasseAbbildungBearbeitenLayerComponent,
    ImportNetzklasseSackgassenLayerComponent,
    ImportNetzklasseSackgassenComponent,
    ImportNetzklasseParameterEingebenComponent,
    ImportAttributeParameterEingebenComponent,
    ImportAttributeAbbildungBearbeitenComponent,
    ImportAttributeAbschliessenComponent,
    ImportAttributeLayerComponent,
    TransformDialogComponent,
    ImportAttributeKonflikteLayerComponent,
    NetzklassePipe,
  ],
  imports: [CommonModule, RouterModule, SharedModule, EditorSharedModule],
})
export class ManuellerImportModule {}
