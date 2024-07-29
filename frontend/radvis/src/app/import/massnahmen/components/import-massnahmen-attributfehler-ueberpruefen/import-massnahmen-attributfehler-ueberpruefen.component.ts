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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { Subscription, interval } from 'rxjs';
import { exhaustMap, startWith, take, takeWhile } from 'rxjs/operators';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { MassnahmenImportZuordnungStatus } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-status';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { Severity } from 'src/app/import/models/import-session-view';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import invariant from 'tiny-invariant';

export interface MassnahmenAttributFehlerUeberpruefenRow {
  status: string;
  id: string;
  attribut: string;
  hinweis: string;
  first: boolean;
  rowspan: number;
}

@Component({
  selector: 'rad-import-massnahmen-attributfehler-ueberpruefen',
  templateUrl: './import-massnahmen-attributfehler-ueberpruefen.component.html',
  styleUrl: './import-massnahmen-attributfehler-ueberpruefen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportMassnahmenAttributfehlerUeberpruefenComponent implements OnInit, OnDestroy {
  private static readonly STEP = 3;
  session: MassnahmenImportSessionView | null = null;
  loading = false;

  MassnahmenImportZuordnungStatus = MassnahmenImportZuordnungStatus;
  dataSource: MatTableDataSource<MassnahmenAttributFehlerUeberpruefenRow> = new MatTableDataSource();
  displayedColumns = ['status', 'id', 'attribut', 'hinweis'];

  pollingSubscription: Subscription | undefined;
  executing: boolean = false;
  anzahlMassnahmen: number = 0;
  anzahlFehlerhafterMassnahmen: number = 0;

  get schrittAbgeschlossenOderHasFehler(): boolean {
    return this.schrittAbgeschlossen || this.hasFehler;
  }

  get isNetzbezugErstellungRunning(): boolean {
    return (
      this.session?.schritt === ImportMassnahmenAttributfehlerUeberpruefenComponent.STEP && this.session?.executing
    );
  }

  get schrittAbgeschlossen(): boolean {
    return (this.session && this.session.schritt > ImportMassnahmenAttributfehlerUeberpruefenComponent.STEP) ?? false;
  }

  get hasFehler(): boolean {
    return this.fehler.length > 0;
  }

  get fehler(): string[] {
    return this.session?.log.filter(l => l.severity === Severity.ERROR).map(l => l.fehlerBeschreibung) || [];
  }

  get hasValideMassnahme(): boolean {
    return this.anzahlMassnahmen > this.anzahlFehlerhafterMassnahmen;
  }

  constructor(
    private massnahmenImportService: MassnahmenImportService,
    private massnahmenImportRoutingService: MassnahmenImportRoutingService,
    private notifyUserService: NotifyUserService,
    private errorHandlingService: ErrorHandlingService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.massnahmenImportService.getImportSession().subscribe(session => {
      this.session = session;
      if (session) {
        if (!this.schrittAbgeschlossen && this.isNetzbezugErstellungRunning) {
          this.startPolling();
        }
        this.loading = false;
        this.changeDetectorRef.detectChanges();
      }
    });
  }

  ngOnInit(): void {
    this.loading = true;
    this.changeDetectorRef.detectChanges();
    this.massnahmenImportService
      .getZuordnungenAttributfehler()
      .subscribe({
        next: zuordnungen => {
          if (zuordnungen) {
            this.anzahlMassnahmen = zuordnungen.length;

            const zuordnungenMitFehlern = zuordnungen.filter(zuordnung => zuordnung.fehler.length > 0);
            this.anzahlFehlerhafterMassnahmen = zuordnungenMitFehlern.length;

            const attributfehlerRows = zuordnungenMitFehlern.flatMap(value => {
              return value.fehler.map((fehler, index) => {
                return {
                  status: value.status,
                  id: value.massnahmeKonzeptId,
                  attribut: fehler.attributName,
                  hinweis: fehler.text,
                  first: index == 0,
                  rowspan: value.fehler.length,
                };
              });
            });
            this.dataSource = new MatTableDataSource<MassnahmenAttributFehlerUeberpruefenRow>(attributfehlerRows);
          }
        },
      })
      .add(() => {
        this.loading = false;
        this.changeDetectorRef.detectChanges();
      });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
  }

  onAbort(): void {
    this.massnahmenImportService.deleteImportSession().subscribe(() => {
      this.massnahmenImportRoutingService.navigateToFirst();
    });
  }

  onPrevious(): void {
    this.massnahmenImportRoutingService.navigateToPrevious(ImportMassnahmenAttributfehlerUeberpruefenComponent.STEP);
  }

  onNext(): void {
    this.navigateToNextStep();
  }

  onStart(): void {
    invariant(!this.schrittAbgeschlossen);

    this.executing = true;
    this.massnahmenImportService.netzbezuegeErstellen().subscribe({
      next: () => {
        if (this.session) {
          this.session.executing = true;
        }
        this.startPolling();
        this.changeDetectorRef.markForCheck();
      },
      error: err => {
        this.errorHandlingService.handleHttpError(err);
        this.changeDetectorRef.markForCheck();
        this.executing = false;
      },
    });
  }

  private startPolling(): void {
    this.pollingSubscription = interval(MassnahmenImportService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        startWith(0),
        take(MassnahmenImportService.MAX_POLLING_CALLS),
        takeWhile(() => this.isNetzbezugErstellungRunning),
        exhaustMap(() => this.massnahmenImportService.getImportSession())
      )
      .subscribe({
        next: session => {
          this.session = session as MassnahmenImportSessionView;
          if (this.schrittAbgeschlossen) {
            this.navigateToNextStep();
          }
          if (this.schrittAbgeschlossenOderHasFehler) {
            this.executing = false;
          }
          this.changeDetectorRef.markForCheck();
        },
        error: () => {
          this.executing = false;
          this.notifyUserService.warn('Fehler bei der Statusabfrage. Wurde der Import abgebrochen?');
          this.changeDetectorRef.markForCheck();
        },
      });
  }

  private navigateToNextStep(): void {
    this.massnahmenImportRoutingService.navigateToNext(ImportMassnahmenAttributfehlerUeberpruefenComponent.STEP);
  }
}
