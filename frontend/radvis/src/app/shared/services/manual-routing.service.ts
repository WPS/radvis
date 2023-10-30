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
export class ManualRoutingService {
  static readonly MANUAL_URL: string = '/manual';
  static readonly MANUAL_EDITOR_URL: string = '/manual/docs/editor';
  static readonly MANUAL_VIEWER_URL: string = '/manual/docs/viewer';
  static readonly MANUAL_BENUTZERVERWALTUNG_URL: string = '/manual/docs/benutzerverwaltung';
  static readonly MANUAL_INTRO_URL: string = '/manual/docs/intro';

  constructor(private router: Router) {}

  openManual(): void {
    const url = this.router.serializeUrl(this.router.createUrlTree([ManualRoutingService.MANUAL_URL]));
    window.open(url, '_blank');
  }

  openManualEditorTransformation(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_EDITOR_URL], { fragment: 'transformation' })
    );
    window.open(url, '_blank');
  }

  openManualRollenRechte(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_BENUTZERVERWALTUNG_URL], { fragment: 'rollen--rechte' })
    );
    window.open(url, '_blank');
  }

  openManualLeihstationenImport(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_VIEWER_URL], { fragment: 'leihstationen' })
    );
    window.open(url, '_blank');
  }

  openManualServicestationenImport(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_VIEWER_URL], { fragment: 'servicestationen' })
    );
    window.open(url, '_blank');
  }

  openManualAbstellanlageImport(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_VIEWER_URL], { fragment: 'abstellanlagen' })
    );
    window.open(url, '_blank');
  }

  openManualWeitereHilfe(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_INTRO_URL], {
        fragment: 'weitere-hilfe-und-informationen',
      })
    );
    window.open(url, '_blank');
  }

  openAnzeigeordnungViewer(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_VIEWER_URL], {
        fragment: 'anzeigeordnung-der-kartenebenen',
      })
    );
    window.open(url, '_blank');
  }

  openRoutingProfile(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree([ManualRoutingService.MANUAL_VIEWER_URL], {
        fragment: 'verwalten-von-routing-profilen',
      })
    );
    window.open(url, '_blank');
  }
}
