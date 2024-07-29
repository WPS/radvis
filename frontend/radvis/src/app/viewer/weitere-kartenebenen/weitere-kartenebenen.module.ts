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

import { CommonModule, registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from 'src/app/shared/shared.module';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { DateiLayerVerwaltungComponent } from 'src/app/viewer/weitere-kartenebenen/components/datei-layer-verwaltung/datei-layer-verwaltung.component';
import { DeckkraftSliderControlComponent } from 'src/app/viewer/weitere-kartenebenen/components/deckkraft-slider-control/deckkraft-slider-control.component';
import { PredefinedLayerMenuComponent } from 'src/app/viewer/weitere-kartenebenen/components/predefined-layer-menu/predefined-layer-menu.component';
import { WeitereKartenebenenDetailViewComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-kartenebenen-detail-view/weitere-kartenebenen-detail-view.component';
import { WeitereKartenebenenDialogComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-kartenebenen-dialog/weitere-kartenebenen-dialog.component';
import { WeitereKartenebenenDisplayLayerComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-kartenebenen-display-layer/weitere-kartenebenen-display-layer.component';
import { WeitereKartenebenenInfrastrukturMenuComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-kartenebenen-infrastruktur-menu/weitere-kartenebenen-infrastruktur-menu.component';
import { WeitereKartenebenenVerwaltungComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-kartenebenen-verwaltung/weitere-kartenebenen-verwaltung.component';
import { WeitereWfsKartenebenenComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-wfs-kartenebenen/weitere-wfs-kartenebenen.component';
import { WeitereWmsKartenebenenComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-wms-kartenebenen/weitere-wms-kartenebenen.component';
import { ZoomstufeSliderControlComponent } from 'src/app/viewer/weitere-kartenebenen/components/zoomstufe-slider-control/zoomstufe-slider-control.component';

registerLocaleData(localeDe, 'de');

@NgModule({
  declarations: [
    WeitereKartenebenenInfrastrukturMenuComponent,
    WeitereKartenebenenVerwaltungComponent,
    WeitereKartenebenenDisplayLayerComponent,
    WeitereWfsKartenebenenComponent,
    WeitereWmsKartenebenenComponent,
    DeckkraftSliderControlComponent,
    WeitereKartenebenenDetailViewComponent,
    ZoomstufeSliderControlComponent,
    WeitereKartenebenenDialogComponent,
    DateiLayerVerwaltungComponent,
    PredefinedLayerMenuComponent,
  ],
  imports: [CommonModule, ReactiveFormsModule, SharedModule, ViewerSharedModule],
  exports: [WeitereKartenebenenDisplayLayerComponent, WeitereKartenebenenInfrastrukturMenuComponent],
})
export class WeitereKartenebenenModule {}
