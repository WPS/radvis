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
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class AdministrationRoutingService {
  public static readonly ADMINISTRATION_BENUTZER_ROUTE: string = 'administration/benutzer';
  public static readonly ADMINISTRATION_ORGANISATION_ROUTE: string = 'administration/organisation';
  public static readonly ADMINISTRATION_DATEI_LAYER_ROUTE: string = 'administration/datei-layer';

  public static readonly BENUTZER_SUCHE_PAGE_INDEX_QUERY_PARAM: string = 'benutzer-suche-page-index';
  public static readonly BENUTZER_SUCHE_PAGE_SIZE_QUERY_PARAM: string = 'benutzer-suche-page-size';
  public static readonly BENUTZER_SUCHE_QUERY_PARAM: string = 'benutzer-suche';

  public static readonly ORGANISATION_SUCHE_PAGE_INDEX_QUERY_PARAM: string = 'organisation-suche-page-index';
  public static readonly ORGANISATION_SUCHE_PAGE_SIZE_QUERY_PARAM: string = 'organisation-suche-page-size';
  public static readonly ORGANISATION_SUCHE_QUERY_PARAM: string = 'organisation-suche';

  public static readonly CREATOR: string = 'new';

  constructor(private router: Router) {}

  public toBenutzerEditor(id: number): void {
    this.router.navigate([AdministrationRoutingService.ADMINISTRATION_BENUTZER_ROUTE, id], {
      queryParamsHandling: 'merge',
    });
  }

  public toOrganisationEditor(id: number): void {
    this.router.navigate([AdministrationRoutingService.ADMINISTRATION_ORGANISATION_ROUTE, id], {
      queryParamsHandling: 'merge',
    });
  }

  public toOrganisationListe(): void {
    this.router.navigate([AdministrationRoutingService.ADMINISTRATION_ORGANISATION_ROUTE], {
      queryParamsHandling: 'merge',
    });
  }

  public toDateiLayerVerwaltung(): void {
    this.router.navigate([AdministrationRoutingService.ADMINISTRATION_DATEI_LAYER_ROUTE], {
      queryParamsHandling: 'merge',
    });
  }

  public toAdministration(): void {
    this.router.navigate([AdministrationRoutingService.ADMINISTRATION_BENUTZER_ROUTE], {
      queryParamsHandling: 'merge',
    });
  }

  public toOrganisationCreator(): void {
    this.router.navigate(
      [AdministrationRoutingService.ADMINISTRATION_ORGANISATION_ROUTE, AdministrationRoutingService.CREATOR],
      {
        queryParamsHandling: 'merge',
      }
    );
  }
}
