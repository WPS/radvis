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
import { Observable, Subject } from 'rxjs';
import { concatAll } from 'rxjs/operators';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { NetzklassenImportSessionView } from 'src/app/editor/manueller-import/models/netzklassen-import-session-view';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';

@Component({
  selector: 'rad-import-abbildung-bearbeiten',
  templateUrl: './import-netzklasse-abbildung-bearbeiten.component.html',
  styleUrls: ['./import-netzklasse-abbildung-bearbeiten.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportNetzklasseAbbildungBearbeitenComponent implements OnDestroy {
  public previousLink: string;
  public nextLink: string;
  public fetching = true;
  public kantenFuerZustaendigkeitsbereich$: Promise<GeoJSONFeatureCollection>;

  public kanteIdsMitNetzklasse: number[] | null = null;
  public featuresWithUnchangedNetzklasseVisible = false;

  netz$: Promise<Netzklasse>;
  // Serialisierung der Toggle Requests
  private toggleRequests$: Subject<Observable<number[]>> = new Subject();

  constructor(
    private manuellerImportService: ManuellerImportService,
    private importRoutingService: ManuellerImportRoutingService,
    private radVisNetzFeatureService: NetzausschnittService,
    private route: ActivatedRoute,
    private router: Router,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.previousLink = `../${importRoutingService.getRouteForStep(
      route.snapshot.data.step - 1,
      ImportTyp.NETZKLASSE_ZUWEISEN
    )}`;
    this.nextLink = `../${importRoutingService.getRouteForStep(
      route.snapshot.data.step + 1,
      ImportTyp.NETZKLASSE_ZUWEISEN
    )}`;

    const session$ = this.manuellerImportService
      .getImportSession()
      .toPromise()
      .then(session => session as NetzklassenImportSessionView);

    this.netz$ = session$.then(session => session.netzklasse as Netzklasse);

    this.kantenFuerZustaendigkeitsbereich$ = session$.then(session =>
      this.radVisNetzFeatureService.getKantenFuerZustaendigkeitsbereich(session.organisationsID, session.netzklasse)
    );

    const kanteIdsMitNetzklasse$ = this.manuellerImportService
      .getKanteIdsMitNetzklasse()
      .then(kanteIdsMitNetzklasse => (this.kanteIdsMitNetzklasse = kanteIdsMitNetzklasse));
    Promise.all([this.kantenFuerZustaendigkeitsbereich$, kanteIdsMitNetzklasse$]).then(() => (this.fetching = false));

    // concatAll subscribed sich erst auf das nächste innere Observable, sobald das vorherige completed wurde
    // D.h. es wird immer gewartet bis das vorherige Request zurückgekommen ist. Dadurch wird eine Serialisierung bewirkt
    // und schnell hintereinander abgefeuerte Änderungen können sich nicht mehr durch Race Conditions in die Quere kommen
    // (https://rxjs.dev/api/operators/concatAll)
    this.toggleRequests$.pipe(concatAll()).subscribe(kanteIdsMitNetzklasse => {
      this.kanteIdsMitNetzklasse = kanteIdsMitNetzklasse;
      this.changeDetectorRef.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.toggleRequests$.unsubscribe();
  }

  onAbort(): void {
    this.manuellerImportService.deleteImportSession().then(() => {
      this.router.navigate(['../' + this.importRoutingService.getStartStepRoute()], {
        relativeTo: this.route,
        queryParamsHandling: 'merge',
      });
    });
  }

  onToggleNetzklasse(id: number): void {
    if (this.kanteIdsMitNetzklasse) {
      this.toggleRequests$.next(this.manuellerImportService.toggleNetzklassenzugehoerigkeit(id));
    }
  }

  onFeaturesWithUnchangedNetzklasseVisible($event: boolean): void {
    this.featuresWithUnchangedNetzklasseVisible = $event;
    this.changeDetectorRef.detectChanges();
  }
}
