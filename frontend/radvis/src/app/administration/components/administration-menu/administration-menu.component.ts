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
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AdministrationRoutingService } from 'src/app/administration/services/administration-routing.service';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';

@Component({
  selector: 'rad-administration-menu',
  templateUrl: './administration-menu.component.html',
  styleUrl: './administration-menu.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class AdministrationMenuComponent {
  public administrationBenutzerRoute = AdministrationRoutingService.ADMINISTRATION_BENUTZER_ROUTE;
  public administrationOrganisationRoute = AdministrationRoutingService.ADMINISTRATION_ORGANISATION_ROUTE;
  public administrationDateiLayerRoute = AdministrationRoutingService.ADMINISTRATION_DATEI_LAYER_ROUTE;

  public showBenutzer: boolean;
  public showOrganisation: boolean;
  public showDateiLayer: boolean;

  constructor(
    private benutzerDetailsService: BenutzerDetailsService,
    private featureTogglzService: FeatureTogglzService
  ) {
    this.showBenutzer = this.benutzerDetailsService.istAktuellerBenutzerOrgaUndNutzerVerwalter();
    this.showOrganisation =
      (this.benutzerDetailsService.canEditZustaendigkeitsBereichOfOrganisation() ||
        this.benutzerDetailsService.istAktuellerBenutzerOrgaUndNutzerVerwalter()) &&
      this.isOrganisationenBearbeitenToggleOn;
    this.showDateiLayer =
      this.benutzerDetailsService.canDateiLayerVerwalten() &&
      featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_DATEILAYER_HOCHLADEN_ANZEIGEN);
  }

  get showMenu(): boolean {
    return [this.showBenutzer, this.showOrganisation, this.showDateiLayer].filter(b => b).length > 1;
  }

  get isOrganisationenBearbeitenToggleOn(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_ORGANISATIONEN_ERSTELLEN_UND_BEARBEITEN);
  }
}
