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
import { AdministrationRoutingModule } from 'src/app/administration/administration-routing.module';
import { SharedModule } from 'src/app/shared/shared.module';
import { BenutzerEditorComponent } from './components/benutzer-editor/benutzer-editor.component';
import { BenutzerListComponent } from './components/benutzer-list/benutzer-list.component';
import { OrganisationEditorComponent } from './components/organisation-editor/organisation-editor.component';
import { OrganisationListComponent } from './components/organisation-list/organisation-list.component';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

@NgModule({
  imports: [
    AdministrationRoutingModule,
    CommonModule,
    ReactiveFormsModule,
    SharedModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  declarations: [
    BenutzerListComponent,
    BenutzerEditorComponent,
    OrganisationListComponent,
    OrganisationEditorComponent,
  ],
})
export class AdministrationModule {}
