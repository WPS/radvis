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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { ImportSessionView } from 'src/app/editor/manueller-import/models/import-session-view';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';

@Component({
  selector: 'rad-import-attribute-abbildung-bearbeiten',
  templateUrl: './import-attribute-abbildung-bearbeiten.component.html',
  styleUrls: ['./import-attribute-abbildung-bearbeiten.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportAttributeAbbildungBearbeitenComponent {
  public previousLink: string;
  public session: ImportSessionView | null = null;
  public fetching = true;
  public radvisFeatures$: Promise<GeoJSONFeatureCollection> | undefined;
  public featureMappings$: Promise<GeoJSONFeatureCollection> | undefined;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private radVisNetzFeatureService: NetzausschnittService,
    private manuellerImportRoutingService: ManuellerImportRoutingService,
    private manuellerImportService: ManuellerImportService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.previousLink =
      '../' +
      manuellerImportRoutingService.getRouteForStep(route.snapshot.data.step - 1, ImportTyp.ATTRIBUTE_UEBERNEHMEN);

    this.manuellerImportService
      .getImportSession()
      .toPromise()
      .then(session => {
        this.session = session;
        this.radvisFeatures$ = this.radVisNetzFeatureService
          .getKantenFuerZustaendigkeitsbereich(session.organisationsID)
          .then(features => {
            this.featureMappings$ = this.manuellerImportService.getFeatureMappings();
            changeDetectorRef.markForCheck();
            return features;
          });
      });
  }

  onAbort(): void {
    this.manuellerImportService.deleteImportSession().then(() => {
      this.router.navigate(['../' + this.manuellerImportRoutingService.getStartStepRoute()], {
        relativeTo: this.route,
        queryParamsHandling: 'merge',
      });
    });
  }

  onStart(): void {
    this.router.navigate(
      [
        '../' +
          this.manuellerImportRoutingService.getRouteForStep(
            this.route.snapshot.data.step + 1,
            ImportTyp.ATTRIBUTE_UEBERNEHMEN
          ),
      ],
      {
        relativeTo: this.route,
        queryParamsHandling: 'merge',
      }
    );
  }

  onLoaded(): void {
    this.fetching = false;
  }
}
