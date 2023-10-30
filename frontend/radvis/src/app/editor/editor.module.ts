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

import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { EditorMenuComponent } from 'src/app/editor/components/editor/editor-menu/editor-menu.component';
import { EditorComponent } from 'src/app/editor/components/editor/editor.component';
import { EditorRoutingModule } from 'src/app/editor/editor-routing.module';
import { EditorSharedModule } from 'src/app/editor/editor-shared/editor-shared.module';
import { KantenModule } from 'src/app/editor/kanten/kanten.module';
import { KnotenModule } from 'src/app/editor/knoten/knoten.module';
import { ManuellerImportModule } from 'src/app/editor/manueller-import/manueller-import.module';
import { SharedModule } from 'src/app/shared/shared.module';

@NgModule({
  declarations: [EditorComponent, EditorMenuComponent],
  imports: [
    BrowserModule,
    HttpClientModule,
    EditorRoutingModule,
    ReactiveFormsModule,
    SharedModule,
    ManuellerImportModule,
    EditorSharedModule,
    KnotenModule,
    KantenModule,
  ],
})
export class EditorModule {}
