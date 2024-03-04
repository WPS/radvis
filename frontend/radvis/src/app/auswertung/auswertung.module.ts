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
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { AuswertungRoutingModule } from 'src/app/auswertung/auswertung-routing.module';
import { SharedModule } from 'src/app/shared/shared.module';
import { AuswertungComponent } from './components/auswertung/auswertung.component';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { MatButtonToggleModule } from '@angular/material/button-toggle';

@NgModule({
  imports: [
    AuswertungRoutingModule,
    MatButtonModule,
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    MatCheckboxModule,
    MatCardModule,
    ViewerSharedModule,
    MatButtonToggleModule,
  ],
  declarations: [AuswertungComponent],
})
export class AuswertungModule {}
