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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { interval, Observable, Subscription } from 'rxjs';
import { exhaustMap, startWith, take, takeWhile } from 'rxjs/operators';
import { MassnahmenImportMassnahmenAuswaehlenCommand } from 'src/app/import/massnahmen/models/massnahmen-import-massnahmen-auswaehlen-command';
import { MassnahmenImportNetzbezugAktualisierenCommand } from 'src/app/import/massnahmen/models/massnahmen-import-netzbezug-aktualisieren-command';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { MassnahmenImportZuordnungStatus } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-status';
import { MassnahmenImportZuordnungUeberpruefung } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-ueberpruefung';
import { MassnahmenImportZuordnungenService } from 'src/app/import/massnahmen/services/massnahmen-import-zuordnungen.service';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { Severity } from 'src/app/import/models/import-session-view';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { Geojson } from 'src/app/shared/models/geojson-geometrie';
import { Netzbezug } from 'src/app/shared/models/netzbezug';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NetzbezugAuswahlModusService } from 'src/app/shared/services/netzbezug-auswahl-modus.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import invariant from 'tiny-invariant';
import { MatomoTracker } from 'ngx-matomo-client';

export interface MassnahmenImportUeberpruefenRow {
  zuordnungId: number;
  massnahmeKonzeptId: string;
  status: string;
  netzbezugHinweise: string[];
  tooltip: string;
  hasFehler: boolean;
  selectionControl: FormControl<boolean>;
}

@Component({
  selector: 'rad-import-massnahmen-import-ueberpruefen',
  templateUrl: './import-massnahmen-import-ueberpruefen.component.html',
  styleUrl: './import-massnahmen-import-ueberpruefen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NetzbezugAuswahlModusService,
      useExisting: forwardRef(() => ImportMassnahmenImportUeberpruefenComponent),
    },
    MassnahmenImportZuordnungenService,
  ],
  standalone: false,
})
export class ImportMassnahmenImportUeberpruefenComponent
  implements NetzbezugAuswahlModusService, OnInit, OnDestroy, DiscardableComponent
{
  private static readonly STEP = 4;

  MassnahmenImportZuordnungStatus = MassnahmenImportZuordnungStatus;
  dataSource: MatTableDataSource<MassnahmenImportUeberpruefenRow> = new MatTableDataSource();
  displayedColumns = ['importieren', 'status', 'massnahmeKonzeptId', 'netzbezugHinweise'];
  loading = false;
  sessionAbort = false;

  private session: MassnahmenImportSessionView | null = null;
  private subscriptions: Subscription[] = [];

  public selektierteZuordnungsId$: Observable<number | undefined>;
  public formGroup: UntypedFormGroup;
  public netzbezugSelektionAktiv = false;
  public netzbezugSelektionLayerName = 'massnahmenImportNetzbezugSelektionLayer';
  public originalGeometrie: Geojson | null = null;
  public executing = false;

  constructor(
    private massnahmenImportService: MassnahmenImportService,
    private massnahmenImportRoutingService: MassnahmenImportRoutingService,
    private massnahmenImportZuordnungenService: MassnahmenImportZuordnungenService,
    private changeDetectorRef: ChangeDetectorRef,
    private notifyUserService: NotifyUserService,
    private errorHandlingService: ErrorHandlingService,
    private dialog: MatDialog,
    private matomoTracker: MatomoTracker
  ) {
    this.selektierteZuordnungsId$ = this.massnahmenImportZuordnungenService.selektierteZuordnungsId$;

    this.formGroup = new FormGroup({
      netzbezug: new FormControl<Netzbezug | null>(null),
    });

    this.subscriptions.push(
      this.massnahmenImportZuordnungenService.zuordnungen$.subscribe(zuordnungen => {
        this.dataSource = new MatTableDataSource<MassnahmenImportUeberpruefenRow>(this.convertZuordnungen(zuordnungen));
        this.dataSource.data.sort((a: MassnahmenImportUeberpruefenRow, b: MassnahmenImportUeberpruefenRow) => {
          // Negative Werte werden nach oben sortiert. Daher -2=Fehler, -1=hinweise und 0=normal.
          const aValue = a.hasFehler ? -2 : a.netzbezugHinweise.length > 0 ? -1 : 0;
          const bValue = b.hasFehler ? -2 : b.netzbezugHinweise.length > 0 ? -1 : 0;

          if (aValue === bValue) {
            // Bei gleichen Werten (beide Fehler, beide Hinweise oder beide weder noch) nach ID sortieren.
            return (a.massnahmeKonzeptId ?? '').localeCompare(b.massnahmeKonzeptId);
          }

          return aValue - bValue;
        });
      }),
      this.selektierteZuordnungsId$.subscribe(() => {
        this.resetFormGroup();
        this.originalGeometrie = this.massnahmenImportZuordnungenService.selektierteZuordnungsOriginalGeometrie;
      }),
      this.massnahmenImportService.getImportSession().subscribe(session => {
        this.session = session;
        if (session) {
          if (this.schrittAbgeschlossen) {
            this.formGroup.disable();
          } else if (this.isSpeichernRunning) {
            this.startPolling();
          }
          this.loading = false;
          this.changeDetectorRef.detectChanges();
        }
      })
    );
  }

  ngOnInit(): void {
    this.ladeZuordnungenTabelle();
  }

  private ladeZuordnungenTabelle(): void {
    this.loading = true;
    this.changeDetectorRef.detectChanges();
    this.massnahmenImportService.getZuordnungUeberpruefung().subscribe({
      next: zuordnungen => {
        this.massnahmenImportZuordnungenService.updateZuordnungen(zuordnungen);
      },
      complete: () => {
        this.loading = false;
        this.resetFormGroup();
        this.changeDetectorRef.detectChanges();
      },
    });
  }

  private resetFormGroup(): void {
    this.formGroup.reset({
      netzbezug: this.massnahmenImportZuordnungenService.selektierterZuordnungsNetzbezug,
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  onAbort(): void {
    this.sessionAbort = true;

    this.massnahmenImportService.deleteImportSession().subscribe(() => {
      this.massnahmenImportRoutingService.navigateToFirst();
    });
  }

  onPrevious(): void {
    this.massnahmenImportRoutingService.navigateToPrevious(ImportMassnahmenImportUeberpruefenComponent.STEP);
  }

  onNext(): void {
    this.navigateToNextStep();
  }

  onStart(): void {
    invariant(!this.schrittAbgeschlossen);
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question:
          'Fehlerhafte Maßnahmen werden nicht oder nur teilweise übernommen. Bitte prüfen Sie vor dem Speichern alle Hinweise.<br><br>Dieser Schritt kann nicht rückgängig gemacht werden.',
        labelYes: 'Speichern',
        labelNo: 'Hinweise prüfen',
        inverseButtonColorCoding: true,
      } as QuestionYesNo,
    });
    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        this.executing = true;

        this.matomoTracker.trackEvent('Import', 'Abschließen', 'Maßnahmen');

        const selektierteZuordnungenIds = this.dataSource.data
          .filter(zuordnung => zuordnung.selectionControl.value)
          .map(zuordnung => zuordnung.zuordnungId);

        const command = {
          zuordnungenIds: selektierteZuordnungenIds,
        } as MassnahmenImportMassnahmenAuswaehlenCommand;

        this.massnahmenImportService.massnahmenSpeichern(command).subscribe({
          next: () => {
            if (this.session) {
              this.session.executing = true;
            }
            this.startPolling();
          },
          error: err => {
            this.errorHandlingService.handleHttpError(err);
            this.formGroup.enable();
            this.changeDetectorRef.markForCheck();
            this.executing = false;
          },
        });
      }
    });
  }

  canDiscard(): boolean {
    return (
      (this.formGroup.pristine && this.dataSource.data.every(entry => entry.selectionControl.pristine)) ||
      this.schrittAbgeschlossen
    );
  }

  get anyZuordnungMarkedForSave(): boolean {
    return this.dataSource.data.some(row => row.selectionControl.value);
  }

  get anyZuordnungSaveable(): boolean {
    return this.dataSource.data.some(row => row.selectionControl.enabled);
  }

  get isSpeichernRunning(): boolean {
    return this.session?.schritt === ImportMassnahmenImportUeberpruefenComponent.STEP && this.session?.executing;
  }

  get schrittAbgeschlossen(): boolean {
    return (this.session && this.session.schritt > ImportMassnahmenImportUeberpruefenComponent.STEP) ?? false;
  }

  get fehler(): string[] {
    return this.session?.log.filter(l => l.severity === Severity.ERROR).map(l => l.fehlerBeschreibung) || [];
  }

  get hasFehler(): boolean {
    return this.fehler.length > 0;
  }

  get schrittAbgeschlossenOderHasFehler(): boolean {
    return this.schrittAbgeschlossen || this.hasFehler;
  }

  public get isSelektierteZuordnungEditierbar(): boolean {
    return (
      !!this.massnahmenImportZuordnungenService.selektierteZuordnung &&
      this.massnahmenImportZuordnungenService.selektierteZuordnung?.status !== MassnahmenImportZuordnungStatus.GELOESCHT
    );
  }

  private convertZuordnungen(zuordnungen: MassnahmenImportZuordnungUeberpruefung[]): MassnahmenImportUeberpruefenRow[] {
    return zuordnungen.map(zuordnung => {
      const hasFehler = zuordnung.netzbezugHinweise.some(h => h.severity === 'ERROR');
      return {
        status: zuordnung.status,
        zuordnungId: zuordnung.id,
        massnahmeKonzeptId: zuordnung.massnahmeKonzeptId,
        netzbezugHinweise: [...new Set(zuordnung.netzbezugHinweise.map(h => h.text)).values()],
        tooltip: [...new Set(zuordnung.netzbezugHinweise.map(h => h.tooltip)).values()].join(`\n\n`),
        hasFehler: hasFehler,
        selectionControl: new FormControl<boolean>(
          {
            value: zuordnung.selected,
            disabled: hasFehler || this.schrittAbgeschlossen,
          },
          { nonNullable: true }
        ),
      } as MassnahmenImportUeberpruefenRow;
    });
  }

  public selektiereZuordnungsId(zuordnungsId: number): void {
    if (!this.netzbezugSelektionAktiv) {
      if (this.massnahmenImportZuordnungenService.selektierteZuordnungsId === zuordnungsId) {
        // Ein Klick auf die gleiche Zeile in der Tabelle deselektiert die aktuell ausgewählte Maßnahmen zuordnung
        this.massnahmenImportZuordnungenService.deselektiereZuordnung();
      } else {
        this.massnahmenImportZuordnungenService.selektiereZuordnung(zuordnungsId);
      }
    }
  }

  public startNetzbezugAuswahl(): void {
    this.netzbezugSelektionAktiv = true;
    this.changeDetectorRef.detectChanges();
  }

  public stopNetzbezugAuswahl(): void {
    if (!this.sessionAbort) {
      this.netzbezugSelektionAktiv = false;
      invariant(this.massnahmenImportZuordnungenService.selektierteZuordnungsId);

      if (this.editorHasBeenUsed()) {
        this.loading = true;
        this.formGroup.disable();
        this.changeDetectorRef.detectChanges();

        this.massnahmenImportService
          .netzbezugAktualisieren({
            massnahmenImportZuordnungId: this.massnahmenImportZuordnungenService.selektierteZuordnungsId,
            netzbezug: this.formGroup.get('netzbezug')?.value,
          } as MassnahmenImportNetzbezugAktualisierenCommand)
          .subscribe({
            next: () => {
              this.ladeZuordnungenTabelle();
              this.formGroup.enable();
              this.changeDetectorRef.detectChanges();
            },
            error: err => {
              this.errorHandlingService.handleHttpError(err);
              this.formGroup.enable();
              this.loading = false;
              this.changeDetectorRef.detectChanges();
            },
          });
      } else {
        this.changeDetectorRef.detectChanges();
      }
    }
  }

  private editorHasBeenUsed(): boolean {
    const neuerNetzbezug = this.formGroup.get('netzbezug')?.value;
    const alterNetzbezug = this.massnahmenImportZuordnungenService.selektierterZuordnungsNetzbezug;

    // Prüft NICHT, ob der Inhalt sich fachlich geändert hat, nur ob der Editor überhaupt benutzt wurde.
    return neuerNetzbezug !== alterNetzbezug;
  }

  private navigateToNextStep(): void {
    this.massnahmenImportRoutingService.navigateToNext(ImportMassnahmenImportUeberpruefenComponent.STEP);
  }

  private startPolling(): void {
    this.subscriptions.push(
      interval(MassnahmenImportService.POLLING_INTERVALL_IN_MILLISECONDS)
        .pipe(
          startWith(0),
          take(MassnahmenImportService.MAX_POLLING_CALLS),
          takeWhile(() => this.isSpeichernRunning),
          exhaustMap(() => this.massnahmenImportService.getImportSession())
        )
        .subscribe({
          next: session => {
            this.session = session!;
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
        })
    );
  }
}
