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
import { map } from 'rxjs/operators';
import { PointGeojson } from 'src/app/shared/models/geojson-geometrie';
import { KnotenDetailView } from 'src/app/shared/models/knoten-detail-view';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';

@Component({
  selector: 'rad-knoten-detail-view',
  templateUrl: './knoten-detail-view.component.html',
  styleUrls: ['./knoten-detail-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class KnotenDetailViewComponent {
  public attribute = new Map<string, { [key: string]: string }>();

  public knotenId = 0;
  public leereAttributeVisible = false;
  public geometrie: PointGeojson = { coordinates: [], type: 'Point' };

  constructor(
    private viewerRoutingService: ViewerRoutingService,
    activatedRoute: ActivatedRoute,
    changeDetector: ChangeDetectorRef,
    olMapService: OlMapService
  ) {
    activatedRoute.data.pipe(map(data => data.knoten)).subscribe((knotenDetails: KnotenDetailView) => {
      this.knotenId = knotenDetails.id;
      this.geometrie = knotenDetails.geometrie;
      this.attribute = new Map<string, { [key: string]: string }>();
      this.attribute.set('', knotenDetails.attribute); // Ohne Gruppierung oder Ueberschrift anzeigen
      olMapService.scrollIntoViewByCoordinate(knotenDetails.geometrie.coordinates);
      changeDetector.markForCheck();
    });
  }

  public onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  public onToggleLeereAttribute(): void {
    this.leereAttributeVisible = !this.leereAttributeVisible;
  }
}
