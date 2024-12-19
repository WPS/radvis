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
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from 'src/app/shared/shared.module';
import { BenachrichtigungButtonComponent } from 'src/app/viewer/viewer-shared/components/benachrichtigung-button/benachrichtigung-button.component';
import { DetailFeatureTableComponent } from 'src/app/viewer/viewer-shared/components/detail-feauture-table/detail-feature-table.component';
import { ExportButtonComponent } from 'src/app/viewer/viewer-shared/components/export-button/export-button.component';
import { InfrastrukturTabelleLayoutComponent } from 'src/app/viewer/viewer-shared/components/infrastruktur-tabelle-layout/infrastruktur-tabelle-layout.component';
import { InfrastrukturTabelleSpalteComponent } from 'src/app/viewer/viewer-shared/components/infrastruktur-tabelle-spalte/infrastruktur-tabelle-spalte.component';
import { OriginalGeometrieAnzeigenButtonComponent } from 'src/app/viewer/viewer-shared/components/original-geometrie-anzeigen-button/original-geometrie-anzeigen-button.component';
import { PositionSelektionControlComponent } from 'src/app/viewer/viewer-shared/components/position-selektion-control/position-selektion-control.component';
import { RightDetailsShortcutDirective } from 'src/app/viewer/viewer-shared/components/right-details-shortcut.directive';
import { CsvImportDialogComponent } from './components/csv-import-dialog/csv-import-dialog.component';
import { FilterHeaderComponent } from './components/filter-header/filter-header.component';
import { FilterMenuComponent } from './components/filter-menu/filter-menu.component';

const exports = [
  InfrastrukturTabelleLayoutComponent,
  FilterMenuComponent,
  FilterHeaderComponent,
  BenachrichtigungButtonComponent,
  OriginalGeometrieAnzeigenButtonComponent,
  ExportButtonComponent,
  PositionSelektionControlComponent,
  DetailFeatureTableComponent,
  RightDetailsShortcutDirective,
  InfrastrukturTabelleSpalteComponent,
];

@NgModule({
  declarations: [...exports, CsvImportDialogComponent],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SharedModule],
  exports: [exports],
})
export class ViewerSharedModule {}
