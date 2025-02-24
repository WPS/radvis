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
import { interval, Subscription } from 'rxjs';
import { exhaustMap, startWith, take, takeWhile } from 'rxjs/operators';
import { AutomatischerImportSchritt } from 'src/app/import/models/automatischer-import-schritt';
import { Severity } from 'src/app/import/models/import-session-view';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import invariant from 'tiny-invariant';
import { NetzklassenImportSessionView } from 'src/app/import/netzklassen/models/netzklassen-import-session-view';

@Component({
  selector: 'rad-import-netzklasse-automatische-abbildung',
  templateUrl: './import-netzklasse-automatische-abbildung.component.html',
  styleUrls: ['./import-netzklasse-automatische-abbildung.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportNetzklasseAutomatischeAbbildungComponent implements OnDestroy {
  private static readonly STEP = 3;

  automatischeImportschritte = ['Import der Geometrien', 'Abbildung auf RadVIS-Netz'];
  fehler: string[] = [];

  warnungen: string[] = [];
  private pollingSubscription: Subscription;
  private session: NetzklassenImportSessionView | null = null;

  constructor(
    private netzklassenImportService: NetzklassenImportService,
    private netzklassenRoutingService: NetzklassenRoutingService,
    notifyUserService: NotifyUserService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.pollingSubscription = interval(NetzklassenImportService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        startWith(0),
        take(NetzklassenImportService.MAX_POLLING_CALLS),
        takeWhile(() => this.isAutomatischeAbbildungRunning),
        exhaustMap(() => netzklassenImportService.getImportSession())
      )
      .subscribe({
        next: session => {
          invariant(session);
          this.session = session;
          this.fehler = session.log.filter(l => l.severity === Severity.ERROR).map(l => l.fehlerBeschreibung);
          this.warnungen = session.log.filter(l => l.severity === Severity.WARN).map(l => l.fehlerBeschreibung);
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
    return !this.session || (this.session.executing && this.session.schritt === 3);
  }

  ngOnDestroy(): void {
    this.pollingSubscription.unsubscribe();
  }

  onAbort(): void {
    this.pollingSubscription.unsubscribe();
    this.netzklassenImportService.deleteImportSession().subscribe(() => {
      this.netzklassenRoutingService.navigateToFirst();
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

  onPrevious(): void {
    this.netzklassenRoutingService.navigateToPrevious(ImportNetzklasseAutomatischeAbbildungComponent.STEP);
  }

  onNext(): void {
    this.netzklassenRoutingService.navigateToNext(ImportNetzklasseAutomatischeAbbildungComponent.STEP);
  }
}
