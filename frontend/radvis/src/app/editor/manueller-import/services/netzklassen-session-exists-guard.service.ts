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
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';

@Injectable({
  providedIn: 'root',
})
export class NetzklassenSessionExistsGuard implements CanActivate {
  constructor(
    private manuellerImportService: ManuellerImportService,
    private router: Router,
    private manuellerImportRoutingService: ManuellerImportRoutingService
  ) {}

  canActivate(): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    return this.manuellerImportService.existsImportSession(ImportTyp.NETZKLASSE_ZUWEISEN).then(exists => {
      if (!exists) {
        return this.router.createUrlTree(
          [
            EditorRoutingService.EDITOR_ROUTE,
            EditorRoutingService.EDITOR_IMPORT_ROUTE,
            this.manuellerImportRoutingService.getStartStepRoute(),
          ],
          { queryParamsHandling: 'merge' }
        );
      }

      return true;
    });
  }
}
