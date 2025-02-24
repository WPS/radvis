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
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Richtung } from 'src/app/editor/kanten/models/richtung';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { KanteDetailView } from 'src/app/shared/models/kante-detail-view';
import { NetzDetailSelektion } from 'src/app/shared/models/netzdetail-selektion';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { NetzDetailFeatureTableLink } from 'src/app/viewer/viewer-shared/models/netzdetail-feature-table-link';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';

@Component({
  selector: 'rad-kanten-detail-view',
  templateUrl: './kanten-detail-view.component.html',
  styleUrls: ['./kanten-detail-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KantenDetailViewComponent {
  public attributeGruppiert: Map<string, { [p: string]: string | NetzDetailFeatureTableLink }> = new Map();

  public leereAttributeVisible = false;
  public selektion: NetzDetailSelektion | null = null;
  mitVerlauf$: Observable<boolean>;

  public isTrennstreifenVorhanden = false;
  public trennstreifenEinseitig?: boolean;
  public trennstreifenRichtungRechts?: Richtung;
  public trennstreifenRichtungLinks?: Richtung;

  constructor(
    private viewerRoutingService: ViewerRoutingService,
    activatedRoute: ActivatedRoute,
    changeDetector: ChangeDetectorRef,
    mapQueryParamsService: MapQueryParamsService,
    olMapService: OlMapService,
    private featureTogglzService: FeatureTogglzService
  ) {
    activatedRoute.data.pipe(map(data => data.kante)).subscribe((kantenDetails: KanteDetailView) => {
      this.attributeGruppiert = this.getGruppierteAttribute(
        kantenDetails.attributeAufGanzerLaenge,
        kantenDetails.attributeAnPosition,
        kantenDetails.trennstreifenAttribute
      );

      this.isTrennstreifenVorhanden = !!kantenDetails.trennstreifenAttribute;
      this.trennstreifenEinseitig = kantenDetails.trennstreifenEinseitig;
      this.trennstreifenRichtungRechts = kantenDetails.trennstreifenRichtungRechts;
      this.trennstreifenRichtungLinks = kantenDetails.trennstreifenRichtungLinks;

      this.selektion = {
        hauptGeometry: kantenDetails.geometrie,
        id: kantenDetails.id,
        seite: kantenDetails.seite || null,
        verlaufLinks: kantenDetails.verlaufLinks,
        verlaufRechts: kantenDetails.verlaufRechts,
      };
      olMapService.scrollIntoViewByCoordinate(kantenDetails.geometrie.coordinates[0]);
      changeDetector.markForCheck();
    });

    this.mitVerlauf$ = mapQueryParamsService.mitVerlauf$;
  }

  public onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  public onToggleLeereAttribute(): void {
    this.leereAttributeVisible = !this.leereAttributeVisible;
  }

  public get isTrennstreifenFeatureEnabled(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_SICHERHEITSTRENNSTREIFEN);
  }

  private getGruppierteAttribute(
    aufGanzerLaenge: { [key: string]: string },
    anPosition: { [key: string]: string },
    trennstreifen?: { [key: string]: string }
  ): Map<string, { [key: string]: string | NetzDetailFeatureTableLink }> {
    const result = new Map<string, { [key: string]: string | NetzDetailFeatureTableLink }>();
    if (aufGanzerLaenge) {
      result.set('Auf ganzer Länge', aufGanzerLaenge);
    }
    if (anPosition && Object.entries(anPosition).length) {
      result.set('An Position', anPosition);
    }
    if (this.isTrennstreifenFeatureEnabled && trennstreifen && Object.entries(trennstreifen).length) {
      result.set('Sicherheitstrennstreifen', trennstreifen);
    }
    return result;
  }
}
