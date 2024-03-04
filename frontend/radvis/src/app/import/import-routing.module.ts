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
import { RouterModule, Routes } from '@angular/router';
import { AttributeImportRoutes } from 'src/app/import/attribute/attribute-import-routes';
import { ImportAttributeToolComponent } from 'src/app/import/attribute/components/import-attribute-tool/import-attribute-tool.component';
import { AttributeRoutingService } from 'src/app/import/attribute/services/attribute-routing.service';
import { ImportComponent } from 'src/app/import/components/import/import.component';
import { MassnahmenDateianhaengeToolComponent } from 'src/app/import/massnahmen-dateianhaenge/components/massnahmen-dateianhaenge-tool/massnahmen-dateianhaenge-tool.component';
import { MassnahmenDateianhaengeImportRoutes } from 'src/app/import/massnahmen-dateianhaenge/massnahmen-dateianhaenge-import-routes';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { ImportMassnahmenToolComponent } from 'src/app/import/massnahmen/components/import-massnahmen-tool/import-massnahmen-tool.component';
import { MassnahmenImportRoutes } from 'src/app/import/massnahmen/massnahmen-import-routes';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { ImportRoutes } from 'src/app/import/models/import-routes';
import { ImportNetzklasseToolComponent } from 'src/app/import/netzklassen/components/import-netzklasse-tool/import-netzklasse-tool.component';
import { NetzklassenImportRoutes } from 'src/app/import/netzklassen/netzklassen-import-routes';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { benutzerAktivGuard } from 'src/app/shared/services/benutzer-aktiv.guard';
import { benutzerRegistriertGuard } from 'src/app/shared/services/benutzer-registriert.guard';

const routes: Routes = [
  {
    path: ImportRoutes.IMPORT_ROUTE,
    component: ImportComponent,
    canActivate: [benutzerRegistriertGuard, benutzerAktivGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: ImportRoutes.NETZKLASSEN_IMPORT_ROUTE,
      },
      {
        path: ImportRoutes.NETZKLASSEN_IMPORT_ROUTE,
        component: ImportNetzklasseToolComponent,
        data: { steps: NetzklassenRoutingService.NETZKLASSEN_IMPORT_STEPS },
        children: NetzklassenImportRoutes.CHILDROUTES,
      },
      {
        path: ImportRoutes.ATTRIBUTE_IMPORT_ROUTE,
        component: ImportAttributeToolComponent,
        data: { steps: AttributeRoutingService.ATTRIBUTE_IMPORT_STEPS },
        children: AttributeImportRoutes.CHILDROUTES,
      },
      {
        path: ImportRoutes.MASSNAHMEN_IMPORT_ROUTE,
        component: ImportMassnahmenToolComponent,
        data: { steps: MassnahmenImportRoutingService.MASSNAHMEN_IMPORT_STEPS },
        children: MassnahmenImportRoutes.CHILDROUTES,
      },
      {
        path: ImportRoutes.MASSNAHMEN_DATEIANHAENGE_IMPORT_ROUTE,
        component: MassnahmenDateianhaengeToolComponent,
        data: { steps: MassnahmenDateianhaengeRoutingService.DATEIANHAENGE_IMPORT_STEPS },
        children: MassnahmenDateianhaengeImportRoutes.CHILDROUTES,
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ImportRoutingModule {}
