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
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { interval, Subscription } from 'rxjs';
import { exhaustMap, take, takeWhile } from 'rxjs/operators';
import { AttributeImportSessionView } from 'src/app/editor/manueller-import/models/attribute-import-session-view';
import { ImportSessionStatus } from 'src/app/editor/manueller-import/models/import-session-status';
import { ImportSessionView, Severity } from 'src/app/editor/manueller-import/models/import-session-view';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { Property } from 'src/app/editor/manueller-import/models/property';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { FehlerprotokollService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import invariant from 'tiny-invariant';

export interface Attribut {
  key: string;
  value: any;
}

@Component({
  selector: 'rad-import-attribute-abschliessen',
  templateUrl: './import-attribute-abschliessen.component.html',
  styleUrls: ['./import-attribute-abschliessen.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportAttributeAbschliessenComponent implements OnDestroy {
  public previousLink: string;
  public session: AttributeImportSessionView | null = null;
  public netzFetching = true;
  public radvisFeatures$: Promise<GeoJSONFeatureCollection> | undefined;
  public featureMappings$: Promise<GeoJSONFeatureCollection> | undefined;

  public ausgewaehlteKonflikte: Property[][] = [];
  fehlerProtokollDownloadLink: string | null = null;

  private pollingSubscription: Subscription = Subscription.EMPTY;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private importRoutingService: ManuellerImportRoutingService,
    private radVisNetzFeatureService: NetzausschnittService,
    private manuellerImportService: ManuellerImportService,
    private changeDetectorRef: ChangeDetectorRef,
    private notifyUserService: NotifyUserService,
    private organisationenService: OrganisationenService,
    private fehlerprotokollService: FehlerprotokollService
  ) {
    this.previousLink =
      '../' + importRoutingService.getRouteForStep(route.snapshot.data.step - 1, ImportTyp.ATTRIBUTE_UEBERNEHMEN);

    this.manuellerImportService
      .getImportSession()
      .toPromise()
      .then(session => {
        this.session = session as AttributeImportSessionView;
        if (this.isExecuting) {
          this.startPollingImportStatus();
        }
        this.organisationenService.getOrganisation(this.session?.organisationsID).then(orga => {
          this.fehlerProtokollDownloadLink = this.fehlerprotokollService.getFehlerprotokollDownloadLink(
            orga,
            'attribute'
          );
        });
        this.radvisFeatures$ = this.radVisNetzFeatureService
          .getKantenFuerZustaendigkeitsbereich(session.organisationsID)
          .then(features => {
            this.featureMappings$ = this.manuellerImportService.getFeatureMappings();
            changeDetectorRef.markForCheck();
            return features;
          });
      });
  }

  ngOnDestroy(): void {
    this.pollingSubscription.unsubscribe();
  }

  onAbort(): void {
    this.manuellerImportService.deleteImportSession().then(() => {
      this.router.navigate(['../' + this.importRoutingService.getStartStepRoute()], {
        relativeTo: this.route,
        queryParamsHandling: 'merge',
      });
    });
  }

  get isExecutable(): boolean {
    return this.session?.status === ImportSessionStatus.AUTOMATISCHE_ABBILDUNG_DONE;
  }

  get isExecuting(): boolean {
    return this.session?.status === ImportSessionStatus.UPDATE_EXECUTING;
  }

  get isDone(): boolean {
    return this.session?.status === ImportSessionStatus.UPDATE_DONE;
  }

  get hasFehler(): boolean {
    return this.session?.log.some(l => l.severity === Severity.ERROR) || false;
  }

  get fehler(): string {
    return this.session?.log.find(l => l.severity === Severity.ERROR)?.fehlerBeschreibung || '';
  }

  get konflikteAusgewaehlt(): boolean {
    return this.ausgewaehlteKonflikte.length > 0;
  }

  onExecute(): void {
    invariant(this.session);
    this.session.status = ImportSessionStatus.UPDATE_EXECUTING;

    this.manuellerImportService.executeAttributeUebernehmen().catch(() => {
      this.notifyUserService.warn('Bei der Attributübernahme ist ein unerwarteter Fehler aufgetreten');
    });
    this.startPollingImportStatus();
  }

  onDone(): void {
    this.manuellerImportService.deleteImportSession().then(() => {
      this.router.navigate(['../' + this.importRoutingService.getStartStepRoute()], {
        relativeTo: this.route,
        queryParamsHandling: 'merge',
      });
    });
  }

  onKonfliktkanteAusgewaehlt(event: Property[][]): void {
    this.ausgewaehlteKonflikte = event;
  }

  onCloseKonflikte(): void {
    this.ausgewaehlteKonflikte = [];
  }

  onLoaded(): void {
    this.netzFetching = false;
  }

  private startPollingImportStatus(): void {
    this.pollingSubscription = interval(ManuellerImportService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        take(ManuellerImportService.MILLISECONDS_IN_HOUR / ManuellerImportService.POLLING_INTERVALL_IN_MILLISECONDS),
        takeWhile(() => !this.session || !this.isDone),
        exhaustMap(() => this.manuellerImportService.getImportSession())
      )
      .subscribe({
        next: this.handleSessionupdate,
        error: () => {
          this.notifyUserService.warn('Fehler bei der Statusabfrage. Wurde der Import abgebrochen?');
        },
      });
  }

  private readonly handleSessionupdate: any = (session: ImportSessionView) => {
    const oldSession = this.session;
    this.session = session as AttributeImportSessionView;
    if (this.isDone) {
      this.changeDetectorRef.markForCheck();
    } else if (oldSession?.status === ImportSessionStatus.UPDATE_EXECUTING && this.isExecutable) {
      this.notifyUserService.warn(
        'Kanten in der Organisation wurden während des Speicherns aus einer anderen Quelle verändert. Bitte versuchen Sie es erneut.'
      );
      this.changeDetectorRef.markForCheck();
    }
  };
}