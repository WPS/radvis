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
export class AttributeRoutingService {
  private static readonly CLOSED = 0;

  public static readonly DATEI_UPLOAD_ROUTE = 'upload';
  public static readonly PARAMETER_EINGEBEN_ROUTE = 'parameter';
  public static readonly AUTOMATISCHE_ABBILDUNG_ROUTE = 'abbildung';
  public static readonly KORREKTUR_ROUTE = 'korrektur';
  public static readonly ABSCHLUSS_ROUTE = 'abschluss';
  public static ATTRIBUTE_IMPORT_STEPS: Map<number, ImportStep> = new Map([
    [
      1,
      {
        bezeichnung: 'Datei hochladen',
        path: AttributeRoutingService.DATEI_UPLOAD_ROUTE,
      },
    ],
    [
      2,
      {
        bezeichnung: 'Parameter Eingeben',
        path: AttributeRoutingService.PARAMETER_EINGEBEN_ROUTE,
      },
    ],
    [
      3,
      {
        bezeichnung: 'Automatische Abbildung',
        path: AttributeRoutingService.AUTOMATISCHE_ABBILDUNG_ROUTE,
      },
    ],
    [
      4,
      {
        bezeichnung: 'Abbildung bearbeiten',
        path: AttributeRoutingService.KORREKTUR_ROUTE,
      },
    ],
    [
      5,
      {
        bezeichnung: 'Import abschließen',
        path: AttributeRoutingService.ABSCHLUSS_ROUTE,
      },
    ],
    [
      AttributeRoutingService.CLOSED,
      {
        bezeichnung: 'Import abschließen',
        path: AttributeRoutingService.ABSCHLUSS_ROUTE,
        hiddenStep: true,
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
    this.router.navigate([ImportRoutes.IMPORT_ROUTE, ImportRoutes.ATTRIBUTE_IMPORT_ROUTE, this.getRouteForStep(step)], {
      queryParamsHandling: 'merge',
    });
  }

  private getRouteForStep(step: number): string {
    invariant(AttributeRoutingService.ATTRIBUTE_IMPORT_STEPS.has(step));
    return AttributeRoutingService.ATTRIBUTE_IMPORT_STEPS.get(step)!.path;
  }
}
