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
import { ImportRoutes } from 'src/app/import/models/import-routes';
import { ImportStep } from 'src/app/import/models/import-step';
import invariant from 'tiny-invariant';

@Injectable()
export class MassnahmenImportRoutingService {
  public static readonly DATEI_UPLOAD_ROUTE = 'upload';
  public static readonly ATTRIBUTE_AUSWAEHLEN_ROUTE = 'attribute-auswaehlen';
  public static readonly ATTRIBUTFEHLER_UEBERPRUEFEN_ROUTE = 'attributfehler-ueberpruefen';
  public static readonly IMPORT_UEBERPRUEFEN = 'import-ueberpruefen';
  public static readonly FEHLERPROTOKOLL_HERUNTERLADEN_ROUTE = 'fehlerprotokoll-herunterladen';
  public static MASSNAHMEN_IMPORT_STEPS: Map<number, ImportStep> = new Map([
    [
      1,
      {
        bezeichnung: 'Datei hochladen',
        path: MassnahmenImportRoutingService.DATEI_UPLOAD_ROUTE,
      },
    ],
    [
      2,
      {
        bezeichnung: 'Attribute auswählen',
        path: MassnahmenImportRoutingService.ATTRIBUTE_AUSWAEHLEN_ROUTE,
      },
    ],
    [
      3,
      {
        bezeichnung: 'Attributfehler überprüfen',
        path: MassnahmenImportRoutingService.ATTRIBUTFEHLER_UEBERPRUEFEN_ROUTE,
      },
    ],
    [
      4,
      {
        bezeichnung: 'Import überprüfen und speichern',
        path: MassnahmenImportRoutingService.IMPORT_UEBERPRUEFEN,
      },
    ],
    [
      5,
      {
        bezeichnung: 'Fehlerprotokoll herunterladen',
        path: MassnahmenImportRoutingService.FEHLERPROTOKOLL_HERUNTERLADEN_ROUTE,
      },
    ],
  ]);

  constructor(private router: Router) {}

  public navigateToFirst(): void {
    this.navigateToStep(1);
  }

  public navigateToNext(currentStep: number): void {
    this.navigateToStep(currentStep + 1);
  }

  public navigateToPrevious(currentStep: number): void {
    this.navigateToStep(currentStep - 1);
  }

  public navigateToStep(step: number): void {
    this.router.navigate(
      [ImportRoutes.IMPORT_ROUTE, ImportRoutes.MASSNAHMEN_IMPORT_ROUTE, this.getRouteForStep(step)],
      {
        queryParamsHandling: 'merge',
      }
    );
  }

  private getRouteForStep(step: number): string {
    invariant(MassnahmenImportRoutingService.MASSNAHMEN_IMPORT_STEPS.has(step));
    return MassnahmenImportRoutingService.MASSNAHMEN_IMPORT_STEPS.get(step)!.path;
  }
}
