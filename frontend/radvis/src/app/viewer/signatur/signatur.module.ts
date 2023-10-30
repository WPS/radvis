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
import { RadvisSignaturAuswahlComponent } from 'src/app/viewer/signatur/components/radvis-signatur-auswahl/radvis-signatur-auswahl.component';
import { RadvisSignaturLayerComponent } from 'src/app/viewer/signatur/components/radvis-signatur-layer/radvis-signatur-layer.component';
import { SignaturNetzklasseLayerComponent } from 'src/app/viewer/signatur/components/signatur-netzklasse-layer/signatur-netzklasse-layer.component';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';
import { RadnetzSignaturLayerComponent } from './components/radnetz-signatur-layer/radnetz-signatur-layer.component';

const exports = [
  SignaturNetzklasseLayerComponent,
  RadvisSignaturAuswahlComponent,
  RadvisSignaturLayerComponent,
  RadnetzSignaturLayerComponent,
];

@NgModule({
  declarations: exports,
  exports,
  imports: [SharedModule, ViewerSharedModule],
})
export class SignaturModule {}
