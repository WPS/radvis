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
import { RadnetzMatchingComponent } from 'src/app/radnetzmatching/components/radnetz-matching/radnetz-matching.component';
import { MatchingAuthorizationGuard } from 'src/app/radnetzmatching/services/matching-authorization.guard';
import { RadnetzMatchingRoutingService } from 'src/app/radnetzmatching/services/radnetz-matching-routing.service';
import { BenutzerAktivGuard } from 'src/app/shared/services/benutzer-aktiv.guard';
import { BenutzerRegistriertGuard } from 'src/app/shared/services/benutzer-registriert.guard';

const routes: Routes = [
  {
    path: RadnetzMatchingRoutingService.RADNETZ_MATCHING_ROUTE,
    component: RadnetzMatchingComponent,
    canActivate: [BenutzerRegistriertGuard, BenutzerAktivGuard, MatchingAuthorizationGuard],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class RadnetzMatchingRoutingModule {}
