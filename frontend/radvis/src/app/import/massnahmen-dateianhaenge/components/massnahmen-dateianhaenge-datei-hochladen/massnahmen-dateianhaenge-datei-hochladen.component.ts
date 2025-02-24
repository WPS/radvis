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
import { MassnahmenDateianhaengeImportSessionView } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-view';
import { StartMassnahmenDateianhaengeImportSessionCommand } from 'src/app/import/massnahmen-dateianhaenge/models/start-massnahmen-dateianhaenge-import-session-command';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';
import { Severity } from 'src/app/import/models/import-session-view';
import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import { Konzeptionsquelle } from 'src/app/viewer/massnahme/models/konzeptionsquelle';
import { SollStandard } from 'src/app/viewer/massnahme/models/soll-standard';

@Component({
  selector: 'rad-massnahmen-dateianhaenge-datei-hochladen',
  templateUrl: './massnahmen-dateianhaenge-datei-hochladen.component.html',
  styleUrl: './massnahmen-dateianhaenge-datei-hochladen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class MassnahmenDateianhaengeDateiHochladenComponent implements OnDestroy {
  private static readonly STEP = 1;

  formGroup: UntypedFormGroup;

  pollingSubscription: Subscription | undefined;
  sessionExists = false;

  zuweisbareOrganisationen$: Promise<Verwaltungseinheit[]>;
  konzeptionsquelleOptions = Konzeptionsquelle.options;
  sollStandardOptions = SollStandard.options;

  uploading = false;
  session: MassnahmenDateianhaengeImportSessionView | null = null;
  sessionCreated = false;

  get massnahmenDateianhaengeSessionExists(): boolean {
    return !!this.session;
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

  get isDateiHochladenRunning(): boolean {
    return this.session?.schritt === 1 && this.session.executing;
  }

  private get schrittAbgeschlossen(): boolean {
    return (this.session && this.session.schritt > MassnahmenDateianhaengeDateiHochladenComponent.STEP) ?? false;
  }

  constructor(
    private service: MassnahmenDateianhaengeService,
    private routingService: MassnahmenDateianhaengeRoutingService,
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

    service.existsImportSession().subscribe(exists => {
      this.sessionExists = exists;
      if (exists) {
        this.formGroup.disable();
      }
      this.changeDetectorRef.markForCheck();
    });

    this.service.getImportSession().subscribe(session => {
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
    if (this.session && this.session.schritt > MassnahmenDateianhaengeDateiHochladenComponent.STEP) {
      this.navigateToNextStep();
      return;
    }

    this.uploading = true;
    const gebietskoerperschaften = this.formGroup.value.gebietskoerperschaften.map((v: Verwaltungseinheit) => v.id);
    if (!this.session) {
      this.session = {
        gebietskoerperschaften,
        executing: true,
        konzeptionsquelle: this.formGroup.value.konzeptionsquelle,
        sollStandard: this.formGroup.value.sollStandard,
        log: [],
        schritt: 1,
        zuordnungen: [],
      };
    }
    this.service
      .createSessionAndStartMassnahmenDateianhaengeImport(
        {
          gebietskoerperschaften: gebietskoerperschaften,
          konzeptionsquelle: this.formGroup.value.konzeptionsquelle,
          sollStandard: this.formGroup.value.sollStandard,
        } as StartMassnahmenDateianhaengeImportSessionCommand,
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
    this.routingService.navigateToNext(MassnahmenDateianhaengeDateiHochladenComponent.STEP);
  }

  private startPolling(): void {
    this.pollingSubscription = interval(MassnahmenDateianhaengeService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        startWith(0),
        take(MassnahmenDateianhaengeService.MAX_POLLING_CALLS),
        takeWhile(() => this.isDateiHochladenRunning),
        exhaustMap(() => this.service.getImportSession())
      )
      .subscribe({
        next: session => {
          this.session = session;
          this.sessionExists = true;
          if (this.schrittAbgeschlossen) {
            this.navigateToNextStep();
          }
          if (this.schrittAbgeschlossenOderHasFehler) {
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
    this.service.deleteImportSession().subscribe(() => {
      this.formGroup.enable();
      this.formGroup.reset();
      this.sessionExists = false;
      this.sessionCreated = false;
      this.session = null;
      this.changeDetectorRef.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
  }
}
