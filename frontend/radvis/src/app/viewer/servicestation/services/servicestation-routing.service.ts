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
import { Route, Router } from '@angular/router';
import { DiscardGuardService } from 'src/app/shared/services/discard-guard.service';
import { DokumentListeComponent } from 'src/app/viewer/dokument/components/dokument-liste/dokument-liste.component';
import {
  ServicestationEditorComponent
} from 'src/app/viewer/servicestation/components/servicestation-editor/servicestation-editor.component';
import { SERVICESTATIONEN } from 'src/app/viewer/servicestation/models/servicestation.infrastruktur';
import {
  ServicestationDokumentListeResolverService
} from 'src/app/viewer/servicestation/services/servicestation-dokument-liste-resolver.service';
import { ServicestationService } from 'src/app/viewer/servicestation/services/servicestation.service';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import {
  AbstractInfrastrukturenRoutingService
} from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-routing.service';

@Injectable({
  providedIn: 'root',
})
export class ServicestationRoutingService extends AbstractInfrastrukturenRoutingService {
  public static CREATOR_ROUTE = 'new';
  public static EIGENSCHAFTEN = 'eigenschaften';

  public static DATEIEN = 'dateien';

  constructor(router: Router) {
    super(router, SERVICESTATIONEN);
  }

  public static getChildRoutes(): Route[] {
    return [
      { path: '', redirectTo: this.EIGENSCHAFTEN, pathMatch: 'full' },
      {
        path: this.EIGENSCHAFTEN,
        component: ServicestationEditorComponent,
        canDeactivate: [DiscardGuardService],
        data: { isCreator: false },
        resolve: { servicestation: ServicestationService },
      },
      {
        path: this.DATEIEN,
        component: DokumentListeComponent,
        canDeactivate: [DiscardGuardService],
        resolve: {
          [DokumentListeComponent.DOKUMENTLISTE_DATA_KEY]: ServicestationDokumentListeResolverService,
        },
      },
    ];
  }

  public toCreator(): void {
    this.router.navigate([VIEWER_ROUTE, SERVICESTATIONEN.pathElement, ServicestationRoutingService.CREATOR_ROUTE], {
      queryParamsHandling: 'merge',
    });
  }

  // prettier kann nicht mit overrides um
  // eslint-disable-next-line prettier/prettier
  public override getInfrastrukturenEditorRoute(id: number): string[] {
    const result = super.getInfrastrukturenEditorRoute(id);
    const activeSubroute = this.getActiveSubroute();
    if (activeSubroute) {
      result.push(activeSubroute);
    }
    return result;
  }

  private getActiveSubroute(): string | null {
    return (new RegExp(`${this.infrastrukturArt.pathElement}/\\d+/(.*?)($|\\?)`).exec(this.router.url))?.[1] || null;
  }
}
