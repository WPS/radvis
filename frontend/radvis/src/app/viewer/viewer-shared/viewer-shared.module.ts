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
import { ExpandLinkComponent } from 'src/app/viewer/viewer-shared/components/expandable-content/expand-link/expand-link.component';
import { ExpandableContentComponent } from 'src/app/viewer/viewer-shared/components/expandable-content/expandable-content.component';
import { ExportButtonComponent } from 'src/app/viewer/viewer-shared/components/export-button/export-button.component';
import { InfrastrukturTabelleLayoutComponent } from 'src/app/viewer/viewer-shared/components/infrastruktur-tabelle-layout/infrastruktur-tabelle-layout.component';
import { NetzbezugHighlightLayerComponent } from 'src/app/viewer/viewer-shared/components/netzbezug-highlight-layer/netzbezug-highlight-layer.component';
import { BearbeitungsModusToggleButtonComponent } from 'src/app/viewer/viewer-shared/components/netzbezug-selektion-control/bearbeitungs-modus-toggle-button/bearbeitungs-modus-toggle-button.component';
import { NetzbezugSelektionControlComponent } from 'src/app/viewer/viewer-shared/components/netzbezug-selektion-control/netzbezug-selektion-control.component';
import { NetzbezugSelektionLayerComponent } from 'src/app/viewer/viewer-shared/components/netzbezug-selektion-control/netzbezug-selektion-layer/netzbezug-selektion-layer.component';
import { OriginalGeometrieAnzeigenButtonComponent } from 'src/app/viewer/viewer-shared/components/original-geometrie-anzeigen-button/original-geometrie-anzeigen-button.component';
import { OriginalGeometrieLayerComponent } from 'src/app/viewer/viewer-shared/components/original-geometrie-layer/original-geometrie-layer.component';
import { PositionSelektionControlComponent } from 'src/app/viewer/viewer-shared/components/position-selektion-control/position-selektion-control.component';
import { FilterHeaderComponent } from './components/filter-header/filter-header.component';
import { FilterMenuComponent } from './components/filter-menu/filter-menu.component';
import { CsvImportDialogComponent } from './components/csv-import-dialog/csv-import-dialog.component';
import { DetailFeatureTableComponent } from 'src/app/viewer/viewer-shared/components/detail-feauture-table/detail-feature-table.component';
import { RightDetailsShortcutDirective } from 'src/app/viewer/viewer-shared/components/right-details-shortcut.directive';

const exports = [
  InfrastrukturTabelleLayoutComponent,
  FilterMenuComponent,
  FilterHeaderComponent,
  BenachrichtigungButtonComponent,
  OriginalGeometrieAnzeigenButtonComponent,
  OriginalGeometrieLayerComponent,
  ExportButtonComponent,
  NetzbezugSelektionControlComponent,
  NetzbezugHighlightLayerComponent,
  PositionSelektionControlComponent,
  ExpandableContentComponent,
  DetailFeatureTableComponent,
  RightDetailsShortcutDirective,
];

@NgModule({
  declarations: [
    ...exports,
    ExpandLinkComponent,
    NetzbezugSelektionLayerComponent,
    BearbeitungsModusToggleButtonComponent,
    CsvImportDialogComponent,
  ],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SharedModule],
  exports,
})
export class ViewerSharedModule {}
