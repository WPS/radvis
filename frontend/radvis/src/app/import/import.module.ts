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
import { FehlerprotokollModule } from 'src/app/fehlerprotokoll/fehlerprotokoll.module';
import { AttributeImportModule } from 'src/app/import/attribute/attribute-import.module';
import { AttributeRoutingService } from 'src/app/import/attribute/services/attribute-routing.service';
import { ImportMenuComponent } from 'src/app/import/components/import-menu/import-menu.component';
import { ImportComponent } from 'src/app/import/components/import/import.component';
import { ImportRoutingModule } from 'src/app/import/import-routing.module';
import { MassnahmenDateianhaengeImportModule } from 'src/app/import/massnahmen-dateianhaenge/massnahmen-dateianhaenge-import.module';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenImportModule } from 'src/app/import/massnahmen/massnahmen-import.module';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { NetzklassenImportModule } from 'src/app/import/netzklassen/netzklassen-import.module';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { CreateSessionStateService } from 'src/app/import/services/create-session.state.service';
import { ImportRoutingService } from 'src/app/import/services/import-routing.service';
import { ImportService } from 'src/app/import/services/import.service';
import { TransformShpService } from 'src/app/import/services/transform-shp.service';
import { KarteModule } from 'src/app/karte/karte.module';
import { SharedModule } from 'src/app/shared/shared.module';

@NgModule({
  declarations: [ImportComponent, ImportMenuComponent],
  imports: [
    SharedModule,
    KarteModule,
    FehlerprotokollModule,
    ImportRoutingModule,
    AttributeImportModule,
    NetzklassenImportModule,
    MassnahmenImportModule,
    MassnahmenDateianhaengeImportModule,
  ],
  providers: [
    ImportService,
    ImportRoutingService,
    CreateSessionStateService,
    TransformShpService,
    AttributeRoutingService,
    NetzklassenRoutingService,
    MassnahmenImportRoutingService,
    MassnahmenDateianhaengeRoutingService,
  ],
})
export class ImportModule {}
