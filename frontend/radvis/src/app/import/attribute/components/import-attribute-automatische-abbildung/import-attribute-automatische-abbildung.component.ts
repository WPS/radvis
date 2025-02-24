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
import { Subscription, interval } from 'rxjs';
import { exhaustMap, startWith, take, takeWhile } from 'rxjs/operators';
import { AttributeImportSessionView } from 'src/app/import/attribute/models/attribute-import-session-view';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { AttributeRoutingService } from 'src/app/import/attribute/services/attribute-routing.service';
import { AutomatischerImportSchritt } from 'src/app/import/models/automatischer-import-schritt';
import { Severity } from 'src/app/import/models/import-session-view';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-import-attribute-automatische-abbildung',
  templateUrl: './import-attribute-automatische-abbildung.component.html',
  styleUrls: ['./import-attribute-automatische-abbildung.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportAttributeAutomatischeAbbildungComponent implements OnDestroy {
  private static readonly STEP = 3;
  automatischeImportschritte = ['Import der Geometrien', 'Abbildung auf RadVIS-Netz'];

  fehler: string[] = [];
  warnungen: string[] = [];

  private pollingSubscription: Subscription;
  private session: AttributeImportSessionView | null = null;

  constructor(
    private attributeImportService: AttributeImportService,
    private attributeRoutingService: AttributeRoutingService,
    notifyUserService: NotifyUserService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.pollingSubscription = interval(AttributeImportService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        startWith(0),
        take(AttributeImportService.MAX_POLLING_CALLS),
        takeWhile(() => this.isAutomatischeAbbildungRunning),
        exhaustMap(() => attributeImportService.getImportSession())
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
    this.attributeImportService.deleteImportSession().subscribe(() => {
      this.attributeRoutingService.navigateToFirst();
    });
  }

  onPrevious(): void {
    this.attributeRoutingService.navigateToPrevious(ImportAttributeAutomatischeAbbildungComponent.STEP);
  }

  onNext(): void {
    this.attributeRoutingService.navigateToNext(ImportAttributeAutomatischeAbbildungComponent.STEP);
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
