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
import { MatomoTracker } from 'ngx-matomo-client';

@Injectable({
  providedIn: 'root',
})
export class ManualRoutingService {
  static readonly MANUAL_URL: string = '/manual';
  static readonly MANUAL_EDITOR_URL: string = '/manual/docs/editor';
  static readonly MANUAL_VIEWER_URL: string = '/manual/docs/viewer';
  static readonly MANUAL_BENUTZERVERWALTUNG_URL: string = '/manual/docs/benutzerverwaltung';
  static readonly MANUAL_INTRO_URL: string = '/manual/docs/intro';
  static readonly MANUAL_DATENSCHNITTSTELLEN_URL: string = '/manual/docs/datenschnittstellen';
  static readonly MANUAL_IMPORT_URL: string = '/manual/docs/import';

  constructor(
    private router: Router,
    private matomoTracker: MatomoTracker
  ) {}

  openManual(): void {
    const url = this.router.serializeUrl(this.router.createUrlTree([ManualRoutingService.MANUAL_URL]));
    this.openManualInNewTab(url);
  }

  private openManualInNewTab(url: string): void {
    this.matomoTracker.trackEvent('Handbuch', 'Aufruf', url);
    window.open(url, '_blank');
  }

  openManualImportTransformation(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_IMPORT_URL], { fragment: 'transformation' })
    );
    this.openManualInNewTab(url);
  }

  openManualRollenRechte(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_BENUTZERVERWALTUNG_URL], { fragment: 'rollen--rechte' })
    );
    this.openManualInNewTab(url);
  }

  openManualLeihstationenImport(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_VIEWER_URL], { fragment: 'leihstationen' })
    );
    this.openManualInNewTab(url);
  }

  openManualServicestationenImport(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_VIEWER_URL], { fragment: 'servicestationen' })
    );
    this.openManualInNewTab(url);
  }

  openManualAbstellanlageImport(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_VIEWER_URL], { fragment: 'abstellanlagen' })
    );
    this.openManualInNewTab(url);
  }

  openManualWeitereHilfe(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_INTRO_URL], {
        fragment: 'weitere-hilfe-und-informationen',
      })
    );
    this.openManualInNewTab(url);
  }

  openManualAnzeigeordnungViewer(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_VIEWER_URL], {
        fragment: 'anzeigeordnung-der-kartenebenen',
      })
    );
    this.openManualInNewTab(url);
  }

  openManualRoutingProfile(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_VIEWER_URL], {
        fragment: 'verwalten-von-routing-profilen',
      })
    );
    this.openManualInNewTab(url);
  }

  openManualWmsWfsSchnittstelle(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_DATENSCHNITTSTELLEN_URL])
    );
    this.openManualInNewTab(url);
  }

  openManualPflichtattribute(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_IMPORT_URL], {
        fragment: 'pflichtattribute',
      })
    );
    this.openManualInNewTab(url);
  }
}
