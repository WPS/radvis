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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { asString } from 'ol/color';
import { Point } from 'ol/geom';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { NetzService } from 'src/app/editor/editor-shared/services/netz.service';
import { CreateKanteZwischenKnotenCommand } from 'src/app/editor/kanten/models/create-kante-zwischen-knoten-command';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NotifyGeometryChangedService } from 'src/app/editor/kanten/services/notify-geometry-changed.service';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { MapStyles } from 'src/app/shared/models/layers/map-styles';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { CreateAnpassungswunschRouteProvider } from 'src/app/shared/services/create-anpassungswunsch-route.provider';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-kanten-creator',
  templateUrl: './kanten-creator.component.html',
  styleUrls: ['./kanten-creator.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KantenCreatorComponent implements OnInit {
  isFetching = false;
  vonKnotenId: string | undefined;
  bisKnotenId: string | undefined;
  bisKnotenGeom: Point | undefined;
  showValidationMessage = false;
  validInputColor = asString(MapStyles.VALID_INPUT_COLOR);
  previousNetzklassenfilter: Netzklassefilter[];
  createAnpassungswunschRoute: string;

  constructor(
    private editorRoutingService: EditorRoutingService,
    private netzService: NetzService,
    private notifyUserService: NotifyUserService,
    private notifyGeometryChangedService: NotifyGeometryChangedService,
    private changeDetector: ChangeDetectorRef,
    private kanteSelektionService: KantenSelektionService,
    private mapQueryParamsService: MapQueryParamsService,
    createAnpassungswunschRouteProvider: CreateAnpassungswunschRouteProvider
  ) {
    this.previousNetzklassenfilter = mapQueryParamsService.mapQueryParamsSnapshot.netzklassen;
    this.createAnpassungswunschRoute = createAnpassungswunschRouteProvider.getCreatorRoute();
  }

  ngOnInit(): void {
    if (this.previousNetzklassenfilter.length !== Netzklassefilter.getAll().length) {
      this.mapQueryParamsService.update({
        netzklassen: Netzklassefilter.getAll(),
      });
    }
  }

  get isDirty(): boolean {
    return this.vonKnotenId !== undefined || this.bisKnotenId !== undefined;
  }

  onClose(): void {
    this.editorRoutingService.toEditor();
  }

  onSave(): void {
    if (!this.vonKnotenId || (!this.bisKnotenId && !this.bisKnotenGeom)) {
      this.showValidationMessage = true;
    } else {
      this.isFetching = true;
      let command: CreateKanteZwischenKnotenCommand;
      if (this.bisKnotenId) {
        command = { vonKnotenId: Number(this.vonKnotenId), bisKnotenId: Number(this.bisKnotenId) };
      } else {
        invariant(this.bisKnotenGeom);
        command = {
          vonKnotenId: Number(this.vonKnotenId),
          bisKnotenCoor: { type: 'Point', coordinates: this.bisKnotenGeom.getCoordinates() },
        };
      }
      this.netzService
        .createKanteZwischenKnoten(command)
        .then(newKante => {
          this.notifyUserService.inform('Neue Kante wurde erfolgreich gespeichert.');
          this.notifyGeometryChangedService.notify();
          this.kanteSelektionService.select(newKante.id, false);
          this.editorRoutingService.toKantenEditor();
        })
        .finally(() => {
          this.isFetching = false;
          this.changeDetector.markForCheck();
        });
    }
  }

  onSelectVonKnoten(vonKnotenId: string | undefined): void {
    this.vonKnotenId = vonKnotenId;
    this.showValidationMessage = false;
  }

  onSelectBisKnoten(bisKnotenIdOrGeom: string | Point | undefined): void {
    if (bisKnotenIdOrGeom instanceof Point) {
      this.bisKnotenGeom = bisKnotenIdOrGeom;
      this.bisKnotenId = undefined;
    } else {
      this.bisKnotenId = bisKnotenIdOrGeom;
      this.bisKnotenGeom = undefined;
    }
    this.showValidationMessage = false;
  }
}
