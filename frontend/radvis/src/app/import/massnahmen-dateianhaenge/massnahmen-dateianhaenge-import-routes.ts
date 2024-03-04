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

import { Routes } from '@angular/router';
import { MassnahmenDateianhaengeDateiHochladenComponent } from 'src/app/import/massnahmen-dateianhaenge/components/massnahmen-dateianhaenge-datei-hochladen/massnahmen-dateianhaenge-datei-hochladen.component';
import { MassnahmenDateianhaengeDuplikateUeberpruefenComponent } from 'src/app/import/massnahmen-dateianhaenge/components/massnahmen-dateianhaenge-duplikate-ueberpruefen/massnahmen-dateianhaenge-duplikate-ueberpruefen.component';
import { MassnahmenDateianhaengeFehlerUeberpruefenComponent } from 'src/app/import/massnahmen-dateianhaenge/components/massnahmen-dateianhaenge-fehler-ueberpruefen/massnahmen-dateianhaenge-fehler-ueberpruefen.component';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { massnahmenDateianhaengeSessionExistsGuard } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-session-exists.guard';
import { massnahmenDateianhaengeSessionResolver } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-session.resolver';

export class MassnahmenDateianhaengeImportRoutes {
  public static CHILDROUTES: Routes = [
    {
      path: '',
      pathMatch: 'full',
      redirectTo: MassnahmenDateianhaengeRoutingService.DATEI_UPLOAD_ROUTE,
    },
    {
      path: MassnahmenDateianhaengeRoutingService.DATEI_UPLOAD_ROUTE,
      component: MassnahmenDateianhaengeDateiHochladenComponent,
    },
    {
      path: MassnahmenDateianhaengeRoutingService.FEHLER_UEBERPRUEFEN_ROUTE,
      component: MassnahmenDateianhaengeFehlerUeberpruefenComponent,
      canActivate: [massnahmenDateianhaengeSessionExistsGuard],
      resolve: { session: massnahmenDateianhaengeSessionResolver },
    },
    {
      path: MassnahmenDateianhaengeRoutingService.DUPLIKATE_UEBERPRUEFEN_ROUTE,
      component: MassnahmenDateianhaengeDuplikateUeberpruefenComponent,
      canActivate: [massnahmenDateianhaengeSessionExistsGuard],
    },
    // {
    //   path: MassnahmenDateianhaengeRoutingService.FEHLERPROTOKOLL_HERUNTERLADEN_ROUTE,
    //   component: undefined,
    //   canActivate: [massnahmenDateianhaengeSessionExistsGuard],
    // },
  ];
}
