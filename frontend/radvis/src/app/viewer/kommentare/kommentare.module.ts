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
import { KommentarHinzufuegenComponent } from './components/kommentar-hinzufuegen/kommentar-hinzufuegen.component';
import { SharedModule } from 'src/app/shared/shared.module';
import { KommentarListeComponent } from 'src/app/viewer/kommentare/components/kommentar-liste/kommentar-liste.component';

@NgModule({
  declarations: [KommentarHinzufuegenComponent, KommentarListeComponent],
  imports: [CommonModule, SharedModule],
  exports: [KommentarHinzufuegenComponent],
})
export class KommentareModule {}
