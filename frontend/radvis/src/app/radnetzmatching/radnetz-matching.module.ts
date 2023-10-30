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

import { DatePipe } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FehlerprotokollModule } from 'src/app/fehlerprotokoll/fehlerprotokoll.module';
import { KarteModule } from 'src/app/karte/karte.module';
import { FeatureLayerComponent } from 'src/app/radnetzmatching/components/feature-layer/feature-layer.component';
import { LayerAuswahlComponent } from 'src/app/radnetzmatching/components/layer-auswahl/layer-auswahl.component';
import { MatchingFeatureDetailsComponent } from 'src/app/radnetzmatching/components/matching-feature-details/matching-feature-details.component';
import { MatchingFeatureTableComponent } from 'src/app/radnetzmatching/components/matching-feature-details/matching-feature-table/matching-feature-table.component';
import { RadnetzKantenLayerComponent } from 'src/app/radnetzmatching/components/radnetz-kanten-layer/radnetz-kanten-layer.component';
import { RadnetzMatchingRoutingModule } from 'src/app/radnetzmatching/radnetz-matching-routing.module';
import { SharedModule } from 'src/app/shared/shared.module';
import { DlmKantenSelektionComponent } from './components/dlm-kanten-selektion/dlm-kanten-selektion.component';
import { KoordinatenSucheComponent } from './components/koordinaten-suche/koordinaten-suche.component';
import { LandkreisErledigtMarkierenComponent } from './components/landkreis-erledigt-markieren/landkreis-erledigt-markieren.component';
import { LandkreiseLayerComponent } from './components/landkreise-layer/landkreise-layer.component';
import { RadnetzMatchingComponent } from './components/radnetz-matching/radnetz-matching.component';
import { ZugeordneteDlmKantenLayerComponent } from './components/zugeordnete-dlm-kanten-layer/zugeordnete-dlm-kanten-layer.component';

@NgModule({
  providers: [DatePipe],
  declarations: [
    RadnetzMatchingComponent,
    MatchingFeatureTableComponent,
    MatchingFeatureDetailsComponent,
    RadnetzKantenLayerComponent,
    DlmKantenSelektionComponent,
    FeatureLayerComponent,
    ZugeordneteDlmKantenLayerComponent,
    LandkreiseLayerComponent,
    LandkreisErledigtMarkierenComponent,
    KoordinatenSucheComponent,
    LayerAuswahlComponent,
  ],
  imports: [
    RadnetzMatchingRoutingModule,
    BrowserModule,
    HttpClientModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    SharedModule,
    KarteModule,
    FehlerprotokollModule,
  ],
})
export class RadnetzMatchingModule {}
