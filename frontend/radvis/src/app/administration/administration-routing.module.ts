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
import { BenutzerEditorComponent } from 'src/app/administration/components/benutzer-editor/benutzer-editor.component';
import { BenutzerListComponent } from 'src/app/administration/components/benutzer-list/benutzer-list.component';
import { OrganisationEditorComponent } from 'src/app/administration/components/organisation-editor/organisation-editor.component';
import { OrganisationListComponent } from 'src/app/administration/components/organisation-list/organisation-list.component';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { BenutzerListResolverService } from 'src/app/administration/services/benutzer-list.resolver.service';
import { BenutzerResolverService } from 'src/app/administration/services/benutzer-resolver.service';
import { OrganisationResolverService } from 'src/app/administration/services/organisation-resolver.service';
import { BenutzerOrganisationenResolverService } from 'src/app/shared/services/benutzer-organisationen.resolver.service';
import { DiscardGuardService } from 'src/app/shared/services/discard-guard.service';
import { BenutzerAktivGuard } from 'src/app/shared/services/benutzer-aktiv.guard';
import { BenutzerRegistriertGuard } from 'src/app/shared/services/benutzer-registriert.guard';

const routes: Routes = [
  {
    path: AdministrationRoutingService.ADMINISTRATION_BENUTZER_ROUTE,
    canActivate: [BenutzerRegistriertGuard, BenutzerAktivGuard],
    component: BenutzerListComponent,
    resolve: {
      benutzer: BenutzerListResolverService,
    },
  },
  {
    path: `${AdministrationRoutingService.ADMINISTRATION_BENUTZER_ROUTE}/:id`,
    canDeactivate: [DiscardGuardService],
    component: BenutzerEditorComponent,
    resolve: {
      benutzer: BenutzerResolverService,
      organisationen: BenutzerOrganisationenResolverService,
    },
  },
  {
    path: AdministrationRoutingService.ADMINISTRATION_ORGANISATION_ROUTE,
    canActivate: [BenutzerRegistriertGuard, BenutzerAktivGuard],
    component: OrganisationListComponent,
  },
  {
    path: `${AdministrationRoutingService.ADMINISTRATION_ORGANISATION_ROUTE}/${AdministrationRoutingService.CREATOR}`,
    canDeactivate: [DiscardGuardService],
    component: OrganisationEditorComponent,
    data: {
      isCreator: true,
    },
  },
  {
    path: `${AdministrationRoutingService.ADMINISTRATION_ORGANISATION_ROUTE}/:id`,
    canDeactivate: [DiscardGuardService],
    component: OrganisationEditorComponent,
    resolve: {
      organisation: OrganisationResolverService,
    },
    data: {
      isCreator: false,
    },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AdministrationRoutingModule {}
