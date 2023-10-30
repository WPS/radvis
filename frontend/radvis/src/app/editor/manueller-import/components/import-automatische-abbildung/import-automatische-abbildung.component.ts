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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { exhaustMap, startWith, take, takeWhile } from 'rxjs/operators';
import { AutomatischerImportSchritt } from 'src/app/editor/manueller-import/models/automatischer-import-schritt';
import { ImportSessionStatus } from 'src/app/editor/manueller-import/models/import-session-status';
import { ImportSessionView, Severity } from 'src/app/editor/manueller-import/models/import-session-view';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

@Component({
  selector: 'rad-automatische-abbildung',
  templateUrl: './import-automatische-abbildung.component.html',
  styleUrls: ['./import-automatische-abbildung.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportAutomatischeAbbildungComponent implements OnDestroy {
  automatischeImportschritte = ['Import der Geometrien', 'Abbildung auf RadVIS-Netz'];

  nextLink: string | null = null;
  previousLink: string | null = null;

  fehler: string[] = [];
  warnungen: string[] = [];

  private pollingSubscription: Subscription;
  private session: ImportSessionView | null = null;

  constructor(
    private manuellerImportService: ManuellerImportService,
    private importRoutingService: ManuellerImportRoutingService,
    notifyUserService: NotifyUserService,
    private route: ActivatedRoute,
    private router: Router,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.pollingSubscription = interval(ManuellerImportService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        startWith(0),
        take(ManuellerImportService.MILLISECONDS_IN_HOUR / ManuellerImportService.POLLING_INTERVALL_IN_MILLISECONDS),
        takeWhile(() => this.isAutomatischeAbbildungRunning),
        exhaustMap(() => manuellerImportService.getImportSession())
      )
      .subscribe({
        next: session => {
          this.session = session;
          this.fehler = session.log.filter(l => l.severity === Severity.ERROR).map(l => l.fehlerBeschreibung);
          this.warnungen = session.log.filter(l => l.severity === Severity.WARN).map(l => l.fehlerBeschreibung);
          this.nextLink = '../' + importRoutingService.getRouteForStep(route.snapshot.data.step + 1, session.typ);
          this.previousLink = '../' + importRoutingService.getRouteForStep(route.snapshot.data.step - 1, session.typ);

          this.changeDetectorRef.markForCheck();
        },
        error: () => {
          notifyUserService.warn('Fehler bei der Statusabfrage. Wurde der Import abgebrochen?');
        },
      });
  }

  get hasFehler(): boolean {
    return this.fehler.length !== 0 || false;
  }

  get hasWarnung(): boolean {
    return this.warnungen.length !== 0 || false;
  }

  get isAutomatischeAbbildungRunning(): boolean {
    return !this.session || this.session?.status === ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_RUNNING;
  }

  ngOnDestroy(): void {
    this.pollingSubscription.unsubscribe();
  }

  onAbort(): void {
    this.pollingSubscription.unsubscribe();
    this.manuellerImportService.deleteImportSession().then(() => {
      this.router.navigate(['../' + this.importRoutingService.getStartStepRoute()], {
        relativeTo: this.route,
        queryParamsHandling: 'merge',
      });
    });
  }

  getAktuellerImportSchrittNumber(): number {
    switch (this.session?.aktuellerImportSchritt) {
      case AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN:
        return 0;
      case AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ:
        return 1;
      case AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN:
        return 2;
      default:
        return -1;
    }
  }
}
