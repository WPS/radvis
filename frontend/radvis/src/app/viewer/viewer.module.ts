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

import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FehlerprotokollModule } from 'src/app/fehlerprotokoll/fehlerprotokoll.module';
import { KarteModule } from 'src/app/karte/karte.module';
import { SharedModule } from 'src/app/shared/shared.module';
import { AbstellanlageModule } from 'src/app/viewer/abstellanlage/abstellanlage.module';
import { AnpassungswunschModule } from 'src/app/viewer/anpassungswunsch/anpassungswunsch.module';
import { BarriereModule } from 'src/app/viewer/barriere/barriere.module';
import { FeatureDetailsComponent } from 'src/app/viewer/components/feature-details/feature-details.component';
import { InfrastrukturenMenuComponent } from 'src/app/viewer/components/infrastruktur-auswahl/infrastrukturen-menu.component';
import { InfrastrukturLayerComponent } from 'src/app/viewer/components/infrastruktur-layer/infrastruktur-layer.component';
import { InfrastrukturenTabellenComponent } from 'src/app/viewer/components/infrastruktur-tabellen/infrastrukturen-tabellen.component';
import { InfrastrukturenTabellenContainerComponent } from 'src/app/viewer/components/infrastrukturen-tabellen-container/infrastrukturen-tabellen-container.component';
import { LocationSelectionLayerComponent } from 'src/app/viewer/components/location-selection-layer/location-selection-layer.component';
import { MeasureDistanceLayerComponent } from 'src/app/viewer/components/measure-distance-layer/measure-distance-layer.component';
import { NetzklasseLayerComponent } from 'src/app/viewer/components/radvis-netz-layer/netzklasse-layer/netzklasse-layer.component';
import { RadvisKnotenLayerComponent } from 'src/app/viewer/components/radvis-netz-layer/radvis-knoten-layer/radvis-knoten-layer.component';
import { RadvisNetzLayerComponent } from 'src/app/viewer/components/radvis-netz-layer/radvis-netz-layer.component';
import { SelectFeatureMenuComponent } from 'src/app/viewer/components/select-feature-menu/select-feature-menu.component';
import { ViewerComponent } from 'src/app/viewer/components/viewer/viewer.component';
import { FahrradrouteModule } from 'src/app/viewer/fahrradroute/fahrradroute.module';
import { FahrradzaehlstelleModule } from 'src/app/viewer/fahrradzaehlstelle/fahrradzaehlstelle.module';
import { FurtenKreuzungenModule } from 'src/app/viewer/furten-kreuzungen/furten-kreuzungen.module';
import { ImportprotokolleModule } from 'src/app/viewer/importprotokolle/importprotokolle.module';
import { LeihstationModule } from 'src/app/viewer/leihstation/leihstation.module';
import { MassnahmeModule } from 'src/app/viewer/massnahme/massnahme.module';
import { NetzDetailsModule } from 'src/app/viewer/netz-details/netz-details.module';
import { ServicestationModule } from 'src/app/viewer/servicestation/servicestation.module';
import { SignaturModule } from 'src/app/viewer/signatur/signatur.module';
import { ViewerRoutingModule } from 'src/app/viewer/viewer-routing.module';
import { ExportButtonComponent } from 'src/app/viewer/viewer-shared/components/export-button/export-button.component';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { WegweisendeBeschilderungModule } from 'src/app/viewer/wegweisende-beschilderung/wegweisende-beschilderung.module';
import { WeitereKartenebenenModule } from 'src/app/viewer/weitere-kartenebenen/weitere-kartenebenen.module';

@NgModule({
  declarations: [
    ViewerComponent,
    FeatureDetailsComponent,
    InfrastrukturenMenuComponent,
    InfrastrukturLayerComponent,
    InfrastrukturenTabellenComponent,
    SelectFeatureMenuComponent,
    RadvisNetzLayerComponent,
    RadvisKnotenLayerComponent,
    NetzklasseLayerComponent,
    LocationSelectionLayerComponent,
    MeasureDistanceLayerComponent,
    InfrastrukturenTabellenContainerComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    BrowserAnimationsModule,
    ViewerRoutingModule,
    ReactiveFormsModule,
    SharedModule,
    FormsModule,
    NetzDetailsModule,
    SignaturModule,
    ViewerSharedModule,
    KarteModule,
    FehlerprotokollModule,
    // Die Reihenfolge ab hier bestimmt die Reihenfolge im Menu links:
    MassnahmeModule,
    FahrradrouteModule,
    FurtenKreuzungenModule,
    BarriereModule,
    WegweisendeBeschilderungModule,
    AbstellanlageModule,
    ServicestationModule,
    LeihstationModule,
    FahrradzaehlstelleModule,
    // und ab hier gilt das mit der Reihenfolge nicht mehr.
    WeitereKartenebenenModule,
    AnpassungswunschModule,
    ImportprotokolleModule,
  ],
  exports: [ExportButtonComponent],
})
export class ViewerModule {}
