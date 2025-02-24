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
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatTabNavPanel } from '@angular/material/tabs';
import { ImportRoutes } from 'src/app/import/models/import-routes';
import { ImportRoutingService } from 'src/app/import/services/import-routing.service';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { IsActiveMatchOptions } from '@angular/router';

@Component({
  selector: 'rad-import-menu',
  templateUrl: './import-menu.component.html',
  styleUrl: './import-menu.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportMenuComponent {
  @Input()
  public tabPanel!: MatTabNavPanel;

  netzklassenRoute: string;
  attributeRoute: string;
  massnahmenRoute: string;
  massnahmenDateianhaengeRoute: string;

  canBenutzerImportNetzKlassenUndAttribute: boolean;
  canBenutzerMassnahmenAndDateianhaenge: boolean;
  routerLinkActiveOptions: IsActiveMatchOptions = {
    queryParams: 'ignored',
    fragment: 'ignored',
    paths: 'subset',
    matrixParams: 'exact',
  };

  get isImportVonMassnahmenToggleOn(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_IMPORT_MASSNAHMEN);
  }

  get isImportVonDateianhaengenToggleOn(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_IMPORT_DATEIANHAENGE_MASSNAHMEN);
  }

  constructor(
    importRoutingService: ImportRoutingService,
    private featureTogglzService: FeatureTogglzService,
    private benutzerDetailsService: BenutzerDetailsService
  ) {
    this.netzklassenRoute = importRoutingService.getNetzklassenImportRoute();
    this.attributeRoute = importRoutingService.getAttributeImportRoute();
    this.massnahmenRoute = importRoutingService.getMassnahmenImportRoute();
    this.massnahmenDateianhaengeRoute = `/${ImportRoutes.IMPORT_ROUTE}/${ImportRoutes.MASSNAHMEN_DATEIANHAENGE_IMPORT_ROUTE}`;
    this.canBenutzerImportNetzKlassenUndAttribute =
      this.benutzerDetailsService.canBenutzerImportNetzklassenAndAttribute();
    this.canBenutzerMassnahmenAndDateianhaenge =
      this.benutzerDetailsService.canBenutzerImportMassnahmenAndDateianhaenge();
  }
}
