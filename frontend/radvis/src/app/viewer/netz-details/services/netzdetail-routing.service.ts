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
import { Params, Router } from '@angular/router';
import { Coordinate } from 'ol/coordinate';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { VIEWER_ROUTE } from 'src/app/viewer/viewer-shared/models/viewer-routes';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class NetzdetailRoutingService {
  public static KANTE = 'kante';
  public static KNOTEN = 'knoten';

  constructor(private router: Router) {}

  public static buildParams(clickposition: number[], selektierteSeite?: KantenSeite | string): Params {
    const queryParams: Params = { position: clickposition.join(',') };

    if (selektierteSeite) {
      invariant(
        selektierteSeite === KantenSeite.LINKS || selektierteSeite === KantenSeite.RECHTS,
        'Die selektierte Seite kann nur Links oder Rechts sein'
      );
    }
    queryParams.seite = selektierteSeite;

    return queryParams;
  }

  public toKanteDetails(id: number, clickposition: Coordinate, selektierteSeite?: KantenSeite): void {
    this.router.navigate([VIEWER_ROUTE, NetzdetailRoutingService.KANTE, id], {
      queryParamsHandling: 'merge',
      queryParams: NetzdetailRoutingService.buildParams(clickposition, selektierteSeite),
    });
  }

  public toKnotenDetails(id: number): void {
    this.router.navigate([VIEWER_ROUTE, NetzdetailRoutingService.KNOTEN, id], {
      queryParamsHandling: 'merge',
    });
  }
}
