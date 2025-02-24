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
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { ActivatedRoute } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { exhaustMap, startWith, take, takeWhile } from 'rxjs/operators';
import { MassnahmenDateianhaengeImportSessionView } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-view';
import {
  MassnahmenDateianhaengeImportDatei,
  MassnahmenDateianhaengeZuordnungStatus,
} from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-zuordnung';
import { SaveMassnahmenDateianhaengeCommand } from 'src/app/import/massnahmen-dateianhaenge/models/save-massnahmen-dateianhaenge-command';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import invariant from 'tiny-invariant';
import { MatomoTracker } from 'ngx-matomo-client';

export interface MassnahmenDateianhaengeDuplikateUeberpruefenRow {
  massnahmeId: number;
  massnahmeKonzeptId: string;
  datei: string;
  duplicate: boolean;
  selectionControl: FormControl<boolean>;
}

@Component({
  selector: 'rad-massnahmen-dateianhaenge-duplikate-ueberpruefen',
  templateUrl: './massnahmen-dateianhaenge-duplikate-ueberpruefen.component.html',
  styleUrl: './massnahmen-dateianhaenge-duplikate-ueberpruefen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class MassnahmenDateianhaengeDuplikateUeberpruefenComponent implements OnDestroy, DiscardableComponent {
  private static readonly STEP = 3;

  dataSource: MatTableDataSource<MassnahmenDateianhaengeDuplikateUeberpruefenRow>;
  displayedColumns = ['importieren', 'massnahmenId', 'datei', 'hinweis'];

  // Rows gruppiert nach MassnahmenKonzeptId
  massnahmen: Map<string, MassnahmenDateianhaengeDuplikateUeberpruefenRow[]>;

  session: MassnahmenDateianhaengeImportSessionView;
  pollingSubscription?: Subscription;

  isSaving = false;

  public get canContinue(): boolean {
    return this.dataSource.data.some(row => row.selectionControl.value);
  }

  get schrittAbgeschlossen(): boolean {
    return this.session?.schritt > MassnahmenDateianhaengeDuplikateUeberpruefenComponent.STEP;
  }

  constructor(
    private service: MassnahmenDateianhaengeService,
    private routingService: MassnahmenDateianhaengeRoutingService,
    private notifyUserService: NotifyUserService,
    private changeDetecor: ChangeDetectorRef,
    private dialog: MatDialog,
    private matomoTracker: MatomoTracker,
    route: ActivatedRoute
  ) {
    this.session = route.snapshot.data.session;
    this.massnahmen = new Map();
    this.dataSource = new MatTableDataSource<MassnahmenDateianhaengeDuplikateUeberpruefenRow>([]);
    this.resetForm(this.session);
  }

  canDiscard(): boolean {
    return (
      this.session.schritt > MassnahmenDateianhaengeDuplikateUeberpruefenComponent.STEP ||
      this.dataSource.data.every(d => d.selectionControl.pristine)
    );
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
  }

  onAbort(): void {
    this.service.deleteImportSession().subscribe(() => this.routingService.navigateToFirst());
  }

  onPrevious(): void {
    this.routingService.navigateToPrevious(MassnahmenDateianhaengeDuplikateUeberpruefenComponent.STEP);
  }

  onNext(): void {
    this.navigateToNextStep();
  }

  onSave(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question:
          'Fehlerhafte Dateien werden nicht übernommen. Bitte prüfen Sie vor dem Speichern alle Hinweise.<br><br>Dieser Schritt kann nicht rückgängig gemacht werden.',
        labelYes: 'Speichern',
        labelNo: 'Hinweise prüfen',
        inverseButtonColorCoding: true,
      } as QuestionYesNo,
    });

    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        this.isSaving = true;

        this.matomoTracker.trackEvent('Import', 'Abschließen', 'Maßnahmen-Dateianhänge');

        const commands: SaveMassnahmenDateianhaengeCommand[] = [];
        this.massnahmen.forEach((value, key) => {
          commands.push({
            massnahmeKonzeptId: key,
            dateien: value.map(e => {
              return {
                datei: e.datei,
                selected: e.selectionControl.value,
              };
            }),
          });
        });

        this.service.saveSelectedDateianhaengeCommand(commands).subscribe(() => {
          this.session.executing = true;
          this.startPolling();
        });
      }
    });
  }

  private navigateToNextStep(): void {
    this.routingService.navigateToNext(MassnahmenDateianhaengeDuplikateUeberpruefenComponent.STEP);
  }

  private resetForm(session: MassnahmenDateianhaengeImportSessionView): void {
    let rows: MassnahmenDateianhaengeDuplikateUeberpruefenRow[] = [];
    session.zuordnungen
      .filter(z => z.status === MassnahmenDateianhaengeZuordnungStatus.ZUGEORDNET)
      .forEach(zuordnung => {
        invariant(zuordnung.massnahmeId);
        const massnahmeId: number = zuordnung.massnahmeId;
        const massnahmenRows = zuordnung.dateien.map(datei =>
          this.convertDatei(massnahmeId, zuordnung.ordnername, datei)
        );
        this.massnahmen.set(zuordnung.ordnername, massnahmenRows);
        rows.push(...massnahmenRows);
      });

    rows = rows.sort((a, b) => {
      if (a.duplicate === b.duplicate) {
        return a.datei.localeCompare(b.datei);
      }
      return a.duplicate ? -1 : 1;
    });

    if (this.schrittAbgeschlossen) {
      rows.forEach(row => row.selectionControl.disable());
    }

    this.dataSource.data = rows;
  }

  private convertDatei(
    massnahmeId: number,
    massnahmeKonzeptId: string,
    datei: MassnahmenDateianhaengeImportDatei
  ): MassnahmenDateianhaengeDuplikateUeberpruefenRow {
    return {
      massnahmeId,
      massnahmeKonzeptId,
      datei: datei.dateiname,
      duplicate: datei.isDuplicate,
      selectionControl: new FormControl<boolean>(datei.isSelected, { nonNullable: true }),
    };
  }

  private startPolling(): void {
    this.pollingSubscription = interval(MassnahmenDateianhaengeService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        startWith(0),
        take(MassnahmenDateianhaengeService.MAX_POLLING_CALLS),
        takeWhile(() => this.isDateianhaengeAnwendenRunning()),
        exhaustMap(() => this.service.getImportSession())
      )
      .subscribe({
        next: session => {
          invariant(session);
          this.session = session;
          if (this.schrittAbgeschlossen) {
            this.isSaving = false;
            this.navigateToNextStep();
          }
          this.changeDetecor.markForCheck();
        },
        error: () => {
          this.notifyUserService.warn('Fehler bei der Statusabfrage. Wurde der Import abgebrochen?');
          this.isSaving = false;
          this.changeDetecor.markForCheck();
        },
      });
  }

  private isDateianhaengeAnwendenRunning(): boolean {
    return (
      this.session.schritt === MassnahmenDateianhaengeDuplikateUeberpruefenComponent.STEP && this.session.executing
    );
  }
}
