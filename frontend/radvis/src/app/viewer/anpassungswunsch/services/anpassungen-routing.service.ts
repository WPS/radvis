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

import { Injectable } from '@angular/core';
import { Data, Route, Router } from '@angular/router';
import { CreateAnpassungswunschRouteProvider } from 'src/app/shared/services/create-anpassungswunsch-route.provider';
import { discardGuard } from 'src/app/shared/services/discard.guard';
import { AnpassungenEditorComponent } from 'src/app/viewer/anpassungswunsch/components/anpassungen-editor/anpassungen-editor.component';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { anpassungswunschResolver } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch.resolver';
import { anpassungwunschKommentarListeResolver } from 'src/app/viewer/anpassungswunsch/services/anpassungwunsch-kommentar-liste.resolver';
import { KommentarListeComponent } from 'src/app/viewer/kommentare/components/kommentar-liste/kommentar-liste.component';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import { AbstractInfrastrukturenRoutingService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-routing.service';

@Injectable({
  providedIn: 'root',
})
export class AnpassungenRoutingService
  extends AbstractInfrastrukturenRoutingService
  implements CreateAnpassungswunschRouteProvider
{
  public static readonly CREATOR_ROUTE = 'new';
  public static readonly EIGENSCHAFTEN = 'eigenschaften';
  public static readonly KOMMENTARE = 'kommentare';

  constructor(router: Router) {
    super(router, ANPASSUNGSWUNSCH);
  }

  public static getChildRoutes(): Route[] {
    const kommentareData: Data = {};
    kommentareData[KommentarListeComponent.KOMMENTARLISTE_DATA_KEY] = anpassungwunschKommentarListeResolver;

    return [
      { path: '', redirectTo: this.EIGENSCHAFTEN, pathMatch: 'full' },
      {
        path: this.KOMMENTARE,
        component: KommentarListeComponent,
        resolve: kommentareData,
        canDeactivate: [discardGuard],
      },
      {
        path: this.EIGENSCHAFTEN,
        component: AnpassungenEditorComponent,
        data: {
          isCreator: false,
        },
        resolve: {
          anpassungswunsch: anpassungswunschResolver,
        },
        canDeactivate: [discardGuard],
      },
    ];
  }

  public getCreatorRoute(): string {
    return `/${VIEWER_ROUTE}/${ANPASSUNGSWUNSCH.pathElement}/${AnpassungenRoutingService.CREATOR_ROUTE}`;
  }
}
