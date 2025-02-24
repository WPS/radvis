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
import { GeoJSONFeatureCollection } from 'ol/format/GeoJSON';
import { AttributeImportService } from 'src/app/import/attribute/services/attribute-import.service';
import { AttributeRoutingService } from 'src/app/import/attribute/services/attribute-routing.service';
import { ImportSessionView } from 'src/app/import/models/import-session-view';
import { NetzausschnittService } from 'src/app/shared/services/netzausschnitt.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-import-attribute-abbildung-bearbeiten',
  templateUrl: './import-attribute-abbildung-bearbeiten.component.html',
  styleUrls: ['./import-attribute-abbildung-bearbeiten.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ImportAttributeAbbildungBearbeitenComponent {
  private static readonly STEP = 4;

  public session: ImportSessionView | null = null;
  public fetching = true;
  public radvisFeatures$: Promise<GeoJSONFeatureCollection> | undefined;
  public featureMappings$: Promise<GeoJSONFeatureCollection> | undefined;

  constructor(
    private radVisNetzFeatureService: NetzausschnittService,
    private attributeImportService: AttributeImportService,
    private attributeRoutingService: AttributeRoutingService,
    changeDetectorRef: ChangeDetectorRef
  ) {
    this.attributeImportService.getImportSession().subscribe(session => {
      invariant(session);
      this.session = session;
      this.radvisFeatures$ = this.radVisNetzFeatureService
        .getKantenFuerZustaendigkeitsbereich(session.organisationsID)
        .then(features => {
          this.featureMappings$ = this.attributeImportService.getFeatureMappings();
          changeDetectorRef.markForCheck();
          return features;
        });
    });
  }

  onAbort(): void {
    this.attributeImportService.deleteImportSession().subscribe(() => {
      this.attributeRoutingService.navigateToFirst();
    });
  }

  onNext(): void {
    if (this.session?.schritt === 4) {
      this.attributeImportService.bearbeitungAbschliessen().subscribe(() => {
        this.routeToNext();
      });
    } else {
      this.routeToNext();
    }
  }

  onPrevious(): void {
    this.attributeRoutingService.navigateToPrevious(ImportAttributeAbbildungBearbeitenComponent.STEP);
  }

  private routeToNext(): void {
    this.attributeRoutingService.navigateToNext(ImportAttributeAbbildungBearbeitenComponent.STEP);
  }

  onLoaded(): void {
    this.fetching = false;
  }
}
