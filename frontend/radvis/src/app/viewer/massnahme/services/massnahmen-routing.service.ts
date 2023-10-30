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
import {
  KommentarListeComponent,
} from 'src/app/viewer/kommentare/components/kommentar-liste/kommentar-liste.component';
import {
  MassnahmenUmsetzungsstandComponent,
} from 'src/app/viewer/massnahme/components/massnahme-umsetzungsstand/massnahmen-umsetzungsstand.component';
import {
  MassnahmenAttributeEditorComponent,
} from 'src/app/viewer/massnahme/components/massnahmen-attribute-editor/massnahmen-attribute-editor.component';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import {
  MassnahmeDokumentListeResolverService,
} from 'src/app/viewer/massnahme/services/massnahme-dokument-liste-resolver.service';
import {
  MassnahmeKommentarListeResolverService,
} from 'src/app/viewer/massnahme/services/massnahme-kommentar-liste-resolver.service';
import { MassnahmeResolverService } from 'src/app/viewer/massnahme/services/massnahme-resolver.service';
import { UmsetzungsstandGuardService } from 'src/app/viewer/massnahme/services/umsetzungsstand-guard.service';
import { UmsetzungsstandResolverService } from 'src/app/viewer/massnahme/services/umsetzungsstand-resolver.service';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import {
  AbstractInfrastrukturenRoutingService,
} from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-routing.service';
import { DokumentListeComponent } from 'src/app/viewer/dokument/components/dokument-liste/dokument-liste.component';

@Injectable({
  providedIn: 'root',
})
export class MassnahmenRoutingService extends AbstractInfrastrukturenRoutingService {
  public static EIGENSCHAFTEN = 'eigenschaften';
  public static UMSETZUNGSSTAND = 'umsetzungsstand';
  public static DATEIEN = 'dateien';
  public static CREATOR = 'new';
  public static KOMMENTARE = 'kommentare';

  constructor(router: Router) {
    super(router, MASSNAHMEN);
  }

  public static getChildRoutes(): Route[] {
    return [
      { path: '', redirectTo: this.EIGENSCHAFTEN, pathMatch: 'full' },
      {
        path: this.EIGENSCHAFTEN,
        component: MassnahmenAttributeEditorComponent,
        canDeactivate: [DiscardGuardService],
        resolve: { massnahme: MassnahmeResolverService },
      },
      {
        path: this.UMSETZUNGSSTAND,
        component: MassnahmenUmsetzungsstandComponent,
        canDeactivate: [DiscardGuardService],
        canActivate: [UmsetzungsstandGuardService],
        resolve: { umsetzungsstand: UmsetzungsstandResolverService },
      },
      {
        path: this.DATEIEN,
        component: DokumentListeComponent,
        canDeactivate: [DiscardGuardService],
        resolve: {
          [DokumentListeComponent.DOKUMENTLISTE_DATA_KEY]: MassnahmeDokumentListeResolverService,
        },
      },
      {
        path: this.KOMMENTARE,
        component: KommentarListeComponent,
        canDeactivate: [DiscardGuardService],
        resolve: { [KommentarListeComponent.KOMMENTARLISTE_DATA_KEY]: MassnahmeKommentarListeResolverService },
      }];
  }

  public getCreatorRoute(): string {
    return `/${VIEWER_ROUTE}/${MASSNAHMEN.pathElement}/${MassnahmenRoutingService.CREATOR}`;
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

  public getEigenschaftenRoute(id: number): string[] {
    const result = super.getInfrastrukturenEditorRoute(id);
    result.push(MassnahmenRoutingService.EIGENSCHAFTEN);
    return result;
  }

  public toUmsetzungstandEditor(id: number): void {
    const umsetzungstandEditorRoute = [...super.getInfrastrukturenEditorRoute(id), MassnahmenRoutingService.UMSETZUNGSSTAND];
    this.router.navigate(umsetzungstandEditorRoute, {
      queryParamsHandling: 'merge',
    });
  }

  private getActiveSubroute(): string | null {
    return (new RegExp(`${this.infrastrukturArt.pathElement}/\\d+/(.*?)($|\\?)`).exec(this.router.url))?.[1] || null;
  }
}
