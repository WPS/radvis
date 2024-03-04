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
import { ImportNetzklasseAbbildungBearbeitenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-abbildung-bearbeiten/import-netzklasse-abbildung-bearbeiten.component';
import { ImportNetzklasseAbschliessenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-abschliessen/import-netzklasse-abschliessen.component';
import { ImportNetzklasseAutomatischeAbbildungComponent } from 'src/app/import/netzklassen/components/import-netzklasse-automatische-abbildung/import-netzklasse-automatische-abbildung.component';
import { ImportNetzklasseDateiHochladenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-datei-hochladen/import-netzklasse-datei-hochladen.component';
import { ImportNetzklasseParameterEingebenComponent } from 'src/app/import/netzklassen/components/import-netzklasse-parameter-eingeben/import-netzklasse-parameter-eingeben.component';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { netzklassenSessionExistsGuard } from 'src/app/import/netzklassen/services/netzklassen-session-exists.guard';

export class NetzklassenImportRoutes {
  public static CHILDROUTES: Routes = [
    {
      path: '',
      pathMatch: 'full',
      redirectTo: NetzklassenRoutingService.DATEI_UPLOAD_ROUTE,
    },
    {
      path: NetzklassenRoutingService.DATEI_UPLOAD_ROUTE,
      component: ImportNetzklasseDateiHochladenComponent,
    },
    {
      path: NetzklassenRoutingService.PARAMETER_EINGEBEN_ROUTE,
      component: ImportNetzklasseParameterEingebenComponent,
    },
    {
      path: NetzklassenRoutingService.AUTOMATISCHE_ABBILDUNG_ROUTE,
      component: ImportNetzklasseAutomatischeAbbildungComponent,
      canActivate: [netzklassenSessionExistsGuard],
    },
    {
      path: NetzklassenRoutingService.KORREKTUR_ROUTE,
      component: ImportNetzklasseAbbildungBearbeitenComponent,
      canActivate: [netzklassenSessionExistsGuard],
    },
    {
      path: NetzklassenRoutingService.ABSCHLUSS_ROUTE,
      component: ImportNetzklasseAbschliessenComponent,
      canActivate: [netzklassenSessionExistsGuard],
    },
  ];
}
