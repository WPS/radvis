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
import { SharedModule } from 'src/app/shared/shared.module';
import { InfrastrukturToken } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { WegweisendeBeschilderungDetailViewComponent } from 'src/app/viewer/wegweisende-beschilderung/components/wegweisende-beschilderung-detail-view/wegweisende-beschilderung-detail-view.component';
import { WegweisendeBeschilderungLayerComponent } from 'src/app/viewer/wegweisende-beschilderung/components/wegweisende-beschilderung-layer/wegweisende-beschilderung-layer.component';
import { WegweisendeBeschilderungTabelleComponent } from 'src/app/viewer/wegweisende-beschilderung/components/wegweisende-beschilderung-tabelle/wegweisende-beschilderung-tabelle.component';
import { WEGWEISENDE_BESCHILDERUNG } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung.infrastruktur';

@NgModule({
  declarations: [
    WegweisendeBeschilderungTabelleComponent,
    WegweisendeBeschilderungLayerComponent,
    WegweisendeBeschilderungDetailViewComponent,
  ],
  imports: [CommonModule, SharedModule, ViewerSharedModule],
  providers: [{ provide: InfrastrukturToken, useValue: WEGWEISENDE_BESCHILDERUNG, multi: true }],
  exports: [WegweisendeBeschilderungTabelleComponent, WegweisendeBeschilderungLayerComponent],
})
export class WegweisendeBeschilderungModule {}
