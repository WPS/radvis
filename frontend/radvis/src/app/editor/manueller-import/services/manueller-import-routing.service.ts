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
import { Route } from '@angular/router';
import { ImportStep } from 'src/app/editor/manueller-import/models/import-step';
import { IMPORT_STEPS } from 'src/app/editor/manueller-import/models/import-steps';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import invariant from 'tiny-invariant';

@Injectable({
  providedIn: 'root',
})
export class ManuellerImportRoutingService {
  public static IMPORT_DATEI_UPLOAD_ROUTE = 'upload';
  public static IMPORT_NETZKLASSE_PARAMETER_EINGEBEN_ROUTE = 'netzklasse-parameter';
  public static IMPORT_ATTRIBUTE_PARAMETER_EINGEBEN_ROUTE = 'attribute-parameter';
  public static IMPORT_AUTOMATISCHE_ABBILDUNG_ROUTE = 'abbildung';
  public static IMPORT_NETZKLASSE_KORREKTUR_ROUTE = 'netzklasse-korrektur';
  public static IMPORT_ATTRIBUTE_KORREKTUR_ROUTE = 'attribute-korrektur';
  public static IMPORT_NETZKLASSE_ABSCHLUSS_ROUTE = 'netzklasse-abschluss';
  public static IMPORT_ATTRIBUTE_ABSCHLUSS_ROUTE = 'attribute-abschluss';

  public static getChildRoutes(steps: ImportStep[]): Route[] {
    const childRoutes: Route[] = [];
    steps.forEach((step, index) => {
      if (step.abweichendeAttributImportRoute) {
        childRoutes.push({
          path: step.route.link,
          component: step.route.component,
          data: { step: index, importTyp: ImportTyp.NETZKLASSE_ZUWEISEN },
          canActivate: step.route.guard ? [step.route.guard] : [],
        } as Route);
        childRoutes.push({
          path: step.abweichendeAttributImportRoute.link,
          component: step.abweichendeAttributImportRoute.component,
          data: { step: index, importTyp: ImportTyp.ATTRIBUTE_UEBERNEHMEN },
          canActivate: step.abweichendeAttributImportRoute.guard ? [step.abweichendeAttributImportRoute.guard] : [],
        } as Route);
      } else {
        childRoutes.push({
          path: step.route.link,
          component: step.route.component,
          data: { step: index },
          canActivate: step.route.guard ? [step.route.guard] : [],
        } as Route);
      }
    });
    return childRoutes;
  }

  public getStartStepRoute(): string {
    return IMPORT_STEPS[0].route.link;
  }

  public getRouteForStep(stepIndex: number, typ: ImportTyp): string {
    invariant(stepIndex >= 0 && stepIndex < IMPORT_STEPS.length);
    const step = IMPORT_STEPS[stepIndex];
    if (typ === ImportTyp.NETZKLASSE_ZUWEISEN) {
      return step.route.link;
    } else {
      return step.abweichendeAttributImportRoute?.link || step.route.link;
    }
  }
}
