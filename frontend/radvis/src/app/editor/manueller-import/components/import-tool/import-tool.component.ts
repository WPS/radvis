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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { ActivatedRoute, NavigationEnd, NavigationExtras, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { EditorLayerZindexConfig } from 'src/app/editor/editor-shared/models/editor-layer-zindex-config';
import { ImportSessionStatus } from 'src/app/editor/manueller-import/models/import-session-status';
import { Severity } from 'src/app/editor/manueller-import/models/import-session-view';
import { IMPORT_STEPS } from 'src/app/editor/manueller-import/models/import-steps';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { CreateSessionStateService } from 'src/app/editor/manueller-import/services/create-session.state.service';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';

@Component({
  selector: 'rad-import-tool',
  templateUrl: './import-tool.component.html',
  styleUrls: ['./import-tool.component.scss', '../../../editor-tool-styles.scss'],
  providers: [CreateSessionStateService],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportToolComponent implements OnDestroy {
  @ViewChild('toolContainer', { read: ElementRef })
  toolContainer: ElementRef | undefined;

  readonly steps = IMPORT_STEPS;

  public activeStepIndex = 0;
  public fehlerprotokolleEnabled: boolean;
  public fehlerprotokollZIndex = EditorLayerZindexConfig.FEHLERPROTOKOLL_LAYER;

  private subscriptions: Subscription[] = [];

  constructor(
    router: Router,
    changeDetector: ChangeDetectorRef,
    manuellerImportService: ManuellerImportService,
    activatedRoute: ActivatedRoute,
    featureTogglzService: FeatureTogglzService
  ) {
    this.fehlerprotokolleEnabled = featureTogglzService.fehlerprotokoll;

    this.subscriptions.push(
      router.events
        .pipe(
          filter(ev => ev instanceof NavigationEnd),
          map(() =>
            this.steps.findIndex(
              s =>
                router.url.includes(s.route.link) ||
                (s.abweichendeAttributImportRoute && router.url.includes(s.abweichendeAttributImportRoute.link))
            )
          )
        )
        .subscribe(activeStepIndex => {
          this.activeStepIndex = activeStepIndex;
          changeDetector.markForCheck();
        })
    );

    manuellerImportService.existsImportSession().then(exists => {
      const extras: NavigationExtras = {
        relativeTo: activatedRoute,
        queryParamsHandling: 'merge',
      };

      // (1) Datei hochladen
      if (!exists) {
        router.navigate([ManuellerImportRoutingService.IMPORT_DATEI_UPLOAD_ROUTE], extras);
        return;
      }

      manuellerImportService.getImportSession().subscribe(session => {
        // (2) Parameter eingeben
        if (session.status === ImportSessionStatus.SESSION_CREATED) {
          router.navigate(
            [
              session.typ === ImportTyp.NETZKLASSE_ZUWEISEN
                ? ManuellerImportRoutingService.IMPORT_NETZKLASSE_PARAMETER_EINGEBEN_ROUTE
                : ManuellerImportRoutingService.IMPORT_ATTRIBUTE_PARAMETER_EINGEBEN_ROUTE,
            ],
            extras
          );
          return;
        }

        // (3) Automatische Abbildung
        if (
          session.status === ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_RUNNING ||
          (session.status === ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE &&
            session.log.some(l => l.severity === Severity.ERROR))
        ) {
          router.navigate([ManuellerImportRoutingService.IMPORT_AUTOMATISCHE_ABBILDUNG_ROUTE], extras);
          return;
        }

        // (4) Abbildung bearbeiten
        if (session.status === ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE) {
          router.navigate(
            [
              session.typ === ImportTyp.NETZKLASSE_ZUWEISEN
                ? ManuellerImportRoutingService.IMPORT_NETZKLASSE_KORREKTUR_ROUTE
                : ManuellerImportRoutingService.IMPORT_ATTRIBUTE_KORREKTUR_ROUTE,
            ],
            extras
          );
          return;
        }

        // (5) Import abschließen
        router.navigate(
          [
            session.typ === ImportTyp.NETZKLASSE_ZUWEISEN
              ? ManuellerImportRoutingService.IMPORT_NETZKLASSE_ABSCHLUSS_ROUTE
              : ManuellerImportRoutingService.IMPORT_ATTRIBUTE_ABSCHLUSS_ROUTE,
          ],
          extras
        );
      });
    });
  }

  @HostListener('document:keydown.control.alt.shift.d')
  onShortcut(): void {
    this.toolContainer?.nativeElement.querySelector('button, input, mat-select')?.focus();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }
}
