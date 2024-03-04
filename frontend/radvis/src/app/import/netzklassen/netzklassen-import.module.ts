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
import { ImportNetzklasseAbbildungBearbeitenLayerComponent } from 'src/app/import/netzklassen/components/import-netzklasse-abbildung-bearbeiten-layer/import-netzklasse-abbildung-bearbeiten-layer.component';
import { ImportNetzklasseAbbildungBearbeitenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-abbildung-bearbeiten/import-netzklasse-abbildung-bearbeiten.component';
import { ImportNetzklasseAbschliessenLayerComponent } from 'src/app/import/netzklassen/components/import-netzklasse-abschliessen-layer/import-netzklasse-abschliessen-layer.component';
import { ImportNetzklasseAbschliessenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-abschliessen/import-netzklasse-abschliessen.component';
import { ImportNetzklasseAutomatischeAbbildungComponent } from 'src/app/import/netzklassen/components/import-netzklasse-automatische-abbildung/import-netzklasse-automatische-abbildung.component';
import { ImportNetzklasseDateiHochladenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-datei-hochladen/import-netzklasse-datei-hochladen.component';
import { ImportNetzklasseParameterEingebenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-parameter-eingeben/import-netzklasse-parameter-eingeben.component';
import { ImportNetzklasseSackgassenLayerComponent } from 'src/app/import/netzklassen/components/import-netzklasse-sackgassen-layer/import-netzklasse-sackgassen-layer.component';
import { ImportNetzklasseSackgassenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-sackgassen/import-netzklasse-sackgassen.component';
import { ImportNetzklasseToolComponent } from 'src/app/import/netzklassen/components/import-netzklasse-tool/import-netzklasse-tool.component';
import { NetzklassePipe } from 'src/app/import/netzklassen/netzklasse.pipe';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { SharedModule } from 'src/app/shared/shared.module';
import { ImportSharedModule } from 'src/app/import/import-shared/import-shared.module';

@NgModule({
  declarations: [
    ImportNetzklasseToolComponent,
    ImportNetzklasseDateiHochladenComponent,
    ImportNetzklasseParameterEingebenComponent,
    ImportNetzklasseSackgassenLayerComponent,
    ImportNetzklasseSackgassenComponent,
    ImportNetzklasseAutomatischeAbbildungComponent,
    ImportNetzklasseAbbildungBearbeitenComponent,
    ImportNetzklasseAbbildungBearbeitenLayerComponent,
    ImportNetzklasseAbschliessenComponent,
    ImportNetzklasseAbschliessenLayerComponent,
    NetzklassePipe,
  ],
  imports: [SharedModule, ImportSharedModule],
  providers: [NetzklassenImportService],
})
export class NetzklassenImportModule {}
