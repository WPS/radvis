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
import { interval, Subscription } from 'rxjs';
import { exhaustMap, take, takeWhile } from 'rxjs/operators';
import { FehlerprotokollService } from 'src/app/fehlerprotokoll/services/fehlerprotokoll.service';
import { AttributeImportSessionView } from 'src/app/import/attribute/models/attribute-import-session-view';
import { Property } from 'src/app/import/attribute/models/property';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { AttributeRoutingService } from 'src/app/import/attribute/services/attribute-routing.service';
import { Severity } from 'src/app/import/models/import-session-view';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OrganisationenService } from 'src/app/shared/services/organisationen.service';
import invariant from 'tiny-invariant';
import { MatomoTracker } from 'ngx-matomo-client';

@Component({
  selector: 'rad-import-attribute-abschliessen',
  templateUrl: './import-attribute-abschliessen.component.html',
  styleUrls: ['./import-attribute-abschliessen.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportAttributeAbschliessenComponent implements OnDestroy {
  private static readonly STEP = 5;

  public session: AttributeImportSessionView | null = null;
  public netzFetching = true;
  public radvisFeatures$: Promise<GeoJSONFeatureCollection> | undefined;
  public featureMappings$: Promise<GeoJSONFeatureCollection> | undefined;

  public ausgewaehlteKonflikte: Property[][] = [];
  fehlerProtokollDownloadLink: string | null = null;

  private pollingSubscription: Subscription = Subscription.EMPTY;

  constructor(
    private attributeImportService: AttributeImportService,
    private attributeRoutingService: AttributeRoutingService,
    private radVisNetzFeatureService: NetzausschnittService,
    private changeDetectorRef: ChangeDetectorRef,
    private notifyUserService: NotifyUserService,
    private organisationenService: OrganisationenService,
    private fehlerprotokollService: FehlerprotokollService,
    private matomoTracker: MatomoTracker
  ) {
    this.attributeImportService.getImportSession().subscribe(session => {
      invariant(session);
      this.session = session;
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
          this.featureMappings$ = this.attributeImportService.getFeatureMappings();
          changeDetectorRef.markForCheck();
          return features;
        });
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription.unsubscribe();
  }

  onPrevious(): void {
    this.attributeRoutingService.navigateToPrevious(ImportAttributeAbschliessenComponent.STEP);
  }

  onAbort(): void {
    this.attributeImportService.deleteImportSession().subscribe(() => {
      this.attributeRoutingService.navigateToFirst();
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

  get konflikteAusgewaehlt(): boolean {
    return this.ausgewaehlteKonflikte.length > 0;
  }

  onExecute(): void {
    invariant(this.session);
    this.session.executing = true;

    this.matomoTracker.trackEvent('Import', 'Abschließen', 'Attribute');

    this.attributeImportService.executeAttributeUebernehmen().catch(() => {
      this.notifyUserService.warn('Bei der Attributübernahme ist ein unerwarteter Fehler aufgetreten');
    });
    this.startPollingImportStatus();
  }

  onDone(): void {
    this.attributeImportService.deleteImportSession().subscribe(() => {
      this.attributeRoutingService.navigateToFirst();
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
    this.pollingSubscription = interval(AttributeImportService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        take(AttributeImportService.MAX_POLLING_CALLS),
        takeWhile(() => !this.session || !this.isDone),
        exhaustMap(() => this.attributeImportService.getImportSession())
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

  private handleSessionupdate(session: AttributeImportSessionView): void {
    const oldSession = this.session;
    this.session = session;
    if (this.isDone) {
      this.changeDetectorRef.markForCheck();
    } else if (oldSession?.executing && this.isExecutable) {
      this.notifyUserService.warn(
        'Kanten in der Organisation wurden während des Speicherns aus einer anderen Quelle verändert. Bitte versuchen Sie es erneut.'
      );
      this.changeDetectorRef.markForCheck();
    }
  }
}
