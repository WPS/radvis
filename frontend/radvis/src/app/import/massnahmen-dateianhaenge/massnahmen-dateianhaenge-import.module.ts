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
import { MassnahmenDateianhaengeDateiHochladenComponent } from 'src/app/import/massnahmen-dateianhaenge/components/massnahmen-dateianhaenge-datei-hochladen/massnahmen-dateianhaenge-datei-hochladen.component';
import { MassnahmenDateianhaengeDuplikateUeberpruefenComponent } from 'src/app/import/massnahmen-dateianhaenge/components/massnahmen-dateianhaenge-duplikate-ueberpruefen/massnahmen-dateianhaenge-duplikate-ueberpruefen.component';
import { MassnahmenDateianhaengeFehlerUeberpruefenComponent } from 'src/app/import/massnahmen-dateianhaenge/components/massnahmen-dateianhaenge-fehler-ueberpruefen/massnahmen-dateianhaenge-fehler-ueberpruefen.component';
import { MassnahmenDateianhaengeToolComponent } from 'src/app/import/massnahmen-dateianhaenge/components/massnahmen-dateianhaenge-tool/massnahmen-dateianhaenge-tool.component';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';
import { SharedModule } from 'src/app/shared/shared.module';
import { ImportSharedModule } from 'src/app/import/import-shared/import-shared.module';

@NgModule({
  declarations: [
    MassnahmenDateianhaengeToolComponent,
    MassnahmenDateianhaengeDateiHochladenComponent,
    MassnahmenDateianhaengeFehlerUeberpruefenComponent,
    MassnahmenDateianhaengeDuplikateUeberpruefenComponent,
  ],
  imports: [SharedModule, ImportSharedModule],
  providers: [MassnahmenDateianhaengeService],
})
export class MassnahmenDateianhaengeImportModule {}
