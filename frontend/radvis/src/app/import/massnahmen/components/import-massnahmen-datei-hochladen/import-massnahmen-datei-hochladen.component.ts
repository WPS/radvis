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
import { FormControl, UntypedFormGroup } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { exhaustMap, startWith, take, takeWhile } from 'rxjs/operators';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { StartMassnahmenImportSessionCommand } from 'src/app/import/massnahmen/models/start-massnahmen-import-session-command';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { Severity } from 'src/app/import/models/import-session-view';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-import-massnahmen-datei-hochladen',
  templateUrl: './import-massnahmen-datei-hochladen.component.html',
  styleUrl: './import-massnahmen-datei-hochladen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportMassnahmenDateiHochladenComponent implements OnDestroy {
  private static readonly STEP = 1;

  formGroup: UntypedFormGroup;

  pollingSubscription: Subscription | undefined;
  /**
   * Ist auch true, wenn es sich um Session eines anderen Typs handelt
   */
  sessionExists = false;

  zuweisbareOrganisationen$: Promise<Verwaltungseinheit[]>;
  konzeptionsquelleOptions = Konzeptionsquelle.options;
  sollStandardOptions = SollStandard.options;

  uploading = false;
  session: MassnahmenImportSessionView | null = null;
  sessionCreated: boolean = false;

  get massnahmenSessionExists(): boolean {
    return Boolean(this.session);
  }

  get fehler(): string[] {
    return this.session?.log.filter(l => l.severity === Severity.ERROR).map(l => l.fehlerBeschreibung) || [];
  }

  get hasFehler(): boolean {
    return this.fehler.length > 0;
  }

  get isDateiHochladenRunning(): boolean {
    return this.session?.schritt === ImportMassnahmenDateiHochladenComponent.STEP && this.session?.executing;
  }

  private get schrittAbgeschlossen(): boolean {
    return (this.session && this.session.schritt > ImportMassnahmenDateiHochladenComponent.STEP) ?? false;
  }

  get schrittAbgeschlossenOderFehler(): boolean {
    return this.schrittAbgeschlossen || this.hasFehler;
  }

  constructor(
    private massnahmenImportService: MassnahmenImportService,
    private massnahmenImportRoutingService: MassnahmenImportRoutingService,
    private organisationenService: OrganisationenService,
    private notifyUserService: NotifyUserService,
    private errorHandlingService: ErrorHandlingService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.zuweisbareOrganisationen$ = this.organisationenService.getGebietskoerperschaften();

    this.formGroup = new UntypedFormGroup({
      gebietskoerperschaften: new FormControl<Verwaltungseinheit[]>([], RadvisValidators.isNotEmpty),
      konzeptionsquelle: new FormControl<Konzeptionsquelle | null>(null, RadvisValidators.isNotNullOrEmpty),
      sollStandard: new FormControl<SollStandard | null>(null),
      file: new FormControl<File | null>(null, RadvisValidators.isNotNullOrEmpty),
    });

    massnahmenImportService.existsImportSession().subscribe(exists => {
      this.sessionExists = exists;
      if (exists) {
        this.formGroup.disable();
      }
      this.changeDetectorRef.markForCheck();
    });

    this.massnahmenImportService.getImportSession().subscribe(session => {
      if (session) {
        this.formGroup.disable();
        this.session = session;
        if (this.isDateiHochladenRunning) {
          this.startPolling();
        }

        this.formGroup.patchValue({
          konzeptionsquelle: session.konzeptionsquelle,
          sollStandard: session.sollStandard,
        });
        this.zuweisbareOrganisationen$.then(organisationen => {
          const gebietskoerperschaften = organisationen.filter(org =>
            session.gebietskoerperschaften.find(id => org.id === id)
          );
          this.formGroup.patchValue({
            gebietskoerperschaften: gebietskoerperschaften,
          });
          this.changeDetectorRef.markForCheck();
        });
      }
      changeDetectorRef.markForCheck();
    });
  }

  onStart(): void {
    this.formGroup.disable();
    invariant(!this.session);

    this.uploading = true;
    const gebietskoerperschaften = this.formGroup.value.gebietskoerperschaften.map((v: Verwaltungseinheit) => v.id);
    this.session = {
      gebietskoerperschaften,
      executing: true,
      konzeptionsquelle: this.formGroup.value.konzeptionsquelle,
      sollStandard: this.formGroup.value.sollStandard,
      log: [],
      schritt: 1,
      attribute: [],
    };

    this.massnahmenImportService
      .createSessionAndStartMassnahmenImport(
        {
          gebietskoerperschaften: gebietskoerperschaften,
          konzeptionsquelle: this.formGroup.value.konzeptionsquelle,
          sollStandard: this.formGroup.value.sollStandard,
        } as StartMassnahmenImportSessionCommand,
        this.formGroup.value.file
      )
      .subscribe({
        next: () => {
          this.startPolling();
          this.sessionCreated = true;
          this.changeDetectorRef.markForCheck();
        },
        error: err => {
          this.errorHandlingService.handleHttpError(err);
          this.sessionCreated = false;
          this.session = null;
          this.formGroup.enable();
          this.uploading = false;
          this.changeDetectorRef.markForCheck();
        },
      });
  }

  onNext(): void {
    this.navigateToNextStep();
  }

  private navigateToNextStep(): void {
    this.massnahmenImportRoutingService.navigateToNext(ImportMassnahmenDateiHochladenComponent.STEP);
  }

  private startPolling(): void {
    this.pollingSubscription = interval(MassnahmenImportService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        startWith(0),
        take(MassnahmenImportService.MAX_POLLING_CALLS),
        takeWhile(() => this.isDateiHochladenRunning),
        exhaustMap(() => this.massnahmenImportService.getImportSession())
      )
      .subscribe({
        next: session => {
          this.session = session as MassnahmenImportSessionView;
          this.sessionExists = true;
          if (this.schrittAbgeschlossen) {
            this.navigateToNextStep();
          }
          if (this.schrittAbgeschlossenOderFehler) {
            this.uploading = false;
          }
          this.changeDetectorRef.markForCheck();
        },
        error: () => {
          this.uploading = false;
          this.notifyUserService.warn('Fehler bei der Statusabfrage. Wurde der Import abgebrochen?');
          this.changeDetectorRef.markForCheck();
        },
      });
  }

  onAbort(): void {
    this.massnahmenImportService.deleteImportSession().subscribe(() => {
      this.formGroup.enable();
      this.formGroup.reset();
      this.sessionExists = false;
      this.sessionCreated = false;
      this.session = null;
      this.pollingSubscription?.unsubscribe();
      this.changeDetectorRef.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
  }
}
