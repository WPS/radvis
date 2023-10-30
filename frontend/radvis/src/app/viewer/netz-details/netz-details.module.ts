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
import { SharedModule } from 'src/app/shared/shared.module';
import { FeatureTableHeaderComponent } from 'src/app/viewer/netz-details/components/feature-table-header/feature-table-header.component';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { KantenDetailViewComponent } from './components/kanten-detail-view/kanten-detail-view.component';
import { KantenHighlightLayerComponent } from './components/kanten-highlight-layer/kanten-highlight-layer.component';
import { KnotenDetailViewComponent } from './components/knoten-detail-view/knoten-detail-view.component';
import { KnotenHighlightLayerComponent } from './components/knoten-highlight-layer/knoten-highlight-layer.component';

@NgModule({
  declarations: [
    KnotenDetailViewComponent,
    KantenDetailViewComponent,
    FeatureTableHeaderComponent,
    KnotenHighlightLayerComponent,
    KantenHighlightLayerComponent,
  ],
  imports: [SharedModule, ViewerSharedModule],
})
export class NetzDetailsModule {}
