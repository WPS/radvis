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
import { Observable, Subject } from 'rxjs';
import { concatAll } from 'rxjs/operators';
import { NetzklassenImportSessionView } from 'src/app/import/netzklassen/models/netzklassen-import-session-view';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';

@Component({
  selector: 'rad-import-abbildung-bearbeiten',
  templateUrl: './import-netzklasse-abbildung-bearbeiten.component.html',
  styleUrls: ['./import-netzklasse-abbildung-bearbeiten.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportNetzklasseAbbildungBearbeitenComponent implements OnDestroy {
  private static readonly STEP = 4;
  public fetching = true;

  public kantenFuerZustaendigkeitsbereich$: Promise<GeoJSONFeatureCollection>;
  public kanteIdsMitNetzklasse: number[] | null = null;

  public featuresWithUnchangedNetzklasseVisible = false;
  netz$: Promise<Netzklasse>;
  // Serialisierung der Toggle Requests
  private toggleRequests$: Subject<Observable<number[]>> = new Subject();
  private session: NetzklassenImportSessionView | null = null;

  constructor(
    private netzklassenImportService: NetzklassenImportService,
    private netzklassenRoutingService: NetzklassenRoutingService,
    private radVisNetzFeatureService: NetzausschnittService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    const session$ = this.netzklassenImportService
      .getImportSession()
      .toPromise()
      .then(session => session as NetzklassenImportSessionView);

    session$.then(session => (this.session = session));

    this.netz$ = session$.then(session => session.netzklasse as Netzklasse);

    this.kantenFuerZustaendigkeitsbereich$ = session$.then(session =>
      this.radVisNetzFeatureService.getKantenFuerZustaendigkeitsbereich(session.organisationsID, session.netzklasse)
    );

    const kanteIdsMitNetzklasse$ = this.netzklassenImportService
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
    this.netzklassenImportService.deleteImportSession().subscribe(() => {
      this.netzklassenRoutingService.navigateToFirst();
    });
  }

  onStart(): void {
    if (this.session?.schritt === 4) {
      this.netzklassenImportService.bearbeitungAbschliessen().subscribe(() => {
        this.navigateToNext();
      });
    } else {
      this.navigateToNext();
    }
  }

  onPrevious(): void {
    this.netzklassenRoutingService.navigateToPrevious(ImportNetzklasseAbbildungBearbeitenComponent.STEP);
  }

  private navigateToNext(): void {
    this.netzklassenRoutingService.navigateToNext(ImportNetzklasseAbbildungBearbeitenComponent.STEP);
  }

  onToggleNetzklasse(id: number): void {
    if (this.kanteIdsMitNetzklasse) {
      this.toggleRequests$.next(this.netzklassenImportService.toggleNetzklassenzugehoerigkeit(id));
    }
  }

  onFeaturesWithUnchangedNetzklasseVisible($event: boolean): void {
    this.featuresWithUnchangedNetzklasseVisible = $event;
    this.changeDetectorRef.detectChanges();
  }
}
