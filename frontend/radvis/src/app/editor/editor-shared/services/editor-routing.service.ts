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
import { NavigationExtras, Params, Router } from '@angular/router';
import { Coordinate } from 'ol/coordinate';
import { Extent, getCenter } from 'ol/extent';
import { AttributGruppe } from 'src/app/editor/kanten/models/attribut-gruppe';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { AbstractQueryParams } from 'src/app/shared/models/abstract-query-params';
import { MapQueryParams } from 'src/app/shared/models/map-query-params';

@Injectable({
  providedIn: 'root',
})
export class EditorRoutingService {
  public static EDITOR_ROUTE = 'editor';
  public static EDITOR_KANTEN_ROUTE = 'kanten';
  public static EDITOR_KNOTEN_ROUTE = 'knoten';

  public static EDITOR_CREATE_SUBROUTE = 'new';

  constructor(private router: Router, private mapQueryParamsService: MapQueryParamsService) {}

  public toEditor(): Promise<boolean> {
    return this.router.navigate([EditorRoutingService.EDITOR_ROUTE], {
      queryParamsHandling: 'merge',
    });
  }

  public toKnotenAttributeEditor(id: number, params?: MapQueryParams): void {
    const navigationExtras: NavigationExtras = { queryParamsHandling: 'merge' };

    if (params) {
      navigationExtras.queryParams = MapQueryParams.merge(
        params,
        this.mapQueryParamsService.mapQueryParamsSnapshot
      ).toRouteParams();
    }

    this.router.navigate(
      [EditorRoutingService.EDITOR_ROUTE, EditorRoutingService.EDITOR_KNOTEN_ROUTE, id],
      navigationExtras
    );
  }

  public toKantenEditor(): void {
    this.toKantenSubEditor(AttributGruppe.ALLGEMEIN);
  }

  public toKnotenEditor(): void {
    this.router.navigate([EditorRoutingService.EDITOR_ROUTE, EditorRoutingService.EDITOR_KNOTEN_ROUTE], {
      queryParamsHandling: 'merge',
    });
  }

  public toKantenSubEditor(group: AttributGruppe): void {
    this.router.navigate([this.getAttributGruppeRoute(group)], {
      queryParamsHandling: 'merge',
    });
  }

  getAttributGruppeRoute(group: AttributGruppe): string {
    return `/${EditorRoutingService.EDITOR_ROUTE}/${EditorRoutingService.EDITOR_KANTEN_ROUTE}/${group}`;
  }

  getKantenRoute(): string {
    return `/${EditorRoutingService.EDITOR_ROUTE}/${EditorRoutingService.EDITOR_KANTEN_ROUTE}`;
  }

  getKantenCreatorRoute(): string {
    return `/${EditorRoutingService.EDITOR_ROUTE}/${EditorRoutingService.EDITOR_KANTEN_ROUTE}/${EditorRoutingService.EDITOR_CREATE_SUBROUTE}`;
  }

  getKnotenRoute(): string {
    return `/${EditorRoutingService.EDITOR_ROUTE}/${EditorRoutingService.EDITOR_KNOTEN_ROUTE}`;
  }

  getEditorRoute(): string {
    return `/${EditorRoutingService.EDITOR_ROUTE}`;
  }

  getViewParamForCenterAtCoordinate(coordinate: Coordinate | null): Params {
    const oldExtent = this.mapQueryParamsService.mapQueryParamsSnapshot.view;
    if (oldExtent) {
      const oldCenter = getCenter(oldExtent);
      coordinate = coordinate ?? oldCenter;
      const displacementX = coordinate[0] - oldCenter[0];
      const displacementY = coordinate[1] - oldCenter[1];
      const newExtent: Extent = [
        oldExtent[0] + displacementX,
        oldExtent[1] + displacementY,
        oldExtent[2] + displacementX,
        oldExtent[3] + displacementY,
      ];

      return { view: AbstractQueryParams.listToParam(newExtent.map(ord => ord.toString())) };
    }
    return { view: undefined };
  }
}
