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
import { CommonModule } from '@angular/common';
import { FAHRRADZAEHLSTELLE } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle.infrastruktur';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { FahrradzaehlstelleTabelleComponent } from 'src/app/viewer/fahrradzaehlstelle/components/fahrradzaehlstelle-tabelle/fahrradzaehlstelle-tabelle.component';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { FahrradzaehlstelleLayerComponent } from 'src/app/viewer/fahrradzaehlstelle/components/fahrradzaehlstelle-layer/fahrradzaehlstelle-layer.component';
import { FahrradzaehlstelleDetailViewComponent } from 'src/app/viewer/fahrradzaehlstelle/components/fahrradzaehlstelle-detail-view/fahrradzaehlstelle-detail-view.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterModule } from '@angular/router';
import { FahrradzaehlstelleToolComponent } from 'src/app/viewer/fahrradzaehlstelle/components/fahrradzaehlstelle-tool/fahrradzaehlstelle-tool.component';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FahrradzaehlstelleStatistikComponent } from 'src/app/viewer/fahrradzaehlstelle/components/fahrradzaehlstelle-statistik/fahrradzaehlstelle-statistik.component';
import { SharedModule } from 'src/app/shared/shared.module';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { JahrDropdownControlComponent } from './components/jahr-dropdown-control/jahr-dropdown-control.component';
import { DateRangePickerControlComponent } from './components/date-range-picker-control/date-range-picker-control.component';
import { JahreszeitraumControlComponent } from './components/jahreszeitraum-control/jahreszeitraum-control.component';

@NgModule({
  declarations: [
    FahrradzaehlstelleTabelleComponent,
    FahrradzaehlstelleLayerComponent,
    FahrradzaehlstelleDetailViewComponent,
    FahrradzaehlstelleToolComponent,
    FahrradzaehlstelleStatistikComponent,
    JahrDropdownControlComponent,
    DateRangePickerControlComponent,
    JahreszeitraumControlComponent,
  ],
  imports: [
    CommonModule,
    ViewerSharedModule,
    MatTableModule,
    MatSortModule,
    MatIconModule,
    MatButtonModule,
    MatToolbarModule,
    MatIconModule,
    RouterModule,
    MatTabsModule,
    MatTooltipModule,
    SharedModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  providers: [{ provide: InfrastrukturToken, useValue: FAHRRADZAEHLSTELLE, multi: true }],
  exports: [FahrradzaehlstelleTabelleComponent, FahrradzaehlstelleLayerComponent],
})
export class FahrradzaehlstelleModule {}
