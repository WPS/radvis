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
import { ImportMassnahmenAttributeAuswaehlenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-attribute-auswaehlen/import-massnahmen-attribute-auswaehlen.component';
import { ImportMassnahmenAttributfehlerUeberpruefenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-attributfehler-ueberpruefen/import-massnahmen-attributfehler-ueberpruefen.component';
import { ImportMassnahmenDateiHochladenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-datei-hochladen/import-massnahmen-datei-hochladen.component';
import { ImportMassnahmenFehlerprotokollHerunterladenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-fehlerprotokoll-herunterladen/import-massnahmen-fehlerprotokoll-herunterladen.component';
import { ImportMassnahmenImportUeberpruefenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-import-ueberpruefen/import-massnahmen-import-ueberpruefen.component';
import { massnahmenImportSessionExistsGuard } from 'src/app/import/massnahmen/services/massnahmen-import-session-exists.guard';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';

export class MassnahmenImportRoutes {
  public static CHILDROUTES: Routes = [
    {
      path: '',
      pathMatch: 'full',
      redirectTo: MassnahmenImportRoutingService.DATEI_UPLOAD_ROUTE,
    },
    {
      path: MassnahmenImportRoutingService.DATEI_UPLOAD_ROUTE,
      component: ImportMassnahmenDateiHochladenComponent,
    },
    {
      path: MassnahmenImportRoutingService.ATTRIBUTE_AUSWAEHLEN_ROUTE,
      component: ImportMassnahmenAttributeAuswaehlenComponent,
      canActivate: [massnahmenImportSessionExistsGuard],
    },
    {
      path: MassnahmenImportRoutingService.ATTRIBUTFEHLER_UEBERPRUEFEN_ROUTE,
      component: ImportMassnahmenAttributfehlerUeberpruefenComponent,
      canActivate: [massnahmenImportSessionExistsGuard],
    },
    {
      path: MassnahmenImportRoutingService.IMPORT_UEBERPRUEFEN,
      component: ImportMassnahmenImportUeberpruefenComponent,
      canActivate: [massnahmenImportSessionExistsGuard],
    },
    {
      path: MassnahmenImportRoutingService.FEHLERPROTOKOLL_HERUNTERLADEN_ROUTE,
      component: ImportMassnahmenFehlerprotokollHerunterladenComponent,
      canActivate: [massnahmenImportSessionExistsGuard],
    },
  ];
}
