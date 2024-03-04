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
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { Subscription, interval } from 'rxjs';
import { exhaustMap, take, takeWhile } from 'rxjs/operators';
import { FehlerprotokollService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll.service';
import { Severity } from 'src/app/import/models/import-session-view';
import { NetzklassenImportSessionView } from 'src/app/import/netzklassen/models/netzklassen-import-session-view';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-import-netzklasse-abschliessen',
  templateUrl: './import-netzklasse-abschliessen.component.html',
  styleUrls: ['./import-netzklasse-abschliessen.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportNetzklasseAbschliessenComponent implements OnDestroy {
  private static readonly STEP = 5;

  public alleFeatures$: Promise<GeoJSONFeatureCollection> | undefined;
  public session: NetzklassenImportSessionView | null = null;

  public kanteIdsMitNetzklasse: number[] = [];
  public fetchingFeatures = true;

  pollingSubscription: Subscription = Subscription.EMPTY;
  fehlerProtokollDownloadLink: string | null = null;

  constructor(
    private netzklassenImportService: NetzklassenImportService,
    private netzklassenRoutingService: NetzklassenRoutingService,
    private radVisNetzFeatureService: NetzausschnittService,
    private changeDetectorRef: ChangeDetectorRef,
    private notifyUserService: NotifyUserService,
    private organisationenService: OrganisationenService,
    private fehlerprotokollService: FehlerprotokollService
  ) {
    this.netzklassenImportService.getImportSession().subscribe(session => {
      invariant(session);
      this.session = session;
      if (session.executing) {
        this.startPollingImportStatus();
      }
      this.organisationenService.getOrganisation(this.session?.organisationsID).then(orga => {
        this.fehlerProtokollDownloadLink = this.fehlerprotokollService.getFehlerprotokollDownloadLink(
          orga,
          'netzklasse'
        );
      });
      this.changeDetectorRef.markForCheck();
      this.alleFeatures$ = this.radVisNetzFeatureService
        .getKantenFuerZustaendigkeitsbereich(session.organisationsID, session.netzklasse)
        .then(alleFeatures => {
          this.fetchingFeatures = false;
          changeDetectorRef.markForCheck();
          return alleFeatures;
        });
    });

    this.netzklassenImportService
      .getKanteIdsMitNetzklasse()
      .then(kanteIdsMitNetzklasse => (this.kanteIdsMitNetzklasse = kanteIdsMitNetzklasse));
  }

  onAbort(): void {
    this.netzklassenImportService.deleteImportSession().subscribe(() => {
      this.netzklassenRoutingService.navigateToFirst();
    });
  }

  get isExecutable(): boolean {
    return this.session?.schritt === 5 && !this.isExecuting;
  }

  get isExecuting(): boolean {
    return this.session?.executing || false;
  }

  get isDone(): boolean {
    return this.session?.schritt === 0;
  }

  get hasFehler(): boolean {
    return this.session?.log.some(l => l.severity === Severity.ERROR) || false;
  }

  get fehler(): string {
    return this.session?.log.find(l => l.severity === Severity.ERROR)?.fehlerBeschreibung || '';
  }

  onExecute(): void {
    invariant(this.session);
    this.session.executing = true;

    this.netzklassenImportService.executeNetzklassenZuweisen().catch(() => {
      invariant(this.session);
      this.notifyUserService.warn('Bei der Durchführung des Imports ist ein unerwarteter Fehler aufgetreten');
    });
    this.startPollingImportStatus();
  }

  onDone(): void {
    this.netzklassenImportService.deleteImportSession().subscribe(() => {
      this.netzklassenRoutingService.navigateToFirst();
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription.unsubscribe();
  }

  private startPollingImportStatus(): void {
    this.pollingSubscription = interval(NetzklassenImportService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        take(NetzklassenImportService.MAX_POLLING_CALLS),
        takeWhile(() => !this.session || !this.isDone),
        exhaustMap(() => this.netzklassenImportService.getImportSession())
      )
      .subscribe({
        next: session => {
          invariant(session);
          this.handleSessionupdate(session);
        },
        error: () => {
          this.notifyUserService.warn('Fehler bei der Statusabfrage. Wurde der Import abgebrochen?');
        },
      });
  }

  private handleSessionupdate(session: NetzklassenImportSessionView): void {
    const oldSession = this.session;
    this.session = session;
    if (this.isDone) {
      this.changeDetectorRef.markForCheck();
    } else if (oldSession?.executing && this.isExecutable) {
      this.notifyUserService.warn(
        'Kanten in der Organisation wurden während des Speicherns durch einen anderen Prozess verändert. Bitte versuchen Sie es erneut.'
      );
      this.changeDetectorRef.markForCheck();
    }
  }

  onPrevious(): void {
    this.netzklassenRoutingService.navigateToPrevious(ImportNetzklasseAbschliessenComponent.STEP);
  }
}
