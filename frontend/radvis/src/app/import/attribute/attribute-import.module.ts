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
import { ImportAttributeAbbildungBearbeitenComponent } from 'src/app/import/attribute/components/import-attribute-abbildung-bearbeiten/import-attribute-abbildung-bearbeiten.component';
import { ImportAttributeAbschliessenComponent } from 'src/app/import/attribute/components/import-attribute-abschliessen/import-attribute-abschliessen.component';
import { ImportAttributeAutomatischeAbbildungComponent } from 'src/app/import/attribute/components/import-attribute-automatische-abbildung/import-attribute-automatische-abbildung.component';
import { ImportAttributeDateiHochladenComponent } from 'src/app/import/attribute/components/import-attribute-datei-hochladen/import-attribute-datei-hochladen.component';
import { ImportAttributeKonflikteLayerComponent } from 'src/app/import/attribute/components/import-attribute-konflikte-layer/import-attribute-konflikte-layer.component';
import { ImportAttributeLayerComponent } from 'src/app/import/attribute/components/import-attribute-layer/import-attribute-layer.component';
import { ImportAttributeParameterEingebenComponent } from 'src/app/import/attribute/components/import-attribute-parameter-eingeben/import-attribute-parameter-eingeben.component';
import { ImportAttributeToolComponent } from 'src/app/import/attribute/components/import-attribute-tool/import-attribute-tool.component';
import { TransformAttributeDialogComponent } from 'src/app/import/attribute/components/transform-attribute-dialog/transform-attribute-dialog.component';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { SharedModule } from 'src/app/shared/shared.module';
import { ImportSharedModule } from 'src/app/import/import-shared/import-shared.module';

@NgModule({
  declarations: [
    ImportAttributeToolComponent,
    TransformAttributeDialogComponent,
    ImportAttributeDateiHochladenComponent,
    ImportAttributeParameterEingebenComponent,
    ImportAttributeAutomatischeAbbildungComponent,
    ImportAttributeAbbildungBearbeitenComponent,
    ImportAttributeAbschliessenComponent,
    ImportAttributeLayerComponent,
    ImportAttributeKonflikteLayerComponent,
  ],
  imports: [SharedModule, ImportSharedModule],
  providers: [AttributeImportService],
})
export class AttributeImportModule {}
