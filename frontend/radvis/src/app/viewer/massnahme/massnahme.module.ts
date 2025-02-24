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
import { SharedModule } from 'src/app/shared/shared.module';
import { DokumentModule } from 'src/app/viewer/dokument/dokument.module';
import { KommentareModule } from 'src/app/viewer/kommentare/kommentare.module';
import { ErweiterterMassnahmenFilterDialogComponent } from 'src/app/viewer/massnahme/components/erweiterter-massnahmen-filter-dialog/erweiterter-massnahmen-filter-dialog.component';
import { MassnahmeNetzbezugSnapshotLayerComponent } from 'src/app/viewer/massnahme/components/massnahme-netzbezug-snapshot-layer/massnahme-netzbezug-snapshot-layer.component';
import { MassnahmenUmsetzungsstandComponent } from 'src/app/viewer/massnahme/components/massnahme-umsetzungsstand/massnahmen-umsetzungsstand.component';
import { MassnahmenAttributeEditorComponent } from 'src/app/viewer/massnahme/components/massnahmen-attribute-editor/massnahmen-attribute-editor.component';
import { MassnahmenCreatorComponent } from 'src/app/viewer/massnahme/components/massnahmen-creator/massnahmen-creator.component';
import { MassnahmenLayerComponent } from 'src/app/viewer/massnahme/components/massnahmen-layer/massnahmen-layer.component';
import { MassnahmenTabelleComponent } from 'src/app/viewer/massnahme/components/massnahmen-tabelle/massnahmen-tabelle.component';
import { MassnahmenToolComponent } from 'src/app/viewer/massnahme/components/massnahmen-tool/massnahmen-tool.component';
import { MassnahmenkategorienDropdownControlComponent } from 'src/app/viewer/massnahme/components/massnahmenkategorien-dropdown-control/massnahmenkategorien-dropdown-control.component';
import { UmsetzungsstandAbfrageVorschauDialogComponent } from 'src/app/viewer/massnahme/components/umsetzungsstand-abfrage-vorschau-dialog/umsetzungsstand-abfrage-vorschau-dialog.component';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { MassnahmeUmsetzungsstandButtonMenuComponent } from './components/massnahme-umsetzungsstand-button-menu/massnahme-umsetzungsstand-button-menu.component';
import { MassnahmeUmsetzungsstandDialogComponent } from './components/massnahme-umsetzungsstand-dialog/massnahme-umsetzungsstand-dialog.component';

@NgModule({
  declarations: [
    MassnahmenkategorienDropdownControlComponent,
    MassnahmenUmsetzungsstandComponent,
    MassnahmenCreatorComponent,
    MassnahmenLayerComponent,
    MassnahmenTabelleComponent,
    MassnahmenAttributeEditorComponent,
    MassnahmenToolComponent,
    MassnahmeUmsetzungsstandButtonMenuComponent,
    MassnahmeUmsetzungsstandDialogComponent,
    ErweiterterMassnahmenFilterDialogComponent,
    UmsetzungsstandAbfrageVorschauDialogComponent,
    MassnahmeNetzbezugSnapshotLayerComponent,
  ],
  imports: [CommonModule, SharedModule, ViewerSharedModule, RouterModule, KommentareModule, DokumentModule],
  exports: [MassnahmenTabelleComponent, MassnahmenLayerComponent],
  providers: [{ provide: InfrastrukturToken, useValue: MASSNAHMEN, multi: true }],
})
export class MassnahmeModule {}
